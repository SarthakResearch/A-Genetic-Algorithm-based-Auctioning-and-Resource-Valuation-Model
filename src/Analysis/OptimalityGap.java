package GARVaverageEachRunResultEvaluation;



import java.io.*;
import java.util.*;


public class OptimalityGap {

    
    static String DATASET_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\GARV_Datasets\\evaluation";
    static String RESULTS_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Evaluation_Results";
    static String OUT_FILE = "Optimality_Gap.csv";   // written into RESULTS_DIR

    static final double OMEGA = 100;     // must equal OMEGA in GeneticAlgorithm
    static final int[] ROUNDS = {1, 2, 3, 4, 5};
    static final int INSTANCES = 30;

    public static void main(String[] args) throws Exception {
        File out = new File(RESULTS_DIR, OUT_FILE);
        try (PrintWriter w = new PrintWriter(new FileWriter(out))) {
            w.println("Round,Mean Value of GARV,Optimal Value,Percentage Optimum");
            System.out.printf("%-6s %20s %15s %20s%n", "Round", "Mean Value of GARV", "Optimal Value", "Percentage Optimum");
            System.out.println("-".repeat(64));

            for (int r : ROUNDS) {
                Map<Integer, Double> garv = readGarvValues(r);
                double sumGarv = 0, sumOpt = 0;
                for (int i = 1; i <= INSTANCES; i++) {
                    Double g = garv.get(i);
                    if (g == null)
                        throw new IllegalStateException("Instance " + i + " not found in GARV_AR" + r + "_InstanceMeans.csv");
                    sumGarv += g;
                    sumOpt += optimalValue(r, i);
                }
                double meanGarv = sumGarv / INSTANCES;
                double meanOpt = sumOpt / INSTANCES;
                double pct = meanGarv / meanOpt * 100.0;

                w.println(String.format(Locale.US, "AR%d,%.2f,%.2f,%.2f", r, meanGarv, meanOpt, pct));
                System.out.printf(Locale.US, "%-6s %20.2f %15.2f %20.2f%n", "AR" + r, meanGarv, meanOpt, pct);
            }
        }
        System.out.println();
        System.out.println("Written: " + out.getAbsolutePath());
    }

    // ---- Optimal (Revenue - Penalty) of one instance ----------------------------
    static double optimalValue(int round, int inst) throws IOException {
        String base = DATASET_DIR + File.separator + "AR" + round + File.separator
                + String.format("EVAL_AR%d_I%02d", round, inst);
        List<double[]> tasks = readCsv(base + "_Tasks.csv");   // ID, Length, Budget, Deadline, Pref
        List<double[]> vms = readCsv(base + "_VMs.csv");       // ID, MIPS, Rate

        int nT = tasks.size(), nV = vms.size();
        int n = Math.max(nT, nV);

        double[][] value = new double[n][n];
        for (int a = 0; a < nT; a++)
            for (int b = 0; b < nV; b++)
                value[a][b] = pairValue(tasks.get(a), vms.get(b));

        int[] assign = hungarianMax(value);
        double total = 0;
        for (int a = 0; a < nT; a++)
            if (assign[a] < nV) total += value[a][assign[a]];
        return total;
    }


    static double pairValue(double[] t, double[] v) {
        double length = t[1], budget = t[2], deadline = t[3];
        char pref = (char) t[4];
        double mips = v[1], rate = v[2];

        double execTime = length / mips;
        double cost = execTime * rate;
        double revenue = 0, penalty = 0;

        if (execTime > deadline && cost > budget) penalty += OMEGA;
        if (cost <= budget && execTime <= deadline) revenue += budget;
        if (execTime <= deadline && cost > budget && pref == 'T') revenue += budget / 2;
        if (execTime > deadline && cost <= budget && pref == 'C') revenue += budget / 2;
        if (execTime <= deadline && cost > budget && pref == 'C') penalty += OMEGA / 2;
        if (execTime > deadline && cost <= budget && pref == 'T') penalty += OMEGA / 2;
        return revenue - penalty;
    }

    // ---- Hungarian algorithm (maximisation), O(n^3) -----------------------------
    // Returns assign[row] = column of the optimal one-to-one assignment.
    static int[] hungarianMax(double[][] value) {
        int n = value.length;
        double[] u = new double[n + 1], v = new double[n + 1];
        int[] p = new int[n + 1], way = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[n + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[n + 1];
            do {
                used[j0] = true;
                int i0 = p[j0], j1 = 0;
                double delta = Double.POSITIVE_INFINITY;
                for (int j = 1; j <= n; j++) {
                    if (used[j]) continue;
                    double cur = -value[i0 - 1][j - 1] - u[i0] - v[j];   // cost = -value
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0; }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j; }
                }
                for (int j = 0; j <= n; j++) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta; }
                    else minv[j] -= delta;
                }
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }
        int[] assign = new int[n];
        for (int j = 1; j <= n; j++) assign[p[j] - 1] = j - 1;
        return assign;
    }

    // ---- Readers ----------------------------------------------------------------
    // Rows as numbers; a non-numeric field (the preference T/C) is stored as its character code.
    static List<double[]> readCsv(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) throw new FileNotFoundException("Missing dataset file: " + f.getAbsolutePath());
        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            br.readLine();   // header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                double[] row = new double[p.length];
                for (int k = 0; k < p.length; k++) {
                    String s = p[k].trim();
                    try { row[k] = Double.parseDouble(s); }
                    catch (NumberFormatException e) { row[k] = s.isEmpty() ? 0 : s.charAt(0); }
                }
                rows.add(row);
            }
        }
        return rows;
    }

    // Instance -> RawRevenuePenalty from GARV_AR<r>_InstanceMeans.csv
    static Map<Integer, Double> readGarvValues(int round) throws IOException {
        File f = new File(RESULTS_DIR, "GARV_AR" + round + "_InstanceMeans.csv");
        if (!f.exists()) throw new FileNotFoundException("Missing file: " + f.getAbsolutePath());
        Map<Integer, Double> m = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cI = idx(h, "Instance"), cV = idx(h, "RawRevenuePenalty");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                m.put(Integer.parseInt(p[cI].trim()), Double.parseDouble(p[cV].trim()));
            }
        }
        return m;
    }

    static int idx(String[] h, String name) {
        for (int i = 0; i < h.length; i++) if (h[i].trim().equals(name)) return i;
        throw new IllegalStateException("Column " + name + " not found");
    }
}
