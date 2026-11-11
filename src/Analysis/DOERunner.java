package GARVaverageEachRunResultEvaluation;

import java.io.*;
import java.util.*;


public class DOERunner {

    // ---- Settings to edit ------------------------------------------------------
    static int STAGE = 3;
    static String DATA_ROOT = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\GARV_Datasets";
    static String OUT_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\DOE_Results_G800";
    static int NUM_INSTANCES = 10;
    static int REPS = 5;

    // Values chosen in earlier stages (update after Stage I and Stage II)
    static int    CHOSEN_POP  = 100;
    static int    CHOSEN_GEN  = 800;
    static double CHOSEN_CX   = 0.9;
    static double CHOSEN_MUT  = 0.2;
    static int    CHOSEN_TOUR = 5;

    // Defaults used before the secondary parameters are tuned (manuscript)
    static final double DEF_CX = 0.8, DEF_MUT = 0.1;
    static final int DEF_TOUR = 3;
    static final double DEF_OMEGA = 10;

    // ---- Levels (manuscript) -----------------------------------------------
    static final int[]    POP_LEVELS   = {25, 50, 100, 200, 300};
    static final int[]    GEN_LEVELS   = {25, 50, 100, 200, 300};
    static final int[]    GEN_EXTENDED = {400, 500, 600, 700, 800};
    static final double[] CX_LEVELS    = {0.5, 0.6, 0.7, 0.8, 0.9};
    static final double[] MUT_LEVELS   = {0.01, 0.05, 0.1, 0.2, 0.3};
    static final int[]    TOUR_LEVELS  = {2, 3, 5, 7, 9, 10};
    static final double[] OMEGA_LEVELS = {5, 10, 20, 30, 40, 50, 75, 100, 150, 200, 500, 1000};

    // ---- One parameter configuration ------------------------------------------
    static class Config {
        String block;   // e.g. "PopGen", "PopGenExt", "Crossover", "Mutation", "Tournament", "Omega"
        int pop, gen, tour; double cx, mut, omega;
        Config(String block, int pop, int gen, double cx, double mut, int tour, double omega) {
            this.block = block; this.pop = pop; this.gen = gen;
            this.cx = cx; this.mut = mut; this.tour = tour; this.omega = omega;
        }
    }

    // ============================================================================
    public static void main(String[] args) throws Exception {
        if (args.length > 0) STAGE = Integer.parseInt(args[0]);
        new File(OUT_DIR).mkdirs();

        // Load all tuning instances once
        List<List<Task>> instTasks = new ArrayList<>();
        List<List<VM>> instVms = new ArrayList<>();
        for (int i = 1; i <= NUM_INSTANCES; i++) {
            String base = DATA_ROOT + File.separator + "tuning" + File.separator + "AR2" + File.separator
                    + String.format("TUNE_AR2_I%02d", i);
            instTasks.add(Utils.readTasks(base + "_Tasks.csv"));
            instVms.add(Utils.readVMs(base + "_VMs.csv"));
        }

        List<Config> configs = buildConfigs(STAGE);
        System.out.printf("DOE Stage %d: %d configurations x %d instances x %d reps = %d runs%n",
                STAGE, configs.size(), NUM_INSTANCES, REPS, configs.size() * NUM_INSTANCES * REPS);

        String runsFile = OUT_DIR + File.separator + "DOE_Stage" + STAGE + "_Runs.csv";
        String sumFile = OUT_DIR + File.separator + "DOE_Stage" + STAGE + "_Summary.csv";

        try (PrintWriter runs = new PrintWriter(new FileWriter(runsFile));
             PrintWriter sum = new PrintWriter(new FileWriter(sumFile))) {

            runs.println("Stage,Block,Pop,Gen,Crossover,Mutation,Tournament,Omega,Instance,Rep,Seed,CSR,PSR,SW,TimeSec");
            sum.println("Stage,Block,Pop,Gen,Crossover,Mutation,Tournament,Omega,Runs,"
                    + "MeanCSR,SDCSR,MeanPSR,SDPSR,MeanSW,SDSW,MeanTimeSec");

            System.out.println();
            System.out.printf("%-11s %5s %5s %5s %5s %5s %6s | %8s %7s | %8s %7s | %9s %8s | %8s%n",
                    "Block", "Pop", "Gen", "Cx", "Mut", "Tour", "Omega",
                    "MeanCSR", "SD", "MeanPSR", "SD", "MeanSW", "SD", "Time(s)");
            System.out.println("-".repeat(118));

            for (Config c : configs) {
                List<Double> csr = new ArrayList<>(), psr = new ArrayList<>(), sw = new ArrayList<>();
                double timeSum = 0;

                for (int i = 1; i <= NUM_INSTANCES; i++) {
                    for (int rep = 1; rep <= REPS; rep++) {
                        long seed = 1000L * i + rep;
                        double[] r = runOnce(instTasks.get(i - 1), instVms.get(i - 1), c, seed);
                        csr.add(r[0]); psr.add(r[1]); sw.add(r[2]); timeSum += r[3];
                        runs.printf(Locale.US, "%d,%s,%d,%d,%.2f,%.2f,%d,%.1f,%d,%d,%d,%.4f,%.4f,%.4f,%.5f%n",
                                STAGE, c.block, c.pop, c.gen, c.cx, c.mut, c.tour, c.omega,
                                i, rep, seed, r[0], r[1], r[2], r[3]);
                    }
                }
                runs.flush();

                int nRuns = csr.size();
                sum.printf(Locale.US, "%d,%s,%d,%d,%.2f,%.2f,%d,%.1f,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.5f%n",
                        STAGE, c.block, c.pop, c.gen, c.cx, c.mut, c.tour, c.omega, nRuns,
                        mean(csr), sd(csr), mean(psr), sd(psr), mean(sw), sd(sw), timeSum / nRuns);
                sum.flush();

                System.out.printf(Locale.US, "%-11s %5d %5d %5.2f %5.2f %5d %6.1f | %8.2f %7.2f | %8.2f %7.2f | %9.2f %8.2f | %8.4f%n",
                        c.block, c.pop, c.gen, c.cx, c.mut, c.tour, c.omega,
                        mean(csr), sd(csr), mean(psr), sd(psr), mean(sw), sd(sw), timeSum / nRuns);
            }
        }
        System.out.println();
        System.out.println("Per-run results : " + new File(runsFile).getAbsolutePath());
        System.out.println("Summary         : " + new File(sumFile).getAbsolutePath());
    }

