package DatasetGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class DatasetGenerator {

    // ---- Experiment design ------------------------------------------------------
    static final int NUM_VMS = 100;
    static final int[] TASKS_PER_ROUND = {0, 50, 100, 150, 200, 250};   // index = AR number
    static final int TUNING_ROUND = 2;
    static final int NUM_TUNING_INSTANCES = 10;
    static final int NUM_EVAL_INSTANCES = 30;

    static long tuningSeed(int inst) { return inst; }
    static long evalSeed(int round, int inst) { return 100000L + 1000L * round + inst; }

    // ---- Table 8 ranges ---------------------------------------------------------
    static final int LEN_MIN = 10000, LEN_MAX = 90000;
    static final int DL_MIN = 20, DL_MAX = 200;
    static final int BG_MIN = 10, BG_MAX = 100;
    static final int MIPS_MIN = 100, MIPS_MAX = 1000;
    static final double RATE_MIN = 0.1, RATE_MAX = 1.0;

    // ---- Feasibility guard ------------------------------------------------------
    static final double MIN_DENSITY = 0.25, MAX_DENSITY = 0.45;
    static final double MAX_NO_FEASIBLE_FRAC = 0.20;
    static final int MAX_ATTEMPTS = 1000;

    // ---- Data holders -----------------------------------------------------------
    static class GTask { int id, length, budget, deadline; char pref; String cls; }
    static class GVM   { int id, mips; double rate; String cls; }

    static class Instance {
        List<GTask> tasks; List<GVM> vms;
        long seed; int attempt;
        double density, csrUpperBound, prefUpperBound; int tasksNoFeasibleVm;
    }

    // ============================================================================
    public static void main(String[] args) throws IOException {
        String root = args.length > 0 ? args[0] : "GARV_Datasets";
        new File(root).mkdirs();

        try (PrintWriter manifest = new PrintWriter(new FileWriter(root + "/manifest.csv"))) {
            manifest.println("Set,Round,Instance,Tasks,VMs,Seed,Attempt,TaskFile,VmFile,"
                    + "Density,TasksWithNoFeasibleVM,CSRUpperBound,PrefUpperBound,"
                    + "DeadlineTight,BudgetTight,Moderate,BothTight,PrefT,PrefC,"
                    + "VmProportional,VmBargain,VmPremiumSlow");

            for (int i = 1; i <= NUM_TUNING_INSTANCES; i++) {
                int r = TUNING_ROUND;
                Instance in = generate(TASKS_PER_ROUND[r], NUM_VMS, tuningSeed(i));
                writeInstance(root, "tuning", "TUNE", r, i, in, manifest);
            }
            for (int r = 1; r <= 5; r++) {
                for (int i = 1; i <= NUM_EVAL_INSTANCES; i++) {
                    Instance in = generate(TASKS_PER_ROUND[r], NUM_VMS, evalSeed(r, i));
                    writeInstance(root, "evaluation", "EVAL", r, i, in, manifest);
                }
            }
        }
        System.out.println("Done. Datasets written under: " + new File(root).getAbsolutePath());
    }

    // ============================================================================
    // Generate one accepted instance
    // ============================================================================
    static Instance generate(int numTasks, int numVms, long seed) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Random r = new Random(seed * 1000L + attempt);
            Instance in = new Instance();
            in.vms = generateVMs(numVms, r);
            in.tasks = generateTasks(numTasks, in.vms, r);
            in.seed = seed; in.attempt = attempt;
            evaluateFeasibility(in);

            boolean densOk = in.density >= MIN_DENSITY && in.density <= MAX_DENSITY;
            boolean coverOk = (double) in.tasksNoFeasibleVm / numTasks <= MAX_NO_FEASIBLE_FRAC;
            if (densOk && coverOk) return in;
        }
        throw new IllegalStateException("No instance passed the feasibility guard for seed " + seed);
    }

    // ---- VM pool ----------------------------------------------------------------
    static List<GVM> generateVMs(int n, Random r) {
        List<GVM> vms = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            GVM v = new GVM();
            double u = r.nextDouble(), mips, rate;
            if (u < 0.50) {
                v.cls = "PROPORTIONAL";
                mips = uni(r, MIPS_MIN, MIPS_MAX);
                rate = 0.001 * mips * uni(r, 0.85, 1.15);
            } else if (u < 0.75) {
                v.cls = "BARGAIN";
                mips = uni(r, 550, MIPS_MAX);
                rate = uni(r, 0.10, 0.40);
            } else {
                v.cls = "PREMIUM_SLOW";
                mips = uni(r, MIPS_MIN, 550);
                rate = uni(r, 0.60, RATE_MAX);
            }
            v.mips = (int) Math.round(clip(mips, MIPS_MIN, MIPS_MAX));
            v.rate = Math.round(clip(rate, RATE_MIN, RATE_MAX) * 1000.0) / 1000.0;
            vms.add(v);
        }
        Collections.shuffle(vms, r);                      // VM type is not visible from file order
        for (int j = 0; j < n; j++) vms.get(j).id = j;
        return vms;
    }

    // ---- Tasks ------------------------------------------------------------------
    static List<GTask> generateTasks(int n, List<GVM> vms, Random r) {
        List<GTask> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            GTask t = new GTask();
            t.id = i;
            t.length = (LEN_MIN + r.nextInt(LEN_MAX - LEN_MIN + 1)) / 10 * 10;   // multiple of 10

            GVM ref = vms.get(r.nextInt(vms.size()));
            double refTime = (double) t.length / ref.mips;
            double refCost = refTime * ref.rate;

            double u = r.nextDouble(), sD, sB, pDeadline;
            if (u < 0.30)      { t.cls = "DEADLINE_TIGHT"; sD = uni(r, 0.6, 1.0); sB = uni(r, 1.2, 2.0); pDeadline = 0.6; }
            else if (u < 0.60) { t.cls = "BUDGET_TIGHT";   sD = uni(r, 1.2, 2.0); sB = uni(r, 0.6, 1.0); pDeadline = 0.4; }
            else if (u < 0.85) { t.cls = "MODERATE";       sD = uni(r, 0.9, 1.4); sB = uni(r, 0.9, 1.4); pDeadline = 0.5; }
            else               { t.cls = "BOTH_TIGHT";     sD = uni(r, 0.6, 0.9); sB = uni(r, 0.6, 0.9); pDeadline = 0.5; }

            t.deadline = (int) Math.round(clip(refTime * sD, DL_MIN, DL_MAX));
            t.budget   = (int) Math.round(clip(refCost * sB, BG_MIN, BG_MAX));
            t.pref = r.nextDouble() < pDeadline ? 'T' : 'C';
            tasks.add(t);
        }
        return tasks;
    }

    // ============================================================================
    // Feasibility statistics (computed on the rounded values that are written)
    // ============================================================================
    static void evaluateFeasibility(Instance in) {
        int n = in.tasks.size(), m = in.vms.size();
        List<List<Integer>> both = new ArrayList<>(), pref = new ArrayList<>();
        int noFeasible = 0;
        double densSum = 0;
        for (GTask t : in.tasks) {
            List<Integer> b = new ArrayList<>(), p = new ArrayList<>();
            for (GVM v : in.vms) {
                double time = (double) t.length / v.mips, cost = time * v.rate;
                boolean okT = time <= t.deadline, okC = cost <= t.budget;
                if (okT && okC) b.add(v.id);
                if ((t.pref == 'T' && okT) || (t.pref == 'C' && okC)) p.add(v.id);
            }
            if (b.isEmpty()) noFeasible++;
            densSum += (double) b.size() / m;
            both.add(b); pref.add(p);
        }
        double C = Math.min(n, m);
        in.density = densSum / n;
        in.tasksNoFeasibleVm = noFeasible;
        in.csrUpperBound = maxMatching(both, m) / C;
        in.prefUpperBound = maxMatching(pref, m) / C;
    }

    // Maximum one-to-one matching (Kuhn's augmenting-path algorithm)
    static int maxMatching(List<List<Integer>> adj, int m) {
        int[] matchVm = new int[m];
        Arrays.fill(matchVm, -1);
        int result = 0;
        for (int t = 0; t < adj.size(); t++) if (augment(t, adj, matchVm, new boolean[m])) result++;
        return result;
    }

    static boolean augment(int t, List<List<Integer>> adj, int[] matchVm, boolean[] seen) {
        for (int v : adj.get(t)) {
            if (seen[v]) continue;
            seen[v] = true;
            if (matchVm[v] == -1 || augment(matchVm[v], adj, matchVm, seen)) { matchVm[v] = t; return true; }
        }
        return false;
    }

    // ============================================================================
    // Output
    // ============================================================================
    static void writeInstance(String root, String setDir, String setTag, int round, int inst,
                              Instance in, PrintWriter manifest) throws IOException {
        String dir = setDir + "/AR" + round;
        String base = String.format("%s_AR%d_I%02d", setTag, round, inst);
        String taskFile = dir + "/" + base + "_Tasks.csv";
        String vmFile = dir + "/" + base + "_VMs.csv";
        new File(root + "/" + dir).mkdirs();

        try (PrintWriter w = new PrintWriter(new FileWriter(root + "/" + taskFile))) {
            w.println("Task ID,Length,Budget,Deadline,Pref");
            for (GTask t : in.tasks) w.printf(Locale.US, "%d,%d,%d,%d,%c%n", t.id, t.length, t.budget, t.deadline, t.pref);
        }
        try (PrintWriter w = new PrintWriter(new FileWriter(root + "/" + vmFile))) {
            w.println("VM_ID,MIPS,Rate");
            for (GVM v : in.vms) w.printf(Locale.US, "%d,%d,%.3f%n", v.id, v.mips, v.rate);
        }

        Map<String, Integer> tc = new HashMap<>(), vc = new HashMap<>();
        int prefT = 0;
        for (GTask t : in.tasks) { tc.merge(t.cls, 1, Integer::sum); if (t.pref == 'T') prefT++; }
        for (GVM v : in.vms) vc.merge(v.cls, 1, Integer::sum);

        manifest.printf(Locale.US, "%s,AR%d,%d,%d,%d,%d,%d,%s,%s,%.3f,%d,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                setTag, round, inst, in.tasks.size(), in.vms.size(), in.seed, in.attempt, taskFile, vmFile,
                in.density, in.tasksNoFeasibleVm, in.csrUpperBound, in.prefUpperBound,
                tc.getOrDefault("DEADLINE_TIGHT", 0), tc.getOrDefault("BUDGET_TIGHT", 0),
                tc.getOrDefault("MODERATE", 0), tc.getOrDefault("BOTH_TIGHT", 0),
                prefT, in.tasks.size() - prefT,
                vc.getOrDefault("PROPORTIONAL", 0), vc.getOrDefault("BARGAIN", 0),
                vc.getOrDefault("PREMIUM_SLOW", 0));
    }

    // ---- helpers ----------------------------------------------------------------
    static double uni(Random r, double a, double b) { return a + (b - a) * r.nextDouble(); }
    static double clip(double x, double a, double b) { return Math.max(a, Math.min(b, x)); }
}
