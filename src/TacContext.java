import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class TacContext {
    public String funcionActual = "global";
    public final Map<String, Nodo> varsConInicializacion = new LinkedHashMap<>();
    public final Map<String, Nodo> globalInits = new LinkedHashMap<>();  
    public final Map<String, String> localsActuales = new LinkedHashMap<>();
    
    private int tempCount = 0;
    private int globalTempCount = 0;

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
        localsActuales.clear();  
    }

    public String newTemp() {
        tempCount++;
        return "t" + tempCount;
    }

    public String newGlobalTemp() {
        globalTempCount++;
        return "t_global" + globalTempCount;
    }

    public void agregarLocal(String nombre, String tipo) {  
        localsActuales.put(nombre, tipo);
    }

    public void clearInitializations() {
        varsConInicializacion.clear();
    }

        public void clearAll() {  
        funcionActual = "global";
        varsConInicializacion.clear();
        globalInits.clear();
        localsActuales.clear();
        tempCount = 0;
        globalTempCount = 0;
        forCount = 0;
        decideCount = 0;
        loopCount = 0;
        breakTargets.clear();
    }
}
