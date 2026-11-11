import java.io.*;
import java.util.*;

public class ScalabilityRunner {

    // ---- Settings to edit ------------------------------------------------------
    static String INSTANCE = "GARV_Datasets\\evaluation\\AR5\\EVAL_AR5_I01";
    static String OUT_DIR = "Results\\Scalability_Results";
    static final int[] VM_SIZES = {20, 30, 40, 50, 60, 70, 80, 90, 100};
    static int NUM_RUNS = 10;
    static int WARMUP_RUNS = 3;

    public static void main(String[] args) {
        try {
            List<Task> tasks = Utils.readTasks(INSTANCE + "_Tasks.csv");
            List<VM> allVms = Utils.readVMs(INSTANCE + "_VMs.csv");
            for (int k : VM_SIZES)
                if (k > allVms.size())
                    throw new IllegalStateException("Pool size " + k + " exceeds the " + allVms.size() + " VMs of the instance");
            new File(OUT_DIR).mkdirs();

            System.out.printf("Scalability (Scenario 2): %s, %d tasks, %d runs per pool size%n",
                    new File(INSTANCE).getName(), tasks.size(), NUM_RUNS);

            // ---- JVM warm-up (not recorded) --------------------------------------
            for (int w = 1; w <= WARMUP_RUNS; w++) timeOnce(tasks, allVms, 999000L + w);
            System.out.println(WARMUP_RUNS + " warm-up runs done.\n");

            System.out.printf("%-6s %-6s %12s %12s %12s %12s%n", "VMs", "Tasks", "Mean (s)", "SD (s)", "CI95 low", "CI95 high");
            System.out.println("-".repeat(66));

            File runsFile = new File(OUT_DIR, "Scalability_VMs_Runs.csv");
            File sumFile = new File(OUT_DIR, "Scalability_VMs.csv");
            try (PrintWriter runs = new PrintWriter(new FileWriter(runsFile));
                 PrintWriter sum = new PrintWriter(new FileWriter(sumFile))) {
                runs.println("VMs,Tasks,Run,Seed,GATimeSec");
                sum.println("VMs,Tasks,Runs,Mean,SD,CI95_Low,CI95_High");
                double tCrit = Main.tQuantile975(NUM_RUNS - 1);

                for (int k : VM_SIZES) {
                    List<VM> vms = new ArrayList<>(allVms.subList(0, k));
                    double[] t = new double[NUM_RUNS];
                    for (int r = 1; r <= NUM_RUNS; r++) {
                        long seed = 900000L + 1000L * k + r;
                        t[r - 1] = timeOnce(tasks, vms, seed);
                        runs.printf(Locale.US, "%d,%d,%d,%d,%.6f%n", k, tasks.size(), r, seed, t[r - 1]);
                    }
                    runs.flush();
                    double mu = Main.mean(t), s = Main.sd(t), half = tCrit * s / Math.sqrt(NUM_RUNS);
                    sum.printf(Locale.US, "%d,%d,%d,%.6f,%.6f,%.6f,%.6f%n", k, tasks.size(), NUM_RUNS, mu, s, mu - half, mu + half);
                    sum.flush();
                    System.out.printf(Locale.US, "%-6d %-6d %12.4f %12.4f %12.4f %12.4f%n", k, tasks.size(), mu, s, mu - half, mu + half);
                }
            }
            System.out.println();
            System.out.println("Files written:");
            System.out.println("  " + runsFile.getAbsolutePath());
            System.out.println("  " + sumFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- One timed GA run (only ga.run() is timed, as in Main.runOnce) ------------
    static double timeOnce(List<Task> tasks, List<VM> vms, long seed) {
        for (Task t : tasks) t.reset();
        for (VM v : vms) v.reset();
        GeneticAlgorithm ga = new GeneticAlgorithm(tasks, vms, seed);   // final tuned defaults
        ga.VERBOSE = false;
        long t0 = System.nanoTime();
        ga.run();
        long t1 = System.nanoTime();
        return (t1 - t0) / 1e9;
    }
}
