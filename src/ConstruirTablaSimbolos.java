import java.util.ArrayList;
import java.util.List;

public class ConstruirTablaSimbolos {

    private final TablaSimbolos ts;
    private int contadorBloques = 0;

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

        // 1) MAIN: manejar main correctamente
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

        // 2) FUNCIÓN: Declarar función en tabla global y procesar cuerpo
        if (lx.equals("funcion")) {
            String nombreFun = extraerNombreFuncion(n);
            String tipoRetorno = extraerTipoRetornoFuncion(n);
            List<String> tiposParametros = extraerTiposParametrosFuncion(n);
            
            // Declarar la función en el ámbito global
            ts.declararFuncion(nombreFun, tipoRetorno, tiposParametros, -1, -1);
            
            // Entrar al scope de la función para procesar su cuerpo
            String key = "func:" + nombreFun;
            ts.entrarFuncion(key);

            // Procesar cuerpo de la función (parámetros y bloque)
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

        // 3) BLOQUE: manejar bloques correctamente
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

        // 4) DECLARACIONES (LOCALES Y GLOBALES)
        if (lx.equals("declaracion_local") || lx.equals("declaracion_global")) {
            procesarDeclaracionConTipos(n, lx);
        }

        // 5) ASIGNACIÓN: con verificación de tipos
        if (lx.equals("asignacion") && n.hijos.size() >= 2) {
            procesarAsignacionConTipos(n);
        }

        // 6) Parámetros
        if (lx.equals("param")) {
            procesarParametro(n);
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }

        // 7) LLAMADA A FUNCIÓN: Verificar que la función existe
        if (lx.equals("llamada_funcion")) {
            procesarLlamadaFuncion(n);
            return;
        }

        // 8) IDENTIFICADORES USADOS en expresiones
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty()) {
                ts.usarIdentificador(id, -1, -1);
            }
        }

        // 9) Recorrido normal de hijos
        for (Nodo h : n.hijos)
            visitar(h);
    }

    // ==================== DECLARACIONES CON VERIFICACIÓN DE TIPOS ====================

    private void procesarDeclaracionConTipos(Nodo n, String tipoDecl) {
        String clase = tipoDecl.equals("declaracion_global") ? "global" : "local";
        String tipoVar = "desconocido";
        String id = "";
        boolean esArreglo = false;
        int dims = 0;

        // Primero encontrar el tipo y el ID
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

    // ==================== ASIGNACIONES CON VERIFICACIÓN DE TIPOS ====================

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

    // ==================== EVALUACIÓN DE EXPRESIONES (DEL PRIMER CÓDIGO) ====================

    private String evaluarTipoExpresion(Nodo n) {
        if (n == null) return "desconocido";

        String lx = safe(n.lexema);

        // LITERAL
        String lit = tipoLiteral(lx);
        if (!lit.equals("desconocido"))
            return lit;

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
            // Buscar tipo de retorno de la función
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

        return "desconocido";
    }

    private String verificarOperacionBinaria(String op, String t1, String t2) {
        if (t1.equals("desconocido") || t2.equals("desconocido"))
            return "desconocido";

        if (op.equals("+")) {
            if (t1.equals("string") && t2.equals("string"))
                return "string";

            if (t1.equals("int") && t2.equals("int"))
                return "int";

            if ((t1.equals("int") || t1.equals("float")) &&
                (t2.equals("int") || t2.equals("float")))
                return "float";
        }

        return "desconocido";
    }

    private String tipoLiteral(String lx) {
        if (lx.contains("Entero")) return "int";
        if (lx.contains("Flotante")) return "float";
        if (lx.contains("Cadena")) return "string";
        if (lx.contains("Caracter")) return "char";
        if (lx.equals("True") || lx.equals("False")) return "bool";
        return "desconocido";
    }

    // ==================== MÉTODOS PARA FUNCIONES (DEL SEGUNDO CÓDIGO) ====================

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