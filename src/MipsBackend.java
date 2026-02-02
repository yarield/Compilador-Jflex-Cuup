import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class MipsBackend {

    public static void compile(Path tacPath, Path asmPath) throws IOException {
    TacParser.Program prog = TacParser.parse(tacPath);

    MipsEmitter out = new MipsEmitter();
    StringPool strPool = new StringPool();
    FloatPool fltPool = new FloatPool();

    Set<String> globalScalars = new LinkedHashSet<>();
    Map<String, TacParser.Type> globalTypes = new LinkedHashMap<>();
    Map<String, TacParser.Operand> globalInitValues = new LinkedHashMap<>();

    Map<String, String> funcMap = prog.simpleToFullFuncLabel;

    // Recolectar globales de funciones (para variables globales usadas en funciones)
    for (TacParser.Function fn : prog.functions) {
        for (TacParser.Instr ins : fn.instrs) {
            collectConstsAndGlobals(fn, ins, strPool, fltPool, globalScalars, globalTypes);
        }
    }
    
    // NUEVO: Procesar asignaciones globales del parser
    for (TacParser.GlobalAssignment ga : prog.globalAssignments) {
        String varName = ga.varName;
        globalScalars.add(varName);
        globalTypes.put(varName, ga.type);
        globalInitValues.put(varName, ga.value);
    }

    out.emit(".data");
    out.emit("nl: .asciiz \"\\n\"");
    out.blank();

    // Emitir globales con sus valores de inicialización
    for (String g : globalScalars) {
        TacParser.Type t = globalTypes.getOrDefault(g, TacParser.Type.INT);
        TacParser.Operand initValue = globalInitValues.get(g);
        
        if (initValue != null) {
            // Hay valor de inicialización
            if (initValue instanceof TacParser.OInt) {
                int val = ((TacParser.OInt) initValue).value;
                out.emit(g + ": .word " + val);
            } else if (initValue instanceof TacParser.OFloat) {
                String floatVal = ((TacParser.OFloat) initValue).raw;
                out.emit(g + ": .float " + floatVal);
            } else if (initValue instanceof TacParser.OString) {
                // Para strings, necesitamos manejar de manera especial
                String strVal = ((TacParser.OString) initValue).rawWithQuotes;
                String label = strPool.labelFor(strVal);
                out.emit(g + ": .word " + label);
            } else {
                // Por defecto
                emitDefaultGlobal(out, g, t);
            }
        } else {
            // Sin inicialización
            emitDefaultGlobal(out, g, t);
        }
    }

    for (Map.Entry<String, String> e : strPool.entries().entrySet()) {
        String raw = e.getKey();
        String label = e.getValue();
        String unesc = StringPool.unescapeTacString(raw);
        out.emit(label + ": .asciiz " + StringPool.toAsciizLiteral(unesc));
    }

    for (Map.Entry<String, String> e : fltPool.entries().entrySet()) {
        out.emit(e.getValue() + ": .float " + e.getKey());
    }

    out.blank();

    out.emit(".text");
    out.emit(".globl main");
    out.blank();

    // Si hay inicializaciones complejas que necesiten código, agregar aquí
    boolean needsRuntimeInit = false;
    for (Map.Entry<String, TacParser.Operand> entry : globalInitValues.entrySet()) {
        TacParser.Operand value = entry.getValue();
        if (value instanceof TacParser.OVar) {
            needsRuntimeInit = true;
            break;
        }
    }
    
    if (needsRuntimeInit) {
        out.label("init_globals:");
        for (Map.Entry<String, TacParser.Operand> entry : globalInitValues.entrySet()) {
            String var = entry.getKey();
            TacParser.Operand value = entry.getValue();
            
            if (value instanceof TacParser.OVar) {
                String srcVar = ((TacParser.OVar) value).name;
                out.emitIndented("lw $t0, " + srcVar);
                out.emitIndented("sw $t0, " + var);
            }
        }
        out.emitIndented("jr $ra");
        out.blank();
    }

    for (TacParser.Function fn : prog.functions) {
        compileFunction(out, fn, strPool, fltPool, funcMap);
        out.blank();
    }

    out.writeTo(asmPath);
}

    private static void emitDefaultGlobal(MipsEmitter out, String name, TacParser.Type t) {
        switch (t) {
            case FLOAT:
                out.emit(name + ": .float 0.0");
                break;
            case STRING:
                out.emit(name + ": .word 0");
                break;
            default:
                out.emit(name + ": .word 0");
                break;
        }
    }
    private static void collectConstsAndGlobals(
            TacParser.Function fn,
            TacParser.Instr ins,
            StringPool strPool,
            FloatPool fltPool,
            Set<String> globals,
            Map<String, TacParser.Type> globalTypes) {

        List<TacParser.Operand> ops = new ArrayList<>();

        if (ins instanceof TacParser.IAssign) {
            TacParser.IAssign a = (TacParser.IAssign) ins;
            ops.add(a.src);
            maybeGlobal(fn, a.dst, globals, globalTypes);
        } else if (ins instanceof TacParser.IUn) {
            TacParser.IUn u = (TacParser.IUn) ins;
            ops.add(u.a);
            maybeGlobal(fn, u.dst, globals, globalTypes);
        } else if (ins instanceof TacParser.IBin) {
            TacParser.IBin b = (TacParser.IBin) ins;
            ops.add(b.a);
            ops.add(b.b);
            maybeGlobal(fn, b.dst, globals, globalTypes);
        } else if (ins instanceof TacParser.IIf) {
            ops.add(((TacParser.IIf) ins).cond);
        } else if (ins instanceof TacParser.IParam) {
            ops.add(((TacParser.IParam) ins).value);
        } else if (ins instanceof TacParser.ICall) {
            maybeGlobal(fn, ((TacParser.ICall) ins).dst, globals, globalTypes);
        } else if (ins instanceof TacParser.IReturn) {
            ops.add(((TacParser.IReturn) ins).value);
        } else if (ins instanceof TacParser.IPrint) {
            ops.add(((TacParser.IPrint) ins).value);
        } else if (ins instanceof TacParser.IRead) {
            maybeGlobal(fn, ((TacParser.IRead) ins).target, globals, globalTypes);
        }

        for (TacParser.Operand op : ops) {
            if (op == null)
                continue;

            if (op instanceof TacParser.OString) {
                strPool.labelFor(((TacParser.OString) op).rawWithQuotes);
            } else if (op instanceof TacParser.OFloat) {
                fltPool.labelFor(((TacParser.OFloat) op).raw);
            } else if (op instanceof TacParser.OVar) {
                maybeGlobal(fn, ((TacParser.OVar) op).name, globals, globalTypes);
            }
        }
    }

    private static void maybeGlobal(
            TacParser.Function fn,
            String name,
            Set<String> globals,
            Map<String, TacParser.Type> globalTypes) {

        if (name == null)
            return;

        if (fn.locals.containsKey(name))
            return;
        if (fn.paramsInOrder.containsKey(name))
            return;
        if (fn.temps.contains(name))
            return;

        globals.add(name);
        globalTypes.putIfAbsent(name, fn.typeEnv.getOrDefault(name, TacParser.Type.INT));
    }

    private static void compileFunction(
            MipsEmitter out,
            TacParser.Function fn,
            StringPool strPool,
            FloatPool fltPool,
            Map<String, String> funcMap) {

        String[] params = fn.paramsInOrder.keySet().toArray(new String[0]);
        FrameLayout frame = new FrameLayout(fn.locals.keySet(), fn.temps, params);

        out.label(fn.label);

        out.emitIndented("addiu $sp, $sp, -" + frame.frameSize());
        out.emitIndented("sw $ra, " + (frame.frameSize() - 4) + "($sp)");
        out.emitIndented("sw $fp, " + (frame.frameSize() - 8) + "($sp)");
        out.emitIndented("addiu $fp, $sp, " + frame.frameSize());
        out.blank();

        boolean epilogEmitted = false;

        for (TacParser.Instr ins : fn.instrs) {

            if (ins instanceof TacParser.ILabel) {
                String lbl = ((TacParser.ILabel) ins).name;

                if (lbl.equals(fn.label))
                    continue;

                if (lbl.equals(fn.endLabel)) {
                    out.label(lbl);
                    // Epilog (recupera usando $fp como base)
                    out.emitIndented("lw $ra, " + frame.savedRaOffset() + "($fp)");
                    out.emitIndented("lw $fp, " + frame.savedFpOffset() + "($fp)");
                    out.emitIndented("addiu $sp, $sp, " + frame.frameSize());
                    out.emitIndented("jr $ra");
                    epilogEmitted = true;
                    continue;
                }

                out.label(lbl);
                continue;
            }

            if (ins instanceof TacParser.IGoto) {
                out.emitIndented("j " + ((TacParser.IGoto) ins).target);
                continue;
            }

            if (ins instanceof TacParser.IIf) {
                TacParser.IIf i = (TacParser.IIf) ins;
                loadOperandToIntReg(out, fn, frame, strPool, fltPool, i.cond, "$t0");
                out.emitIndented("bne $t0, $zero, " + i.target);
                continue;
            }

            if (ins instanceof TacParser.IAssign) {
                TacParser.IAssign a = (TacParser.IAssign) ins;
                TacParser.Type td = fn.typeEnv.getOrDefault(a.dst, TacParser.Type.INT);
                emitAssign(out, fn, frame, strPool, fltPool, a.dst, a.src, td);
                continue;
            }

            if (ins instanceof TacParser.IUn) {
                emitUn(out, fn, frame, strPool, fltPool, (TacParser.IUn) ins);
                continue;
            }

            if (ins instanceof TacParser.IBin) {
                emitBin(out, fn, frame, strPool, fltPool, (TacParser.IBin) ins);
                continue;
            }

            if (ins instanceof TacParser.IParam) {
                TacParser.Operand v = ((TacParser.IParam) ins).value;
                TacParser.Type tv = typeOfOperand(fn, v);

                if (tv == TacParser.Type.FLOAT) {
                    loadOperandToFloatReg(out, fn, frame, strPool, fltPool, v, "$f0");
                    out.emitIndented("addiu $sp, $sp, -4");
                    out.emitIndented("s.s $f0, 0($sp)");
                } else {
                    loadOperandToIntReg(out, fn, frame, strPool, fltPool, v, "$t0");
                    out.emitIndented("addiu $sp, $sp, -4");
                    out.emitIndented("sw $t0, 0($sp)");
                }
                continue;
            }

            if (ins instanceof TacParser.ICall) {
                TacParser.ICall c = (TacParser.ICall) ins;

                String callee = funcMap.getOrDefault(c.funcSimple, c.funcSimple);
                out.emitIndented("jal " + callee);

                int bytes = 4 * c.nArgs;
                if (bytes != 0)
                    out.emitIndented("addiu $sp, $sp, " + bytes);

                TacParser.Type td = fn.typeEnv.getOrDefault(c.dst, TacParser.Type.UNKNOWN);
                if (td == TacParser.Type.FLOAT) {
                    storeFloatRegToVar(out, fn, frame, c.dst, "$f0");
                } else {
                    storeIntRegToVar(out, fn, frame, c.dst, "$v0");
                }
                continue;
            }

            if (ins instanceof TacParser.IPrint) {
                emitPrint(out, fn, frame, strPool, fltPool, ((TacParser.IPrint) ins).value);
                continue;
            }

            if (ins instanceof TacParser.IRead) {
                emitRead(out, fn, frame, ((TacParser.IRead) ins).target);
                continue;
            }

            if (ins instanceof TacParser.IReturn) {
                TacParser.IReturn r = (TacParser.IReturn) ins;
                if (r.value != null) {
                    TacParser.Type tv = typeOfOperand(fn, r.value);
                    if (tv == TacParser.Type.FLOAT) {
                        loadOperandToFloatReg(out, fn, frame, strPool, fltPool, r.value, "$f0");
                    } else {
                        loadOperandToIntReg(out, fn, frame, strPool, fltPool, r.value, "$v0");
                    }
                }
                out.emitIndented("j " + fn.endLabel);
                continue;
            }

            throw new IllegalStateException("Instrucción TAC no soportada: " + ins.getClass().getName());
        }

        if (!epilogEmitted) {
            out.label(fn.endLabel);
            out.emitIndented("move $sp, $fp");
            out.emitIndented("lw $fp, " + frame.savedFpOffset() + "($sp)");
            out.emitIndented("lw $ra, " + frame.savedRaOffset() + "($sp)");
            out.emitIndented("addiu $sp, $sp, " + frame.frameSize());
            out.emitIndented("jr $ra");
        }
    }

    private static void emitUn(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.IUn u) {

        if (u.op.equals("!") || u.op.equals("Σ")) {
            loadOperandToIntReg(out, fn, frame, strPool, fltPool, u.a, "$t0");
            out.emitIndented("sltu $t1, $zero, $t0"); // t1 = (a != 0)
            out.emitIndented("xori $t2, $t1, 1"); // t2 = !t1
            storeIntRegToVar(out, fn, frame, u.dst, "$t2");
            return;
        }

        if (u.op.equals("-")) {
            TacParser.Type td = fn.typeEnv.getOrDefault(u.dst, TacParser.Type.INT);
            if (td == TacParser.Type.FLOAT) {
                loadOperandToFloatReg(out, fn, frame, strPool, fltPool, u.a, "$f0");
                out.emitIndented("neg.s $f1, $f0");
                storeFloatRegToVar(out, fn, frame, u.dst, "$f1");
            } else {
                loadOperandToIntReg(out, fn, frame, strPool, fltPool, u.a, "$t0");
                out.emitIndented("subu $t2, $zero, $t0");
                storeIntRegToVar(out, fn, frame, u.dst, "$t2");
            }
            return;
        }

        throw new UnsupportedOperationException("Unario no soportado: " + u.op);
    }



    private static void emitAssign(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            String dst,
            TacParser.Operand src,
            TacParser.Type td) {

        if (td == TacParser.Type.FLOAT) {
            loadOperandToFloatReg(out, fn, frame, strPool, fltPool, src, "$f0");
            storeFloatRegToVar(out, fn, frame, dst, "$f0");
            return;
        }

        loadOperandToIntReg(out, fn, frame, strPool, fltPool, src, "$t0");
        storeIntRegToVar(out, fn, frame, dst, "$t0");
    }

    private static void emitBin(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.IBin b) {

        boolean isCmp = b.op.equals("<") || b.op.equals("<=") || b.op.equals(">") || b.op.equals(">=")
                || b.op.equals("==") || b.op.equals("!=");

        TacParser.Type ta = typeOfOperand(fn, b.a);
        TacParser.Type tb = typeOfOperand(fn, b.b);
        boolean floatOp = (ta == TacParser.Type.FLOAT) || (tb == TacParser.Type.FLOAT);

        if (isCmp && floatOp) {
            emitFloatCompareToBool(out, fn, frame, strPool, fltPool, b);
            return;
        }

        if (!isCmp && floatOp) {
            emitFloatArith(out, fn, frame, strPool, fltPool, b);
            return;
        }

        loadOperandToIntReg(out, fn, frame, strPool, fltPool, b.a, "$t0");
        loadOperandToIntReg(out, fn, frame, strPool, fltPool, b.b, "$t1");

        switch (b.op) {
            case "+":
                out.emitIndented("addu $t2, $t0, $t1");
                break;
            case "-":
                out.emitIndented("subu $t2, $t0, $t1");
                break;
            case "*":
                out.emitIndented("mul $t2, $t0, $t1");
                break;
            case "/":
            case "//":
                out.emitIndented("div $t0, $t1");
                out.emitIndented("mflo $t2");
                break;
            case "%":
                out.emitIndented("div $t0, $t1");
                out.emitIndented("mfhi $t2");
                break;
            case "<":
                out.emitIndented("slt $t2, $t0, $t1");
                break;
            case ">":
                out.emitIndented("slt $t2, $t1, $t0");
                break;
            case "<=":
                out.emitIndented("slt $t2, $t1, $t0");
                out.emitIndented("xori $t2, $t2, 1");
                break;
            case ">=":
                out.emitIndented("slt $t2, $t0, $t1");
                out.emitIndented("xori $t2, $t2, 1");
                break;
            case "==":
                out.emitIndented("xor $t2, $t0, $t1");
                out.emitIndented("sltiu $t2, $t2, 1");
                break;
            case "!=":
                out.emitIndented("xor $t2, $t0, $t1");
                out.emitIndented("sltu $t2, $zero, $t2");
                break;
            case "@":
                out.emitIndented("sltu $t3, $zero, $t0");
                out.emitIndented("sltu $t4, $zero, $t1");
                out.emitIndented("and  $t2, $t3, $t4");
                break;
            case "~":
                out.emitIndented("sltu $t3, $zero, $t0");
                out.emitIndented("sltu $t4, $zero, $t1");
                out.emitIndented("or   $t2, $t3, $t4");
                break;
            default:
                throw new UnsupportedOperationException("Operador no soportado: " + b.op);
        }

        storeIntRegToVar(out, fn, frame, b.dst, "$t2");
    }

    private static void emitFloatArith(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.IBin b) {

        loadOperandToFloatReg(out, fn, frame, strPool, fltPool, b.a, "$f0");
        loadOperandToFloatReg(out, fn, frame, strPool, fltPool, b.b, "$f1");

        switch (b.op) {
            case "+":
                out.emitIndented("add.s $f2, $f0, $f1");
                break;
            case "-":
                out.emitIndented("sub.s $f2, $f0, $f1");
                break;
            case "*":
                out.emitIndented("mul.s $f2, $f0, $f1");
                break;
            case "/":
                out.emitIndented("div.s $f2, $f0, $f1");
                break;
            default:
                throw new UnsupportedOperationException("Operador float no soportado: " + b.op);
        }

        storeFloatRegToVar(out, fn, frame, b.dst, "$f2");
    }

    private static void emitFloatCompareToBool(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.IBin b) {

        loadOperandToFloatReg(out, fn, frame, strPool, fltPool, b.a, "$f0");
        loadOperandToFloatReg(out, fn, frame, strPool, fltPool, b.b, "$f1");

        String trueLbl = fn.label + "_fcmp_true_" + System.identityHashCode(b);
        String endLbl = fn.label + "_fcmp_end_" + System.identityHashCode(b);

        switch (b.op) {
            case "<":
                out.emitIndented("c.lt.s $f0, $f1");
                break;
            case "<=":
                out.emitIndented("c.le.s $f0, $f1");
                break;
            case ">":
                out.emitIndented("c.lt.s $f1, $f0");
                break;
            case ">=":
                out.emitIndented("c.le.s $f1, $f0");
                break;
            case "==":
                out.emitIndented("c.eq.s $f0, $f1");
                break;
            case "!=":
                out.emitIndented("c.eq.s $f0, $f1");
                break;
            default:
                throw new UnsupportedOperationException("Comparación float no soportada: " + b.op);
        }

        out.emitIndented("li $t2, 0");
        if (b.op.equals("!="))
            out.emitIndented("bc1f " + trueLbl);
        else
            out.emitIndented("bc1t " + trueLbl);

        out.emitIndented("j " + endLbl);
        out.label(trueLbl);
        out.emitIndented("li $t2, 1");
        out.label(endLbl);

        storeIntRegToVar(out, fn, frame, b.dst, "$t2");
    }

    private static void emitPrint(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.Operand v) {

        TacParser.Type t = typeOfOperand(fn, v);

        if (v instanceof TacParser.OString) {
            String lbl = strPool.labelFor(((TacParser.OString) v).rawWithQuotes);
            out.emitIndented("la $a0, " + lbl);
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            out.emitIndented("la $a0, nl");
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            return;
        }

        if (t == TacParser.Type.STRING) {
            loadOperandToIntReg(out, fn, frame, strPool, fltPool, v, "$a0");
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            out.emitIndented("la $a0, nl");
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            return;
        }

        if (t == TacParser.Type.FLOAT) {
            loadOperandToFloatReg(out, fn, frame, strPool, fltPool, v, "$f12");
            out.emitIndented("li $v0, 2");
            out.emitIndented("syscall");
            out.emitIndented("la $a0, nl");
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            return;
        }

        if (t == TacParser.Type.CHAR) {
            loadOperandToIntReg(out, fn, frame, strPool, fltPool, v, "$a0");
            out.emitIndented("li $v0, 11");
            out.emitIndented("syscall");
            out.emitIndented("la $a0, nl");
            out.emitIndented("li $v0, 4");
            out.emitIndented("syscall");
            return;
        }

        loadOperandToIntReg(out, fn, frame, strPool, fltPool, v, "$a0");
        out.emitIndented("li $v0, 1");
        out.emitIndented("syscall");
        out.emitIndented("la $a0, nl");
        out.emitIndented("li $v0, 4");
        out.emitIndented("syscall");
    }

    private static void emitRead(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            String target) {

        TacParser.Type t = fn.typeEnv.getOrDefault(target, TacParser.Type.INT);

        if (t == TacParser.Type.FLOAT) {
            out.emitIndented("li $v0, 6");
            out.emitIndented("syscall");
            storeFloatRegToVar(out, fn, frame, target, "$f0");
            return;
        }

        if (t == TacParser.Type.STRING) {
            throw new UnsupportedOperationException("read string pendiente (buffer + syscall 8).");
        }

        out.emitIndented("li $v0, 5");
        out.emitIndented("syscall");
        storeIntRegToVar(out, fn, frame, target, "$v0");
    }

    private static void loadOperandToIntReg(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.Operand op,
            String reg) {

        if (op == null) {
            out.emitIndented("move " + reg + ", $zero");
            return;
        }

        if (op instanceof TacParser.OInt) {
            out.emitIndented("li " + reg + ", " + ((TacParser.OInt) op).value);
            return;
        }
        if (op instanceof TacParser.OChar) {
            out.emitIndented("li " + reg + ", " + ((TacParser.OChar) op).value);
            return;
        }
        if (op instanceof TacParser.OString) {
            String lbl = strPool.labelFor(((TacParser.OString) op).rawWithQuotes);
            out.emitIndented("la " + reg + ", " + lbl);
            return;
        }
        if (op instanceof TacParser.OFloat) {
            throw new UnsupportedOperationException("Uso de float donde se esperaba int (load).");
        }
        if (op instanceof TacParser.OVar) {
            loadVarToIntReg(out, frame, ((TacParser.OVar) op).name, reg);
            return;
        }

        throw new IllegalStateException("Operando no soportado: " + op.getClass().getName());
    }

    private static void loadOperandToFloatReg(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            StringPool strPool,
            FloatPool fltPool,
            TacParser.Operand op,
            String freg) {

        if (op instanceof TacParser.OFloat) {
            String lbl = fltPool.labelFor(((TacParser.OFloat) op).raw);
            out.emitIndented("l.s " + freg + ", " + lbl);
            return;
        }

        if (op instanceof TacParser.OVar) {
            loadVarToFloatReg(out, frame, ((TacParser.OVar) op).name, freg);
            return;
        }

        throw new UnsupportedOperationException("Operando float no soportado: " + op.getClass().getName());
    }

    private static void loadVarToIntReg(MipsEmitter out, FrameLayout frame, String name, String reg) {
        if (frame.isLocalOrTemp(name)) {
            out.emitIndented("lw " + reg + ", " + frame.offsetOfLocalOrTemp(name) + "($fp)");
            return;
        }
        if (frame.isParam(name)) {
            out.emitIndented("lw " + reg + ", " + frame.offsetOfParam(name) + "($fp)");
            return;
        }
        out.emitIndented("lw " + reg + ", " + name);
    }

    private static void loadVarToFloatReg(MipsEmitter out, FrameLayout frame, String name, String freg) {
        if (frame.isLocalOrTemp(name)) {
            out.emitIndented("l.s " + freg + ", " + frame.offsetOfLocalOrTemp(name) + "($fp)");
            return;
        }
        if (frame.isParam(name)) {
            out.emitIndented("l.s " + freg + ", " + frame.offsetOfParam(name) + "($fp)");
            return;
        }
        out.emitIndented("l.s " + freg + ", " + name);
    }

    private static void storeIntRegToVar(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            String name,
            String reg) {

        if (frame.isLocalOrTemp(name)) {
            out.emitIndented("sw " + reg + ", " + frame.offsetOfLocalOrTemp(name) + "($fp)");
            return;
        }
        if (frame.isParam(name)) {
            out.emitIndented("sw " + reg + ", " + frame.offsetOfParam(name) + "($fp)");
            return;
        }
        out.emitIndented("sw " + reg + ", " + name);
    }

    private static void storeFloatRegToVar(
            MipsEmitter out,
            TacParser.Function fn,
            FrameLayout frame,
            String name,
            String freg) {

        if (frame.isLocalOrTemp(name)) {
            out.emitIndented("s.s " + freg + ", " + frame.offsetOfLocalOrTemp(name) + "($fp)");
            return;
        }
        if (frame.isParam(name)) {
            out.emitIndented("s.s " + freg + ", " + frame.offsetOfParam(name) + "($fp)");
            return;
        }
        out.emitIndented("s.s " + freg + ", " + name);
    }

    private static TacParser.Type typeOfOperand(TacParser.Function fn, TacParser.Operand v) {
        if (v == null)
            return TacParser.Type.UNKNOWN;
        if (v instanceof TacParser.OInt)
            return TacParser.Type.INT;
        if (v instanceof TacParser.OChar)
            return TacParser.Type.CHAR;
        if (v instanceof TacParser.OFloat)
            return TacParser.Type.FLOAT;
        if (v instanceof TacParser.OString)
            return TacParser.Type.STRING;
        if (v instanceof TacParser.OVar)
            return fn.typeEnv.getOrDefault(((TacParser.OVar) v).name, TacParser.Type.UNKNOWN);
        return TacParser.Type.UNKNOWN;
    }
}
