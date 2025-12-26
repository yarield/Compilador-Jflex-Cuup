import java_cup.runtime.*;
%%
%class LexerCup
%type Symbol
%cup 
%full 
%line 
%char 

L=[a-zA-Z_]+
D=[0-9]+
espacio=[ ,\t,\r,\n]+

%{
    private Symbol symbol(int type, Object value){
        return new Symbol(type, yyline, yycolumn, value);
    }

    private Symbol symbol(int type){
        return new Symbol(type, yyline, yycolumn);
    }
%}
%%

/* Palabras reservadas */
"int"       {return symbol(sym.Int, yytext());}
"if"        {return symbol(sym.If, yytext());}
"else"      {return symbol(sym.Else, yytext());}
"while"     {return symbol(sym.While, yytext());}
"for"       {return symbol(sym.For, yytext());}
"do"        {return symbol(sym.Do, yytext());}
"return"    {return symbol(sym.Return, yytext());}  
"navidad"   {return symbol(sym.Navidad, yytext());}

/* Operadores y símbolos */
"="       {return symbol(sym.Igual, yytext());}
"+"       {return symbol(sym.Suma, yytext());}
"-"       {return symbol(sym.Resta, yytext());}
"*"       {return symbol(sym.Multiplicacion, yytext());}
"/"       {return symbol(sym.Division, yytext());}
"("       {return symbol(sym.Parentesis_a, yytext());}
")"       {return symbol(sym.Parentesis_c, yytext());}
"{"       {return symbol(sym.Llave_a, yytext());}
"}"       {return symbol(sym.Llave_c, yytext());}
";"       {return symbol(sym.P_coma, yytext());}

/* Operadores adicionales (para tu gramática completa) */
"=="|"!="|"<"|">"|"<="|">=" {return symbol(sym.Op_relacional, yytext());}
"&&"|"||"                   {return symbol(sym.Op_logico, yytext());}
"++"|"--"                   {return symbol(sym.Op_incremento, yytext());}
"+="|"-="|"*="|"/="        {return symbol(sym.Op_atribucion, yytext());}
"true"|"false"             {return symbol(sym.Op_booleano, yytext());}

{espacio} {/*Ignore*/}
"//".*    {/*Ignore*/}

/* Identificadores y números */
{L}({L}|{D})*     {return symbol(sym.Identificador, yytext());}
("-"{D}+)|{D}+    {return symbol(sym.Numero, yytext());}

/* Cualquier otro carácter (error) */
.                 {return symbol(sym.ERROR, yytext());}