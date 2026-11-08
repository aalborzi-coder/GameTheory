import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

public class KuhnTrainer {

    // ===== Kuhn Poker definitions =====
    public static final int PASS = 0, BET = 1, NUM_ACTIONS = 2;
    public static final Random random = new Random();
    public TreeMap<String, Node> nodeMap = new TreeMap<String, Node>();

    // ===== Information set node class definition =====
    class Node {
        String infoSet;
        double[] regretSum = new double[NUM_ACTIONS],
                 strategy = new double[NUM_ACTIONS],
                 strategySum = new double[NUM_ACTIONS];

        // Get current information set mixed strategy through regret-matching
        private double[] getStrategy(double realizationWeight) {
            double normalizingSum = 0;
            for (int a = 0; a < NUM_ACTIONS; a++) {
                strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
                normalizingSum += strategy[a];
            }
            for (int a = 0; a < NUM_ACTIONS; a++) {
                if (normalizingSum > 0)
                    strategy[a] /= normalizingSum;
                else
                    strategy[a] = 1.0 / NUM_ACTIONS;
                strategySum[a] += realizationWeight * strategy[a];
            }
            return strategy;
        }

        // Get average information set mixed strategy across all training iterations
        public double[] getAverageStrategy() {
            double[] avgStrategy = new double[NUM_ACTIONS];
            double normalizingSum = 0;
            for (int a = 0; a < NUM_ACTIONS; a++)
                normalizingSum += strategySum[a];
            for (int a = 0; a < NUM_ACTIONS; a++)
                if (normalizingSum > 0)
                    avgStrategy[a] = strategySum[a] / normalizingSum;
                else
                    avgStrategy[a] = 1.0 / NUM_ACTIONS;
            return avgStrategy;
        }

        // Get information set string representation
        public String toString() {
            return String.format("%4s: %s", infoSet, Arrays.toString(getAverageStrategy()));
        }
    }

    // ===== Train Kuhn poker =====
    public void train(int iterations) throws IOException {
        int[] cards = {1, 2, 3};
        double util = 0;

        // NEW: record exploitability at checkpoints into a CSV file for plotting
        new File("results").mkdirs();
        PrintWriter csv = new PrintWriter("results/kuhn_exploitability.csv");
        csv.println("iteration,exploitability");
        System.out.println("Exploitability before training (uniform play): " + exploitability());
        int nextCheckpoint = 1;

        for (int i = 0; i < iterations; i++) {
            // Shuffle cards
            for (int c1 = cards.length - 1; c1 > 0; c1--) {
                int c2 = random.nextInt(c1 + 1);
                int tmp = cards[c1];
                cards[c1] = cards[c2];
                cards[c2] = tmp;
            }
            util += cfr(cards, "", 1, 1);

            // NEW: measure at iterations 1, 2, 3, ... growing by about 20% each time
            int t = i + 1;
            if (t == nextCheckpoint || t == iterations) {
                csv.println(t + "," + exploitability());
                nextCheckpoint = Math.max(t + 1, (int) Math.ceil(t * 1.2));
            }
        }
        csv.close();
        System.out.println("Average game value: " + util / iterations);
        System.out.println("Final exploitability: " + exploitability());
        for (Node n : nodeMap.values())
            System.out.println(n);
    }

    // ===== Counterfactual regret minimization iteration =====
    private double cfr(int[] cards, String history, double p0, double p1) {
        int plays = history.length();
        int player = plays % 2;
        int opponent = 1 - player;

        // Return payoff for terminal states
        if (plays > 1) {
            boolean terminalPass = history.charAt(plays - 1) == 'p';
            boolean doubleBet = history.substring(plays - 2, plays).equals("bb");
            boolean isPlayerCardHigher = cards[player] > cards[opponent];
            if (terminalPass)
                if (history.equals("pp"))
                    return isPlayerCardHigher ? 1 : -1;
                else
                    return 1;
            else if (doubleBet)
                return isPlayerCardHigher ? 2 : -2;
        }

        String infoSet = cards[player] + history;

        // Get information set node or create it if nonexistant
        Node node = nodeMap.get(infoSet);
        if (node == null) {
            node = new Node();
            node.infoSet = infoSet;
            nodeMap.put(infoSet, node);
        }

        // For each action, recursively call cfr with additional history and probability
        double[] strategy = node.getStrategy(player == 0 ? p0 : p1);
        double[] util = new double[NUM_ACTIONS];
        double nodeUtil = 0;
        for (int a = 0; a < NUM_ACTIONS; a++) {
            String nextHistory = history + (a == 0 ? "p" : "b");
            util[a] = player == 0
                ? - cfr(cards, nextHistory, p0 * strategy[a], p1)
                : - cfr(cards, nextHistory, p0, p1 * strategy[a]);
            nodeUtil += strategy[a] * util[a];
        }

        // For each action, compute and accumulate counterfactual regret
        for (int a = 0; a < NUM_ACTIONS; a++) {
            double regret = util[a] - nodeUtil;
            node.regretSum[a] += (player == 0 ? p1 : p0) * regret;
        }

        return nodeUtil;
    }

