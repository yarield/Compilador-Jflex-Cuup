import java.util.*;

public final class FrameLayout {

    private final Map<String, Integer> localTempOffset = new LinkedHashMap<>();
    private final Map<String, Integer> paramOffset = new LinkedHashMap<>();

    private final int frameSize;

    // Con esta convención:
    //   $fp = old $sp (tope del frame, justo donde estaban los args)
    //
    // Guardados:
    //   -4($fp)  = saved $ra
    //   -8($fp)  = saved $fp
    //
    // Locals/temps:
    //   -12($fp), -16($fp), ...
    //
    // Params (en pila):
    //   0($fp)   = último param empujado (top)
    //   4($fp)   = anterior
    // Por orden declarado (a,b,c...):
    //   offset(param[i]) = (n-1-i)*4

    public FrameLayout(Collection<String> locals, Collection<String> temps, String[] paramsInOrder) {

        int n = paramsInOrder.length;
        for (int i = 0; i < n; i++) {
            int off = (n - 1 - i) * 4;
            paramOffset.put(paramsInOrder[i], off);
        }

        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(locals);
        all.addAll(temps);

        int off = -12; // -4 ra, -8 fp, luego locals/temps
        for (String name : all) {
            localTempOffset.put(name, off);
            off -= 4;
        }

        int localsBytes = (-off) - 12;
        int raw = 8 + localsBytes; // ra+fp + locals/temps

        int aligned = ((raw + 15) / 16) * 16;
        frameSize = aligned;
    }

    public int frameSize() {
        return frameSize;
    }

    public int savedRaOffset() {
        return -4;
    }

    public int savedFpOffset() {
        return -8;
    }

    public boolean isLocalOrTemp(String name) {
        return localTempOffset.containsKey(name);
    }

    public int offsetOfLocalOrTemp(String name) {
        Integer off = localTempOffset.get(name);
        if (off == null) throw new IllegalArgumentException("Local/Temp no encontrado: " + name);
        return off;
    }

    public boolean isParam(String name) {
        return paramOffset.containsKey(name);
    }

    public int offsetOfParam(String name) {
        Integer off = paramOffset.get(name);
        if (off == null) throw new IllegalArgumentException("Param no encontrado: " + name);
        return off;
    }
}
