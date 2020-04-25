grammar Arc;

//tokens { INDENT, DEDENT }
//
//@lexer::header {
//  import com.yuvalshavit.antlr4.DenterHelper;
//}
//@lexer::members {
//  private final DenterHelper denter = new DenterHelper(NEWLINE, SimpleParser.INDENT, SimpleParser.DEDENT) {
//    @Override
//    public Token pullToken() {
//      return SimpleLexer.super.nextToken();
//    }
//  };
//
//  @Override
//  public Token nextToken() {
//    return denter.nextToken();
//  }
//
//  public Token rawNextToken() {
//    return super.nextToken();
//  }
//}

program : statement* EOF;

block: OBRACE statement* CBRACE;
//block: INDENT statement (NEWLINE statement)* DEDENT;

statement :
    (assignment
    | memberAccess
    | ifBlock
    | foreachBlock
    | whileBlock
    | methodDef
    );

assignment : ID '=' generator;

// anything that produces values
generator : literal | tuple | memberAccess | array;


array : '[' generator ? (',' generator)* ']' ;
arrayAccess : ID '[' expr ']';

member : ID | methodInvocation | arrayAccess ;
memberAccess : member | memberAccess '.' member | namespaceAccess ;
namespaceAccess : ID '::' member;
methodInvocation : ID '(' (paramList? | lambda) ')' ;
lambda : ID '->' expr;
// control
foreachBlock : 'foreach' ID 'in' generator block ;
ifBlock : 'if' expr block elseIfBlock* elseBlock?;
elseIfBlock : 'else if' expr block;
elseBlock   : 'else' block ;
whileBlock  : WHILE expr block;
methodDef : ID ':=' varList? block;
varList : ID (',' ID)*;

expr :
    generator
    | op=(NOT|MINUS) expr
    | expr op=(MUL|DIV) expr
    | expr op=(PLUS|MINUS) expr
    | expr op=(EQ|NEQ|GT|GTEQ|LT|LTEQ) expr
    | expr op=(AND|XOR|NAND) expr
    | expr OR expr
    ;

paramList : expr (',' expr)* ;

tuple : '(' INTEGER ',' INTEGER ')' ;

literal :
    INTEGER
    | BIT_STRING
    | ENUM
    ;

INTEGER     : [0-9]+;
ID          : [a-z][a-zA-Z0-9_]*;
BIT_STRING  : '"' [0-9]+ '"' ;
ENUM        : [A-Z][a-z]* ;

MUL : '*';
DIV : '/';
MINUS : '-';
PLUS : '+';
EQ : '==';
NEQ : '!=';
GT : '>';
LT : '<';
GTEQ : '>=';
LTEQ : '<=';
XOR : '^' | 'xor';
OR : '|' | 'or';
AND : '&' | 'and';
NAND : '!&' | 'nand';
NOT : '!' | 'not';

OBRACE : '{' ;
CBRACE : '}' ;
WHILE : 'while';
//IF : 'if';
//ELSE : 'else';
THEN : 'then';

SPACE : [ \t\n]+ -> skip ;

NEWLINE : '\r'? '\n';

fragment SEMICOLON : ';';
