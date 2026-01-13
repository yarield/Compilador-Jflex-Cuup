import java_cup.runtime.*;

%%
%class LexerCup
%type Symbol
%cup
%full
%line
%char
%unicode

L = [a-zA-Z_]
D = [0-9]
espacio = [ \t\r\n]+
comentarioLinea = "|".*
comentarioMultilinea =   "є" ~"э"

%{
    private Symbol symbol(int type, Object value){
        return new Symbol(type, yyline, yycolumn, value);
    }

    private Symbol symbol(int type){
        return new Symbol(type, yyline, yycolumn);
    }
%}

%%

{comentarioMultilinea} { /* ignorar */ }
{comentarioLinea}      { /* ignorar */ }
{espacio}+             { /* ignorar */ }


/* Palabras reservadas */
"int"       { return symbol(sym.Int, yytext()); }
"float"     { return symbol(sym.Float, yytext()); }
"bool"      { return symbol(sym.Bool, yytext()); }
"char"      { return symbol(sym.Char, yytext()); }
"string"    { return symbol(sym.String, yytext()); }
"coal"      { return symbol(sym.Coal, yytext()); }

"world"     { return symbol(sym.World, yytext()); }
"local"     { return symbol(sym.Local, yytext()); }
"return"    { return symbol(sym.Return, yytext()); }
"break"     { return symbol(sym.Break, yytext()); }

"gift"      { return symbol(sym.Gift, yytext()); }
"navidad"   { return symbol(sym.Navidad, yytext()); }

"show"      { return symbol(sym.Show, yytext()); }
"get"       { return symbol(sym.Get, yytext()); }

"decide"    { return symbol(sym.Decide, yytext()); }
"of"        { return symbol(sym.Of, yytext()); }
"else"      { return symbol(sym.Else, yytext()); }
"end"       { return symbol(sym.End, yytext()); }
"loop"      { return symbol(sym.Loop, yytext()); }
"exit"      { return symbol(sym.Exit, yytext()); }
"when"      { return symbol(sym.When, yytext()); }
"for"       { return symbol(sym.For, yytext()); }

"true"      { return symbol(sym.True, yytext()); }
"false"     { return symbol(sym.False, yytext()); }
"endl"      { return symbol(sym.Endl, yytext()); }


/* Operadores - TODOS */
"++"  { return symbol(sym.Op_incremento, yytext()); }
"--"  { return symbol(sym.Op_incremento, yytext()); }

"+"   { return symbol(sym.Suma, yytext()); }
"-"   { return symbol(sym.Resta, yytext()); }
"*"   { return symbol(sym.Multiplicacion, yytext()); }
"//"  { return symbol(sym.Division_entera, yytext()); }
"/"   { return symbol(sym.Division, yytext()); }
"%"   { return symbol(sym.Modulo, yytext()); }
"^"   { return symbol(sym.Potencia, yytext()); }

"<="  { return symbol(sym.Menor_igual, yytext()); }
">="  { return symbol(sym.Mayor_igual, yytext()); }
"<"   { return symbol(sym.Menor, yytext()); }
">"   { return symbol(sym.Mayor, yytext()); }
"!="  { return symbol(sym.Diferente, yytext()); }
"->"  { return symbol(sym.Flecha, yytext()); }


"@"   { return symbol(sym.Conjuncion, yytext()); }
"~"   { return symbol(sym.Disyuncion, yytext()); }
"Σ"   { return symbol(sym.Negacion, yytext()); }

"="   { return symbol(sym.Igual, yytext()); }

"¿"   { return symbol(sym.S_pregunta_a, yytext()); }
"?"   { return symbol(sym.S_pregunta_c, yytext()); }
"¡"   { return symbol(sym.S_exclamacion_a, yytext()); }
"!"   { return symbol(sym.S_exclamacion_c, yytext()); }

"["   { return symbol(sym.Corchete_a, yytext()); }
"]"   { return symbol(sym.Corchete_c, yytext()); }
";"   { return symbol(sym.P_coma, yytext()); }
","   { return symbol(sym.Coma, yytext()); }

/* Identificadores y números */
{D}+"."{D}+   { return symbol(sym.Flotante, yytext()); }
{D}+          { return symbol(sym.Entero, yytext()); }
{L}({L}|{D})* { return symbol(sym.Identificador, yytext()); }
"'"([^'\n]|"\\'")"'" { return symbol(sym.Caracter, yytext()); }
\"([^\"\n]|\\\")*\" { return symbol(sym.Cadena, yytext()); }


/* Error léxico */
. { return symbol(sym.ERROR, yytext()); }
