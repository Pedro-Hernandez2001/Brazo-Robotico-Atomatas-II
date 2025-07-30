package a_lexico_robot;

import static a_lexico_robot.Token.*;

%%

%class Lexer
%type Token
%public
%unicode
%line
%column

L=[a-zA-Z]
D=[0-9]
ID=r[0-9]+

%{
    public String lexeme;
    public int linea = 1;
%}

%%

// Saltos de línea
\n                              { linea++; }

// Espacios y comentarios
[ \t\r]+                        { /* ignorar */ }
"//".*                          { /* ignorar */ }

// Palabras clave
"Robot"                         { lexeme = yytext(); return Palabra_r; }
"iniciar"|"finalizar"          { lexeme = yytext(); return Metodo; }
"abrirGarra"|"cerrarGarra"     { lexeme = yytext(); return Metodo; }
"velocidad"|"repetir"          { lexeme = yytext(); return Metodo; }
"base"|"hombro"|"codo"|"garra" { lexeme = yytext(); return Metodo; }

// Identificadores
{ID}                            { lexeme = yytext(); return identificador; }

// Números enteros
{D}+                            { lexeme = yytext(); return Numero; }

// Símbolos
"="                             { lexeme = yytext(); return ASIGNACION; }
"."                             { lexeme = yytext(); return PUNTO; }
"("                             { lexeme = yytext(); return PARENTESIS_ABRE; }
")"                             { lexeme = yytext(); return PARENTESIS_CIERRA; }
"{"                             { lexeme = yytext(); return LLAVE_ABRE; }
"}"                             { lexeme = yytext(); return LLAVE_CIERRA; }

// Cualquier otro carácter no válido
.                               { lexeme = yytext(); return ERROR; }
