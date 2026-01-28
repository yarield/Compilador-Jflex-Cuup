import java.util.*;

public class AnalizadorSemantico {

    private final TablaSimbolos ts;

    public AnalizadorSemantico(TablaSimbolos ts) {
        this.ts = ts;
    }

    public void analizar(Nodo raiz) {
        if (raiz == null) return;

        System.out.println("\n[DEBUG] ===== ANALISIS SEMANTICO =====");
        System.out.println("[DEBUG] SCOPE RESETEADO A GLOBAL");
        ts.resetearScopeAGlobal();

        recorrer(raiz, false);
    }

    private void recorrer(Nodo n, boolean enDeclaracion) {
        if (n == null) return;

        String lx = safe(n.lexema);

        /* =====================
           DECLARACIONES
           ===================== */

        if (lx.equals("declaracion_global") || lx.equals("declaracion_local")) {

            System.out.println("\n[DEBUG] >>> ENTRA A " + lx);
            System.out.println("[DEBUG] Hijos:");

            for (Nodo h : n.hijos) {
                System.out.println("   - " + safe(h.lexema));
            }

            Nodo tipoNodo = null;
            Nodo idNodo   = null;

            for (Nodo h : n.hijos) {
                if (tipoNodo == null && esTipo(h.lexema)) {
                    tipoNodo = h;
                    System.out.println("[DEBUG] Tipo detectado: " + h.lexema);
                } 
                else if (idNodo == null && esIdentificador(h.lexema)) {
                    idNodo = h;
                    System.out.println("[DEBUG] ID detectado: " + h.lexema);
                }
            }

            Nodo exprNodo = obtenerExpresionAsignada(n);

            if (exprNodo != null) {
                System.out.println("[DEBUG] Expresion asignada encontrada: "
                        + safe(exprNodo.lexema));
            } else {
                System.out.println("[DEBUG] NO hay expresion asignada");
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

            for (Nodo h : n.hijos) {
                recorrer(h, true);
            }
            return;
        }

        /* =====================
           USO DE IDENTIFICADORES
           ===================== */

        if (esIdentificador(lx)) {
            if (!enDeclaracion) {
                System.out.println("[DEBUG] USO de identificador: "
                        + extraerIdentificador(lx));
                ts.usarIdentificador(
                        extraerIdentificador(lx),
                        n.getLinea(),
                        n.getColumna()
                );
            }
            return;
        }

        for (Nodo h : n.hijos) {
            recorrer(h, false);
        }
    }

    /* =====================
       EVALUACIÓN DE TIPOS
       ===================== */

    private String evaluarTipo(Nodo n) {
        if (n == null) {
            System.out.println("[DEBUG][TIPO] Nodo null → desconocido");
            return "desconocido";
        }

        String lx = safe(n.lexema);
        System.out.println("[DEBUG][TIPO] Evaluando nodo: " + lx);

        /* === LITERALES DEL AST === */

        if (lx.startsWith("Entero(") && lx.endsWith(")")) {
            System.out.println("[DEBUG][TIPO] Literal Entero detectado");
            return "int";
        }

        if (lx.startsWith("Decimal(") && lx.endsWith(")")) {
            System.out.println("[DEBUG][TIPO] Literal Decimal detectado");
            return "float";
        }

        if (lx.startsWith("Cadena(") && lx.endsWith(")")) {
            System.out.println("[DEBUG][TIPO] Literal Cadena detectado");
            return "string";
        }

        if (lx.startsWith("Caracter(") && lx.endsWith(")")) {
            System.out.println("[DEBUG][TIPO] Literal Caracter detectado");
            return "char";
        }

        if (lx.startsWith("Booleano(") && lx.endsWith(")")) {
            System.out.println("[DEBUG][TIPO] Literal Booleano detectado");
            return "bool";
        }


        /* === LITERALES PLANOS (backup) === */

        if (lx.matches("-?\\d+")) return "int";
        if (lx.matches("-?\\d+\\.\\d+")) return "float";
        if (lx.startsWith("\"") && lx.endsWith("\"")) return "string";
        if (lx.startsWith("'") && lx.endsWith("'")) return "char";
        if (lx.equals("true") || lx.equals("false")) return "bool";

        /* === IDENTIFICADORES === */

        if (esIdentificador(lx)) {
            String id = extraerIdentificador(lx);
            Simbolo s = ts.buscarSimbolo(id);
            if (s != null) {
                System.out.println("[DEBUG][TIPO] Tipo de ID " + id + " = " + s.tipo);
                return s.tipo;
            } else {
                System.out.println("[DEBUG][TIPO] ID no encontrado: " + id);
            }
        }

        /* === RECORRIDO === */

        for (Nodo h : n.hijos) {
            String t = evaluarTipo(h);
            if (!t.equals("desconocido")) return t;
        }

        System.out.println("[DEBUG][TIPO] No se pudo inferir tipo → desconocido");
        return "desconocido";
    }

    private boolean sonCompatibles(String var, String expr) {
        if (var.equals(expr)) return true;
        if (var.equals("float") && expr.equals("int")) return true;
        return false;
    }

    /* =====================
       UTILIDADES
       ===================== */

    private Nodo obtenerExpresionAsignada(Nodo n) {
        System.out.println("[DEBUG] Buscando '=' en declaracion");

        for (int i = 0; i < n.hijos.size(); i++) {
            System.out.println("   hijo[" + i + "] = " + safe(n.hijos.get(i).lexema));
            if ("=".equals(n.hijos.get(i).lexema)) {
                System.out.println("[DEBUG] '=' encontrado en posicion " + i);
                if (i + 1 < n.hijos.size()) {
                    return n.hijos.get(i + 1);
                }
            }
        }
        return null;
    }

    private boolean esTipo(String lx) {
        lx = safe(lx).toLowerCase();
        return lx.contains("int") ||
               lx.contains("float") ||
               lx.contains("string") ||
               lx.contains("bool") ||
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
        lx = safe(lx).toLowerCase();
        if (lx.contains("int")) return "int";
        if (lx.contains("float")) return "float";
        if (lx.contains("string")) return "string";
        if (lx.contains("bool")) return "bool";
        if (lx.contains("char")) return "char";
        return "desconocido";
    }
}
