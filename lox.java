@namespace lox
@import io,nio.charset,nio.file,util

class lox {
  @psv main(@str[] args) @io_throw {
    if (args.length > 1) {
      @out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      run_file(args[0]);
    } else {
      run_prompt();
    }
  }

  @sv run_file(@str path) @io_throw {
    var bytes = Files.readAllBytes(Paths.get(path));
    run(new @str(bytes, Charset.defaultCharset()));
    if (has_err)
      System.exit(65);
  }

  @sv run_prompt() @io_throw {
    var input = new InputStreamReader(System.in);
    var reader = new BufferedReader(input);

    for (;;) {
      @out.print("> ");
      var line = reader.readLine();
      if (line == null  or  line.equals(""+(char)0x04))
        break;
      run(line);
      has_err = false;
    }
  }

  @sv run(@str source) {
    var scanner = new Scanner(source);
    var tokens = scanner.scan_tokens();
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
    has_err = true;
  }

  static @bool has_err = false;
}

@static enum TokenType {
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

  List<Token> scan_tokens() {
    while (!is_end()) {
      start = current;
      scan_token();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  int start = 0;
  int current = 0;
  int line = 1;

  @bool is_end() {
    return current >= source.length();
  }

  void scan_token() {
    var c = advance();
    switch (c) {
      @case(add_token, "(){},.-+;*");
      case '!': add_token(match('=') ? BANG_EQUAL : BANG); break;
      case '<': add_token(match('=') ? LESS_EQUAL : LESS); break;
      case '>': add_token(match('=') ? GREATER_EQUAL: GREATER); break;
      case '/':
        if (match('/')) {
          while(peek() != '\n'  and !is_end()) advance();
        } else {
          add_token(SLASH);
        }
        break;
      case ' ': break;
      case '\r': break;
      case '\t': break;
      case '\n': line++; break;
      case '"': string(); break;
      default:
        if(is_digit(c)){
          number();
        }else if(is_alpha(c)){
          identifier();
        }else{
          lox.error(line, "Unexpected character.");
        }
        break;
    }
  }
  void identifier(){
    while(is_alnum(peek()))
      advance();
    var text = source.substring(start, current);
    var type = keywords.get(text);
    if(type == null)
      type = IDENTIFIER;
    add_token(IDENTIFIER);
  }
  @bool is_alpha(char c){
    return (c>='a' and c<='z') or 
      (c>='A' and c<='Z') or 
      c=='_';
  }
  @bool is_alnum(char c){
    return is_alpha(c) or is_digit(c);
  }
  static Map<@str, TokenType> keywords;
  static{
    keywords=new HashMap<>();
    @INSERT_CAP(keywords, "and", "class", "else", "false", "for",
      "fun", "if", "nil", "or", "print", "return",
      "super", "this", "true", "var", "while");
  }
  @bool is_digit(char c){
    return c>='0' and c<='9';
  }
  void number(){
    while(is_digit(peek())) advance();
    if(peek()=='.' and is_digit(peek_next())){
      advance();
      while(is_digit(peek())) advance();
    }
    add_token(NUMBER, Double.parseDouble(source.substring(start, current)));
  }
  char peek_next(){
    if(current+1>=source.length()) return '\0';
    return source.charAt(current+1);
  }
  void string(){
    while(peek() != '"' and !is_end()){
      if(peek() == '\n') line++;
      advance();
    }
    if(is_end()){
      lox.error(line, "Unterminated string.");
      return;
    }
    advance();
    @str value=source.substring(start+1,current-1);
    add_token(STRING, value);
  }

  char peek() {
    if (is_end()) return '\0';
    return source.charAt(current);
  }

  @bool match(char expected) {
    if(is_end()) return false;
    if (source.charAt(current) != expected) return false;
    current++;
    return true;
  }

  char advance() {
    return source.charAt(current++);
  }

  void add_token(TokenType type) {
    add_token(type, null);
  }

  void add_token(TokenType type, Object literal) {
    var text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}

@GEN_AST(Expr, "Binary : Expr left, Token operator, Expr right",
          "Grouping : Expr expression",
          "Literal : Object value",
          "Unary : Token operator, Expr right");

