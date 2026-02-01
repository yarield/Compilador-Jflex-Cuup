import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MipsEmitter {
    private final StringBuilder sb = new StringBuilder();

    public void emit(String line) {
        sb.append(line).append('\n');
    }

    public void blank() {
        sb.append('\n');
    }

    public void label(String name) {
        sb.append(name).append(':').append('\n');
    }

    public void emitIndented(String line) {
        sb.append("  ").append(line).append('\n');
    }

    public void writeTo(Path outPath) throws IOException {
        if (outPath.getParent() != null) {
            Files.createDirectories(outPath.getParent());
        }
        try (BufferedWriter bw = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
            bw.write(sb.toString());
        }
    }
}
