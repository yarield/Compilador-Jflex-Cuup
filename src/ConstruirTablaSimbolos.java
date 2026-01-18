public class ConstruirTablaSimbolos {

    private final TablaSimbolos ts;
    private int contadorBloques = 0;

    public ConstruirTablaSimbolos(TablaSimbolos ts) {
        this.ts = ts;
    }

    public void construir(Nodo raiz) {
        ts.iniciarGlobal();
        if (raiz != null) visitar(raiz);
    }

    private void visitar(Nodo n) {
        if (n == null) return;

        String lx = safe(n.lexema);

        // Entradas “tipo profe”: tablas por función/main
        if (lx.equals("main")) {
            ts.entrarFuncion("main");
        }

        // Si en tu AST las funciones se llaman "funcion" y guardan el identificador adentro:
        if (lx.equals("funcion") || lx.equals("funciones")) {
            // solo seguimos; el nombre se detecta dentro por el Ident(...)
        }

        // Si detectamos un bloque real, abrimos scope (mejora importante)
        if (lx.equals("bloque")) {
            contadorBloques++;
            ts.entrarBloque("bloque#" + contadorBloques);
        }

        // Declaraciones (local/global) según tu AST
        if (lx.equals("declaracion_local") || lx.equals("declaracion_global")) {
            procesarDeclaracion(n, lx);
        }

        // Asignación: usualmente usa Ident(...) => verificar uso
        if (lx.equals("asignacion")) {
            // hijo 0 suele ser Ident(...)
            if (!n.hijos.isEmpty()) {
                String id = extraerIdentificador(n.hijos.get(0));
                if (!id.isEmpty()) ts.usarIdentificador(id, -1, -1);
            }
        }

        // También revisamos expresiones: si aparece Ident(...)
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty()) ts.usarIdentificador(id, -1, -1);
        }

        // Recorremos hijos
        for (Nodo h : n.hijos) {
            // Si tu AST de funciones tiene el nombre como Ident(...)
            if (lx.equals("funcion")) {
                // típico: hijo[1] = Ident(nombre)
                String posible = extraerIdentificador(h);
                if (!posible.isEmpty()) {
                    ts.entrarFuncion("func:" + posible);
                    break;
                }
            }

            visitar(h);
        }

        // Cerrar bloque
        if (lx.equals("bloque")) {
            ts.salirBloque();
        }
    }

    private void procesarDeclaracion(Nodo n, String tipoDecl) {
        String clase = tipoDecl.equals("declaracion_global") ? "global" : "local";

        // Intentamos sacar:
        // - tipo: desde hijo con lexema "tipo:int"
        // - id: desde hijo con "Ident(x)"
        String tipo = "desconocido";
        String id = "";

        boolean esArreglo = false;
        int dims = 0;

        for (Nodo h : n.hijos) {
            String lx = safe(h.lexema);

            if (lx.startsWith("tipo:")) {
                tipo = lx.substring("tipo:".length());
            }
            if (lx.startsWith("Ident(")) {
                id = extraerIdentificador(h);
            }
            if (lx.equals("arreglo")) {
                esArreglo = true;
                // tu AST anterior marcaba 1D, pero tu gramática nueva tiene 2D
                // estimamos por cantidad de corchetes que aparezcan:
                dims = contarDimensionesArreglo(h);
            }
        }

        if (id.isEmpty()) return;

        // línea/columna reales: solo si tu Nodo las guarda (ahora no). Dejamos -1.
        Simbolo s = new Simbolo(
                id, tipo, clase, ambitoActualGuess(),
                -1, -1,
                esArreglo, dims,
                ""
        );
        ts.declarar(s);
    }

    // Como no tenemos posiciones reales dentro del Nodo, al menos el ámbito por nombre de función
    private String ambitoActualGuess() {
        // Se imprime dentro de Simbolo como @ ambito, pero aquí ya va el nombre del "scope" actual en TS
        // Para simplificar: dejamos vacío y TS lo muestra por scope.
        return "";
    }

    private int contarDimensionesArreglo(Nodo arreglo) {
        // Busca "[" dentro del sub-árbol. Si hay dos pares, es 2D.
        int corchetes = contarLexema(arreglo, "[");
        if (corchetes >= 2) return 2;
        if (corchetes == 1) return 1;
        return 1; // por defecto
    }

    private int contarLexema(Nodo n, String target) {
        int c = safe(n.lexema).equals(target) ? 1 : 0;
        for (Nodo h : n.hijos) c += contarLexema(h, target);
        return c;
    }

    private String extraerIdentificador(Nodo n) {
        String lx = safe(n.lexema);
        // "Ident(x)" => x
        if (lx.startsWith("Ident(") && lx.endsWith(")")) {
            return lx.substring(6, lx.length() - 1);
        }
        // También por si viene "Identificador" o "IDENTIFIER" en otros ASTs
        return "";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
