grammar cfg;

node : CLASS | NAME;

definition : NAME SEMI OBR NL statement+ CBR ;

statement :
    node (edge node+)+ NL
    ;

NAME : [a-z][a-z0-9]* ;
CLASS : [A-Z]+ ;

edge : INST_EDGE | ARG_EDGE | SUCC_EDGE | ASSIGN_EDGE | DECL_EDGE | TRUE_EDGE | FALSE_EDGE | MEMB_EDGE;

MEMB_EDGE : '<-' ;
INST_EDGE : '<' ;
ARG_EDGE : '-' ;
SUCC_EDGE : '>' ;
ASSIGN_EDGE : '~' ;
DECL_EDGE : '+' ;
TRUE_EDGE : 'y' ;
FALSE_EDGE : 'n' ;

SEMI : ';' ;

OBR : '{' ;
CBR : '}' ;

NL : '\r'? '\n';
SPACE : [ \t\n]+ -> skip ;
