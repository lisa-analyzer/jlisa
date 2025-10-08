parser grammar LibraryDefinitionParser;

@ header
{
    package it.unive.jlisa.antlr;
}

options { tokenVocab = LibraryDefinitionLexer; }
lisatype
   : TYPE type_name = IDENTIFIER DOUBLE_COLON type_field = IDENTIFIER
   ;

libtype
   : LIBTYPE type_name = IDENTIFIER STAR?
   ;

type
   : lisatype
   | libtype
   ;

value
   : NUMBER
   | BOOLEAN
   | STRING
   | NONE
   ;

param
   : PARAM (STAR? | POWER? | AMP?) name = IDENTIFIER type (DEFAULT val = value)?
   ;

field
   : INSTANCE? FIELD name = IDENTIFIER type
   ;

method
   : INSTANCE? SEALED? METHOD name = IDENTIFIER COLON implementation = IDENTIFIER type param*
   ;

classDef
   : ROOT? SEALED? CLASS name = IDENTIFIER (EXTENDS base = IDENTIFIER)? (COLON (type_name = IDENTIFIER)? (method | field)*)?
   ;

file
   : (method | field | classDef)*
   ;

