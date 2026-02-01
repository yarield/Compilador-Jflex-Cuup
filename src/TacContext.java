import java.util.ArrayDeque;
import java.util.Deque;

public class TacContext {
    public String funcionActual = "global";

    // temporales se reinician por función (recomendado por legibilidad)
    private int tempCount = 0;

    // contadores por función para labels
    public int forCount = 0;
    public int decideCount = 0;
    public int loopCount = 0;

    //pila de destinos para break
    public final Deque<String> breakTargets = new ArrayDeque<>();

    public void enterFunction(String funcName) {
        funcionActual = funcName;
        tempCount = 0;
        forCount = 0;
        decideCount = 0;
        loopCount = 0;
        breakTargets.clear();
    }

    public String newTemp() {
        tempCount++;
        return "t" + tempCount;
    }
}
