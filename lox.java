@namespace lox
@import io,nio.charset,nio.file,util

class lox {
  @io_throw @main(@str[] args) {
    expr := @Binary(
      @Binary(
        @Literal(1),
        @T(PLUS, "+", null, 1),
        @Literal(2)
      ),
      @T(STAR, "*", null, 1),
      @Binary(
        @Literal(4),
        @T(MINUS, "-", null, 1),
        @Literal(3)
      )
    );
    //println!(new ast_printer_rpn().print(expr));
    if (args.length > 1) {
      println!("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length is 1) {
      run_file(args[0]);
    } else {
      run_prompt();
    }
  }

  @io_throw @sv run_file(@str path) {
    bytes := Files.readAllBytes(Paths.get(path));
    run(new @str(bytes, Charset.defaultCharset()));
    if(has_err) System.exit(65);
    if(has_rt_err) System.exit(70);
  }

  static @bool ctrl_d_in_str(@str s){
    return s.contains(String.valueOf((char)0x04));
  }

  @io_throw @sv run_prompt() {
    input := new InputStreamReader(System.in);
    reader := new BufferedReader(input);

    for (;;) {
      print!("> ");
      line := reader.readLine();
      if (line is null or ctrl_d_in_str(line))
        break;
      run(line);
      has_err = false;
    }
  }

  @sv run(@str source) {
    s := new scanner(source);
    ts := s.scan_tokens();
    // for(var t:ts)
    //   println!(t);
    p := new parser(ts);
    if(has_err) return;
    stmts := p.parse();
    if(has_err) return;
    print!(new ast_printer().print(stmts));
    I.interpret(stmts);
  }
  static Interpreter I=new Interpreter();

  @sv error(int line, @str message) {
    report(line, "", message);
  }

  @sv error(token t, @str m){
    if(t.type is EOF) report(t.line, " at end", m);
    else report(t.line, " at '" + t.lexeme +"'" , m);
  }

  @sv runtime_err(runtime_err err){
    eprintln!(err.message + "\n[line "+err.t.line + "]");
    has_rt_err = true;
  }

  static @bool has_rt_err=false;

  @sv report(int line, @str where, @str message) {
    eprintln!(
        "[line " + line + "] Error" + where + ": " + message);
    has_err = true;
  }

  static @bool has_err = false;
}

@static enum token_type {
  @INSERT_TR_1("(){},.-+;/*");
  @INSERT_TR("!", "!=", "=", "==", ">", ">=", "<", "<=");
  @INSERT_TR("and", "class", "else", "false", "for",
      "fun", "if", "nil", "or", "print", "return",
      "super", "this", "true", "var", "while");
  IDENTIFIER, STRING, NUMBER, EOF
}

class token {
  @gen_class_member(token,
  "token_type type, @str lexeme, Object literal, int line");

  public @str to@str() {
    return type + " " + lexeme + " " + literal;
  }
}

class scanner {
  @str source;
  List<token> tokens = new ArrayList<>();

  scanner(@str source) {
    this.source = source;
  }

  List<token> scan_tokens() {
    while (!is_end()) {
      start = current;
      scan_token();
    }

    tokens.add(@T(EOF, "", null, line));
    return tokens;
  }

  int start = 0;
  int current = 0;
  int line = 1;

  @bool is_end() {
    return current >= source.length();
  }

  void scan_token() {
    c := advance();
    switch (c) {
      @case(add_token, "(){},.-+;*");
      case '!': add_token(match('=') ? BANG_EQUAL : BANG); break;
      case '<': add_token(match('=') ? LESS_EQUAL : LESS); break;
      case '>': add_token(match('=') ? GREATER_EQUAL: GREATER); break;
      case '=': add_token(match('=') ? EQUAL_EQUAL : EQUAL); break;
      case '/':
        if (match('/'))
          while(peek() != '\n' and !is_end()) advance();
        else
          add_token(SLASH);
        break;
      case ' ': break;
      case '\r': break;
      case '\t': break;
      case '\n': line++; break;
      case '"': string(); break;
      default:
        if(is_digit(c))
          number();
        else if(is_alpha(c))
          identifier();
        else
          lox.error(line, "Unexpected character.");
        break;
    }
  }
  void identifier(){
    while(is_alnum(peek())) advance();
    text := source.substring(start, current);
    type := keywords.get(text);
    if(type is null) type = IDENTIFIER;
    add_token(type);
  }

