package lox;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import static lox.TokenType.*;

class Lox {
  @psv main(@str[] args) @io_throw {
    if (args.length > 1) {
      @out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  @sv runFile(@str path) @io_throw {
    var bytes = Files.readAllBytes(Paths.get(path));
    run(new @str(bytes, Charset.defaultCharset()));
    if (hadError)
      System.exit(65);
  }

  @sv runPrompt() @io_throw {
    var input = new InputStreamReader(System.in);
    var reader = new BufferedReader(input);

    for (;;) {
      @out.print("> ");
      var line = reader.readLine();
      if (line == null || line.equals(""+(char)0x04))
        break;
      run(line);
      hadError = false;
    }
  }

  @sv run(@str source) {
    var scanner = new Scanner(source);
    var tokens = scanner.scanTokens();
    for (var token : tokens) {
      @out.println(token);
    }
  }

  @sv error(int line, @str message) {
    report(line, "", message);
  }

  @sv report(int line, @str where, @str message) {
    @err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static @bool hadError = false;
}

enum TokenType {
  @translate_one("(){},.-+;/*");

  @translate("!", "!=", "=", "==", ">", ">=", "<", "<=");

  IDENTIFIER, STRING, NUMBER,

  @translate("and", "class", "else", "false", "for",
      "fun", "if", "nil", "or", "print", "return",
      "super", "this", "true", "var", "while");

    EOF
}

class Token {

  @gen_class_member(Token, 
  "TokenType type, String lexeme, Object literal, int line");

  public @str to@str() {
    return type + " " + lexeme + " " + literal;
  }
}

class Scanner {
  @str source;
  List<Token> tokens = new ArrayList<>();

  Scanner(@str source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  int start = 0;
  int current = 0;
  int line = 1;

  @bool isAtEnd() {
    return current >= source.length();
  }

  void scanToken() {
    var c = advance();
    switch (c) {
      @case(addToken, "(){},.-+;*");
      case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL: GREATER); break;
      case '/':
        if (match('/')) {
          while(peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
        break;
      case ' ': break;
      case '\r': break;
      case '\t': break;
      case '\n': line++; break;
      case '"': string(); break;
      default:
        if(isDigit(c)){
          number();
        }else if(isAlpha(c)){
          identifier();
        }else{
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }
  void identifier(){
    while(isAlphaNumeric(peek())) advance();
    var text=source.substring(start, current);
    var type=keywords.get(text);
    if(type==null) type=IDENTIFIER;
    addToken(IDENTIFIER);
  }
  @bool isAlpha(char c){
    return (c>='a'&& c<='z')||
      (c>='A'&& c<='Z')||
      c=='_';
  }
  @bool isAlphaNumeric(char c){
    return isAlpha(c)||isDigit(c);
  }
  static Map<@str, TokenType> keywords;
  static{
    keywords=new HashMap<>();
    @INSERT_CAP(keywords, "and", "class", "else", "false", "for",
      "fun", "if", "nil", "or", "print", "return",
      "super", "this", "true", "var", "while");
  }
  @bool isDigit(char c){
    return c>='0' && c<='9';
  }
  void number(){
    while(isDigit(peek())) advance();
    if(peek()=='.' && isDigit(peekNext())){
      advance();
      while(isDigit(peek())) advance();
    }
    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }
  char peekNext(){
    if(current+1>=source.length()) return '\0';
    return source.charAt(current+1);
  }
  void string(){
    while(peek() != '"' && !isAtEnd()){
      if(peek() == '\n') line++;
      advance();
    }
    if(isAtEnd()){
      Lox.error(line, "Unterminated string.");
      return;
    }
    advance();
    @str value=source.substring(start+1,current-1);
    addToken(STRING, value);
  }

  char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  @bool match(char expected) {
    if(isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;
    current++;
    return true;
  }

  char advance() {
    return source.charAt(current++);
  }

  void addToken(TokenType type) {
    addToken(type, null);
  }

  void addToken(TokenType type, Object literal) {
    var text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}

@GEN_AST(Expr, "Binary : Expr left, Token operator, Expr right",
          "Grouping : Expr expression",
          "Literal : Object value",
          "Unary : Token operator, Expr right");

