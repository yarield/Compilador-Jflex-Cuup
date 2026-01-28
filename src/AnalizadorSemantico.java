import java.util.*;

public class AnalizadorSemantico {

    private final TablaSimbolos ts;

    public AnalizadorSemantico(TablaSimbolos ts) {
        this.ts = ts;
    }

    public void analizar(Nodo raiz) {
        if (raiz == null) return;

        System.out.println("[DEBUG] SCOPE RESETEADO A GLOBAL");
        ts.resetearScopeAGlobal();

        recorrer(raiz, false);
    }

    private void recorrer(Nodo n, boolean enDeclaracion) {
        if (n == null) return;

        String lx = safe(n.lexema);

        /* =========================
           DECLARACIONES
           ========================= */

        if (lx.equals("declaracion_global") || lx.equals("declaracion_local")) {

            Nodo tipoNodo = null;
            Nodo idNodo   = null;
            Nodo exprNodo = null;

            for (Nodo h : n.hijos) {
                if (tipoNodo == null && esTipo(h.lexema))
                    tipoNodo = h;
                else if (idNodo == null && esIdentificador(h.lexema))
                    idNodo = h;
                else if (contieneAsignacion(h))
                    exprNodo = h;
            }

            if (tipoNodo != null && idNodo != null && exprNodo != null) {

                String tipoVar  = normalizarTipo(tipoNodo.lexema);
                String tipoExpr = evaluarTipo(exprNodo);

                System.out.println("[DEBUG] CHECK ASIGNACION: " +
                        extraerIdentificador(idNodo.lexema) +
                        " : " + tipoVar + " = " + tipoExpr);

                if (!sonCompatibles(tipoVar, tipoExpr)) {
                    ts.getErrores().add(
                        "ERROR SEMÁNTICO: No se puede asignar " + tipoExpr +
                        " a " + tipoVar +
                        ". [L:" + n.getLinea() + ", C:" + n.getColumna() + "]"
                    );
                }
            }

            for (Nodo h : n.hijos)
                recorrer(h, true);

            return;
        }

        /* =========================
           USO DE IDENTIFICADORES
           ========================= */

        if (esIdentificador(lx)) {
            if (!enDeclaracion) {
                ts.usarIdentificador(
                        extraerIdentificador(lx),
                        n.getLinea(),
                        n.getColumna()
                );
            }
            return;
        }

        for (Nodo h : n.hijos)
            recorrer(h, false);
    }

    /* =========================
       EVALUACIÓN DE TIPOS
       ========================= */

    private String evaluarTipo(Nodo n) {
        if (n == null) return "desconocido";

        String lx = n.lexema;

        if (lx.matches("-?\\d+")) return "int";
        if (lx.matches("-?\\d+\\.\\d+")) return "float";
        if (lx.startsWith("\"") && lx.endsWith("\"")) return "string";
        if (lx.startsWith("'") && lx.endsWith("'")) return "char";
        if (lx.equals("true") || lx.equals("false")) return "bool";

        if (esIdentificador(lx)) {
            Simbolo s = ts.buscarSimbolo(extraerIdentificador(lx));
            if (s != null) return s.tipo;
        }

        for (Nodo h : n.hijos) {
            String t = evaluarTipo(h);
            if (!t.equals("desconocido")) return t;
        }

        return "desconocido";
    }

    private boolean sonCompatibles(String var, String expr) {
        if (var.equals(expr)) return true;
        if (var.equals("float") && expr.equals("int")) return true;
        return false;
    }

    /* =========================
       UTILIDADES
       ========================= */

    private boolean contieneAsignacion(Nodo n) {
        if (n.lexema != null && n.lexema.equals("=")) return true;
        for (Nodo h : n.hijos)
            if (contieneAsignacion(h)) return true;
        return false;
    }

    private boolean esTipo(String lx) {
        lx = lx.toLowerCase();
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

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private String normalizarTipo(String lx) {
        lx = lx.toLowerCase();
        if (lx.contains("int")) return "int";
        if (lx.contains("float")) return "float";
        if (lx.contains("string")) return "string";
        if (lx.contains("bool")) return "bool";
        if (lx.contains("char")) return "char";
        return "desconocido";
    }
}
