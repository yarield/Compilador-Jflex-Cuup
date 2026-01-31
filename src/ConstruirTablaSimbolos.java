import java.util.ArrayList;
import java.util.List;

public class ConstruirTablaSimbolos {

    private final TablaSimbolos ts;
    private int contadorBloques = 0;
    private boolean enLoop = false;         
    private boolean enDecideOf = false;         
    private String funcionActual = "";       

    public ConstruirTablaSimbolos(TablaSimbolos ts) {
        this.ts = ts;
    }

    public void construir(Nodo raiz) {
        ts.iniciarGlobal();
        if (raiz != null)
            visitar(raiz);
    }

    private void visitar(Nodo n) {
        if (n == null)
            return;

        String lx = safe(n.lexema);

        // 1) MAIN
        if (lx.equals("main")) {
            ts.entrarFuncion("main");
            
            for (Nodo h : n.hijos) {
                if (safe(h.lexema).equals("bloque")) {
                    for (Nodo hijoBloque : h.hijos) {
                        visitar(hijoBloque);
                    }
                } else {
                    visitar(h);
                }
            }
            
            ts.salirFuncion();
            return;
        }

        // 2) FUNCIÓN
        if (lx.equals("funcion")) {
            String nombreFun = extraerNombreFuncion(n);
            String tipoRetorno = extraerTipoRetornoFuncion(n);
            List<String> tiposParametros = extraerTiposParametrosFuncion(n);
            
            ts.declararFuncion(nombreFun, tipoRetorno, tiposParametros, -1, -1);
            
            String key = "func:" + nombreFun;
            ts.entrarFuncion(key);

            for (Nodo h : n.hijos) {
                String hijoLex = safe(h.lexema);
                if (!hijoLex.equals("Gift") && 
                    !hijoLex.startsWith("tipo:") &&
                    !(hijoLex.startsWith("Ident(") && 
                      extraerIdentificador(h).equals(nombreFun))) {
                    visitar(h);
                }
            }

            ts.salirFuncion();
            return;
        }

        // 3) BLOQUE
        if (lx.equals("bloque")) {
            if (!ts.getScopeActual().nombre.startsWith("bloque#")) {
                contadorBloques++;
                String nombreBloque = "bloque#" + contadorBloques;
                ts.entrarBloque(nombreBloque);

                for (Nodo h : n.hijos)
                    visitar(h);

                ts.salirBloque();
            }
            return;
        }

        // 4) DECLARACIONES
        if (lx.equals("declaracion_local") || lx.equals("declaracion_global")) {
            procesarDeclaracionConTipos(n, lx);
        }

        // 5) ASIGNACIÓN
        if (lx.equals("asignacion") && n.hijos.size() >= 2) {
            procesarAsignacionConTipos(n);
        }

        // 6) OPERACIONES BINARIAS
        if (lx.equals("op")) {
            procesarOperacionBinariaEStricta(n);
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }

        // 7) LOOP
        if (lx.equals("loop")) {
            procesarLoop(n);
            return;
        }

        // 8) FOR
        if (lx.equals("for")) {
            procesarFor(n);
            return;
        }

        // 9) DECIDE/OF
        if (lx.equals("decide_of")) {
            procesarDecideOf(n);
            return;
        }

        // 10) RETURN
        if (lx.equals("return")) {
            procesarReturn(n);
            return;
        }

        // 11) BREAK
        if (lx.equals("break")) {
            procesarBreak(n);
            return;
        }

        // 12) Parámetros
        if (lx.equals("param")) {
            procesarParametro(n);
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }
        
        // 13) LLAMADA A FUNCIÓN
        if (lx.equals("llamada_funcion")) {
            procesarLlamadaFuncion(n);
            return;
        }

        // 14) IDENTIFICADORES
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty()) {
                ts.usarIdentificador(id, -1, -1);
            }
        }

        // 15) Recorrido normal
        for (Nodo h : n.hijos)
            visitar(h);
    }

    // ==================== DECLARACIONES ====================

    private void procesarDeclaracionConTipos(Nodo n, String tipoDecl) {
        String clase = tipoDecl.equals("declaracion_global") ? "global" : "local";
        String tipoVar = "desconocido";
        String id = "";
        boolean esArreglo = false;
        int dims = 0;

        boolean encontroTipo = false;
        
        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);

            if (lx.startsWith("tipo:")) {
                tipoVar = lx.substring("tipo:".length());
                encontroTipo = true;
            }
            
            if (encontroTipo && lx.startsWith("Ident(")) {
                id = extraerIdentificador(h);
                break;
            }
            
            if (lx.equals("arreglo")) {
                esArreglo = true;
                dims = contarDimensionesArreglo(h);
            }
        }

        if (id.isEmpty()) return;

        // Declarar el símbolo
        Simbolo s = new Simbolo(
            id, tipoVar, clase, ts.getScopeActual().nombre,
            -1, -1,
            esArreglo, dims, ""
        );
        ts.declarar(s);

        // Buscar inicialización y verificar tipos
        for (int i = 0; i < n.hijos.size(); i++) {
            Nodo h = n.hijos.get(i);
            String lx = safe(h.lexema);
            
            if (lx.equals("=")) {
                if (i + 1 < n.hijos.size()) {
                    Nodo expr = n.hijos.get(i + 1);
                    String tipoExpr = evaluarTipoExpresion(expr);
                    
                    // Verificar compatibilidad de tipos
                    if (!tipoExpr.equals("desconocido") && !tipoVar.equals("desconocido")) {
                        ts.verificarTipos(tipoVar, tipoExpr, -1, -1);
                    }
                }
                break;
            }
        }
    }

    // ==================== ASIGNACIONES ====================

    private void procesarAsignacionConTipos(Nodo n) {
        if (n.hijos.size() < 2) return;
        
        Nodo lhs = n.hijos.get(0);
        Nodo rhs = n.hijos.get(1);
        
        String id = extraerIdentificador(lhs);
        if (!id.isEmpty()) {
            // Marcar que se usa el identificador
            ts.usarIdentificador(id, -1, -1);
            
            // Obtener tipo del LHS
            Simbolo s = ts.buscarSimbolo(id);
            if (s != null) {
                String tipoVar = s.tipo;
                String tipoExpr = evaluarTipoExpresion(rhs);
                
                // Verificar compatibilidad
                if (!tipoExpr.equals("desconocido")) {
                    ts.verificarTipos(tipoVar, tipoExpr, -1, -1);
                }
            }
        }
    }

    // ==================== ESTRUCTURAS DE CONTROL ====================

    private void procesarLoop(Nodo n) {
        boolean prevEnLoop = enLoop;
        enLoop = true;
        
        for (Nodo h : n.hijos) {
            visitar(h);
        }
        
        // Verificar la condición del Exit When
        for (int i = 0; i < n.hijos.size(); i++) {
            if (safe(n.hijos.get(i).lexema).equals("When")) {
                if (i + 1 < n.hijos.size()) {
                    Nodo cond = n.hijos.get(i + 1);
                    String tipoCond = evaluarTipoExpresion(cond);
                    if (!tipoCond.equals("bool") && !tipoCond.equals("desconocido")) {
                        reportarError("Condición en 'exit when' debe ser bool, es " + tipoCond, -1, -1);
                    }
                }
                break;
            }
        }
        
        enLoop = prevEnLoop;
    }

