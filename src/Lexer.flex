%%
%class Lexer
%type Tokens
L=[a-zA-Z_]+
D=[0-9]+
espacio=[ ,\t,\r,\n]+
%{
    public String lexeme;
%}
%%

/* Palabras reservadas */
"int"       { lexeme = yytext(); return Tokens.Int; }
"float"     { lexeme = yytext(); return Tokens.Float; }
"bool"      { lexeme = yytext(); return Tokens.Bool; }
"char"      { lexeme = yytext(); return Tokens.Char; }
"string"    { lexeme = yytext(); return Tokens.String; }
"func"      { lexeme = yytext(); return Tokens.Func; }
"if"        { lexeme = yytext(); return Tokens.If; }
"else"      { lexeme = yytext(); return Tokens.Else; }
"while"     { lexeme = yytext(); return Tokens.While; }
"for"       { lexeme = yytext(); return Tokens.For; }
"do"        { lexeme = yytext(); return Tokens.Do; }
"return"    { lexeme = yytext(); return Tokens.Return; }
"navidad"   { lexeme = yytext(); return Tokens.Navidad; }
"world"     { lexeme = yytext(); return Tokens.World; }

/* Operadores - TODOS */
"="   { lexeme = yytext(); return Tokens.Igual; }
"+"   { lexeme = yytext(); return Tokens.Suma; }
"-"   { lexeme = yytext(); return Tokens.Resta; }
"*"   { lexeme = yytext(); return Tokens.Multiplicacion; }
"/"   { lexeme = yytext(); return Tokens.Division; }
"("   { lexeme = yytext(); return Tokens.Parentesis_a; }
")"   { lexeme = yytext(); return Tokens.Parentesis_c; }
"{"   { lexeme = yytext(); return Tokens.Llave_a; }
"}"   { lexeme = yytext(); return Tokens.Llave_c; }
"["   { lexeme = yytext(); return Tokens.Corchete_a; }
"]"   { lexeme = yytext(); return Tokens.Corchete_c; }
";"   { lexeme = yytext(); return Tokens.P_coma; }
","   { lexeme = yytext(); return Tokens.Coma; }

/* Operadores adicionales */
"=="|"!="|"<"|">"|"<="|">=" { lexeme = yytext(); return Tokens.Op_relacional; }
"&&"|"||"                   { lexeme = yytext(); return Tokens.Op_logico; }
"++"|"--"                   { lexeme = yytext(); return Tokens.Op_incremento; }
"+="|"-="|"*="|"/="        { lexeme = yytext(); return Tokens.Op_atribucion; }
"true"|"false"             { lexeme = yytext(); return Tokens.Op_booleano; }

{espacio} {/*Ignore*/}
"//".* {/*Ignore*/}
"\n" { /* Ignore */ }


/* Identificadores y números */
{L}({L}|{D})* { lexeme = yytext(); return Tokens.Identificador; }
("-"{D}+)|{D}+ { lexeme = yytext(); return Tokens.Numero; }

/* Cualquier otro carácter (error) */
. { return Tokens.ERROR; }