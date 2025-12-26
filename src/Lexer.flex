%%
%class Lexer
%type Tokens
//Este apartado se basa en en que va a reconocer el analizador lexico letras de la a a la z y numeros del 0 al 9 ect
L=[a-zA-Z_]+
D=[0-9]+
//Ignorara los espacios en blanco, tabulador ...
espacio=[ ,\t,\r]+
%{
    public String lexeme;
%}
%%

/* Palabras reservadas */
"int"     { lexeme = yytext(); return Tokens.Int;}
"if"      { lexeme = yytext(); return Tokens.If;}
"else"    { lexeme = yytext(); return Tokens.Else;}
"while"   { lexeme = yytext(); return Tokens.While;}

/* Operadores */
"\n"  { lexeme = yytext(); return Tokens.S_Linea;}
"="   { lexeme = yytext(); return Tokens.Igual;}
"+"   { lexeme = yytext(); return Tokens.Suma;}
"-"   { lexeme = yytext(); return Tokens.Resta;}
"*"   { lexeme = yytext(); return Tokens.Multiplicacion;}
"/"   { lexeme = yytext(); return Tokens.Division;}
"("   { lexeme = yytext(); return Tokens.Parentesis_a;}
")"   { lexeme = yytext(); return Tokens.Parentesis_c;}
"{"   { lexeme = yytext(); return Tokens.Llave_a;}
"}"   { lexeme = yytext(); return Tokens.Llave_c;}
";"   { lexeme = yytext(); return Tokens.P_coma;}


{espacio} {/*Ignore*/}
"//".* {/*Ignore*/}


/* Identificadores y números */
{L}({L}|{D})* {lexeme=yytext(); return Tokens.Identificador;}
("(-"{D}+")")|{D}+ {lexeme=yytext(); return Tokens.Numero;}
 . {return Tokens.ERROR;}
 // retorna error si no encuentra nada
