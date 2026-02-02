import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TacParser {

    public enum Type {
        INT, FLOAT, BOOL, CHAR, STRING, UNKNOWN
    }

    public static final class Program {
        public final List<Function> functions = new ArrayList<>();
        public final Map<String, String> simpleToFullFuncLabel = new LinkedHashMap<>();
    }

    public static final class Function {
        public String label;
        public String endLabel;

        public final LinkedHashMap<String, Type> paramsInOrder = new LinkedHashMap<>();
        public final LinkedHashMap<String, Type> locals = new LinkedHashMap<>();
        public final Set<String> temps = new LinkedHashSet<>();
        public final Map<String, Type> typeEnv = new LinkedHashMap<>();

        public final List<Instr> instrs = new ArrayList<>();
    }

    public interface Instr {}

    public static final class ILabel implements Instr {
        public final String name;
        public ILabel(String n) { name = n; }
    }

    public static final class IGoto implements Instr {
        public final String target;
        public IGoto(String t) { target = t; }
    }

    public static final class IIf implements Instr {
        public final Operand cond;
        public final String target;
        public IIf(Operand c, String t) { cond = c; target = t; }
    }

    public static final class IAssign implements Instr {
        public final String dst;
        public final Operand src;
        public IAssign(String d, Operand s) { dst = d; src = s; }
    }

    public static final class IUn implements Instr {
        public final String dst;
        public final String op;
        public final Operand a;
        public IUn(String d, String op, Operand a) {
            dst = d;
            this.op = op;
            this.a = a;
        }
    }

    public static final class IBin implements Instr {
        public final String dst;
        public final Operand a;
        public final String op;
        public final Operand b;

        public IBin(String d, Operand a, String op, Operand b) {
            dst = d;
            this.a = a;
            this.op = op;
            this.b = b;
        }
    }

    public static final class IParam implements Instr {
        public final Operand value;
        public IParam(Operand v) { value = v; }
    }

    public static final class ICall implements Instr {
        public final String dst;
        public final String funcSimple;
        public final int nArgs;

        public ICall(String d, String f, int n) {
            dst = d;
            funcSimple = f;
            nArgs = n;
        }
    }

    public static final class IReturn implements Instr {
        public final Operand value;
        public IReturn(Operand v) { value = v; }
    }

    public static final class IPrint implements Instr {
        public final Operand value;
        public IPrint(Operand v) { value = v; }
    }

    public static final class IRead implements Instr {
        public final String target;
        public IRead(String t) { target = t; }
    }

    public interface Operand {}

    public static final class OVar implements Operand {
        public final String name;
        public OVar(String n) { name = n; }
    }

    public static final class OInt implements Operand {
        public final int value;
        public OInt(int v) { value = v; }
    }

    public static final class OFloat implements Operand {
        public final String raw;
        public final float value;
        public OFloat(String raw, float v) {
            this.raw = raw;
            this.value = v;
        }
    }

    public static final class OChar implements Operand {
        public final int value;
        public OChar(int v) { value = v; }
    }

    public static final class OString implements Operand {
        public final String rawWithQuotes;
        public OString(String r) { rawWithQuotes = r; }
    }

    private static final Pattern P_LABEL      = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\:$");
    private static final Pattern P_PARAM_DECL = Pattern.compile("^param_data_([a-zA-Z]+)\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern P_LOCAL_DECL = Pattern.compile("^local_data_([a-zA-Z]+)\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern P_GOTO       = Pattern.compile("^goto\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern P_IF         = Pattern.compile("^if\\s+(.+)\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern P_ASSIGN     = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.+)$");
    private static final Pattern P_CALL       = Pattern.compile("^call\\s+([A-Za-z_][A-Za-z0-9_]*),\\s*(\\d+)$");
    private static final Pattern P_PARAM      = Pattern.compile("^param\\s+(.+)$");
    private static final Pattern P_PRINT      = Pattern.compile("^print\\s+(.+)$");
    private static final Pattern P_READ       = Pattern.compile("^read\\s+([A-Za-z_][A-Za-z0-9_]*)$");
    private static final Pattern P_RETURN     = Pattern.compile("^return(?:\\s+(.+))?$");

    private static final Pattern P_ARRAY_ANY = Pattern.compile(".*\\[[^\\]]+\\]\\[[^\\]]+\\].*");
    private static final Pattern P_TEMP = Pattern.compile("\\bt\\d+\\b");

    public static Program parse(Path tacPath) throws IOException {
        List<String> lines = Files.readAllLines(tacPath, StandardCharsets.UTF_8);

        Program prog = new Program();
        Function current = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (P_ARRAY_ANY.matcher(line).matches()) {
                throw new UnsupportedOperationException("TAC contiene arreglos 2D. Soporte pendiente. Línea: " + line);
            }

            Matcher mLabel = P_LABEL.matcher(line);
            if (mLabel.matches()) {
                String lbl = mLabel.group(1);

                boolean isFuncStart = lbl.equals("main") || (lbl.startsWith("function_") && !lbl.endsWith("_end"));

                if (isFuncStart) {
                    current = new Function();
                    current.label = lbl;
                    current.endLabel = lbl + "_end";
                    prog.functions.add(current);

                    if (lbl.startsWith("function_")) {
                        int lastUnd = lbl.lastIndexOf('_');
                        if (lastUnd > 0 && lastUnd + 1 < lbl.length()) {
                            String simple = lbl.substring(lastUnd + 1);
                            prog.simpleToFullFuncLabel.put(simple, lbl);
                        }
                    } else {
                        prog.simpleToFullFuncLabel.put("main", "main");
                    }
                }

                if (current != null) current.instrs.add(new ILabel(lbl));
                continue;
            }

            if (current == null) continue;

            Matcher mp = P_PARAM_DECL.matcher(line);
            if (mp.matches()) {
                Type t = parseType(mp.group(1));
                String name = mp.group(2);
                current.paramsInOrder.put(name, t);
                current.typeEnv.put(name, t);
                continue;
            }

            Matcher ml = P_LOCAL_DECL.matcher(line);
            if (ml.matches()) {
                Type t = parseType(ml.group(1));
                String name = ml.group(2);
                current.locals.put(name, t);
                current.typeEnv.put(name, t);
                continue;
            }

            Matcher mg = P_GOTO.matcher(line);
            if (mg.matches()) {
                current.instrs.add(new IGoto(mg.group(1)));
                continue;
            }

            Matcher mif = P_IF.matcher(line);
            if (mif.matches()) {
                Operand cond = parseOperand(mif.group(1).trim(), current);
                String target = mif.group(2);
                current.instrs.add(new IIf(cond, target));
                continue;
            }

            Matcher mparam = P_PARAM.matcher(line);
            if (mparam.matches()) {
                Operand v = parseOperand(mparam.group(1).trim(), current);
                current.instrs.add(new IParam(v));
                continue;
            }

            Matcher mprint = P_PRINT.matcher(line);
            if (mprint.matches()) {
                Operand v = parseOperand(mprint.group(1).trim(), current);
                current.instrs.add(new IPrint(v));
                continue;
            }

            Matcher mread = P_READ.matcher(line);
            if (mread.matches()) {
                current.instrs.add(new IRead(mread.group(1)));
                continue;
            }

            Matcher mret = P_RETURN.matcher(line);
            if (mret.matches()) {
                String g1 = mret.group(1);
                Operand v = (g1 == null) ? null : parseOperand(g1.trim(), current);
                current.instrs.add(new IReturn(v));
                continue;
            }

            Matcher mas = P_ASSIGN.matcher(line);
            if (mas.matches()) {
                String dst = mas.group(1).trim();
                String rhs = mas.group(2).trim();

                addTempsFrom(dst, current);
                addTempsFrom(rhs, current);

                Matcher mcall = P_CALL.matcher(rhs);
                if (mcall.matches()) {
                    String f = mcall.group(1);
                    int n = Integer.parseInt(mcall.group(2));
                    current.instrs.add(new ICall(dst, f, n));
                    current.typeEnv.putIfAbsent(dst, Type.UNKNOWN);
                    continue;
                }

                String[] bin = splitBinary(rhs);
                if (bin != null) {
                    Operand a = parseOperand(bin[0], current);
                    String op = bin[1];
                    Operand b = parseOperand(bin[2], current);
                    current.instrs.add(new IBin(dst, a, op, b));

                    Type ta = typeOf(a, current);
                    Type tb = typeOf(b, current);
                    Type out = inferBinResultType(op, ta, tb);
                    current.typeEnv.put(dst, out);
                    continue;
                }

                String[] un = splitUnary(rhs);
                if (un != null) {
                    String op = un[0];
                    Operand a = parseOperand(un[1], current);
                    current.instrs.add(new IUn(dst, op, a));

                    Type ta = typeOf(a, current);
                    Type out;
                    if (op.equals("!") || op.equals("Σ")) out = Type.BOOL;
                    else if (op.equals("-")) out = ta;
                    else out = Type.UNKNOWN;

                    current.typeEnv.put(dst, out);
                    continue;
                }

                Operand src = parseOperand(rhs, current);
                current.instrs.add(new IAssign(dst, src));
                current.typeEnv.put(dst, typeOf(src, current));
                continue;
            }

            throw new IllegalArgumentException("Línea TAC no reconocida: " + line);
        }

        return prog;
    }

    private static void addTempsFrom(String s, Function fn) {
        Matcher m = P_TEMP.matcher(s);
        while (m.find()) {
            fn.temps.add(m.group());
            fn.typeEnv.putIfAbsent(m.group(), Type.UNKNOWN);
        }
    }

    private static Type parseType(String s) {
        String t = s.toLowerCase(Locale.ROOT);
        if (t.contains("int")) return Type.INT;
        if (t.contains("float")) return Type.FLOAT;
        if (t.contains("bool")) return Type.BOOL;
        if (t.contains("char")) return Type.CHAR;
        if (t.contains("string")) return Type.STRING;
        return Type.UNKNOWN;
    }

    private static Operand parseOperand(String token, Function fn) {
        token = token.trim();

        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            return new OString(token);
        }

        if (token.startsWith("'") && token.endsWith("'") && token.length() >= 3) {
            char c = token.charAt(1);
            return new OChar((int) c);
        }

        if (token.matches("[-+]?\\d+\\.\\d+")) {
            return new OFloat(token, Float.parseFloat(token));
        }

        if (token.matches("[-+]?\\d+")) {
            return new OInt(Integer.parseInt(token));
        }

        addTempsFrom(token, fn);
        return new OVar(token);
    }

    private static Type typeOf(Operand op, Function fn) {
        if (op == null) return Type.UNKNOWN;
        if (op instanceof OInt) return Type.INT;
        if (op instanceof OFloat) return Type.FLOAT;
        if (op instanceof OChar) return Type.CHAR;
        if (op instanceof OString) return Type.STRING;
        if (op instanceof OVar) return fn.typeEnv.getOrDefault(((OVar) op).name, Type.UNKNOWN);
        return Type.UNKNOWN;
    }

    private static Type inferBinResultType(String op, Type a, Type b) {
        if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
            return Type.BOOL;
        }
        if (op.equals("@") || op.equals("~")) {
            return Type.BOOL;
        }
        if (a == Type.FLOAT || b == Type.FLOAT) return Type.FLOAT;
        return Type.INT;
    }

    private static String[] splitUnary(String rhs) {
        String r = rhs.trim();
        if (r.startsWith("!")) {
            return new String[] { "!", r.substring(1).trim() };
        }
        if (r.startsWith("Σ")) {
            return new String[] { "Σ", r.substring(1).trim() };
        }
        if (r.startsWith("-")) {
            return new String[] { "-", r.substring(1).trim() };
        }
        return null;
    }

    private static String[] splitBinary(String rhs) {
        String[] ops = { "==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/", "//", "%", "^", "@", "~" };
        for (String op : ops) {
            String needle = " " + op + " ";
            int idx = rhs.indexOf(needle);
            if (idx > 0) {
                String a = rhs.substring(0, idx).trim();
                String b = rhs.substring(idx + needle.length()).trim();
                return new String[] { a, op, b };
            }
        }
        return null;
    }
}
