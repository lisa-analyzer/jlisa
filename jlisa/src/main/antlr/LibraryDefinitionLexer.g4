lexer grammar LibraryDefinitionLexer;

@ lexer :: header
{package it.unive.jlisa.antlr;}
BOOLEAN
   : 'true'
   | 'false'
   ;

NUMBER
   : '0'
   | NonZeroDigit Digit*
   ;

STRING
   : '"' (~ ["\\\r\n] | EscapeSequence)* '"'
   ;

NONE
   : 'none'
   ;

LIBRARY
   : 'library'
   ;

CLASS
   : 'class'
   ;

METHOD
   : 'method'
   ;

FIELD
   : 'field'
   ;

EXTENDS
   : 'extends'
   ;

ROOT
   : 'root'
   ;

INSTANCE
   : 'instance'
   ;

PARAM
   : 'param'
   ;

LOCATION
   : 'location'
   ;

TYPE
   : 'type'
   ;

LIBTYPE
   : 'libtype'
   ;

DEFAULT
   : 'default'
   ;

SEALED
   : 'sealed'
   ;

COLON
   : ':'
   ;

DOUBLE_COLON
   : '::'
   ;

DOT
   : '.'
   ;

STAR
   : '*'
   ;

POWER
   : '**'
   ;

AMP
   : '&'
   ;

WHITESPACE
   : [ \t\r\n\u000C]+ -> channel (HIDDEN)
   ;

LINE_COMMENT
   : '#' ~ [\r\n]* -> channel (HIDDEN)
   ;

IDENTIFIER
   : Letter LetterOrDigit* ('.' Letter LetterOrDigit*)*
   ;

fragment Digit
   : [0-9]
   ;

fragment NonZeroDigit
   : [1-9]
   ;

fragment Digits
   : [0-9]+
   ;

fragment Letter
   : [a-zA-Z$_]
   ;

fragment LetterOrDigit
   : Letter
   | Digit
   ;

fragment EscapeSequence
   : '\\' [btnfr"'\\]
   | '\\' ([0-3]? [0-7])? [0-7]
   ;

