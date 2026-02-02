import java.util.*;

public final class FrameLayout {

    private final Map<String, Integer> localTempOffset = new LinkedHashMap<>();
    private final Map<String, Integer> paramOffset = new LinkedHashMap<>();

    private final int frameSize;

    public FrameLayout(Collection<String> locals, Collection<String> temps, String[] paramsInOrder) {

        int n = paramsInOrder.length;
        for (int i = 0; i < n; i++) {
            int off = (n - 1 - i) * 4;
            paramOffset.put(paramsInOrder[i], off);
        }

        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(locals);
        all.addAll(temps);

        int off = -12; 
        for (String name : all) {
            localTempOffset.put(name, off);
            off -= 4;
        }

        int localsBytes = (-off) - 12;
        int raw = 8 + localsBytes;

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