    // ---- Configurations for each stage -------------------------------------------
    static List<Config> buildConfigs(int stage) {
        List<Config> list = new ArrayList<>();
        if (stage == 1) {
            for (int p : POP_LEVELS)
                for (int g : GEN_LEVELS)
                    list.add(new Config("PopGen", p, g, DEF_CX, DEF_MUT, DEF_TOUR, DEF_OMEGA));
            for (int g : GEN_EXTENDED)
                list.add(new Config("PopGenExt", CHOSEN_POP, g, DEF_CX, DEF_MUT, DEF_TOUR, DEF_OMEGA));
        } else if (stage == 2) {
            for (double cx : CX_LEVELS)
                list.add(new Config("Crossover", CHOSEN_POP, CHOSEN_GEN, cx, DEF_MUT, DEF_TOUR, DEF_OMEGA));
            for (double mut : MUT_LEVELS)
                list.add(new Config("Mutation", CHOSEN_POP, CHOSEN_GEN, DEF_CX, mut, DEF_TOUR, DEF_OMEGA));
            for (int t : TOUR_LEVELS)
                list.add(new Config("Tournament", CHOSEN_POP, CHOSEN_GEN, DEF_CX, DEF_MUT, t, DEF_OMEGA));
        } else if (stage == 3) {
            for (double w : OMEGA_LEVELS)
                list.add(new Config("Omega", CHOSEN_POP, CHOSEN_GEN, CHOSEN_CX, CHOSEN_MUT, CHOSEN_TOUR, w));
        } else {
            throw new IllegalArgumentException("STAGE must be 1, 2 or 3");
        }
        return list;
    }

    // ---- One GARV run: GA + pricing + metrics --------------------------------------
    static double[] runOnce(List<Task> tasks, List<VM> vms, Config c, long seed) {
        Results.reset();
        for (Task t : tasks) t.reset();
        for (VM v : vms) v.reset();

        GeneticAlgorithm ga = new GeneticAlgorithm(tasks, vms, seed);
        ga.populationSize = c.pop;
        ga.generations = c.gen;
        ga.crossoverRate = c.cx;
        ga.mutationRate = c.mut;
        ga.tournamentSize = c.tour;
        ga.OMEGA = c.omega;
        ga.VERBOSE = false;

        long t0 = System.nanoTime();
        Chromosome best = ga.run();
        long t1 = System.nanoTime();

        List<TaskVmPair> alloc = ga.buildFinalAllocation(best);

        // Results reads these static fields
        Main.tasks = tasks;
        Main.vms = vms;
        Main.finalAllocationList = alloc;

        Execution.performExecution(alloc);
        Results.computeResults();

        return new double[]{Results.TSR, Results.PSR, Results.socialWelfare, (t1 - t0) / 1e9};
    }

    // ---- helpers ------------------------------------------------------------------
    static double mean(List<Double> x) {
        double s = 0; for (double v : x) s += v; return s / x.size();
    }
    static double sd(List<Double> x) {
        double m = mean(x), s = 0;
        for (double v : x) s += (v - m) * (v - m);
        return x.size() > 1 ? Math.sqrt(s / (x.size() - 1)) : 0;
    }
}