  @bool is_alpha(char c){
    return (c>='a' and c<='z') or (c>='A' and c<='Z') or c is '_';
  }

  @bool is_alnum(char c){
    return is_alpha(c) or is_digit(c);
  }

  static Map<@str, token_type> keywords;
  static{
    keywords=new HashMap<>();
    @insert_capval(keywords, "and", "class", "else", "false", "for",
      "fun", "if", "nil", "or", "print", "return",
      "super", "this", "true", "var", "while");
  }

  @bool is_digit(char c){
    return c>='0' and c<='9';
  }

  void number(){
    while(is_digit(peek())) advance();
    if(peek() is '.' and is_digit(peek_next())){
      advance();
      while(is_digit(peek())) advance();
    }
    add_token(NUMBER, to_double(source.substring(start, current)));
  }

  char peek_next(){
    if(current+1>=source.length()) return '\0';
    return source.charAt(current+1);
  }

  void string(){
    while(peek() != '"' and !is_end()){
      if(peek() is '\n') line++;
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

  void add_token(token_type type) {
    add_token(type, null);
  }

  void add_token(token_type type, Object literal) {
    text := source.substring(start, current);
    tokens.add(@T(type, text, literal, line));
  }
}

@gen_ast(Expr,
"Binary : Expr left, token operator, Expr right",
"Grouping : Expr expression",
"Literal : Object value",
"Unary : token operator, Expr right",
"Variable : token name");

@gen_ast(Stmt,
"Expression : Expr expr",
"Print : Expr expr",
"Var : token name, Expr val");

class ast_printer implements Expr.Visitor<@str>, Stmt.Visitor<@str> {
  @str print(Expr expr){
    return expr.accept(this);
  }

  @str print(List<Stmt> stmts){
    sb:=new StringBuilder();
    for(var stmt:stmts){
      val := stmt.accept(this);
      if(val is null) continue;
      sb.append(val);
      sb.append('\n');
    }
    return sb.to@str();
  }

  @impl visitExpressionStmt {
    return null;
  }

  @impl visitPrintStmt {
    return stmt.expr.accept(this);
  }

  @impl visitVarStmt {
    return "(define "+ stmt.name.lexeme + " " + stmt.val.accept(this) + ")";
  }

  @impl visitVariableExpr {
    return expr.name.lexeme;
  }

  @impl visitBinaryExpr {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @impl visitGroupingExpr {
    return expr.expression.accept(this);
  }

  @impl visitLiteralExpr {
    if (expr.value is null) return "nil";
    r := expr.value.to@str();
    r=tool.trim(r);
    return r;
  }

  @impl visitUnaryExpr {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @str parenthesize(@str name, Expr... exprs){
    builder := new StringBuilder();
    builder.append("(").append(name);
    for(var expr:exprs){
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.to@str();
  }
}

class ast_printer_rpn implements Expr.Visitor<@str> {
  @str print(Expr expr){
    return expr.accept(this);
  }

  @impl visitBinaryExpr {
    return to_str(expr.operator.lexeme, expr.left, expr.right);
  }

  @impl visitGroupingExpr {
    return to_str("", expr.expression);
  }

  @impl visitLiteralExpr {
    if (expr.value is null) return "nil";
    return expr.value.to@str();
  }

  @impl visitUnaryExpr {
    return to_str(expr.operator.lexeme, expr.right);
  }

  @impl visitVariableExpr {
    return null;
  }

  @str to_str(@str name, Expr... exprs){
    builder := new StringBuilder();
    for(var expr:exprs){
      builder.append(expr.accept(this));
      builder.append(" ");
    }
    builder.append(name);
    return builder.to@str();
  }
}

class parser {
  List<token> tokens;
  int current = 0;

  parser(List<token> tokens){
    this.tokens = tokens;
  }

  Expr expression() {
    return equality();
  }

  Expr equality() {
    expr := comparision();
    while(match(BANG_EQUAL, EQUAL_EQUAL)){
      operator := previous();
      right := comparision();
      expr = @Binary(expr, operator, right);
    }
    return expr;
  }

  @bool match(token_type... types){
    for(var t:types){
      if(check(t)){
        advance();
        return true;
      }
    }
    return false;
  }

  @bool check(token_type t){
    if(is_end()) return false;
    else return peek().type is t;
  }

  token advance(){
    if(!is_end()) current += 1;
    return previous();
  }

  @bool is_end(){
    return peek().type is EOF;
  }

  token peek(){
    return tokens.get(current);
  }

  token previous(){
    return tokens.get(current-1);
  }

  Expr comparision(){
    expr := term();
    while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
      op := previous();
      right := term();
      expr = @Binary(expr, op, right);
    }
    return expr;
  }

  Expr term(){
    expr := factor();
    while(match(MINUS, PLUS)){
      op := previous();
      right := factor();
      expr = @Binary(expr, op, right);
    }
    return expr;
  }

  Expr factor(){
    expr := unary();
    while(match(SLASH, STAR)){
      op := previous();
      right := unary();
      expr = @Binary(expr, op, right);
    }
    return expr;
  }

  Expr unary(){
    if(match(BANG, BANG_EQUAL)){
      op := previous();
      right := unary();
      return @Unary(op, right);
    }
    return primary();
  }

  Expr primary(){
    if(match(FALSE)) return @Literal(false);
    if(match(TRUE)) return @Literal(true);
    if(match(NIL)) return @Literal(null);

    if(match(NUMBER, STRING)) return @Literal(previous().literal);

    if(match(IDENTIFIER)) return @Variable(previous());

    if(match(LEFT_PAREN)){
      expr := expression();
      consume(RIGHT_PAREN,
          "Exprct ')' after expression.");
      return @Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  token consume(token_type t, @str message){
    if(check(t)) return advance();
    throw error(peek(), message);
  }

  parse_error error(token tok, @str message){
    lox.error(tok, message);
    return new parse_error();
  }
  static class parse_error extends RuntimeException{}


  void sync(){
    advance();
    while(!is_end()){
      if(previous().type is SEMICOLON) return;

      switch(peek().type){
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }
      advance();
    }
  }

  List<Stmt> parse(){
    List<Stmt> statements = new ArrayList<>();
    while(!is_end()) statements.add(declaration());
    return statements;
  }

  Stmt declaration(){
    try{
      if(match(VAR)) return var_decl();
      return statement();
    }catch(parse_error err){
      sync();
      return null;
    }
  }

  Stmt var_decl(){
    name := consume(IDENTIFIER, "Expect variable name.");
    Expr val = null;
    if(match(EQUAL)) val = expression();
    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, val);
  }

  Stmt statement(){
    if(match(PRINT)) return print_stmt();
    return expr_stmt();
  }

  Stmt print_stmt(){
    value := expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  Stmt expr_stmt(){
    expr := expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }
}

class Interpreter implements
  Expr.Visitor<Object>, Stmt.Visitor<Void> {
  @impl visitExpressionStmt {
    eval(stmt.expr);
  }

  @impl visitPrintStmt {
    value := eval(stmt.expr);
    println!(to_str(value));
  }

  Env env = new Env();
  @impl visitVarStmt {
    Object value=null;
    if(stmt.val!=null) value=eval(stmt.val);
    env.define(stmt.name.lexeme, value);
  }

  @impl visitVariableExpr {
    return env.get(expr.name);
  }

  @impl visitLiteralExpr {
    return expr.value;
  }

  @impl visitGroupingExpr {
    return eval(expr.expression);
  }

  Object eval(Expr expr){
    return expr.accept(this);
  }

  @impl visitUnaryExpr {
    right := eval(expr.right);

    switch(expr.operator.type){
      case BANG: return !is_truthy(right);
      case MINUS:
        check_num_oprand(expr.operator, right);
        return -(double)right;
    }
    return null;
  }

  @bool is_truthy(Object obj){
    if(obj is null) return false;
    if(obj is @Bool) return (@bool)obj;
    return true;
  }

  void check_num_oprand(token op, Object oprand){
    if(oprand is Double) return;
    throw new runtime_err(op, "Oprand must be a number.");
  }

  void check_num_oprands(token op, Object l, Object r){
    if(op.type is SLASH and 
    l is Double and 
    r is Double and 
    (double)r is 0) 
    throw new runtime_err(op, "Right oprand must not be zero.");
    if(l is Double and r is Double) return;
    throw new runtime_err(op, "Oprands must be numbers.");
  }

  @impl visitBinaryExpr {
    left := eval(expr.left);
    right := eval(expr.right);

    switch(expr.operator.type){
      case MINUS:
        check_num_oprands(expr.operator, left, right);
        return (double)left - (double)right;

      case SLASH:
        check_num_oprands(expr.operator, left, right);
        return (double)left / (double)right;

      case STAR:
        check_num_oprands(expr.operator, left, right);
        return (double)left * (double)right;

      case PLUS:
        if(left is Double and right is Double)
          return (double)left + (double)right;
        if(left is @str and right is @str)
          return (@str)left + (@str)right;

        @str r = "";
        if(left is @str){
          r += (double)right;
          r = tool.trim(r);
          return (@str)left + r;
        }
        r += (double)left;
        r = tool.trim(r);
        return (@str)right + r;

      case GREATER:
        check_num_oprands(expr.operator, left, right);
        return (double)left > (double)right;

      case GREATER_EQUAL:
        check_num_oprands(expr.operator, left, right);
        return (double)left >= (double)right;

      case LESS:
        check_num_oprands(expr.operator, left, right);
        return (double)left < (double)right;

      case LESS_EQUAL:
        check_num_oprands(expr.operator, left, right);
        return (double)left <= (double)right;

      case BANG_EQUAL:
        return !eq(left, right);

      case EQUAL_EQUAL:
        return eq(left, right);
    }

    return null;
  }

  @bool eq(Object a, Object b){
    if(a is null and b is null) return true;
    if(a is null) return false;
    return a.equals(b);
  }

  void interpret(Expr expr){
    try{
      val := eval(expr);
      println!(to_str(val));
    }catch(runtime_err err){
      lox.runtime_err(err);
    }
  }

  void interpret(List<Stmt> statements){
    try{
      for(var stmt: statements){
        exec(stmt);
      }
    }catch(runtime_err err){
      lox.runtime_err(err);
    }
  }

  void exec(Stmt stmt){
    stmt.accept(this);
  }

  @str to_str(Object o){
    if(o is null) return "nil";
    if(o is Double){
      text:=o.to@str();
      text=tool.trim(text);
      return text;
    }
    return o.to@str();
  }
}

class runtime_err extends RuntimeException{
  @gen_class_member(runtime_err, "token t, @str message");
}

class Env{
  Map<@str, Object> values = new HashMap<>();

  void define(@str name, Object value){
    values.put(name, value);
  }

  Object get(token name){
    if(values.containsKey(name.lexeme)){
      return values.get(name.lexeme);
    }
    throw new runtime_err(name,
        "Undefined variable '"+name.lexeme+"'.");
  }
}

class tool{
  static @str trim(@str text){
    if(text.endsWith(".0")) text=text.substring(0, text.length()-2);
    return text;
  }
}
