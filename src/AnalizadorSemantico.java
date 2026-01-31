import java.util.*;

public class AnalizadorSemantico {

    private final TablaSimbolos ts;

    public AnalizadorSemantico(TablaSimbolos ts) {
        this.ts = ts;
    }

    public void analizar(Nodo raiz) {
        if (raiz == null) return;

        System.out.println("\n[DEBUG] ===== ANALISIS SEMANTICO =====");
        ts.resetearScopeAGlobal();
        recorrer(raiz, false);
    }

    /**
     * @param enEncabezadoFuncion true cuando estamos recorriendo
     *        nombre de función o parámetros
     */
    private void recorrer(Nodo n, boolean enEncabezadoFuncion) {
        if (n == null) return;

        String lx = safe(n.lexema);

        /* =====================
           FUNCIÓN
           ===================== */
        if (lx.equals("funcion")) {
            // Todo hasta el bloque es encabezado
            for (Nodo h : n.hijos) {
                if (safe(h.lexema).equals("bloque")) {
                    recorrer(h, false); // cuerpo real
                } else {
                    recorrer(h, true);  // nombre + parámetros
                }
            }
            return;
        }

        /* =====================
           DECLARACIONES
           ===================== */
        if (lx.equals("declaracion_global") || lx.equals("declaracion_local")) {

            Nodo tipoNodo = null;
            Nodo idNodo   = null;

            for (Nodo h : n.hijos) {
                if (tipoNodo == null && esTipo(h.lexema))
                    tipoNodo = h;
                else if (idNodo == null && esIdentificador(h.lexema))
                    idNodo = h;
            }

            if (tipoNodo == null || idNodo == null) return;

            String nombre  = extraerIdentificador(idNodo.lexema);
            String tipoVar = normalizarTipo(tipoNodo.lexema);

            Nodo exprNodo = obtenerExpresionAsignada(n);
            if (exprNodo != null) {
                String tipoExpr = evaluarTipo(exprNodo);

                System.out.println("[DEBUG] CHECK: " +
                        nombre + " : " + tipoVar + " = " + tipoExpr);

                if (!sonCompatibles(tipoVar, tipoExpr)) {
                    ts.getErrores().add(
                        "ERROR SEMÁNTICO: No se puede asignar " +
                        tipoExpr + " a " + tipoVar +
                        ". [L:" + n.getLinea() +
                        ", C:" + n.getColumna() + "]"
                    );
                }
            }
            return;
        }

        /* =====================
           ASIGNACIONES
           ===================== */
        if (lx.equals("asignacion")) {

            Nodo id = null;
            Nodo expr = null;

            for (Nodo h : n.hijos) {
                if (id == null && esIdentificador(h.lexema))
                    id = h;
                if ("=".equals(safe(h.lexema)))
                    expr = obtenerExpresionAsignada(n);
            }

            if (id == null || expr == null) return;

            String nombre = extraerIdentificador(id.lexema);
            Simbolo s = ts.buscarSimbolo(nombre);

            if (s == null) {
                ts.getErrores().add(
                    "ERROR SEMÁNTICO: Uso de '" + nombre +
                    "' sin declarar. [L:" +
                    id.getLinea() + ", C:" +
                    id.getColumna() + "]"
                );
                return;
            }

            String tipoExpr = evaluarTipo(expr);
            if (!sonCompatibles(s.tipo, tipoExpr)) {
                ts.getErrores().add(
                    "ERROR SEMÁNTICO: No se puede asignar " +
                    tipoExpr + " a " + s.tipo +
                    ". [L:" + n.getLinea() +
                    ", C:" + n.getColumna() + "]"
                );
            }
            return;
        }

        /* =====================
           USO DE IDENTIFICADORES
           ===================== */
        if (esIdentificador(lx) && !enEncabezadoFuncion) {

            String nombre = extraerIdentificador(lx);
            Simbolo s = ts.buscarSimbolo(nombre);

            if (s == null) {
                ts.getErrores().add(
                    "ERROR SEMÁNTICO: Uso de '" + nombre +
                    "' sin declarar. [L:" +
                    n.getLinea() + ", C:" +
                    n.getColumna() + "]"
                );
            }
        }

        for (Nodo h : n.hijos)
            recorrer(h, enEncabezadoFuncion);
    }

    /* =====================
       EVALUACIÓN DE TIPOS
       ===================== */
    private String evaluarTipo(Nodo n) {
        if (n == null) return "desconocido";

        String lx = safe(n.lexema);

        if (lx.equals("op")) {
            if (n.hijos.size() != 3) return "desconocido";

            String op = safe(n.hijos.get(0).lexema);
            String t1 = evaluarTipo(n.hijos.get(1));
            String t2 = evaluarTipo(n.hijos.get(2));

            if (op.equals("+")) {
                if (t1.equals("int") && t2.equals("int")) return "int";
                if (t1.equals("float") && t2.equals("float")) return "float";
            }
            return "desconocido";
        }

        if (lx.startsWith("Entero(")) return "int";
        if (lx.startsWith("Flotante(") || lx.startsWith("Decimal(")) return "float";
        if (lx.startsWith("Cadena(")) return "string";
        if (lx.startsWith("Caracter(")) return "char";
        if (lx.startsWith("Booleano(")) return "bool";

        if (lx.equalsIgnoreCase("true") || lx.equalsIgnoreCase("false")) return "bool";
        if (lx.matches("-?\\d+")) return "int";
        if (lx.matches("-?\\d+\\.\\d+")) return "float";
        if (lx.startsWith("\"") && lx.endsWith("\"")) return "string";
        if (lx.startsWith("'") && lx.endsWith("'")) return "char";

        if (esIdentificador(lx)) {
            Simbolo s = ts.buscarSimbolo(extraerIdentificador(lx));
            if (s != null) return s.tipo;
        }

        return "desconocido";
    }

    private boolean sonCompatibles(String var, String expr) {
        return var.equals(expr);
    }

    /* =====================
       UTILIDADES
       ===================== */
    private Nodo obtenerExpresionAsignada(Nodo n) {
        for (int i = 0; i < n.hijos.size(); i++) {
            if ("=".equals(safe(n.hijos.get(i).lexema))) {
                for (int j = i + 1; j < n.hijos.size(); j++) {
                    if (!"endl".equals(safe(n.hijos.get(j).lexema)))
                        return n.hijos.get(j);
                }
            }
        }
        return null;
    }

    private boolean esTipo(String lx) {
        lx = safe(lx).toLowerCase();
        return lx.contains("int") || lx.contains("float") ||
               lx.contains("string") || lx.contains("bool") ||
               lx.contains("char");
    }

    private boolean esIdentificador(String lx) {
        return lx.startsWith("Ident(") && lx.endsWith(")");
    }

    private String extraerIdentificador(String lx) {
        return lx.substring(6, lx.length() - 1);
    }

    private String normalizarTipo(String lx) {
        lx = safe(lx).toLowerCase();
        if (lx.contains("int")) return "int";
        if (lx.contains("float")) return "float";
        if (lx.contains("string")) return "string";
        if (lx.contains("bool")) return "bool";
        if (lx.contains("char")) return "char";
        return "desconocido";
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
