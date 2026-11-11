package GARVaverageEachRunResultEvaluation;

import java.io.*;
import java.util.*;

/*
 * ============================================================================
 * CONVERGENCE CHECK of the tuned GA on the longest chromosome (AR5)
 * ============================================================================
 * The GA is run with the tuned configuration (pop 100, crossover 0.8,
 * mutation 0.1, tournament 9, omega 50) for MAX_GEN generations, i.e. well
 * beyond the tuned 500. Every CHECKPOINT_EVERY generations the best-so-far
 * allocation is evaluated (fitness, CSR, PSR, SW).
 *
 * Instances: the convergence tuning instances TUNE_AR5_I01..I05 (250 tasks,
 * chromosome length 250). For reference, the first 5 AR2 tuning instances
 * (chromosome length 100) are run as well.
 * Seeds: 1000*instance + rep, as in the DOE.
 *
 * OUTPUT (in OUT_DIR)
 *   Convergence_Runs.csv     one row per run and checkpoint
 *   Convergence_Summary.csv  one row per round and checkpoint (means, SDs)
 *   plus a summary table printed on the console.
 * ============================================================================
 */
public class ConvergenceCheckAR5 {

    // ---- Settings to edit ------------------------------------------------------
	static String DATA_ROOT = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\GARV_Datasets";
    static String OUT_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\DOE_Results";
    static int[] ROUNDS = {2, 5};          // AR2 = reference (length 100), AR5 = longest chromosome (250)
    static int NUM_INSTANCES = 5;
    static int REPS = 5;
    static int MAX_GEN = 2000;
    static int CHECKPOINT_EVERY = 50;

    // ============================================================================
    public static void main(String[] args) throws Exception {
        new File(OUT_DIR).mkdirs();
        String runsFile = OUT_DIR + File.separator + "Convergence_Runs.csv";
        String sumFile = OUT_DIR + File.separator + "Convergence_Summary.csv";

        // results[round][checkpoint] -> list of {fitness, CSR, PSR, SW}
        Map<Integer, Map<Integer, List<double[]>>> results = new LinkedHashMap<>();

        try (PrintWriter runs = new PrintWriter(new FileWriter(runsFile))) {
            runs.println("Round,Tasks,ChromosomeLength,Instance,Rep,Seed,Generation,Fitness,CSR,PSR,SW");

            for (int r : ROUNDS) {
                Map<Integer, List<double[]>> byGen = new TreeMap<>();
                results.put(r, byGen);

                for (int i = 1; i <= NUM_INSTANCES; i++) {
                    String base = DATA_ROOT + File.separator + "tuning" + File.separator + "AR" + r
                            + File.separator + String.format("TUNE_AR%d_I%02d", r, i);
                    List<Task> tasks = Utils.readTasks(base + "_Tasks.csv");
                    List<VM> vms = Utils.readVMs(base + "_VMs.csv");

                    for (int rep = 1; rep <= REPS; rep++) {
                        long seed = 1000L * i + rep;

                        GeneticAlgorithm ga = new GeneticAlgorithm(tasks, vms, seed);  // tuned defaults
                        ga.generations = MAX_GEN;
                        ga.checkpointEvery = CHECKPOINT_EVERY;
                        ga.VERBOSE = false;
                        ga.run();

                        for (Map.Entry<Integer, Chromosome> cp : ga.checkpoints.entrySet()) {
                            double[] m = evaluate(ga, cp.getValue(), tasks, vms);
                            byGen.computeIfAbsent(cp.getKey(), k -> new ArrayList<>()).add(m);
                            runs.printf(Locale.US, "AR%d,%d,%d,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f%n",
                                    r, tasks.size(), ga.chromLength, i, rep, seed, cp.getKey(),
                                    m[0], m[1], m[2], m[3]);
                        }
                        runs.flush();
                        System.out.printf("AR%d instance %d rep %d done%n", r, i, rep);
                    }
                }
            }
        }

        // ---- Summary: console and CSV ------------------------------------------
        try (PrintWriter sum = new PrintWriter(new FileWriter(sumFile))) {
            sum.println("Round,Generation,Runs,MeanFitness,MeanCSR,SDCSR,MeanPSR,SDPSR,MeanSW,SDSW,"
                    + "CSRgainFromPrevious,PSRgainFromPrevious");
            for (int r : ROUNDS) {
                System.out.println();
                System.out.printf("=== AR%d ===%n", r);
                System.out.printf("%6s | %12s | %7s %6s | %7s %6s | %8s %7s | %8s %8s%n",
                        "Gen", "MeanFitness", "CSR", "SD", "PSR", "SD", "SW", "SD", "dCSR", "dPSR");
                System.out.println("-".repeat(96));
                double prevCSR = Double.NaN, prevPSR = Double.NaN;
                for (Map.Entry<Integer, List<double[]>> e : results.get(r).entrySet()) {
                    List<double[]> v = e.getValue();
                    double fit = mean(v, 0), csr = mean(v, 1), psr = mean(v, 2), sw = mean(v, 3);
                    double dC = Double.isNaN(prevCSR) ? 0 : csr - prevCSR;
                    double dP = Double.isNaN(prevPSR) ? 0 : psr - prevPSR;
                    System.out.printf(Locale.US, "%6d | %12.2f | %7.2f %6.2f | %7.2f %6.2f | %8.2f %7.2f | %+8.2f %+8.2f%n",
                            e.getKey(), fit, csr, sd(v, 1), psr, sd(v, 2), sw, sd(v, 3), dC, dP);
                    sum.printf(Locale.US, "AR%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                            r, e.getKey(), v.size(), fit, csr, sd(v, 1), psr, sd(v, 2), sw, sd(v, 3), dC, dP);
                    prevCSR = csr; prevPSR = psr;
                }
            }
        }
        System.out.println();
        System.out.println("Per-run results : " + new File(runsFile).getAbsolutePath());
        System.out.println("Summary         : " + new File(sumFile).getAbsolutePath());
    }

    // ---- Evaluate one checkpoint chromosome: pricing + metrics --------------------
    static double[] evaluate(GeneticAlgorithm ga, Chromosome c, List<Task> tasks, List<VM> vms) {
        Results.reset();
        for (Task t : tasks) t.reset();
        for (VM v : vms) v.reset();

        List<TaskVmPair> alloc = ga.buildFinalAllocation(c);
        Main.tasks = tasks;
        Main.vms = vms;
        Main.finalAllocationList = alloc;

        Execution.performExecution(alloc);
        Results.computeResults();
        return new double[]{c.fitness, Results.TSR, Results.PSR, Results.socialWelfare};
    }

    // ---- helpers ------------------------------------------------------------------
    static double mean(List<double[]> x, int k) {
        double s = 0; for (double[] v : x) s += v[k]; return s / x.size();
    }
    static double sd(List<double[]> x, int k) {
        double m = mean(x, k), s = 0;
        for (double[] v : x) s += (v[k] - m) * (v[k] - m);
        return x.size() > 1 ? Math.sqrt(s / (x.size() - 1)) : 0;
    }
}