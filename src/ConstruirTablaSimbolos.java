import java.util.ArrayList;
import java.util.List;

public class ConstruirTablaSimbolos {

    private final TablaSimbolos ts;
    private int contadorBloques = 0;
    private boolean enLoop = false;         
    private boolean enDecideOf = false;            

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

        // 14) ACCESO A ARRAY (NUEVO)
        if (lx.equals("acceso_array")) {
            procesarAccesoArray(n);
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }

        // 15) IDENTIFICADORES
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty()) {
                ts.usarIdentificador(id, -1, -1);
            }
        }
         // 16) OPERACIONES LÓGICAS CON @ y ~
        if (lx.equals("op_logico") || (lx.equals("op") && !n.hijos.isEmpty())) {
            if (!n.hijos.isEmpty()) {
                String op = safe(n.hijos.get(0).lexema);
                if (op.equals("@") || op.equals("~")) {
                    procesarOperacionLogica(n);
                    return;
                }
            }
        }

        // 17) Recorrido normal
        for (Nodo h : n.hijos)
            visitar(h);
    }


    // ==================== DECLARACIONES ====================
    private void procesarOperacionLogica(Nodo n) {
        if (n.hijos.size() >= 3) {
            String operador = safe(n.hijos.get(0).lexema);
            Nodo operando1 = n.hijos.get(1);
            Nodo operando2 = n.hijos.get(2);
            
            String tipo1 = evaluarTipoExpresion(operando1);
            String tipo2 = evaluarTipoExpresion(operando2);
            
            verificarOperacionBinaria(operador, tipo1, tipo2);
        }
        
        for (Nodo h : n.hijos)
            visitar(h);
    }


    private void procesarDeclaracionConTipos(Nodo n, String tipoDecl) {
        String clase = tipoDecl.equals("declaracion_global") ? "global" : "local";
        String tipoVar = "desconocido";
        String id = "";
        boolean esArreglo = false;
        int dims = 0;

        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);

            if (lx.startsWith("tipo:")) {
                tipoVar = lx.substring("tipo:".length());
            }
            
            if (lx.startsWith("Ident(")) {
                id = extraerIdentificador(h);
            }
        }

        for (Nodo h : n.hijos) {
            if (safe(h.lexema).equals("arreglo")) {
                esArreglo = true;
                dims = contarDimensionesArregloCompleto(h);
                break;
            }
        }

        if (id.isEmpty()) return;

        Simbolo s = new Simbolo(
            id, tipoVar, clase, ts.getScopeActual().nombre,
            -1, -1,
            esArreglo, dims, ""
        );
        ts.declarar(s);
        
        for (int i = 0; i < n.hijos.size(); i++) {
            Nodo h = n.hijos.get(i);
            String lx = safe(h.lexema);
            
            if (esArreglo && lx.equals("init_array_2d")) {
                verificarInicializacionArreglo2D(h, tipoVar);
                break;
            }
            else if (esArreglo && lx.equals("init_array_1d")) {
                verificarInicializacionArreglo1D(h, tipoVar);
                break;
            }
            else if (!esArreglo && lx.equals("=")) {
            
                if (i + 1 < n.hijos.size()) {
                    Nodo expr = n.hijos.get(i + 1);
                    String tipoExpr = evaluarTipoExpresion(expr);
                    
                    if (!tipoExpr.equals("desconocido") && !tipoVar.equals("desconocido")) {
                        if (!tipoVar.equals(tipoExpr)) {
                            reportarError("No se puede asignar " + tipoExpr + " a " + tipoVar + 
                                        " (se requieren tipos iguales)", -1, -1);
                        }
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
            ts.usarIdentificador(id, -1, -1);
            
            Simbolo s = ts.buscarSimbolo(id);
            if (s != null) {
                String tipoVar = s.tipo;
                
               
                if (s.esArreglo && safe(rhs.lexema).equals("init_array_2d")) {
                    verificarInicializacionArreglo2D(rhs, tipoVar);
                }
                else if (s.esArreglo && safe(rhs.lexema).equals("init_array_1d")) {
                    verificarInicializacionArreglo1D(rhs, tipoVar);
                }
         
                else if (!s.esArreglo) {
                    String tipoExpr = evaluarTipoExpresion(rhs);
                    

                    if (!tipoExpr.equals("desconocido") && !tipoVar.equals(tipoExpr)) {
                        reportarError("No se puede asignar " + tipoExpr + " a " + tipoVar + 
                                    " (se requieren tipos iguales)", -1, -1);
                    }
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
        
        Nodo initFor = null;
        Nodo condicionFor = null;
        Nodo updateFor = null;
        Nodo cuerpoFor = null;
        
        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);
            if (lx.equals("init_for")) {
                initFor = h;
            } else if (lx.equals("op_logico") || lx.equals("op")) {
                condicionFor = h;
            } else if (lx.equals("update_for")) {
                updateFor = h;
            } else if (lx.equals("bloque")) {
                cuerpoFor = h;
            }
        }
        
        if (initFor != null) {
            procesarInitFor(initFor);
        }
        
        if (condicionFor != null) {
            visitar(condicionFor);
        }
        
        if (updateFor != null) {
            visitar(updateFor);
        }
        
        if (cuerpoFor != null) {
            visitar(cuerpoFor);
        }
        
        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);
            if (!lx.equals("init_for") && !lx.equals("op_logico") && 
                !lx.equals("op") && !lx.equals("update_for") && 
                !lx.equals("bloque")) {
                visitar(h);
            }
        }
        
        enLoop = prevEnLoop;
    }

    private void procesarInitFor(Nodo initFor) {
        String tipoVar = "int";
        String id = "";
        
        for (Nodo h : initFor.hijos) {
            String lx = safe(h.lexema);
            
            if (lx.startsWith("tipo:")) {
                tipoVar = lx.substring("tipo:".length());
            }
            
            if (lx.startsWith("Ident(")) {
                id = extraerIdentificador(h);
            }
        }
        
        if (!id.isEmpty()) {
            Simbolo s = new Simbolo(
                id, tipoVar, "local", ts.getScopeActual().nombre,
                -1, -1,
                false, 0, ""
            );
            ts.declarar(s);
        }
        
        for (Nodo h : initFor.hijos) {
            String lx = safe(h.lexema);
            if (!lx.startsWith("tipo:") && !lx.startsWith("Ident(")) {
                visitar(h);
            }
        }
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
            
            String tipoEsperado = obtenerTipoRetornoFuncionActual();
            if (!tipoEsperado.equals("void")) {
                ts.verificarTipos(tipoEsperado, tipoExpr, -1, -1);
            }
        }
    }

    private String obtenerTipoRetornoFuncionActual() {
        return "void";
    }

    private void procesarBreak(Nodo n) {
        if (!enLoop && !enDecideOf) {
            reportarError("'break' solo puede usarse dentro de loop o decide of", -1, -1);
        }
    }

    // ==================== EVALUACIÓN DE EXPRESIONES ====================

    private String evaluarTipoExpresion(Nodo n) {
        if (n == null) return "desconocido";

        String lx = safe(n.lexema);

        String lit = tipoLiteral(lx);
        if (!lit.equals("desconocido"))
            return lit;

        if (lx.equals("negativo")) {
            if (n.hijos.size() >= 2) {
                Nodo valor = n.hijos.get(1); 
                String tipoValor = evaluarTipoExpresion(valor);
                return tipoValor;
            }
            return "desconocido";
        }

        // Negacion
        if (lx.equals("negacion")) {
            if (n.hijos.size() >= 2) {
                Nodo operando = n.hijos.get(1);
                String tipoOperando = evaluarTipoExpresion(operando);
                
                if (!tipoOperando.equals("bool") && !tipoOperando.equals("desconocido")) {
                    reportarError("Negación 'Negacion' requiere operando bool, recibió: " + 
                                tipoOperando, -1, -1);
                }
                return "bool";
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

   
        if (lx.equals("¿") && !n.hijos.isEmpty()) {
            for (Nodo h : n.hijos) {
                if (!safe(h.lexema).equals("?")) {
                    String tipoInterno = evaluarTipoExpresion(h);
                    return tipoInterno;
                }
            }
            return "desconocido";
        }

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
            
            if (!t1.equals(t2)) {  
                reportarError("Tipos incompatibles para operador '" + op + "': " + 
                            t1 + " y " + t2 + ". Se requieren tipos iguales.", -1, -1);
                return "desconocido";
            }
            
            // Solo permitir comparación de tipos numéricos y bool
            if (!t1.equals("int") && !t1.equals("float") && 
                !t1.equals("bool") && !t1.equals("char")) {
                reportarError("Tipo '" + t1 + "' no puede usarse en comparación", -1, -1);
                return "desconocido";
            }
            
            return "bool";
        }

        // SUMA 
        if (op.equals("+")) {
            if (t1.equals("string") && t2.equals("string"))
                return "string";
            
            if (!t1.equals(t2)) {
                reportarError("Suma requiere tipos iguales, recibió: " + t1 + " + " + t2, -1, -1);
                return "desconocido";
            }
            
            if (t1.equals("int") || t1.equals("float"))
                return t1;
                
            reportarError("Suma no permitida para tipo: " + t1, -1, -1);
            return "desconocido";
        }
        
        // RESTA, MULTIPLICACIÓN
        if (op.equals("-") || op.equals("*")) {
            if (!t1.equals(t2)) {
                reportarError("Operador '" + op + "' requiere tipos iguales: " + 
                            t1 + " y " + t2, -1, -1);
                return "desconocido";
            }
            
            if (t1.equals("int") || t1.equals("float"))
                return t1;
                
            reportarError("Operador '" + op + "' no permitido para tipo: " + t1, -1, -1);
            return "desconocido";
        }
        
        // DIVISIÓN NORMAL
        if (op.equals("/")) {
            if (!t1.equals("float") || !t2.equals("float")) {
                reportarError("División '/' solo permite tipos float, recibió: " + 
                            t1 + " / " + t2, -1, -1);
                return "desconocido";
            }
            return "float";
        }
        
        // DIVISIÓN ENTERA
        if (op.equals("//")) {
            if (!t1.equals("int") || !t2.equals("int")) {
                reportarError("División entera '//' solo permite tipos int, recibió: " + 
                            t1 + " // " + t2, -1, -1);
                return "desconocido";
            }
            return "int";
        }
        
        // MÓDULO 
        if (op.equals("%")) {
            if (!t1.equals(t2)) {
                reportarError("Módulo '%' requiere tipos iguales, recibió: " + 
                            t1 + " % " + t2, -1, -1);
                return "desconocido";
            }
            
            if (!t1.equals("int") && !t1.equals("float")) {
                reportarError("Módulo '%' solo permite int o float, recibió: " + t1, -1, -1);
                return "desconocido";
            }
            
            return t1; 
        }

        // POTENCIA 
        if (op.equals("^")) {
            if (!t1.equals(t2)) {
                reportarError("Potencia '^' requiere tipos iguales, recibió: " + 
                            t1 + " ^ " + t2, -1, -1);
                return "desconocido";
            }
            
            if (!t1.equals("int") && !t1.equals("float")) {
                reportarError("Potencia '^' solo permite int o float, recibió: " + t1, -1, -1);
                return "desconocido";
            }
            
            return t1; 
        }

        if (op.equals("@") || op.equals("~")) {  
            if (!t1.equals("bool") || !t2.equals("bool")) {
                reportarError("Operador lógico '" + op + "' requiere operandos bool, recibió: " + 
                            t1 + " y " + t2, -1, -1);
                return "desconocido";
            }
            return "bool";
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
        
        verificarOperacionBinaria(operador, tipo1, tipo2);
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

    // ==================== MÉTODOS PARA DETECTAR ARRAYS ====================

    private int contarDimensionesArregloCompleto(Nodo arregloNode) {
        int dimensiones = 0;
        
        for (Nodo h : arregloNode.hijos) {
            if (safe(h.lexema).equals("[]")) {
                dimensiones++;
            }
            dimensiones += contarDimensionesArregloCompleto(h);
        }
        
        return dimensiones;
    }

    private void verificarInicializacionArreglo2D(Nodo initArray2D, String tipoEsperado) {
        for (Nodo h : initArray2D.hijos) {
            if (safe(h.lexema).equals("lista_filas")) {
        
                for (Nodo fila : h.hijos) {
                    if (safe(fila.lexema).equals("fila")) {
                        verificarFilaArreglo(fila, tipoEsperado);
                    }
                }
            }
        }
    }

    private void verificarInicializacionArreglo1D(Nodo initArray1D, String tipoEsperado) { 
        for (Nodo h : initArray1D.hijos) {
            if (safe(h.lexema).equals("lista_expresiones")) {
                for (Nodo expr : h.hijos) {
                    String lx = safe(expr.lexema);
                    if (!lx.equals(",")) {
                        verificarTipoElemento(lx, tipoEsperado);
                    }
                }
                
               
            }
        }
    }

  
    private void verificarFilaArreglo(Nodo filaNode, String tipoEsperado) {
        for (Nodo h : filaNode.hijos) {
            if (safe(h.lexema).equals("lista_expresiones")) {
          
                for (Nodo expr : h.hijos) {
                    String lx = safe(expr.lexema);
                
                    if (!lx.equals(",")) {
                        verificarTipoElemento(lx, tipoEsperado);
                    }
                }
            }
        }
    }

    private void verificarTipoElemento(String lexemaElemento, String tipoEsperado) {
        if (lexemaElemento.contains("Cadena")) {
            if (!tipoEsperado.equals("string")) {
                reportarError("Elemento string en arreglo de tipo " + tipoEsperado, -1, -1);
            }
        }
        else if (lexemaElemento.contains("Entero")) {
            if (!tipoEsperado.equals("int")) {
                reportarError("Elemento int en arreglo de tipo " + tipoEsperado, -1, -1);
            }
        }
        else if (lexemaElemento.contains("Flotante")) {
            if (!tipoEsperado.equals("float")) {
                reportarError("Elemento float en arreglo de tipo " + tipoEsperado, -1, -1);
            }
        }
        else if (lexemaElemento.contains("Caracter")) {
            if (!tipoEsperado.equals("char")) {
                reportarError("Elemento char en arreglo de tipo " + tipoEsperado, -1, -1);
            }
        }
        else if (lexemaElemento.equals("True") || lexemaElemento.equals("False")) {
            if (!tipoEsperado.equals("bool")) {
                reportarError("Elemento bool en arreglo de tipo " + tipoEsperado, -1, -1);
            }
        }
    }

 
    private void procesarAccesoArray(Nodo accesoNode) {
        String id = "";
        

        for (Nodo h : accesoNode.hijos) {
            if (safe(h.lexema).startsWith("Ident(")) {
                id = extraerIdentificador(h);
                break;
            }
        }
        
        if (!id.isEmpty()) {

            ts.usarIdentificador(id, -1, -1);
            
            Simbolo s = ts.buscarSimbolo(id);
            if (s != null) {
                if (!s.esArreglo) {
                    reportarError(id + " no es un arreglo", -1, -1);
                }
            }
        }
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