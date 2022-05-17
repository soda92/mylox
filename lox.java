package lox;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import static lox.TokenType.*;

class Lox {
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  static void runFile(String path) throws IOException {
    var bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError)
      System.exit(65);
  }

  static void runPrompt() throws IOException {
    var input = new InputStreamReader(System.in);
    var reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      var line = reader.readLine();
      if (line == null || line.equals(""+(char)0x04))
        break;
      run(line);
      hadError = false;
    }
  }

  static void run(String source) {
    var scanner = new Scanner(source);
    var tokens = scanner.scanTokens();
    for (var token : tokens) {
      System.out.println(token);
    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void report(int line, String where, String message) {
    System.err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static boolean hadError = false;
}

enum TokenType {
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,

  IDENTIFIER, STRING, NUMBER,

  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE

    , EOF
}

class Token {
  TokenType type;
  String lexeme;
  Object literal;
  int line;

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }

}

class Scanner {
  String source;
  List<Token> tokens = new ArrayList<>();

  Scanner(String source) {
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

  boolean isAtEnd() {
    return current >= source.length();
  }

  void scanToken() {
    var c = advance();
    switch (c) {
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '*':
        addToken(STAR);
        break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL: GREATER);
        break;
      case '/':
        if (match('/')) {
          while(peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
        break;
      case ' ':
        break;
      case '\r':
        break;
      case '\t':
        break;
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
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
  boolean isAlpha(char c){
    return (c>='a'&& c<='z')||
      (c>='A'&& c<='Z')||
      c=='_';
  }
  boolean isAlphaNumeric(char c){
    return isAlpha(c)||isDigit(c);
  }
  static Map<String, TokenType> keywords;
  static{
    keywords=new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }
  boolean isDigit(char c){
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
    String value=source.substring(start+1,current-1);
    addToken(STRING, value);
  }

  char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  boolean match(char expected) {
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
