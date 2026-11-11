import java.io.*;
import java.util.*;
public class ConfirmationCheck {

	static String DATA_ROOT = "GARV_Datasets";
    static String OUT_DIR = "Results\\DOE_Results";
    static int GENERATIONS = 1000;
    static int NUM_INSTANCES = 10;
    static int REPS = 5;
    static double[] OMEGAS = {50, 100, 200};
    static double[][] COMBOS = {{0.9, 0.2, 5}, {0.8, 0.1, 9}, {0.8, 0.1, 3}};

    public static void main(String[] args) throws Exception {
        new File(OUT_DIR).mkdirs();
        List<List<Task>> T = new ArrayList<>();
        List<List<VM>> V = new ArrayList<>();
        for (int i = 1; i <= NUM_INSTANCES; i++) {
            String base = DATA_ROOT + File.separator + "tuning" + File.separator + "AR2" + File.separator
                    + String.format("TUNE_AR2_I%02d", i);
            T.add(Utils.readTasks(base + "_Tasks.csv"));
            V.add(Utils.readVMs(base + "_VMs.csv"));
        }

        String file = OUT_DIR + File.separator + "Confirmation_Runs.csv";
        System.out.printf("%6s %10s | %8s %8s %9s%n", "Omega", "Cx/Mut/T", "CSR", "PSR", "SW");
        System.out.println("-".repeat(48));
        try (PrintWriter w = new PrintWriter(new FileWriter(file))) {
            w.println("Gen,Crossover,Mutation,Tournament,Omega,Instance,Rep,Seed,CSR,PSR,SW,TimeSec");
            for (double om : OMEGAS) {
                for (double[] c : COMBOS) {
                    double sC = 0, sP = 0, sS = 0; int n = 0;
                    for (int i = 1; i <= NUM_INSTANCES; i++) {
                        for (int rep = 1; rep <= REPS; rep++) {
                            long seed = 1000L * i + rep;
                            DOERunner.Config cfg = new DOERunner.Config("Confirm", 100, GENERATIONS,
                                    c[0], c[1], (int) c[2], om);
                            double[] r = DOERunner.runOnce(T.get(i - 1), V.get(i - 1), cfg, seed);
                            w.printf(Locale.US, "%d,%.1f,%.1f,%d,%.0f,%d,%d,%d,%.4f,%.4f,%.4f,%.5f%n",
                                    GENERATIONS, c[0], c[1], (int) c[2], om, i, rep, seed, r[0], r[1], r[2], r[3]);
                            sC += r[0]; sP += r[1]; sS += r[2]; n++;
                        }
                    }
                    w.flush();
                    System.out.printf(Locale.US, "%6.0f %10s | %8.2f %8.2f %9.2f%n", om,
                            String.format("%.1f/%.1f/%d", c[0], c[1], (int) c[2]), sC / n, sP / n, sS / n);
                }
            }
        }
        System.out.println("\nPer-run results: " + new File(file).getAbsolutePath());
    }
}
