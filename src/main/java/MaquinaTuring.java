import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class Transition {
    int from, to;
    String read, write, dir;

    public Transition(int from, int to, String read, String write, String dir) {
        this.from = from;
        this.to = to;
        this.read = read;
        this.write = write;
        this.dir = dir;
    }
}

public class MaquinaTuring {
    private int initialState;
    private Set<Integer> finalStates;
    private String blank;
    private Map<String, Transition> transitions;

    private List<String> tape;
    private int head;
    private int state;

    public MaquinaTuring(String specContent, String input) {
        parseSpec(specContent);

        // Inicializa fita
        this.tape = new ArrayList<>();
        for (char c : input.toCharArray()) {
            tape.add(String.valueOf(c));
        }
        tape.add(blank);
        this.head = 0;
        this.state = initialState;
    }

    private void parseSpec(String specContent) {
        finalStates = new HashSet<>();
        transitions = new HashMap<>();

        // Remover espaços e quebras de linha
        String json = specContent.replaceAll("\\s+", "");

        // Pegar estado inicial
        String initialStr = json.replaceAll(".*\"initial\":(\\d+).*", "$1");
        initialState = Integer.parseInt(initialStr);

        // Pegar estado(s) finais
        String finalsStr = json.replaceAll(".*\"final\":\\[(.*?)\\].*", "$1");
        for (String s : finalsStr.split(",")) {
            if (!s.isEmpty()) finalStates.add(Integer.parseInt(s));
        }

        // Pegar símbolo branco
        blank = json.replaceAll(".*\"white\":\"(.*?)\".*", "$1");

        // Pegar transições
        String transStr = json.substring(json.indexOf("[{"), json.lastIndexOf("}]") + 2);
        String[] rules = transStr.split("\\},\\{");

        for (String rule : rules) {
            rule = rule.replace("{", "").replace("}", "");
            String[] parts = rule.split(",");

            int from = 0, to = 0;
            String read = "", write = "", dir = "";

            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length < 2) continue;
                String key = kv[0].replace("\"", "");
                String value = kv[1].replace("\"", "");

                switch (key) {
                    case "from": from = Integer.parseInt(value); break;
                    case "to": to = Integer.parseInt(value); break;
                    case "read": read = value; break;
                    case "write": write = value; break;
                    case "dir": dir = value; break;
                }
            }
            Transition t = new Transition(from, to, read, write, dir);
            transitions.put(from + "," + read, t);
        }
    }

    private boolean step() {
        String symbol = head < tape.size() ? tape.get(head) : blank;
        String key = state + "," + symbol;

        if (!transitions.containsKey(key)) return false;

        Transition tr = transitions.get(key);
        tape.set(head, tr.write);
        state = tr.to;

        if (tr.dir.equals("R")) {
            head++;
            if (head >= tape.size()) tape.add(blank);
        } else if (tr.dir.equals("L")) {
            if (head > 0) {
                head--;
            } else {
                tape.add(0, blank);
            }
        }
        return true;
    }

    public boolean run(int maxSteps) {
        int steps = 0;
        while (steps < maxSteps) {
            if (finalStates.contains(state)) return true;
            if (!step()) return false;
            steps++;
        }
        return false;
    }

    public String getTapeContent() {
        StringBuilder sb = new StringBuilder();
        for (String s : tape) {
            if (!s.equals(blank)) sb.append(s);
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java MaquinaTuring <especificacao.json> <entrada.txt> <saida.txt>");
            return;
        }

        String specFile = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        // Ler arquivos
        String specContent = new String(Files.readAllBytes(Paths.get(specFile)));
        String input = new String(Files.readAllBytes(Paths.get(inputFile))).trim();

        MaquinaTuring mt = new MaquinaTuring(specContent, input);
        boolean accepted = mt.run(10000);

        // Escrever fita final
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(mt.getTapeContent());
        }

        // Imprimir resultado
        System.out.println(accepted ? 1 : 0);
    }
}
