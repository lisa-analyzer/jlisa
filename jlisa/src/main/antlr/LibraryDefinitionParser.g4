/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 by Bart Kiers
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Project      : python3-parser; an ANTLR4 grammar for Python 3
 *                https://github.com/bkiers/python3-parser
 * Developed by : Bart Kiers, bart@big-o.nl
 */
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

library
   : LIBRARY name = IDENTIFIER COLON LOCATION loc = IDENTIFIER (method | field | classDef)*
   ;

file
   : (method | field | classDef | library)*
   ;

