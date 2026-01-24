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

        // 1) MAIN: abrir tabla "main", recorrer, y cerrar (volver a global)
        if (lx.equals("main")) {
            ts.entrarFuncion("main");
            for (Nodo h : n.hijos)
                visitar(h);
            ts.salirFuncion();
            return;
        }

        // 2) FUNCIÓN: abrir tabla "func:nombre", recorrer, y cerrar (volver a global)
        if (lx.equals("funcion")) {
            String nombreFun = extraerNombreFuncion(n);
            String key = "func:" + nombreFun;
            ts.entrarFuncion(key);

            // recorrer todo lo interno (parametros y bloque vienen como hijos)
            for (Nodo h : n.hijos)
                visitar(h);

            ts.salirFuncion();
            return;
        }

        // 3) BLOQUE: cada "bloque" del AST abre un nuevo scope hijo
        if (lx.equals("bloque")) {
            contadorBloques++;
            ts.entrarBloque("bloque#" + contadorBloques);

            for (Nodo h : n.hijos)
                visitar(h);

            ts.salirBloque();
            return;
        }

        // 4) Parámetros: en tu AST vienen como nodo "param"
        if (lx.equals("param")) {
            procesarParametro(n);
            // igual se recorren hijos por si acaso
            for (Nodo h : n.hijos)
                visitar(h);
            return;
        }

        // 5) Declaraciones
        if (lx.equals("declaracion_local") || lx.equals("declaracion_global")) {
            procesarDeclaracion(n, lx);
        }

        // 6) Asignación: verificar uso del LHS
        if (lx.equals("asignacion")) {
            if (!n.hijos.isEmpty()) {
                String id = extraerIdentificador(n.hijos.get(0));
                if (!id.isEmpty())
                    ts.usarIdentificador(id, -1, -1);
            }
        }

        // 7) Cualquier Ident(x) en expresiones: verificar uso
        if (lx.startsWith("Ident(")) {
            String id = extraerIdentificador(n);
            if (!id.isEmpty())
                ts.usarIdentificador(id, -1, -1);
        }

        // 8) Recorrido normal
        for (Nodo h : n.hijos)
            visitar(h);
    }

    private String extraerNombreFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.hijos) {
            String id = extraerIdentificador(h);
            if (!id.isEmpty())
                return id.trim();
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
                id, tipo, "parametro", "", // clase=parametro
                -1, -1,
                false, 0, "");
        ts.declarar(s);
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

        if (id.isEmpty())
            return;

        // línea/columna reales: solo si tu Nodo las guarda (ahora no). Dejamos -1.
        Simbolo s = new Simbolo(
                id, tipo, clase, ambitoActualGuess(),
                -1, -1,
                esArreglo, dims,
                "");
        ts.declarar(s);
    }

    // Como no tenemos posiciones reales dentro del Nodo, al menos el ámbito por
    // nombre de función
    private String ambitoActualGuess() {
        // Se imprime dentro de Simbolo como @ ambito, pero aquí ya va el nombre del
        // "scope" actual en TS
        // Para simplificar: dejamos vacío y TS lo muestra por scope.
        return "";
    }

    private int contarDimensionesArreglo(Nodo arreglo) {
        // Busca "[" dentro del sub-árbol. Si hay dos pares, es 2D.
        int corchetes = contarLexema(arreglo, "[");
        if (corchetes >= 2)
            return 2;
        if (corchetes == 1)
            return 1;
        return 1; // por defecto
    }

    private int contarLexema(Nodo n, String target) {
        int c = safe(n.lexema).equals(target) ? 1 : 0;
        for (Nodo h : n.hijos)
            c += contarLexema(h, target);
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
