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

        System.out.println("[DEBUG VISITAR] Nodo: " + lx + " (hijos: " + n.hijos.size() + ")");

        // 1) MAIN: manejar main correctamente
        if (lx.equals("main")) {
            System.out.println("[DEBUG] Procesando main");
            ts.entrarFuncion("main");
            
            for (Nodo h : n.hijos) {
                // Si hay un bloque dentro del main, procesar sus hijos directamente
                if (safe(h.lexema).equals("bloque")) {
                    System.out.println("[DEBUG] Encontrado bloque dentro de main, procesando hijos directamente");
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
            
            System.out.println("[DEBUG] Declarando función: " + nombreFun + 
                             " -> retorno: " + tipoRetorno + 
                             ", params: " + tiposParametros);
            
            // 1. Declarar la función en el ámbito global
            ts.declararFuncion(nombreFun, tipoRetorno, tiposParametros, -1, -1);
            
            // 2. Entrar al scope de la función para procesar su cuerpo
            String key = "func:" + nombreFun;
            ts.entrarFuncion(key);

            // Procesar cuerpo de la función (parámetros y bloque)
            for (Nodo h : n.hijos) {
                String hijoLex = safe(h.lexema);
                // No procesar Gift, tipo ni nombre de función como identificadores
                if (!hijoLex.equals("Gift") && 
                    !hijoLex.startsWith("tipo:") &&
                    !(hijoLex.startsWith("Ident(") && 
                      extraerIdentificador(h).equals(nombreFun))) {
                    visitar(h);
                } else {
                    System.out.println("[DEBUG] Saltando: " + hijoLex);
                }
            }

            ts.salirFuncion();
            return;
        }

        // 3) BLOQUE: manejar bloques correctamente
        if (lx.equals("bloque")) {
            // Solo crear bloque si no estamos ya en una situación especial
            if (!ts.getScopeActual().nombre.startsWith("bloque#")) {
                contadorBloques++;
                String nombreBloque = "bloque#" + contadorBloques;
                System.out.println("[DEBUG] Creando nuevo bloque: " + nombreBloque);
                ts.entrarBloque(nombreBloque);

                for (Nodo h : n.hijos)
                    visitar(h);

                ts.salirBloque();
            }
            return;
        }

        // 4) Parámetros
        if (lx.equals("param")) {
            procesarParametro(n);
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }

        // 5) Declaraciones
        if (lx.equals("declaracion_local") || lx.equals("declaracion_global")) {
            procesarDeclaracion(n, lx);
            // IMPORTANTE: No hacer return aquí, seguir procesando hijos
        }

        // 6) Asignación: verificar uso del LHS y procesar RHS
        if (lx.equals("asignacion")) {
            System.out.println("[DEBUG] Procesando asignación");
            if (!n.hijos.isEmpty()) {
                // Procesar LHS
                String id = extraerIdentificador(n.hijos.get(0));
                if (!id.isEmpty()) {
                    System.out.println("[DEBUG] LHS de asignación: " + id);
                    ts.usarIdentificador(id, -1, -1);
                }
                
                // Procesar RHS (expresión)
                if (n.hijos.size() > 1) {
                    System.out.println("[DEBUG] Procesando RHS de asignación");
                    procesarExpresionCompleta(n.hijos.get(1));
                }
            }
        }

        // 7) LLAMADA A FUNCIÓN: Verificar que la función existe
        if (lx.equals("llamada_funcion")) {
            procesarLlamadaFuncion(n);
            return;
        }

        // 8) Cualquier Ident(x) en expresiones
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty()) {
                System.out.println("[DEBUG] Identificador encontrado: " + id);
                ts.usarIdentificador(id, -1, -1);
            }
        }

        // 9) Recorrido normal de hijos
        for (Nodo h : n.hijos)
            visitar(h);
    }

    // ==================== MÉTODOS PARA FUNCIONES ====================

    private String extraerTipoRetornoFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.hijos) {
            String lx = safe(h.lexema);
            if (lx.startsWith("tipo:")) {
                String tipo = lx.substring("tipo:".length());
                System.out.println("[DEBUG] Tipo de retorno extraído: " + tipo);
                return tipo;
            }
        }
        return "void";
    }

    private List<String> extraerTiposParametrosFuncion(Nodo funcionNode) {
        List<String> tipos = new ArrayList<>();
        
        // Buscar nodo de parámetros
        for (Nodo h : funcionNode.hijos) {
            if (safe(h.lexema).equals("parametros")) {
                // Procesar cada parámetro
                for (Nodo param : h.hijos) {
                    if (safe(param.lexema).equals("param")) {
                        String tipoParam = extraerTipoParametro(param);
                        if (!tipoParam.isEmpty()) {
                            tipos.add(tipoParam);
                        }
                    } else if (safe(param.lexema).equals(",")) {
                        // Ignorar comas
                        continue;
                    }
                }
                break;
            }
        }
        
        System.out.println("[DEBUG] Tipos de parámetros extraídos: " + tipos);
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
        
        System.out.println("[DEBUG] Procesando llamada a función");
        
        for (Nodo h : llamadaNode.hijos) {
            String lx = safe(h.lexema);
            
            if (lx.startsWith("Ident(")) {
                nombreFuncion = extraerIdentificador(h);
                System.out.println("[DEBUG] Nombre de función llamada: " + nombreFuncion);
            } else if (lx.equals("argumentos") || lx.equals("expresion")) {
                // Procesar argumentos y determinar sus tipos
                List<String> tipos = extraerTiposArgumentos(h);
                tiposArgumentos.addAll(tipos);
            }
        }
        
        if (!nombreFuncion.isEmpty()) {
            System.out.println("[DEBUG] Verificando llamada a función: " + nombreFuncion + 
                             " con argumentos: " + tiposArgumentos);
            ts.verificarLlamadaFuncion(nombreFuncion, tiposArgumentos, -1, -1);
        }
    }

    private List<String> extraerTiposArgumentos(Nodo argumentosNode) {
        List<String> tipos = new ArrayList<>();
        
        // Este método necesita ser implementado según tu estructura de AST
        // Por ahora, asumiremos que todos los argumentos son del tipo "desconocido"
        // o implementaremos una lógica básica
        
        for (Nodo h : argumentosNode.hijos) {
            String lx = safe(h.lexema);
            
            if (lx.startsWith("Ident(")) {
                // Si es un identificador, buscar su tipo en la tabla
                String id = extraerIdentificador(h);
                Simbolo s = ts.buscarSimbolo(id);
                if (s != null) {
                    tipos.add(s.tipo);
                } else {
                    tipos.add("desconocido");
                }
            } else if (lx.contains("Entero")) {
                tipos.add("int");
            } else if (lx.contains("Flotante")) {
                tipos.add("float");
            } else if (lx.contains("Cadena")) {
                tipos.add("string");
            } else if (lx.contains("Caracter")) {
                tipos.add("char");
            } else if (lx.equals("True") || lx.equals("False")) {
                tipos.add("bool");
            }
            // Si el nodo tiene hijos, procesarlos recursivamente
            else if (!h.hijos.isEmpty()) {
                tipos.addAll(extraerTiposArgumentos(h));
            }
        }
        
        return tipos;
    }

    // ==================== MÉTODOS EXISTENTES ====================

    // NUEVO MÉTODO: Procesar expresiones completas
    private void procesarExpresionCompleta(Nodo expr) {
        if (expr == null) return;
        
        String lx = safe(expr.lexema);
        
        System.out.println("[DEBUG EXPR] Procesando expresión: " + lx);
        
        // Identificador en expresión
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(expr);
            if (!id.isEmpty()) {
                System.out.println("[DEBUG EXPR] Identificador en expresión: " + id);
                ts.usarIdentificador(id, -1, -1);
            }
        }
        
        // Llamada a función en expresión
        if (lx.equals("llamada_funcion")) {
            procesarLlamadaFuncion(expr);
        }
        
        // Recursivamente procesar hijos
        for (Nodo h : expr.hijos) {
            procesarExpresionCompleta(h);
        }
    }

    private String extraerNombreFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.hijos) {
            String id = extraerIdentificador(h);
            if (!id.isEmpty()) {
                System.out.println("[DEBUG] Nombre de función extraído: " + id);
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

        System.out.println("[DEBUG] Declarando parámetro: " + id + " tipo: " + tipo);
        
        Simbolo s = new Simbolo(
                id, tipo, "parametro", ts.getScopeActual().nombre,
                -1, -1,
                false, 0, "");
        ts.declarar(s);
    }

    private void procesarDeclaracion(Nodo n, String tipoDecl) {
        System.out.println("[DEBUG] Procesando declaración: " + tipoDecl);
        
        String clase = tipoDecl.equals("declaracion_global") ? "global" : "local";
        String tipo = "desconocido";
        String id = "";
        boolean esArreglo = false;
        int dims = 0;

        // Buscar en orden específico: primero el tipo, luego el ID
        boolean encontroTipo = false;
        
        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);

            if (lx.startsWith("tipo:")) {
                tipo = lx.substring("tipo:".length());
                System.out.println("[DEBUG] Tipo encontrado: " + tipo);
                encontroTipo = true;
            }
            
            // Solo tomar como ID si ya encontramos el tipo
            if (encontroTipo && lx.startsWith("Ident(")) {
                id = extraerIdentificador(h);
                System.out.println("[DEBUG] ID encontrado: " + id);
                break; // Solo el primer ID después del tipo
            }
            
            if (lx.equals("arreglo")) {
                esArreglo = true;
                dims = contarDimensionesArreglo(h);
            }
        }

        if (id.isEmpty()) {
            System.out.println("[DEBUG] ID vacío, omitiendo declaración");
            return;
        }

        // Declarar el símbolo
        String ambitoActual = ts.getScopeActual().nombre;
        System.out.println("[DEBUG] Declarando símbolo: " + id + " en ámbito: " + ambitoActual);
        
        Simbolo s = new Simbolo(
                id, tipo, clase, ambitoActual,
                -1, -1,
                esArreglo, dims, "");
        ts.declarar(s);
        
        // Buscar y procesar expresión de inicialización
        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);
            if (lx.equals("expresion") || lx.equals("=") || lx.equals("asignacion_simple")) {
                System.out.println("[DEBUG] Procesando expresión de inicialización para: " + id);
                procesarExpresionCompleta(h);
            }
        }
    }

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
            String id = lx.substring(6, lx.length() - 1);
            System.out.println("[DEBUG] Extraído identificador: " + id);
            return id;
        }
        return "";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}