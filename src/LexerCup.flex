import java_cup.runtime.*;

%%
%class LexerCup
%type Symbol
%cup
%full
%line
%char

L = [a-zA-Z_]
D = [0-9]
espacio = [ \t\r\n]+
comentarioLinea = "//".*
comentarioMultilinea = "/\\*"(.|\\n)*?"\\*/"

%{
    private Symbol symbol(int type, Object value){
        return new Symbol(type, yyline, yycolumn, value);
    }

    private Symbol symbol(int type){
        return new Symbol(type, yyline, yycolumn);
    }
%}

%%

/* Espacios y comentarios */
{espacio}              { /* Ignore */ }
{comentarioLinea}      { /* Ignore */ }
{comentarioMultilinea} { /* Ignore */ }

/* Palabras reservadas */
"int"       { return symbol(sym.Int, yytext()); }
"float"     { return symbol(sym.Float, yytext()); }
"bool"      { return symbol(sym.Bool, yytext()); }
"char"      { return symbol(sym.Char, yytext()); }
"string"    { return symbol(sym.String, yytext()); }
"func"      { return symbol(sym.Func, yytext()); }
"if"        { return symbol(sym.If, yytext()); }
"else"      { return symbol(sym.Else, yytext()); }
"while"     { return symbol(sym.While, yytext()); }
"for"       { return symbol(sym.For, yytext()); }
"do"        { return symbol(sym.Do, yytext()); }
"return"    { return symbol(sym.Return, yytext()); }
"navidad"   { return symbol(sym.Navidad, yytext()); }
"world"     { return symbol(sym.World, yytext()); }

/* Operadores relacionales */
"=="|"!="|"<="|">="|"<"|">" { return symbol(sym.Op_relacional, yytext()); }

/* Operadores lógicos */
"&&"|"||" { return symbol(sym.Op_logico, yytext()); }

/* Incremento */
"++"|"--" { return symbol(sym.Op_incremento, yytext()); }

/* Asignación compuesta */
"+="|"-="|"*="|"/=" { return symbol(sym.Op_atribucion, yytext()); }

/* Operadores simples */
"="  { return symbol(sym.Igual, yytext()); }
"+"  { return symbol(sym.Suma, yytext()); }
"-"  { return symbol(sym.Resta, yytext()); }
"*"  { return symbol(sym.Multiplicacion, yytext()); }
"/"  { return symbol(sym.Division, yytext()); }

/* Símbolos */
"("  { return symbol(sym.Parentesis_a, yytext()); }
")"  { return symbol(sym.Parentesis_c, yytext()); }
"{"  { return symbol(sym.Llave_a, yytext()); }
"}"  { return symbol(sym.Llave_c, yytext()); }
"["  { return symbol(sym.Corchete_a, yytext()); }
"]"  { return symbol(sym.Corchete_c, yytext()); }
";"  { return symbol(sym.P_coma, yytext()); }

/* Booleanos */
"true"|"false" { return symbol(sym.Op_booleano, yytext()); }

/* Identificadores y números */
{L}({L}|{D})* { return symbol(sym.Identificador, yytext()); }
{D}+          { return symbol(sym.Numero, yytext()); }

/* Error léxico */
. { return symbol(sym.ERROR, yytext()); }