private void procesarFor(Nodo n) {
    boolean prevEnLoop = enLoop;
    enLoop = true;
    
    // PRIMERO procesar la declaración de la variable
    for (Nodo h : n.hijos) {
        String lx = safe(h.lexema);
        if (lx.equals("¿")) {
            // Buscar declaración dentro del for
            for (Nodo hijoFor : h.hijos) {
                String hijoLex = safe(hijoFor.lexema);
                if (hijoLex.startsWith("tipo:")) {
                    // Encontró una declaración de tipo
                    // El siguiente nodo debería ser el identificador
                    String tipoVar = hijoLex.substring("tipo:".length());
                    String id = "";
                    
                    // Buscar el identificador siguiente
                    for (Nodo hermano : h.hijos) {
                        String hermanoLex = safe(hermano.lexema);
                        if (hermanoLex.startsWith("Ident(")) {
                            id = extraerIdentificador(hermano);
                            break;
                        }
                    }
                    
                    if (!id.isEmpty()) {
                        // Declarar la variable ANTES de usarla
                        Simbolo s = new Simbolo(
                            id, tipoVar, "local", ts.getScopeActual().nombre,
                            -1, -1,
                            false, 0, ""
                        );
                        ts.declarar(s);
                    }
                    break;
                }
            }
            break;
        }
    }
    
    // LUEGO procesar todo normalmente
    for (Nodo h : n.hijos) {
        visitar(h);
    }
    
    enLoop = prevEnLoop;
}


    private void procesarDecideOf(Nodo n) {
        boolean prevEnDecideOf = enDecideOf;
        enDecideOf = true;
        
        for (Nodo h : n.hijos) {
            visitar(h);
        }
        
        for (Nodo h : n.hijos) {
            if (safe(h.lexema).equals("lista_decide_of")) {
                verificarCasosDecideOf(h);
            }
        }
        
        enDecideOf = prevEnDecideOf;
    }

    private void verificarCasosDecideOf(Nodo listaCasos) {
        for (Nodo caso : listaCasos.hijos) {
            if (safe(caso.lexema).equals("case")) {
                if (!caso.hijos.isEmpty()) {
                    Nodo expr = caso.hijos.get(0);
                    String tipoExpr = evaluarTipoExpresion(expr);
                    
                    if (!tipoExpr.equals("bool") && !tipoExpr.equals("desconocido")) {
                        reportarError("Expresión en 'decide of' debe ser bool, es " + tipoExpr, -1, -1);
                    }
                }
            }
        }
    }
    private void procesarReturn(Nodo n) {
        if (!n.hijos.isEmpty()) {
            Nodo expr = n.hijos.get(0);
            String tipoExpr = evaluarTipoExpresion(expr);
            
            // Obtener tipo de retorno de la función actual
            String tipoEsperado = obtenerTipoRetornoFuncionActual();
            if (!tipoEsperado.equals("void")) {
                ts.verificarTipos(tipoEsperado, tipoExpr, -1, -1);
            }
        }
    }

    private String obtenerTipoRetornoFuncionActual() {
        // Por ahora, devuelve "void" como placeholder
        // Necesitarías llevar registro del tipo de función actual
        return "void";
    }

    private void procesarBreak(Nodo n) {
        if (!enLoop && !enDecideOf) {
            reportarError("'break' solo puede usarse dentro de loop o decide of", -1, -1);
        }
    }

    private boolean estaEnLoop() {
        return enLoop;
    }

    private boolean estaEnDecideOf() {
        return enDecideOf;
    }

    // ==================== EVALUACIÓN DE EXPRESIONES ====================

    private String evaluarTipoExpresion(Nodo n) {
        if (n == null) return "desconocido";

        String lx = safe(n.lexema);

        // LITERAL
        String lit = tipoLiteral(lx);
        if (!lit.equals("desconocido"))
            return lit;

        // EXPRESIÓN NEGATIVA (-1.0, -5, etc.)
        if (lx.equals("negativo")) {
            // El primer hijo es el operador "-", el segundo es el valor
            if (n.hijos.size() >= 2) {
                Nodo valor = n.hijos.get(1); // El valor después del "-"
                String tipoValor = evaluarTipoExpresion(valor);
                // -int → int, -float → float
                return tipoValor;
            }
            return "desconocido";
        }

        // IDENTIFICADOR
        if (lx.startsWith("Ident(")) {
            Simbolo s = ts.buscarSimbolo(extraerIdentificador(n));
            return s != null ? s.tipo : "desconocido";
        }

        // OPERACIÓN BINARIA
        if (lx.equals("op") && n.hijos.size() == 3) {
            String op = safe(n.hijos.get(0).lexema);
            String t1 = evaluarTipoExpresion(n.hijos.get(1));
            String t2 = evaluarTipoExpresion(n.hijos.get(2));
            return verificarOperacionBinaria(op, t1, t2);
        }

        // LLAMADA A FUNCIÓN
        if (lx.equals("llamada_funcion")) {
            String nombreFunc = "";
            for (Nodo h : n.hijos) {
                if (safe(h.lexema).startsWith("Ident(")) {
                    nombreFunc = extraerIdentificador(h);
                    break;
                }
            }
            
            if (!nombreFunc.isEmpty()) {
                Simbolo s = ts.buscarFuncion(nombreFunc);
                return s != null ? s.tipo : "desconocido";
            }
        }

        // AQUÍ FALTA EL CASO PARA EXPRESIONES DE COMPARACIÓN (==, !=, <, >, etc.)
        // Agrega este bloque justo aquí:
        if (lx.equals("¿") && !n.hijos.isEmpty()) {
            // Buscar el contenido dentro de ¿ ?
            for (Nodo h : n.hijos) {
                if (!safe(h.lexema).equals("?")) {
                    // Evaluar recursivamente lo que hay dentro
                    String tipoInterno = evaluarTipoExpresion(h);
                    return tipoInterno;
                }
            }
            return "desconocido";
        }

        // AQUÍ TAMBIÉN FALTA MANEJAR EXPRESIONES DE COMPARACIÓN DIRECTAS
        // Si el nodo es una comparación (debería ser "op" pero a veces no)
        // Revisar si es una expresión simple
        for (Nodo h : n.hijos) {
            if (safe(h.lexema).equals("op")) {
                String tipoHijo = evaluarTipoExpresion(h);
                return tipoHijo;
            }
        }

        return "desconocido";
    }

    private String verificarOperacionBinaria(String op, String t1, String t2) {
        if (t1.equals("desconocido") || t2.equals("desconocido"))
            return "desconocido";

        // OPERADORES DE COMPARACIÓN (retornan bool)
        if (op.equals("==") || op.equals("!=") || 
            op.equals("<") || op.equals(">") || 
            op.equals("<=") || op.equals(">=")) {
            
            if (sonComparables(t1, t2)) {
                return "bool";
            } else {
                reportarError("Tipos incompatibles para operador '" + op + "': " + t1 + " y " + t2, -1, -1);
                return "desconocido";
            }
        }

        // SUMA
        if (op.equals("+")) {
            if (t1.equals("string") && t2.equals("string"))
                return "string";
            
            if (t1.equals("int") && t2.equals("int"))
                return "int";
            
            if (t1.equals("float") && t2.equals("float"))
                return "float";
            
            if ((t1.equals("float") && t2.equals("int")) || 
                (t1.equals("int") && t2.equals("float")))
                return "float";
            
            reportarErrorOperacion("+", t1, t2, -1, -1);
            return "desconocido";
        }
        
        // RESTA y MULTIPLICACIÓN
        if (op.equals("-") || op.equals("*")) {
            if (t1.equals("int") && t2.equals("int"))
                return "int";
            
            if (t1.equals("float") && t2.equals("float"))
                return "float";
            
            if ((t1.equals("float") && t2.equals("int")) || 
                (t1.equals("int") && t2.equals("float")))
                return "float";
            
            reportarErrorOperacion(op, t1, t2, -1, -1);
            return "desconocido";
        }
        
        // DIVISIÓN NORMAL
        if (op.equals("/")) {
            if (t1.equals("float") && t2.equals("float"))
                return "float";
            
            reportarErrorOperacion("/", t1, t2, -1, -1);
            return "desconocido";
        }
        
        // DIVISIÓN ENTERA
        if (op.equals("//")) {
            if (t1.equals("int") && t2.equals("int"))
                return "int";
            
            reportarErrorOperacion("//", t1, t2, -1, -1);
            return "desconocido";
        }
        
        // MÓDULO
        if (op.equals("%")) {
            if (t1.equals("int") && t2.equals("int"))
                return "int";
            
            reportarErrorOperacion("%", t1, t2, -1, -1);
            return "desconocido";
        }
        
        // POTENCIA
        if (op.equals("^")) {
            if (t1.equals("float") && t2.equals("float"))
                return "float";
            
            reportarErrorOperacion("^", t1, t2, -1, -1);
            return "desconocido";
        }
        
        return "desconocido";
    }

    // ==================== VERIFICACIÓN ESTRICTA DE OPERACIONES ====================

    private void procesarOperacionBinariaEStricta(Nodo opNode) {
        if (opNode.hijos.size() < 3) return;
        
        String operador = safe(opNode.hijos.get(0).lexema);
        Nodo operando1 = opNode.hijos.get(1);
        Nodo operando2 = opNode.hijos.get(2);
        
        String tipo1 = evaluarTipoExpresion(operando1);
        String tipo2 = evaluarTipoExpresion(operando2);
        
        // La verificación de tipos ya se hace en verificarOperacionBinaria
        verificarOperacionBinaria(operador, tipo1, tipo2);
    }

    private boolean verificarOperacionAritmeticaEstricta(String operador, String tipo1, String tipo2, int linea, int columna) {
        switch(operador) {
            case "+": return verificarSumaEstricta(tipo1, tipo2, linea, columna);
            case "-": return verificarRestaEstricta(tipo1, tipo2, linea, columna);
            case "*": return verificarMultiplicacionEstricta(tipo1, tipo2, linea, columna);
            case "/": return verificarDivisionEstricta(tipo1, tipo2, linea, columna);
            case "//": return verificarDivisionEnteraEstricta(tipo1, tipo2, linea, columna);
            case "%": return verificarModuloEstricto(tipo1, tipo2, linea, columna);
            case "^": return verificarPotenciaEstricta(tipo1, tipo2, linea, columna);
            default: return true;
        }
    }

    private boolean verificarSumaEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("string") && tipo2.equals("string")) {
            return true;
        }
        
        if (tipo1.equals("int") && tipo2.equals("int")) {
            return true;
        }
        
        if (tipo1.equals("float") && tipo2.equals("float")) {
            return true;
        }
        
        if (tipo1.equals("float") && tipo2.equals("int")) {
            return true;
        }
        
        if (tipo1.equals("int") && tipo2.equals("float")) {
            return true;
        }
        
        reportarError("Tipos incompatibles para suma: " + tipo1 + " + " + tipo2, linea, columna);
        return false;
    }

    private boolean verificarRestaEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("int") && tipo2.equals("int")) {
            return true;
        }
        
        if (tipo1.equals("float") && tipo2.equals("float")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en resta: " + tipo1 + " - " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("Tipo no soportado para resta: " + tipo1, linea, columna);
        return false;
    }

    private boolean verificarMultiplicacionEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("int") && tipo2.equals("int")) {
            return true;
        }
        
        if (tipo1.equals("float") && tipo2.equals("float")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en multiplicación: " + tipo1 + " * " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("Tipo no soportado para multiplicación: " + tipo1, linea, columna);
        return false;
    }

    private boolean verificarDivisionEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("float") && tipo2.equals("float")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en división: " + tipo1 + " / " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("División '/' exclusiva para tipos float, recibió: " + tipo1, linea, columna);
        return false;
    }

    private boolean verificarDivisionEnteraEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("int") && tipo2.equals("int")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en división entera: " + tipo1 + " // " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("División entera '//' exclusiva para tipos int, recibió: " + tipo1, linea, columna);
        return false;
    }

    private boolean verificarModuloEstricto(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("int") && tipo2.equals("int")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en módulo: " + tipo1 + " % " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("Operador módulo '%' exclusivo para tipos int, recibió: " + tipo1, linea, columna);
        return false;
    }

    private boolean verificarPotenciaEstricta(String tipo1, String tipo2, int linea, int columna) {
        if (tipo1.equals("float") && tipo2.equals("float")) {
            return true;
        }
        
        if (!tipo1.equals(tipo2)) {
            reportarError("Tipos diferentes en potencia: " + tipo1 + " ^ " + tipo2, linea, columna);
            return false;
        }
        
        reportarError("Potencia '^' exclusiva para tipos float, recibió: " + tipo1, linea, columna);
        return false;
    }

    // ==================== MÉTODOS PARA FUNCIONES ====================

    private String extraerTipoRetornoFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.hijos) {
            String lx = safe(h.lexema);
            if (lx.startsWith("tipo:")) {
                return lx.substring("tipo:".length());
            }
        }
        return "void";
    }

    private List<String> extraerTiposParametrosFuncion(Nodo funcionNode) {
        List<String> tipos = new ArrayList<>();
        
        for (Nodo h : funcionNode.hijos) {
            if (safe(h.lexema).equals("parametros")) {
                for (Nodo param : h.hijos) {
                    if (safe(param.lexema).equals("param")) {
                        String tipoParam = extraerTipoParametro(param);
                        if (!tipoParam.isEmpty()) {
                            tipos.add(tipoParam);
                        }
                    }
                }
                break;
            }
        }
        
        return tipos;
    }

    private String extraerTipoParametro(Nodo paramNode) {
        for (Nodo h : paramNode.hijos) {
            String lx = safe(h.lexema);
            if (lx.startsWith("tipo:")) {
                return lx.substring("tipo:".length());
            }
        }
        return "";
    }

    private void procesarLlamadaFuncion(Nodo llamadaNode) {
        String nombreFuncion = "";
        List<String> tiposArgumentos = new ArrayList<>();
        
        for (Nodo h : llamadaNode.hijos) {
            String lx = safe(h.lexema);
            
            if (lx.startsWith("Ident(")) {
                nombreFuncion = extraerIdentificador(h);
            } else if (lx.equals("argumentos") || lx.equals("expresion")) {
                List<String> tipos = extraerTiposArgumentos(h);
                tiposArgumentos.addAll(tipos);
            }
        }
        
        if (!nombreFuncion.isEmpty()) {
            ts.verificarLlamadaFuncion(nombreFuncion, tiposArgumentos, -1, -1);
        }
    }

    private List<String> extraerTiposArgumentos(Nodo argumentosNode) {
        List<String> tipos = new ArrayList<>();
        
        for (Nodo h : argumentosNode.hijos) {
            String lx = safe(h.lexema);
            
            if (lx.startsWith("Ident(")) {
                String id = extraerIdentificador(h);
                Simbolo s = ts.buscarSimbolo(id);
                if (s != null) {
                    tipos.add(s.tipo);
                } else {
                    tipos.add("desconocido");
                }
            } else {
                String tipoLit = tipoLiteral(lx);
                if (!tipoLit.equals("desconocido")) {
                    tipos.add(tipoLit);
                } else if (!h.hijos.isEmpty()) {
                    tipos.addAll(extraerTiposArgumentos(h));
                }
            }
        }
        
        return tipos;
    }

    private String extraerNombreFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.hijos) {
            String id = extraerIdentificador(h);
            if (!id.isEmpty()) {
                return id.trim();
            }
        }
        return "anonima";
    }

    private void procesarParametro(Nodo paramNode) {
        String tipo = "desconocido";
        String id = "";

        for (Nodo h : paramNode.hijos) {
            String lx = safe(h.lexema);
            if (lx.startsWith("tipo:"))
                tipo = lx.substring("tipo:".length());
            if (lx.startsWith("Ident("))
                id = extraerIdentificador(h);
        }

        if (id.isEmpty())
            return;
        
        Simbolo s = new Simbolo(
            id, tipo, "parametro", ts.getScopeActual().nombre,
            -1, -1,
            false, 0, ""
        );
        ts.declarar(s);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private String tipoLiteral(String lx) {
        if (lx.contains("Entero")) return "int";
        if (lx.contains("Flotante")) return "float";
        if (lx.contains("Cadena")) return "string";
        if (lx.contains("Caracter")) return "char";
        if (lx.equals("True") || lx.equals("False")) return "bool";
        return "desconocido";
    }

    private void reportarError(String mensaje, int linea, int columna) {
        if (ts != null) {
            ts.verificarTipos("error_tipo", "error_tipo", linea, columna);
        }
        System.err.println("ERROR SEMÁNTICO: " + mensaje);
    }

    private void reportarErrorOperacion(String operador, String tipo1, String tipo2, int linea, int columna) {
        reportarError("Operación inválida '" + operador + "' entre " + tipo1 + " y " + tipo2, linea, columna);
    }

    
    private boolean sonComparables(String t1, String t2) {
        if (t1.equals("desconocido") || t2.equals("desconocido")) {
            return true;
        }
        
        if (t1.equals(t2)) return true;
        
        if ((t1.equals("int") && t2.equals("float")) ||
            (t1.equals("float") && t2.equals("int"))) {
            return true;
        }
        
        return false;
    }

    // ==================== MÉTODOS UTILITARIOS ====================

    private int contarDimensionesArreglo(Nodo arreglo) {
        int corchetes = contarLexema(arreglo, "[");
        if (corchetes >= 2)
            return 2;
        if (corchetes == 1)
            return 1;
        return 1;
    }

    private int contarLexema(Nodo n, String target) {
        int c = safe(n.lexema).equals(target) ? 1 : 0;
        for (Nodo h : n.hijos)
            c += contarLexema(h, target);
        return c;
    }

    private String extraerIdentificador(Nodo n) {
        String lx = safe(n.lexema);
        if (lx.startsWith("Ident(") && lx.endsWith(")")) {
            return lx.substring(6, lx.length() - 1);
        }
        return "";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}