import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TacGenerator {

    private final TacEmitter out = new TacEmitter();
    private final TacContext ctx = new TacContext();

    // Para que locals "al inicio" no se repitan
    private final Set<String> localsEmitidos = new LinkedHashSet<>();

    public String generateAndWrite(Nodo raiz, String outputFile) {
        generate(raiz);
        String text = out.build();
        writeFile(outputFile, text);
        return text;
    }

    public String generate(Nodo raiz) {
        if (raiz == null)
            return "";
        visitPrograma(raiz);
        return out.build();
    }

    // -------------------------
    // ESCRITURA DE ARCHIVO (TacWriter dentro)
    // -------------------------
    private void writeFile(String outputFile, String content) {
        if (outputFile == null || outputFile.isBlank())
            return;
        try {
            Files.writeString(Path.of(outputFile), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
        }
    }

    // -------------------------
    // VISITAS PRINCIPALES
    // -------------------------

    private void visitPrograma(Nodo n) {
        // Recorre hijos en orden
        for (Nodo h : n.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("elementos_del_programa")) {
                visitElementosDelPrograma(h);
            } else if (lx.equals("main")) {
                visitMain(h);
            } else {
                // fallback
                visit(h);
            }
        }
    }

    private void visitElementosDelPrograma(Nodo n) {
        for (Nodo h : n.getHijos()) {
            visit(h);
        }
    }

    private void visit(Nodo n) {
        if (n == null)
            return;
        String lx = safe(n.getLexema());

        if (lx.equals("elemento_del_programa") || lx.equals("elemento") || lx.equals("lista_elementos")) {
            for (Nodo h : n.getHijos())
                visit(h);
            return;
        }

        if (lx.equals("funcion")) {
            visitFuncion(n);
            return;
        }

        if (lx.equals("main")) {
            visitMain(n);
            return;
        }

        if (lx.equals("bloque")) {
            visitBloque(n);
            return;
        }

        if (lx.equals("asignacion")) {
            visitAsignacion(n);
            return;
        }

        if (lx.equals("return")) {
            visitReturn(n);
            return;
        }

        if (lx.equals("for")) {
            visitFor(n);
            return;
        }

        if (lx.equals("loop")) {
            visitLoop(n);
            return;
        }

        if (lx.equals("decide_of")) {
            visitDecideOf(n);
            return;
        }

        if (lx.equals("show")) {
            visitShow(n);
            return;
        }

        if (lx.equals("get")) {
            visitGet(n);
            return;
        }

        if (lx.equals("break")) {
            visitBreak(n);
            return;
        }

        // default: recorrer hijos
        for (Nodo h : n.getHijos())
            visit(h);
    }

    // -------------------------
    // FUNCIONES Y MAIN
    // -------------------------

    private void visitFuncion(Nodo funcion) {
        // AST: funcion -> Gift, tipo:*, Ident(nombre), ¿, parametros?, ?, bloque
        String tipo = extraerTipoFuncion(funcion);
        String nombre = extraerNombreFuncion(funcion);

        String labelFun = "function_" + tipo + "_" + nombre;
        ctx.enterFunction(nombre);
        localsEmitidos.clear();

        out.label(labelFun);

        // Params (cabecera)
        Nodo paramsNode = findFirst(funcion, "parametros");
        if (paramsNode != null) {
            emitParametros(paramsNode);
        }

        // Locals al inicio: recolectar dentro del bloque
        Nodo bloque = findFirst(funcion, "bloque");
        if (bloque != null) {
            Map<String, String> locals = collectLocals(bloque);
            emitLocals(locals);
        }

        // Cuerpo: visitar bloque completo (asignaciones, control, return, etc.)
        if (bloque != null) {
            visitBloque(bloque);
        }

        out.label(labelFun + "_end");
        out.blank();
    }

    private void visitMain(Nodo main) {
        ctx.enterFunction("main");
        localsEmitidos.clear();

        out.label("main");

        Nodo bloque = findFirst(main, "bloque");
        if (bloque != null) {
            Map<String, String> locals = collectLocals(bloque);
            emitLocals(locals);
            visitBloque(bloque);
        }

        out.label("main_end");
        out.blank();
    }

    private void visitBloque(Nodo bloque) {
        // "bloque" contiene: ¡ ... !
        // En el AST tus hijos incluyen tokens "¡" / "!" y quizá "lista_elementos"
        for (Nodo h : bloque.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("¡") || lx.equals("!"))
                continue;
            visit(h);
        }
    }

    // -------------------------
    // CABECERAS: PARAMS Y LOCALS
    // -------------------------

    private void emitParametros(Nodo parametros) {
        // Estructura que generaste:
        // parametros -> (param) ("," param) ...
        // cada param -> tipo:* + Ident(x)
        for (Nodo h : parametros.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals(",") || lx.equals("parametros"))
                continue;

            if (lx.equals("param")) {
                String tipo = "desconocido";
                String id = "";
                for (Nodo ph : h.getHijos()) {
                    String plx = safe(ph.getLexema());
                    if (plx.startsWith("tipo:"))
                        tipo = plx.substring("tipo:".length());
                    if (plx.startsWith("Ident("))
                        id = extraerIdent(ph);
                }
                if (!id.isBlank()) {
                    out.emit("param_data_" + tipo + " " + id);
                }
            }
        }
    }

    private void emitLocals(Map<String, String> locals) {
        for (Map.Entry<String, String> e : locals.entrySet()) {
            String id = e.getKey();
            String tipo = e.getValue();
            if (localsEmitidos.contains(id))
                continue;
            localsEmitidos.add(id);
            out.emit("local_data_" + tipo + " " + id);
        }
        if (!locals.isEmpty())
            out.blank();
    }

    private Map<String, String> collectLocals(Nodo root) {
        // Recolecta "declaracion_local" en todo el subárbol
        Map<String, String> locals = new LinkedHashMap<>();
        collectLocalsRec(root, locals);
        return locals;
    }

    private void collectLocalsRec(Nodo n, Map<String, String> locals) {
        if (n == null)
            return;
        String lx = safe(n.getLexema());

        if (lx.equals("declaracion_local")) {
            String tipo = "desconocido";
            String id = "";
            for (Nodo h : n.getHijos()) {
                String hlx = safe(h.getLexema());
                if (hlx.startsWith("tipo:"))
                    tipo = hlx.substring("tipo:".length());
                if (hlx.startsWith("Ident("))
                    id = extraerIdent(h);
            }
            if (!id.isBlank()) {
                locals.putIfAbsent(id, tipo);
            }
        }

        for (Nodo h : n.getHijos())
            collectLocalsRec(h, locals);
    }

    // -------------------------
    // SENTENCIAS
    // -------------------------

    private void visitAsignacion(Nodo asig) {
        // asignacion:
        // - Ident(x) "=" expr
        // - Ident(A) arreglo "=" expr
        if (asig.getHijos().isEmpty())
            return;

        // LHS puede ser Ident o (Ident + arreglo)
        Nodo first = asig.getHijos().get(0);
        String lhsName = extraerIdent(first);

        // Buscar si hay "arreglo" como segundo hijo (para A[i][j])
        Nodo arrNode = null;
        Nodo exprNode = null;

        for (Nodo h : asig.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("arreglo"))
                arrNode = h;
        }

        // RHS es el último hijo que sea expresión (no "=", no "arreglo")
        for (int i = asig.getHijos().size() - 1; i >= 0; i--) {
            Nodo h = asig.getHijos().get(i);
            String lx = safe(h.getLexema());
            if (lx.equals("=") || lx.equals("arreglo"))
                continue;
            exprNode = h;
            break;
        }

        String rhsPlace = evalExpr(exprNode);

        if (arrNode == null) {
            // x = rhs
            out.emit(lhsName + " = " + rhsPlace);
        } else {
            // A[i][j] = rhs
            String iPlace = evalArrayIndex(arrNode, 0);
            String jPlace = evalArrayIndex(arrNode, 1);
            out.emit(lhsName + "[" + iPlace + "][" + jPlace + "] = " + rhsPlace);
        }
    }

    private void visitReturn(Nodo ret) {
        // return expr endl
        Nodo expr = null;
        for (Nodo h : ret.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("Return") || lx.equals("endl"))
                continue;
            expr = h;
            break;
        }
        String place = evalExpr(expr);
        out.emit("return " + place);
    }

    // -------------------------
    // CONTROL: FOR / LOOP / DECIDE
    // -------------------------

    private void visitFor(Nodo n) {
        ctx.forCount++;
        int k = ctx.forCount;

        String f = ctx.funcionActual;
        String base = f + "_for_" + k;

        String L_for = base;
        String L_ini = base + "_ini";
        String L_cond = base + "_cond";
        String L_bloque = base + "_bloque";
        String L_modif = base + "_modif";
        String L_end = base + "_end";

        // hijos esperados (según tu gramática):
        // "For", "¿", init, "endl", cond, "endl", update, "?", bloque
        Nodo init = findFirst(n, "init_for");
        if (init == null)
            init = findFirst(n, "init_for2");
        Nodo cond = findNthExprLike(n, 1);
        Nodo upd = findFirst(n, "update_for");
        Nodo bloque = findFirst(n, "bloque");

        out.label(L_for);
        out.label(L_ini);
        emitInitFor(init);

        out.label(L_cond);
        String condPlace = evalExpr(cond);
        out.emit("if " + condPlace + " " + L_bloque);
        out.emit("goto " + L_end);

        out.label(L_modif);
        emitUpdateFor(upd);
        out.emit("goto " + L_cond);

        out.label(L_bloque);

        // ✅ break dentro de este for debe saltar a L_end
        ctx.breakTargets.push(L_end);

        if (bloque != null)
            visitBloque(bloque);

        // ✅ salir del contexto de break de este for
        ctx.breakTargets.pop();

        out.emit("goto " + L_modif);

        out.label(L_end);
    }

    private void visitLoop(Nodo n) {
        ctx.loopCount++;
        int k = ctx.loopCount;

        String f = ctx.funcionActual;
        String base = f + "_loop_" + k;

        String L_loop = base;
        String L_body = base + "_body";
        String L_end = base + "_end";

        // loop: Loop lista_elementos Exit When expr endl End Loop endl
        Nodo lista = findFirst(n, "lista_elementos");
        Nodo cond = findNthExprLike(n, 1);

        out.label(L_loop);
        out.label(L_body);

        // ✅ break dentro de este loop debe saltar a L_end
        ctx.breakTargets.push(L_end);

        if (lista != null)
            visit(lista);

        // ✅ salir del contexto de break de este loop
        ctx.breakTargets.pop();

        String condPlace = evalExpr(cond);
        out.emit("if " + condPlace + " " + L_end);
        out.emit("goto " + L_body);

        out.label(L_end);
    }

    private void visitDecideOf(Nodo n) {
        ctx.decideCount++;
        int k = ctx.decideCount;

        String f = ctx.funcionActual;
        String base = f + "_decide_" + k;

        String L_decide = base;
        String L_end = base + "_end";
        String L_else = base + "_else";

        out.label(L_decide);

        // lista_decide_of contiene "case" repetidos
        Nodo lista = findFirst(n, "lista_decide_of");
        Nodo elseBloque = findElseBloque(n); // bloque después de Else ->

        // Recolectar cases en orden
        Nodo[] cases = findCases(lista);
        for (int i = 0; i < cases.length; i++) {
            int idx = i + 1;
            String L_cond = base + "_cond_" + idx;
            String L_bloque = base + "_bloque_" + idx;
            String L_next = (i < cases.length - 1) ? (base + "_cond_" + (idx + 1))
                    : (elseBloque != null ? L_else : L_end);

            out.label(L_cond);

            Nodo condExpr = findFirst(cases[i], null); // primer hijo del case es expr
            Nodo bloque = findFirst(cases[i], "bloque");

            // case -> (expr) "->" bloque
            // el primer hijo que no sea "->" y no sea "bloque" debe ser la expr
            condExpr = findExprInsideCase(cases[i]);

            String condPlace = evalExpr(condExpr);
            out.emit("if " + condPlace + " " + L_bloque);
            out.emit("goto " + L_next);

            out.label(L_bloque);
            if (bloque != null)
                visitBloque(bloque);
            out.emit("goto " + L_end);
        }

        if (elseBloque != null) {
            out.label(L_else);
            visitBloque(elseBloque);
            out.emit("goto " + L_end);
        }

        out.label(L_end);
    }

    // -------------------------
    // EXPRESIONES (devuelven "place")
    // -------------------------

    private String evalExpr(Nodo expr) {
        if (expr == null) {
            String t = ctx.newTemp();
            out.emit(t + " = 0");
            return t;
        }

        String lx = safe(expr.getLexema());

        // Literales
        if (lx.startsWith("Entero(") || lx.startsWith("Flotante(") || lx.startsWith("Caracter(")
                || lx.startsWith("Cadena(")) {
            String t = ctx.newTemp();
            out.emit(t + " = " + literalRaw(lx));
            return t;
        }
        if (lx.equals("True") || lx.equals("False")) {
            String t = ctx.newTemp();
            out.emit(t + " = " + (lx.equals("True") ? "1" : "0"));
            return t;
        }

        // Ident(x) -> carga a temporal (estilo profe)
        if (lx.startsWith("Ident(")) {
            String id = extraerIdent(expr);
            String t = ctx.newTemp();
            out.emit(t + " = " + id);
            return t;
        }

        // op binaria
        if (lx.equals("op") || lx.equals("op_logico")) {
            Nodo opNode = expr.getHijos().isEmpty() ? null : expr.getHijos().get(0);
            Nodo e1 = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            Nodo e2 = expr.getHijos().size() > 2 ? expr.getHijos().get(2) : null;

            String op = (opNode != null) ? safe(opNode.getLexema()) : "?";

            String p1 = evalExpr(e1);
            String p2 = evalExpr(e2);

            String t = ctx.newTemp();
            out.emit(t + " = " + p1 + " " + op + " " + p2);
            return t;
        }

        // negativo (unario)
        if (lx.equals("negativo")) {
            // hijos: "-" , literal/expr
            Nodo inner = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            String p = evalExpr(inner);
            String t = ctx.newTemp();
            out.emit(t + " = -" + p);
            return t;
        }

        // negacion (Σ en fuente, normalizada a ! en TAC)
        if (lx.equals("negacion")) {
            Nodo inner = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            String p = evalExpr(inner);
            String t = ctx.newTemp();
            out.emit(t + " = !" + p);
            return t;
        }

        // pre_inc / pre_dec
        if (lx.equals("pre_inc") || lx.equals("pre_dec")) {
            // hijos: "++"/"--", Ident(x)
            Nodo idNode = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            String id = extraerIdent(idNode);

            String t1 = ctx.newTemp();
            out.emit(t1 + " = " + id);

            String t2 = ctx.newTemp();
            String op = lx.equals("pre_inc") ? "+" : "-";
            out.emit(t2 + " = " + t1 + " " + op + " 1");

            out.emit(id + " = " + t2);
            return t2;
        }

        // llamada_funcion como expresión
        if (lx.equals("llamada_funcion")) {
            return emitCallAsExpr(expr);
        }

        // parentesis
        if (lx.equals("parentesis")) {
            // hijos: "¿", expr, "?"
            Nodo inner = null;
            for (Nodo h : expr.getHijos()) {
                String hlx = safe(h.getLexema());
                if (hlx.equals("¿") || hlx.equals("?"))
                    continue;
                inner = h;
                break;
            }
            return evalExpr(inner);
        }

        // acceso_arreglo como expr
        if (lx.equals("acceso_arreglo")) {
            // hijos: Ident(A), arreglo
            Nodo idNode = expr.getHijos().size() > 0 ? expr.getHijos().get(0) : null;
            Nodo arrNode = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            String base = extraerIdent(idNode);

            String i = evalArrayIndex(arrNode, 0);
            String j = evalArrayIndex(arrNode, 1);

            String t = ctx.newTemp();
            out.emit(t + " = " + base + "[" + i + "][" + j + "]");
            return t;
        }

        // fallback: evaluar hijos y devolver último temp
        String last = null;
        for (Nodo h : expr.getHijos())
            last = evalExpr(h);
        if (last == null) {
            String t = ctx.newTemp();
            out.emit(t + " = 0");
            return t;
        }
        return last;
    }

    private String emitCallAsExpr(Nodo callNode) {
        // estructura: llamada_funcion -> Ident(f), ¿, args?, ?
        String fname = "";
        Nodo args = null;

        for (Nodo h : callNode.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.startsWith("Ident("))
                fname = extraerIdent(h);
            if (lx.equals("argumentos"))
                args = h;
        }

        int count = 0;
        if (args != null) {
            // argumentos: expr ( , expr )*
            for (Nodo h : args.getHijos()) {
                String lx = safe(h.getLexema());
                if (lx.equals(","))
                    continue;
                String a = evalExpr(h);
                out.emit("param " + a);
                count++;
            }
        }

        String t = ctx.newTemp();
        out.emit(t + " = call " + fname + "," + count);
        return t;
    }

    // -------------------------
    // SHOW / GET / BREAK
    // -------------------------

    private void visitShow(Nodo n) {
        // show -> "Show" "¿" expr "?" "endl"
        Nodo expr = null;
        for (Nodo h : n.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("Show") || lx.equals("¿") || lx.equals("?") || lx.equals("endl"))
                continue;
            expr = h;
            break;
        }

        String p = evalExpr(expr);
        out.emit("print " + p);
    }

    private void visitGet(Nodo n) {
        // get -> "Get" "¿" expr "?" "endl"
        Nodo expr = null;
        for (Nodo h : n.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("Get") || lx.equals("¿") || lx.equals("?") || lx.equals("endl"))
                continue;
            expr = h;
            break;
        }

        // Ideal: que sea Ident(x) o acceso_arreglo
        String elx = (expr != null) ? safe(expr.getLexema()) : "";

        if (elx.startsWith("Ident(")) {
            String id = extraerIdent(expr);
            out.emit("read " + id);
            return;
        }

        if (elx.equals("acceso_arreglo")) {
            // leer directo a arreglo: A[i][j]
            Nodo idNode = expr.getHijos().size() > 0 ? expr.getHijos().get(0) : null;
            Nodo arrNode = expr.getHijos().size() > 1 ? expr.getHijos().get(1) : null;
            String base = extraerIdent(idNode);

            String i = evalArrayIndex(arrNode, 0);
            String j = evalArrayIndex(arrNode, 1);

            out.emit("read " + base + "[" + i + "][" + j + "]");
            return;
        }

        // Si llega algo no válido, para TAC lo omitimos (o se podría emitir error
        // interno)
        // Como dijiste: semántico lo valida, entonces esto no debería ocurrir en inputs
        // correctos.
    }

    private void visitBreak(Nodo n) {
        // break -> "Break" "endl"
        if (ctx.breakTargets.isEmpty()) {
            // break fuera de ciclo: semántico debería evitarlo
            return;
        }
        out.emit("goto " + ctx.breakTargets.peek());
    }

    // -------------------------
    // HELPERS FOR / LOOP / DECIDE
    // -------------------------

    private void emitInitFor(Nodo init) {
        if (init == null)
            return;

        // init_for: tipo, Ident, "=", expr
        // init_for2: Ident, "=", expr
        String id = "";
        Nodo expr = null;

        for (Nodo h : init.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.startsWith("Ident("))
                id = extraerIdent(h);
        }

        // expr es el último hijo no token
        for (int i = init.getHijos().size() - 1; i >= 0; i--) {
            Nodo h = init.getHijos().get(i);
            String lx = safe(h.getLexema());
            if (lx.equals("=") || lx.startsWith("tipo:") || lx.equals(","))
                continue;
            expr = h;
            break;
        }

        String p = evalExpr(expr);
        out.emit(id + " = " + p);
    }

    private void emitUpdateFor(Nodo upd) {
        if (upd == null)
            return;
        // update_for: "++" Ident OR "--" Ident
        String op = "";
        String id = "";

        for (Nodo h : upd.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("++") || lx.equals("--"))
                op = lx;
            if (lx.startsWith("Ident("))
                id = extraerIdent(h);
        }

        String t1 = ctx.newTemp();
        out.emit(t1 + " = " + id);

        String t2 = ctx.newTemp();
        String sign = op.equals("++") ? "+" : "-";
        out.emit(t2 + " = " + t1 + " " + sign + " 1");

        out.emit(id + " = " + t2);
    }

    private String evalArrayIndex(Nodo arreglo, int which) {
        if (arreglo == null) {
            String t = ctx.newTemp();
            out.emit(t + " = 0");
            return t;
        }

        // arreglo AST: "[" e1 "]" "[" e2 "]" o "[]""[]"
        // Buscar expresiones dentro del arreglo:
        Nodo e1 = null, e2 = null;
        int found = 0;
        for (Nodo h : arreglo.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("[") || lx.equals("]") || lx.equals("[]"))
                continue;

            // e1, e2 son nodos expresión
            if (found == 0) {
                e1 = h;
                found++;
            } else if (found == 1) {
                e2 = h;
                found++;
            }
        }

        Nodo target = (which == 0) ? e1 : e2;
        return evalExpr(target);
    }

    private Nodo[] findCases(Nodo lista) {
        if (lista == null)
            return new Nodo[0];
        return lista.getHijos().stream()
                .filter(h -> safe(h.getLexema()).equals("case"))
                .toArray(Nodo[]::new);
    }

    private Nodo findExprInsideCase(Nodo caseNode) {
        if (caseNode == null)
            return null;
        for (Nodo h : caseNode.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("->") || lx.equals("bloque"))
                continue;
            // el primer hijo que sea expr
            if (!lx.equals("->"))
                return h;
        }
        return null;
    }

    private Nodo findElseBloque(Nodo decideNode) {
        // En tu AST: decide_of -> Decide Of lista Else -> bloque End Decide
        boolean seenElse = false;
        for (Nodo h : decideNode.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.equals("Else"))
                seenElse = true;
            if (seenElse && lx.equals("bloque"))
                return h;
        }
        return null;
    }

    private Nodo findFirst(Nodo n, String lexema) {
        if (n == null)
            return null;
        for (Nodo h : n.getHijos()) {
            if (lexema == null)
                return h;
            if (safe(h.getLexema()).equals(lexema))
                return h;
        }
        // buscar en profundidad si no está en primer nivel
        for (Nodo h : n.getHijos()) {
            Nodo r = findFirst(h, lexema);
            if (r != null)
                return r;
        }
        return null;
    }

    private Nodo findNthExprLike(Nodo n, int nth) {
        // Heurística: tomar el nth hijo que parezca expresión
        // (op/op_logico/Ident/literal/llamada/etc.)
        if (n == null)
            return null;
        int count = 0;
        for (Nodo h : n.getHijos()) {
            String lx = safe(h.getLexema());
            if (looksLikeExpr(lx)) {
                count++;
                if (count == nth)
                    return h;
            }
        }
        // fallback: buscar profundo
        for (Nodo h : n.getHijos()) {
            Nodo r = findNthExprLike(h, nth);
            if (r != null)
                return r;
        }
        return null;
    }

    private boolean looksLikeExpr(String lx) {
        if (lx == null)
            return false;
        lx = lx.trim();
        return lx.equals("op")
                || lx.equals("op_logico")
                || lx.equals("llamada_funcion")
                || lx.equals("acceso_arreglo")
                || lx.equals("parentesis")
                || lx.equals("negativo")
                || lx.equals("negacion")
                || lx.equals("pre_inc")
                || lx.equals("pre_dec")
                || lx.startsWith("Ident(")
                || lx.startsWith("Entero(")
                || lx.startsWith("Flotante(")
                || lx.startsWith("Caracter(")
                || lx.startsWith("Cadena(")
                || lx.equals("True")
                || lx.equals("False");
    }

    // -------------------------
    // EXTRACCIONES: TIPO / NOMBRE
    // -------------------------

    private String extraerNombreFuncion(Nodo funcionNode) {
        // primer Ident(...) que aparezca como hijo directo
        for (Nodo h : funcionNode.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.startsWith("Ident("))
                return extraerIdent(h);
        }
        return "anonima";
    }

    private String extraerTipoFuncion(Nodo funcionNode) {
        for (Nodo h : funcionNode.getHijos()) {
            String lx = safe(h.getLexema());
            if (lx.startsWith("tipo:"))
                return lx.substring("tipo:".length());
        }
        return "desconocido";
    }

    private String extraerIdent(Nodo n) {
        if (n == null)
            return "";
        String lx = safe(n.getLexema());
        if (lx.startsWith("Ident(") && lx.endsWith(")")) {
            return lx.substring(6, lx.length() - 1);
        }
        // Si el nodo no es Ident(...) pero tiene hijos que sí
        for (Nodo h : n.getHijos()) {
            String r = extraerIdent(h);
            if (!r.isBlank())
                return r;
        }
        return "";
    }

    private String literalRaw(String lexema) {
        // Entero(5) -> 5, Cadena("x") -> "x"
        int p = lexema.indexOf('(');
        int q = lexema.lastIndexOf(')');
        if (p >= 0 && q > p)
            return lexema.substring(p + 1, q);
        return lexema;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
