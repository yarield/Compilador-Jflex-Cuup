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
        recorrer(raiz);
    }

    private void recorrer(Nodo n) {
        if (n == null) return;

        String lx = safe(n.lexema);

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

            Nodo exprNodo = obtenerExpresionAsignada(n);

            if (tipoNodo != null && idNodo != null && exprNodo != null) {

                String tipoVar  = normalizarTipo(tipoNodo.lexema);
                String tipoExpr = evaluarTipo(exprNodo);

                System.out.println("[DEBUG] CHECK: " +
                        extraerIdentificador(idNodo.lexema) +
                        " : " + tipoVar + " = " + tipoExpr);

                if (!sonCompatibles(tipoVar, tipoExpr)) {
                    ts.getErrores().add(
                        "ERROR SEMÁNTICO: No se puede asignar " +
                        tipoExpr + " a " + tipoVar +
                        ". [L:" + n.getLinea() +
                        ", C:" + n.getColumna() + "]"
                    );
                }
            }
            return; // 🔑 NO recorrer hijos otra vez
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
                if (safe(h.lexema).equals("="))
                    expr = obtenerExpresionAsignada(n);
            }

            if (id != null && expr != null) {

                String nombre = extraerIdentificador(id.lexema);
                Simbolo s = ts.buscarSimbolo(nombre);

                if (s == null) {
                    ts.getErrores().add(
                        "ERROR SEMÁNTICO: Uso de '" + nombre +
                        "' sin declarar. [L:" + id.getLinea() +
                        ", C:" + id.getColumna() + "]"
                    );
                } else {
                    String tipoExpr = evaluarTipo(expr);
                    if (!sonCompatibles(s.tipo, tipoExpr)) {
                        ts.getErrores().add(
                            "ERROR SEMÁNTICO: No se puede asignar " +
                            tipoExpr + " a " + s.tipo +
                            ". [L:" + n.getLinea() +
                            ", C:" + n.getColumna() + "]"
                        );
                    }
                }
            }
            return;
        }

        for (Nodo h : n.hijos)
            recorrer(h);
    }

    /* =====================
       EVALUACIÓN DE TIPOS
       ===================== */
    private String evaluarTipo(Nodo n) {
        if (n == null) return "desconocido";

        String lx = safe(n.lexema);

        if (lx.equals("op")) {
            if (n.hijos.size() != 3) return "desconocido";

            String operador = safe(n.hijos.get(0).lexema);
            String t1 = evaluarTipo(n.hijos.get(1));
            String t2 = evaluarTipo(n.hijos.get(2));

            if (operador.equals("+")) {
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
                    String lx = safe(n.hijos.get(j).lexema);
                    if (!lx.equals("endl"))
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
