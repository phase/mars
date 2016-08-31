grammar Lang;

program: (externalDeclaration | NL)* EOF;

externalDeclaration
    : functionDeclaration
    | variableDeclaration
    ;

functionDeclaration: ID '(' argumentList ')' typeAnnotation? '{' (statement | NL)* '}';
variableDeclaration: variableSignature '=' expression;

functionCall: ID '(' expressionList? ')';

statement
    : variableDeclaration
    | 'if' '(' expression ')' expression
    | 'if' '(' expression ')' expression 'else' expression
    | 'for' '(' ID 'in' expression ')' expression
    | 'while' '(' expression ')' expression
    | 'continue'
    | 'break'
    | 'return' expression
    ;

expressionList
    : expression
    | expression ',' expressionList
    ;

expression
    : expression '^'<assoc=right> expression
    | ('-'|'+') expression
    | expression ':' expression
    | expression ('*'|'/') expression
    | expression ('+'|'-') expression
    | expression ('>'|'>='|'<'|'<='|'=='|'!=') expression
    | '!' expression
    | expression ('&'|'&&') expression
    | expression ('|'|'||') expression
    | '~' expression
    | expression '~' expression
    | expression ('<-'|'<<-'|'='|'->'|'->>'|':=') expression
    | expression '%' ID '%' expression // call function as operator
    | '(' expression ')'
    | functionCall
    | ID
    | STRING
    | HEX
    | INT
    | FLOAT
    | 'true'
    | 'false'
    ;

argumentList: argument (',' argument)*;

argument
    : variableSignature
    | variableSignature '=' expression
    ;

typeAnnotation: ':' ID;
variableSignature: ID typeAnnotation?;

HEX: '0' ('x'|'X') HEXDIGIT+ [Ll]?;

INT: DIGIT+ [Ll]?;

fragment HEXDIGIT: ('0'..'9'|'a'..'f'|'A'..'F');

FLOAT
    : DIGIT+ '.' DIGIT* EXP? [Ll]?
    | DIGIT+ EXP? [Ll]?
    | '.' DIGIT+ EXP? [Ll]?
    ;

fragment DIGIT: '0'..'9';
fragment EXP: ('E' | 'e') ('+' | '-')? INT;

STRING
    : '"' ( ESC | ~[\\"] )*? '"'
    | '\'' ( ESC | ~[\\'] )*? '\''
    | '`' ( ESC | ~[\\'] )*? '`'
    ;

fragment ESC
    : '\\' [abtnfrv"'\\]
    | UNICODE_ESCAPE
    | HEX_ESCAPE
    | OCTAL_ESCAPE
    ;

fragment UNICODE_ESCAPE
    : '\\' 'u' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT
    | '\\' 'u' '{' HEXDIGIT HEXDIGIT HEXDIGIT HEXDIGIT '}'
    ;

fragment OCTAL_ESCAPE
    : '\\' [0-3] [0-7] [0-7]
    | '\\' [0-7] [0-7]
    | '\\' [0-7]
    ;

fragment HEX_ESCAPE
    : '\\' HEXDIGIT HEXDIGIT?
    ;

ID: LETTER (LETTER|DIGIT|'_')*;

fragment LETTER: [a-zA-Z];

COMMENT: '#' .*? '\r'? '\n' -> type(NL);

NL
    : '\r'? '\n'
    | ';'
    ;

WS: [ \t\u000C]+ -> skip;
