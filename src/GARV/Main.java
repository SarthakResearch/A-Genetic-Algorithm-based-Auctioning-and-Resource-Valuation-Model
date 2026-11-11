package GARVaverageEachRunResultEvaluation;
import java.io.*;
import java.util.*;
import java.util.regex.*;
 

public class Main {
    static List<Task> tasks = new ArrayList<>();
    static List<VM> vms = new ArrayList<>();
    static List<TaskVmPair> finalAllocationList = new ArrayList<>();
    static double rawScore = 0;
 
    // ---- Settings to edit ------------------------------------------------------
    static String AR_FOLDER = "datasets\\evaluationDataset\\AR1";
    static String OUT_DIR = "Results\\Evaluation_Results";
    static int NUM_RUNS = 10;
    static final String ALGO = "GARV";
 
    // Metrics recorded for every run (order = column order in the CSV files)
    static final String[] METRICS = {"CSR", "PSR", "IR", "AIV", "CR", "ACC", "OPR", "AOP",
            "Makespan", "SW", "GATimeSec", "RawRevenuePenalty"};
 
    public static void main(String[] args) {
        try {
            File arDir = new File(AR_FOLDER);
            int round = roundFromFolder(arDir.getName());
            List<Integer> instances = findInstances(arDir, round);
            if (instances.isEmpty()) throw new IllegalStateException("No EVAL_AR" + round + "_Ixx_Tasks.csv files in " + AR_FOLDER);
            new File(OUT_DIR).mkdirs();
 
            String prefix = OUT_DIR + File.separator + ALGO + "_AR" + round;
            int nM = METRICS.length;
            double[][] instMean = new double[instances.size()][nM];
            double[][] instSD = new double[instances.size()][nM];
            int[] instTasks = new int[instances.size()];
 
            System.out.printf("%s evaluation: AR%d, %d instances x %d runs (folder: %s)%n%n",
                    ALGO, round, instances.size(), NUM_RUNS, AR_FOLDER);
            System.out.printf("%-6s %6s | %7s %7s %7s %8s %7s %7s %7s %7s %9s %9s %8s%n",
                    "Inst", "Tasks", "CSR", "PSR", "IR", "AIV", "CR", "ACC", "OPR", "AOP", "Makespan", "SW", "GA(s)");
            System.out.println("-".repeat(118));
 
            try (PrintWriter runs = new PrintWriter(new FileWriter(prefix + "_Runs.csv"));
                 PrintWriter inst = new PrintWriter(new FileWriter(prefix + "_InstanceMeans.csv"));
                 PrintWriter alloc = new PrintWriter(new FileWriter(prefix + "_Allocations.csv"))) {
 
                runs.println("Algorithm,Round,Instance,Tasks,VMs,Run,Seed," + String.join(",", METRICS));
                StringBuilder h = new StringBuilder("Algorithm,Round,Instance,Tasks,VMs,Runs");
                for (String m : METRICS) h.append(",").append(m);
                for (String m : METRICS) h.append(",SD_").append(m);
                inst.println(h);
                EachRunAllocationResult.writeHeader(alloc);
 
                for (int k = 0; k < instances.size(); k++) {
                    int i = instances.get(k);
                    String base = AR_FOLDER + File.separator + String.format("EVAL_AR%d_I%02d", round, i);
                    tasks = Utils.readTasks(base + "_Tasks.csv");
                    vms = Utils.readVMs(base + "_VMs.csv");
                    instTasks[k] = tasks.size();
 
                    double[][] perRun = new double[NUM_RUNS][];
                    for (int r = 1; r <= NUM_RUNS; r++) {
                        long seed = 100000L * round + 100L * i + r;
                        perRun[r - 1] = runOnce(seed);
 
                        runs.printf(Locale.US, "%s,AR%d,%d,%d,%d,%d,%d", ALGO, round, i, tasks.size(), vms.size(), r, seed);
                        for (double v : perRun[r - 1]) runs.printf(Locale.US, ",%.6f", v);
                        runs.println();
                        EachRunAllocationResult.writeRunAllocation(alloc, round, i, r, seed, finalAllocationList);
                    }
                    runs.flush(); alloc.flush();
 
                    for (int m = 0; m < nM; m++) {
                        double[] col = new double[NUM_RUNS];
                        for (int r = 0; r < NUM_RUNS; r++) col[r] = perRun[r][m];
                        instMean[k][m] = mean(col);
                        instSD[k][m] = sd(col);
                    }
                    inst.printf(Locale.US, "%s,AR%d,%d,%d,%d,%d", ALGO, round, i, tasks.size(), vms.size(), NUM_RUNS);
                    for (int m = 0; m < nM; m++) inst.printf(Locale.US, ",%.6f", instMean[k][m]);
                    for (int m = 0; m < nM; m++) inst.printf(Locale.US, ",%.6f", instSD[k][m]);
                    inst.println();
                    inst.flush();
 
                    double[] a = instMean[k];
                    System.out.printf(Locale.US, "I%02d    %6d | %7.2f %7.2f %7.2f %8.3f %7.2f %7.3f %7.2f %7.3f %9.2f %9.2f %8.4f%n",
                            i, tasks.size(), a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10]);
                }
            }
 
            // ---- Round summary over instances -----------------------------------
            int n = instances.size();
            double tCrit = tQuantile975(n - 1);
            System.out.println();
            System.out.printf("=== %s AR%d summary over %d instances (each = mean of %d runs); 95%% CI uses t(%d) = %.4f ===%n",
                    ALGO, round, n, NUM_RUNS, n - 1, tCrit);
            System.out.printf("%-18s %12s %12s %12s %12s %16s%n", "Metric", "Mean", "SD", "CI95 low", "CI95 high", "Mean run-SD");
            System.out.println("-".repeat(88));
            try (PrintWriter sum = new PrintWriter(new FileWriter(prefix + "_Summary.csv"))) {
                sum.println("Algorithm,Round,Metric,Instances,RunsPerInstance,Mean,SD,CI95_Low,CI95_High,MeanRunToRunSD");
                for (int m = 0; m < nM; m++) {
                    double[] col = new double[n], sdc = new double[n];
                    for (int k = 0; k < n; k++) { col[k] = instMean[k][m]; sdc[k] = instSD[k][m]; }
                    double mu = mean(col), s = sd(col), half = tCrit * s / Math.sqrt(n);
                    sum.printf(Locale.US, "%s,AR%d,%s,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                            ALGO, round, METRICS[m], n, NUM_RUNS, mu, s, mu - half, mu + half, mean(sdc));
                    System.out.printf(Locale.US, "%-18s %12.4f %12.4f %12.4f %12.4f %16.4f%n",
                            METRICS[m], mu, s, mu - half, mu + half, mean(sdc));
                }
            }
            System.out.println();
            System.out.println("Files written:");
            for (String f : new String[]{"_Runs.csv", "_InstanceMeans.csv", "_Summary.csv", "_Allocations.csv"})
                System.out.println("  " + new File(prefix + f).getAbsolutePath());
 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    // ---- One GARV run on the currently loaded instance ---------------------------
    static double[] runOnce(long seed) {
        Results.reset();
        for (Task t : tasks) t.reset();
        for (VM v : vms) v.reset();
 
        GeneticAlgorithm ga = new GeneticAlgorithm(tasks, vms, seed);   // final tuned defaults
        ga.VERBOSE = false;
 
        long t0 = System.nanoTime();
        Chromosome best = ga.run();                                   // only the GA is timed
        long t1 = System.nanoTime();
 
        rawScore = ga.computeRawRevenuePenalty(best);
        finalAllocationList = ga.buildFinalAllocation(best);
        Results.executionTimeSec = (t1 - t0) / 1e9;
 
        Execution.performExecution(finalAllocationList);
        Results.computeResults();
 
        return new double[]{Results.TSR, Results.PSR, Results.IR, Results.AIV, Results.CR, Results.ACC,
                Results.OPR, Results.AOP, Results.makespan, Results.socialWelfare,
                Results.executionTimeSec, rawScore};
    }
 
    // ---- Instance discovery -------------------------------------------------------
    static int roundFromFolder(String name) {
        Matcher m = Pattern.compile("AR(\\d+)").matcher(name);
        if (!m.find()) throw new IllegalArgumentException("AR_FOLDER must end with ARn, e.g. ...\\evaluation\\AR1");
        return Integer.parseInt(m.group(1));
    }
 
    static List<Integer> findInstances(File dir, int round) {
        Pattern p = Pattern.compile("EVAL_AR" + round + "_I(\\d+)_Tasks\\.csv");
        List<Integer> list = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) throw new IllegalStateException("Folder not found: " + dir.getAbsolutePath());
        for (File f : files) {
            Matcher m = p.matcher(f.getName());
            if (m.matches()) {
                int i = Integer.parseInt(m.group(1));
                if (!new File(dir, String.format("EVAL_AR%d_I%02d_VMs.csv", round, i)).exists())
                    throw new IllegalStateException("Missing VM file for instance " + i);
                list.add(i);
            }
        }
        Collections.sort(list);
        return list;
    }
 
    // ---- Statistics helpers ---------------------------------------------------------
    static double mean(double[] x) { double s = 0; for (double v : x) s += v; return s / x.length; }
 
    static double sd(double[] x) {
        if (x.length < 2) return 0;
        double m = mean(x), s = 0;
        for (double v : x) s += (v - m) * (v - m);
        return Math.sqrt(s / (x.length - 1));
    }
 
    // 97.5% quantile of Student's t with df degrees of freedom (two-sided 95% CI)
    static double tQuantile975(int df) {
        double lo = 0, hi = 50;
        for (int it = 0; it < 200; it++) {
            double mid = 0.5 * (lo + hi);
            double upper = 0.5 * regIncBeta(df / (df + mid * mid), df / 2.0, 0.5);   // P(T > mid)
            if (upper > 0.025) lo = mid; else hi = mid;
        }
        return 0.5 * (lo + hi);
    }
 
    static double regIncBeta(double x, double a, double b) {
        if (x <= 0) return 0; if (x >= 1) return 1;
        double bt = Math.exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * Math.log(x) + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) return bt * betaCF(x, a, b) / a;
        return 1 - bt * betaCF(1 - x, b, a) / b;
    }
 
    static double betaCF(double x, double a, double b) {
        final double EPS = 1e-15, FPMIN = 1e-300;
        double qab = a + b, qap = a + 1, qam = a - 1, c = 1, d = 1 - qab * x / qap;
        if (Math.abs(d) < FPMIN) d = FPMIN; d = 1 / d; double h = d;
        for (int m = 1; m <= 10000; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d; if (Math.abs(d) < FPMIN) d = FPMIN;
            c = 1 + aa / c; if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1 / d; h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d; if (Math.abs(d) < FPMIN) d = FPMIN;
            c = 1 + aa / c; if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1 / d; double del = d * c; h *= del;
            if (Math.abs(del - 1) < EPS) break;
        }
        return h;
    }
 
    static double logGamma(double x) {
        double[] g = {676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
                12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (x < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1 - x);
        x -= 1; double a = 0.99999999999980993; double t = x + 7.5;
        for (int i = 0; i < 8; i++) a += g[i] / (x + i + 1);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }
}
