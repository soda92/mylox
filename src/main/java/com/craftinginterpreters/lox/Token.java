package com.craftinginterpreters.lox;

public class Token {
    final TokenType type;
    final String lexeme;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    final Object literal;
    final int line;

    @Override
    public String toString(){
        return type + " "+lexeme + " "+literal;
    }
}
