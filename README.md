# Maquina_de_Turing
O objetivo desse projeto é desenvolver um simulador de Maquina de Turing, através do arquivo duplo_bal.json é determinado a estrutura da MT e a o duplobal.in é a entrada para verificação na MT. O simulador da MT foi desenvolvida na linguagem java.

Iniciamos criando uma classe Transition, em que será armazenado o estado atual, o próximo estado, o caracter de entrada, o caracter de saida e a direção da leitura da fita.
```
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
```
A próxima classe MaquinaTuring foi desenvolvida para interpretar o arquivo de entrada e o arquivo .json, com essa interpretação temos a MT processando a fita de entrada, assim ao final do processamento do código é gerado um arquivo de saída e a verificação de aceita ou rejeita.

Os atributos criados nessa classe armazenam o estado inicial, o estado final, a transitions, a fita como tape, head(posição da cabeça de leitura) e o state. O construtor recebe a json, inicia a fita com a palavra de entrada, define a cabeça e o estado inicial.

```
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
```
O método parseSpec faz parsing manual do JSON para extrair, o estado inicial, estados finais, os símbolo branco e as regras de transição

```
private void parseSpec(String specContent) {
        finalStates = new HashSet<>();
        transitions = new HashMap<>();

        String json = specContent.replaceAll("\\s+", "");

        String initialStr = json.replaceAll(".*\"initial\":(\\d+).*", "$1");
        initialState = Integer.parseInt(initialStr);

        String finalsStr = json.replaceAll(".*\"final\":\\[(.*?)\\].*", "$1");
        for (String s : finalsStr.split(",")) {
            if (!s.isEmpty()) finalStates.add(Integer.parseInt(s));
        }

        blank = json.replaceAll(".*\"white\":\"(.*?)\".*", "$1");

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
```
O método step processa o caracter e executa um passo da MT, através da leitura do símbolo é aplicado a transição

```
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
```
O método run executa até o final da fita, se atigir o estado final aceita senão rejeita
```
public boolean run(int maxSteps) {
        int steps = 0;
        while (steps < maxSteps) {
            if (finalStates.contains(state)) return true;
            if (!step()) return false;
            steps++;
        }
        return false;
    }
```
O método getTapeContent retorna a fita final

```
public String getTapeContent() {
        StringBuilder sb = new StringBuilder();
        for (String s : tape) {
            if (!s.equals(blank)) sb.append(s);
        }
        return sb.toString();
    }
```

Por último o método main realiza a leitura dos arquivos, cria e executa a MT, salva a fita final passada pelo metodo getTapeContent no arquivo saida.txt e mostra no console/terminal 1(aceita) ou 0(rejeita).

```
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
```