    // ===== NEW: Exploitability =====
    // Info sets owned by each player (P1 = player 0, P2 = player 1)
    static final String[][] INFO_SETS = {
        {"1", "2", "3", "1pb", "2pb", "3pb"},
        {"1p", "2p", "3p", "1b", "2b", "3b"}
    };
    // All 6 possible deals: {P1's card, P2's card}, each with probability 1/6
    static final int[][] DEALS = {{1, 2}, {1, 3}, {2, 1}, {2, 3}, {3, 1}, {3, 2}};

    // exploitability = 1/2 * (best P1 vs my P2 + best P2 vs my P1)
    public double exploitability() {
        return 0.5 * (bestResponseValue(0) + bestResponseValue(1));
    }

    // Try every pure strategy for brPlayer against my average strategy; return the best expected payoff.
    // A pure strategy picks one action per info set: 6 info sets, 2 actions each, so 2^6 = 64 of them.
    // Bit k of `mask` is the action (0 = pass, 1 = bet) at INFO_SETS[brPlayer][k].
    private double bestResponseValue(int brPlayer) {
        double best = Double.NEGATIVE_INFINITY;
        for (int mask = 0; mask < 64; mask++) {
            double total = 0;
            for (int[] deal : DEALS)
                total += valueForP1(deal, "", brPlayer, mask) / DEALS.length;
            double valueForBr = (brPlayer == 0) ? total : -total;
            best = Math.max(best, valueForBr);
        }
        return best;
    }

    // Expected payoff to P1 from this history: brPlayer plays the pure strategy `mask`,
    // the other player plays my average strategy.
    private double valueForP1(int[] cards, String history, int brPlayer, int mask) {
        if (isTerminal(history))
            return payoffForP1(cards, history);
        int player = history.length() % 2;
        String infoSet = cards[player] + history;
        if (player == brPlayer) {
            int action = (mask >> indexOf(INFO_SETS[brPlayer], infoSet)) & 1;
            return valueForP1(cards, history + (action == 0 ? "p" : "b"), brPlayer, mask);
        }
        double[] s = averageStrategy(infoSet);
        return s[0] * valueForP1(cards, history + "p", brPlayer, mask)
             + s[1] * valueForP1(cards, history + "b", brPlayer, mask);
    }

    // My average strategy at an info set (uniform if training never reached it)
    private double[] averageStrategy(String infoSet) {
        Node node = nodeMap.get(infoSet);
        return node == null ? new double[] {0.5, 0.5} : node.getAverageStrategy();
    }

    private static boolean isTerminal(String h) {
        return h.equals("pp") || h.equals("bp") || h.equals("bb") || h.equals("pbp") || h.equals("pbb");
    }

    // Payoff to P1 at a finished hand (written separately from cfr on purpose, as an independent check)
    private static double payoffForP1(int[] cards, String h) {
        boolean p1Higher = cards[0] > cards[1];
        switch (h) {
            case "pp":  return p1Higher ? 1 : -1;  // check-check: showdown for the antes
            case "bp":  return 1;                  // P1 bet, P2 folded
            case "pbp": return -1;                 // P2 bet, P1 folded
            default:    return p1Higher ? 2 : -2;  // "bb" or "pbb": bet-call showdown
        }
    }

    private static int indexOf(String[] arr, String x) {
        for (int k = 0; k < arr.length; k++)
            if (arr[k].equals(x)) return k;
        throw new IllegalArgumentException("unknown info set " + x);
    }

    // ===== KuhnTrainer main method =====
    public static void main(String[] args) throws IOException {
        int iterations = 1000000;
        new KuhnTrainer().train(iterations);
    }
}