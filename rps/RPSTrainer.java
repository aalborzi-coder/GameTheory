import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;


public class RPSTrainer {


    public static final int Rock = 0, Paper = 1, Scissors = 2, NUM_ACTIONS = 3;
    public static final Random random = new Random();
    double[][] regretSum = new double[2][NUM_ACTIONS], 
            strategy = new double[2][NUM_ACTIONS], 
            strategySum = new double[2][NUM_ACTIONS];

           

            private double[] getStrategy(int player) {
                double normalizingSum = 0;
                for (int a = 0; a < NUM_ACTIONS; a++) {
                    strategy[player][a] = regretSum[player][a] > 0 ? regretSum[player][a] : 0;
                    normalizingSum += strategy[player][a];
                }
                for (int a = 0; a < NUM_ACTIONS; a++) {
                    if (normalizingSum > 0) {
                        strategy[player][a] /= normalizingSum;
                    } else {
                        strategy[player][a] = 1.0 / NUM_ACTIONS;
                    }
                    strategySum[player][a] += strategy[player][a];
                }
                return strategy[player];
            }
            public int getAction(double[] strategy) {
                double r = random.nextDouble();
                int a =0;
                double cumulativeProbability = 0;
                while ( a < NUM_ACTIONS-1) {
                    cumulativeProbability += strategy[a];
                    if (r < cumulativeProbability) {
                        break;
                    }
                    a++;
                }
                return a;
            }

            public double[] getUtility(int otherAction) {
                double[] actionUtilitys = new double[NUM_ACTIONS];
                actionUtilitys[otherAction] = 0;
                actionUtilitys[(otherAction + 1) % NUM_ACTIONS] = 1;
                actionUtilitys[(otherAction + 2) % NUM_ACTIONS] = -1;
                    return actionUtilitys;
}

            public void playHuman() {
    Scanner in = new Scanner(System.in);
    String[] names = { "Rock", "Paper", "Scissors" };
    int you = 0, bot = 0;
    while (true) {
        System.out.print("\nr/p/s (q to quit): ");
        String line = in.nextLine().trim().toLowerCase();
        if (line.startsWith("q")) break;
        int humanAction = line.startsWith("r") ? 0 : line.startsWith("p") ? 1
                        : line.startsWith("s") ? 2 : -1;
        if (humanAction < 0) continue;

        double[] botStrategy = getStrategy(0);
        double[] shown = botStrategy.clone();
        int botAction = getAction(botStrategy);

        double[] u = getUtility(humanAction);
        for (int a = 0; a < NUM_ACTIONS; a++) {
            regretSum[0][a] += u[a] - u[botAction];
        }

        if (u[botAction] > 0) bot++; else if (u[botAction] < 0) you++;
        System.out.printf("you %s, bot %s -> %s   [%d : %d]%n", names[humanAction],
            names[botAction], u[botAction] > 0 ? "bot wins" : u[botAction] < 0 ? "you win" : "tie",
            you, bot);
        System.out.printf("bot was playing  R %.0f%%  P %.0f%%  S %.0f%%%n",
            shown[0] * 100, shown[1] * 100, shown[2] * 100);
    }
}

            public void train(int iterations) {
    for (int i = 0; i < iterations; i++) {
        int[] actions = { getAction(getStrategy(0)), getAction(getStrategy(1)) };
        for (int p = 0; p < 2; p++) {
            double[] actionUtility = getUtility(actions[1 - p]);
            for (int a = 0; a < NUM_ACTIONS; a++) {
                regretSum[p][a] += actionUtility[a] - actionUtility[actions[p]];
            }
        }
    }
}
            public double[] getAverageStrategy(int player) {
                double[] avgStrategy = new double[NUM_ACTIONS];
                double normalizingSum = 0;
                for (int a = 0; a < NUM_ACTIONS; a++) {
                    normalizingSum += strategySum[player][a];
                }
                for (int a = 0; a < NUM_ACTIONS; a++) {
                    if (normalizingSum > 0) {
                        avgStrategy[a] = strategySum[player][a] / normalizingSum;
                    } else {
                        avgStrategy[a] = 1.0 / NUM_ACTIONS;
                    }
                }
                return avgStrategy;
            }

            public static void main(String[] args) {
                RPSTrainer trainer = new RPSTrainer();
                trainer.playHuman();

              /*  System.out.println("player 0: " + Arrays.toString(trainer.getAverageStrategy(0)));
System.out.println("player 1: " + Arrays.toString(trainer.getAverageStrategy(1)));*/
            }
    
}
