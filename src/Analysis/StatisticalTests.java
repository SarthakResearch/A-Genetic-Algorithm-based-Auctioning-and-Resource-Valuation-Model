package GARVaverageEachRunResultEvaluation;


import java.io.*;
import java.util.*;


public class StatisticalTests {

    // ---- Settings to edit ------------------------------------------------------
    static String RESULTS_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Evaluation_Results";
    static String OUT_DIR = RESULTS_DIR + File.separator + "Statistical_Results";
    static double ALPHA = 0.05;
    static double Q_ALPHA = 2.569;   // Studentized range / sqrt(2), k = 4, alpha = 0.05 (Demsar, 2006)

    static final String[] ALGOS = {"GARV", "Algo1", "Algo2", "Algo3"};   // GARV = control
    static final int[] ROUNDS = {1, 2, 3, 4, 5};

    // label, column in the InstanceMeans files, higher is better?
    static final Object[][] METRICS = {
            {"CSR", "CSR", true},
            {"PSR", "PSR", true},
            {"IR", "IR", true},
            {"AIV", "AIV", true},
            {"CVR", "CR", false},
            {"ACC", "ACC", true},
            {"Makespan", "Makespan", false},
            {"SW", "SW", true},
            {"OPR", "OPR", false},
            {"AOP", "AOP", false},
    };

    // data[algo][round] -> instance -> metric column -> value
    static Map<String, Map<Integer, Map<Integer, Map<String, Double>>>> data = new LinkedHashMap<>();

    // ============================================================================
    public static void main(String[] args) throws Exception {
        if (args.length > 0) { RESULTS_DIR = args[0]; OUT_DIR = RESULTS_DIR + File.separator + "Statistical_Results"; }
        new File(OUT_DIR).mkdirs();
        for (String a : ALGOS) {
            Map<Integer, Map<Integer, Map<String, Double>>> byRound = new LinkedHashMap<>();
            for (int r : ROUNDS) byRound.put(r, readInstanceMeans(a, r));
            data.put(a, byRound);
        }
        checkSameInstances();

        int k = ALGOS.length, nM = METRICS.length;
        double[][] avgRank = new double[nM][k];
        double[] friedmanP = new double[nM];
        int N = 0;

        // ---------------- Friedman (pooled) and average ranks --------------------
        try (PrintWriter fr = new PrintWriter(new FileWriter(OUT_DIR + File.separator + "Friedman_Test.csv"))) {
            fr.println("Parameter,ChiSquare_F,df,p-value,Is Significant?");
            System.out.println("=== Friedman test (blocks = all instances of all rounds) ===");
            System.out.printf("%-10s %12s %4s %14s %8s%n", "Parameter", "Chi2_F", "df", "p-value", "Signif.");
            for (int m = 0; m < nM; m++) {
                String label = (String) METRICS[m][0], col = (String) METRICS[m][1];
                boolean higher = (Boolean) METRICS[m][2];
                List<double[]> blocks = new ArrayList<>();
                for (int r : ROUNDS)
                    for (int inst : data.get(ALGOS[0]).get(r).keySet()) {
                        double[] v = new double[k];
                        for (int a = 0; a < k; a++) v[a] = data.get(ALGOS[a]).get(r).get(inst).get(col);
                        blocks.add(v);
                    }
                N = blocks.size();
                double[] rankSum = new double[k];
                double tieSum = 0;
                for (double[] v : blocks) {
                    double[] rk = ranks(v, higher);
                    for (int a = 0; a < k; a++) rankSum[a] += rk[a];
                    tieSum += tieTerm(rk);
                }
                double sumR2 = 0;
                for (double R : rankSum) sumR2 += R * R;
                double chi2 = 12.0 / (N * k * (k + 1)) * sumR2 - 3.0 * N * (k + 1);
                chi2 = chi2 / (1 - tieSum / (N * (double) k * (k * k - 1)));
                int df = k - 1;
                double p = gammaQ(df / 2.0, chi2 / 2.0);
                friedmanP[m] = p;
                for (int a = 0; a < k; a++) avgRank[m][a] = rankSum[a] / N;
                String sig = p <= ALPHA ? "Yes" : "No";
                fr.printf(Locale.US, "%s,%.2f,%d,%s,%s%n", label, chi2, df, fmtP(p), sig);
                System.out.printf(Locale.US, "%-10s %12.2f %4d %14s %8s%n", label, chi2, df, fmtP(p), sig);
            }
        }

        // ---------------- Rank summary (Table 12) -------------------------------
        try (PrintWriter rs = new PrintWriter(new FileWriter(OUT_DIR + File.separator + "Rank_Summary.csv"))) {
            rs.println("Parameter," + String.join(",", ALGOS));
            System.out.println("\n=== Average ranks over " + N + " instances (rank 1 = best) ===");
            System.out.printf("%-10s", "Parameter");
            for (String a : ALGOS) System.out.printf(" %8s", a);
            System.out.println();
            double[] overall = new double[k];
            for (int m = 0; m < nM; m++) {
                StringBuilder line = new StringBuilder((String) METRICS[m][0]);
                System.out.printf("%-10s", METRICS[m][0]);
                for (int a = 0; a < k; a++) {
                    line.append(String.format(Locale.US, ",%.2f", avgRank[m][a]));
                    System.out.printf(Locale.US, " %8.2f", avgRank[m][a]);
                    overall[a] += avgRank[m][a] / nM;
                }
                rs.println(line);
                System.out.println();
            }
            StringBuilder line = new StringBuilder("Average");
            System.out.printf("%-10s", "Average");
            for (int a = 0; a < k; a++) {
                line.append(String.format(Locale.US, ",%.2f", overall[a]));
                System.out.printf(Locale.US, " %8.2f", overall[a]);
            }
            rs.println(line);
            System.out.println();
        }

        // ---------------- Critical difference (Table 13) ------------------------
        double CD = Q_ALPHA * Math.sqrt(k * (k + 1) / (6.0 * N));
        try (PrintWriter cd = new PrintWriter(new FileWriter(OUT_DIR + File.separator + "Critical_Difference.csv"))) {
            StringBuilder h = new StringBuilder("Metric,GARV Avg. Rank");
            for (int a = 1; a < k; a++) h.append(",").append(ALGOS[a]).append(" Delta,Significant?");
            cd.println(h);
            System.out.printf(Locale.US, "%n=== Nemenyi critical difference: CD = %.4f (q = %.3f, k = %d, N = %d) ===%n", CD, Q_ALPHA, k, N);
            System.out.printf("%-10s %10s", "Metric", "GARV rank");
            for (int a = 1; a < k; a++) System.out.printf(" %10s %8s", ALGOS[a] + " D", "Sig.");
            System.out.println();
            for (int m = 0; m < nM; m++) {
                StringBuilder line = new StringBuilder(String.format(Locale.US, "%s,%.2f", METRICS[m][0], avgRank[m][0]));
                System.out.printf(Locale.US, "%-10s %10.2f", METRICS[m][0], avgRank[m][0]);
                for (int a = 1; a < k; a++) {
                    double d = avgRank[m][a] - avgRank[m][0];   // > 0: GARV ranked better
                    String s = Math.abs(d) > CD ? (d > 0 ? "Yes" : "Yes**") : "No";
                    line.append(String.format(Locale.US, ",%.2f,%s", d, s));
                    System.out.printf(Locale.US, " %10.2f %8s", d, s);
                }
                cd.println(line);
                System.out.println();
            }
            cd.printf(Locale.US, "CD = %.2f (q_0.05 = %.3f; k = %d; N = %d)%n", CD, Q_ALPHA, k, N);
        }

        // ---------------- Wilcoxon per round (Tables 16) -------------------------
        try (PrintWriter ws = new PrintWriter(new FileWriter(OUT_DIR + File.separator + "Wilcoxon_Summary.csv"));
             PrintWriter wd = new PrintWriter(new FileWriter(OUT_DIR + File.separator + "Wilcoxon_Details.csv"))) {
            StringBuilder h = new StringBuilder("Parameter");
            for (int a = 1; a < k; a++) for (int r : ROUNDS) h.append(",vs ").append(ALGOS[a]).append(" AR").append(r);
            ws.println(h);
            wd.println("Parameter,Better,Round,Comparison,Pairs,Wins,Ties,Losses,NonZeroPairs,W_plus,W_minus,W,Method,Z,p-value,Holm adjusted p-value,Significant?,Result");
            System.out.println("\n=== Wilcoxon signed-rank tests per round (Holm-adjusted over 3 comparisons) ===");

            for (int m = 0; m < nM; m++) {
                String label = (String) METRICS[m][0], col = (String) METRICS[m][1];
                boolean higher = (Boolean) METRICS[m][2];
                String[][] result = new String[k][ROUNDS.length];
                if (friedmanP[m] > ALPHA) {
                    for (int a = 1; a < k; a++) Arrays.fill(result[a], "Not tested");
                } else {
                    for (int ri = 0; ri < ROUNDS.length; ri++) {
                        int r = ROUNDS[ri];
                        WResult[] res = new WResult[k];
                        double[] p = new double[k - 1];
                        for (int a = 1; a < k; a++) {
                            List<Double> diff = new ArrayList<>();
                            for (int inst : data.get(ALGOS[0]).get(r).keySet()) {
                                double g = data.get(ALGOS[0]).get(r).get(inst).get(col);
                                double b = data.get(ALGOS[a]).get(r).get(inst).get(col);
                                diff.add(higher ? g - b : b - g);   // positive = GARV better
                            }
                            res[a] = wilcoxon(diff);
                            p[a - 1] = res[a].p;
                        }
                        double[] adj = holm(p);
                        for (int a = 1; a < k; a++) {
                            WResult w = res[a];
                            boolean sig = adj[a - 1] <= ALPHA;
                            String outcome = !sig ? "No" : (w.wPlus > w.wMinus ? "Yes" : "Yes**");
                            result[a][ri] = outcome;
                            wd.printf(Locale.US, "%s,%s,AR%d,GARV vs %s,%d,%d,%d,%d,%d,%.1f,%.1f,%.1f,%s,%s,%s,%s,%s,%s%n",
                                    label, higher ? "Higher" : "Lower", r, ALGOS[a], w.pairs, w.wins, w.ties, w.losses, w.nz,
                                    w.wPlus, w.wMinus, Math.min(w.wPlus, w.wMinus), w.method,
                                    Double.isNaN(w.z) ? "" : String.format(Locale.US, "%.4f", w.z),
                                    fmtP(w.p), fmtP(adj[a - 1]), sig ? "Yes" : "No", outcome);
                        }
                    }
                }
                StringBuilder line = new StringBuilder(label);
                for (int a = 1; a < k; a++) for (int ri = 0; ri < ROUNDS.length; ri++) line.append(",").append(result[a][ri]);
                ws.println(line);
                System.out.printf("%-10s", label);
                for (int a = 1; a < k; a++) {
                    System.out.print(" | " + ALGOS[a] + ":");
                    for (int ri = 0; ri < ROUNDS.length; ri++) System.out.printf(" %-5s", result[a][ri].equals("Not tested") ? "-" : result[a][ri]);
                }
                System.out.println();
            }
        }
        System.out.println("\nFiles written to: " + new File(OUT_DIR).getAbsolutePath());
    }

    // ============================================================================
    // Wilcoxon signed-rank test (two-sided)
    // ============================================================================
    static class WResult {
        int pairs, wins, ties, losses, nz;
        double wPlus, wMinus, z = Double.NaN, p;
        String method;
    }

    static WResult wilcoxon(List<Double> d) {
        WResult w = new WResult();
        w.pairs = d.size();
        List<Double> nz = new ArrayList<>();
        for (double x : d) {
            if (x > 0) w.wins++; else if (x < 0) w.losses++; else w.ties++;
            if (x != 0) nz.add(x);
        }
        int n = nz.size();
        w.nz = n;
        if (n == 0) { w.p = 1.0; w.method = "none"; return w; }

        double[] abs = new double[n];
        for (int i = 0; i < n; i++) abs[i] = Math.abs(nz.get(i));
        double[] rk = ranks(abs, false);   // rank 1 = smallest |d|
        for (int i = 0; i < n; i++) { if (nz.get(i) > 0) w.wPlus += rk[i]; else w.wMinus += rk[i]; }
        double W = Math.min(w.wPlus, w.wMinus);
        double tie = tieTerm(rk);
        boolean hasTies = tie > 0, hasZeros = w.ties > 0;

        if (!hasTies && !hasZeros) {
            // exact distribution of the rank sum T over ranks 1..n
            int max = n * (n + 1) / 2;
            long[] cnt = new long[max + 1];
            cnt[0] = 1;
            for (int i = 1; i <= n; i++) for (int s = max; s >= i; s--) cnt[s] += cnt[s - i];
            long below = 0;
            for (int s = 0; s <= (int) Math.floor(W + 1e-9); s++) below += cnt[s];
            w.p = Math.min(1.0, 2.0 * below / Math.pow(2, n));
            w.method = "exact";
        } else {
            double mean = n * (n + 1) / 4.0;
            double var = n * (n + 1) * (2 * n + 1) / 24.0 - tie / 48.0;
            w.z = (W - mean) / Math.sqrt(var);
            w.p = Math.min(1.0, 2.0 * normalCdf(w.z));
            w.method = "normal";
        }
        return w;
    }

    // Holm step-down adjustment
    static double[] holm(double[] p) {
        int m = p.length;
        Integer[] idx = new Integer[m];
        for (int i = 0; i < m; i++) idx[i] = i;
        Arrays.sort(idx, Comparator.comparingDouble(i -> p[i]));
        double[] adj = new double[m];
        double running = 0;
        for (int j = 0; j < m; j++) {
            double v = Math.min(1.0, (m - j) * p[idx[j]]);
            running = Math.max(running, v);
            adj[idx[j]] = running;
        }
        return adj;
    }

    // ============================================================================
    // Ranking helpers
    // ============================================================================
    // Ranks 1..n with average ranks for ties; higher=true gives rank 1 to the largest value
    static double[] ranks(double[] v, boolean higher) {
        int n = v.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> higher ? Double.compare(v[b], v[a]) : Double.compare(v[a], v[b]));
        double[] r = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            while (j + 1 < n && v[idx[j + 1]] == v[idx[i]]) j++;
            double avg = (i + j) / 2.0 + 1;
            for (int t = i; t <= j; t++) r[idx[t]] = avg;
            i = j + 1;
        }
        return r;
    }

    // sum of (t^3 - t) over groups of tied ranks
    static double tieTerm(double[] rk) {
        Map<Double, Integer> c = new HashMap<>();
        for (double x : rk) c.merge(x, 1, Integer::sum);
        double s = 0;
        for (int t : c.values()) s += (double) t * t * t - t;
        return s;
    }

    // ============================================================================
    // Distributions (no external library)
    // ============================================================================
    static double normalCdf(double z) {   // P(Z <= z)
        double x = Math.abs(z) / Math.sqrt(2);
        double erfc = x == 0 ? 1.0 : gammaQ(0.5, x * x);   // erfc(x) = Q(1/2, x^2)
        return z < 0 ? 0.5 * erfc : 1 - 0.5 * erfc;
    }

    // Regularized upper incomplete gamma Q(a, x) (Numerical Recipes)
    static double gammaQ(double a, double x) {
        if (x <= 0) return 1.0;
        if (x < a + 1) return 1.0 - gammaSeries(a, x);
        return gammaCF(a, x);
    }

    static double gammaSeries(double a, double x) {
        double ap = a, sum = 1.0 / a, del = sum;
        for (int n = 1; n < 100000; n++) {
            ap += 1; del *= x / ap; sum += del;
            if (Math.abs(del) < Math.abs(sum) * 1e-16) break;
        }
        return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
    }

    static double gammaCF(double a, double x) {
        final double FPMIN = 1e-300;
        double b = x + 1 - a, c = 1 / FPMIN, d = 1 / b, h = d;
        for (int i = 1; i < 100000; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b; if (Math.abs(d) < FPMIN) d = FPMIN;
            c = b + an / c; if (Math.abs(c) < FPMIN) c = FPMIN;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < 1e-16) break;
        }
        return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
    }

    static double logGamma(double x) {
        double[] g = {676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
                12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (x < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1 - x);
        x -= 1; double a = 0.99999999999980993; double t = x + 7.5;
        for (int i = 0; i < 8; i++) a += g[i] / (x + i + 1);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    // ============================================================================
    // Input
    // ============================================================================
    static Map<Integer, Map<String, Double>> readInstanceMeans(String algo, int round) throws IOException {
        File f = new File(RESULTS_DIR, algo + "_AR" + round + "_InstanceMeans.csv");
        if (!f.exists()) throw new FileNotFoundException("Missing file: " + f.getAbsolutePath());
        Map<Integer, Map<String, Double>> m = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cInst = -1;
            for (int i = 0; i < h.length; i++) if (h[i].trim().equals("Instance")) cInst = i;
            if (cInst < 0) throw new IllegalStateException("No Instance column in " + f.getName());
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                Map<String, Double> row = new HashMap<>();
                for (Object[] mt : METRICS) {
                    String col = (String) mt[1];
                    int c = -1;
                    for (int i = 0; i < h.length; i++) if (h[i].trim().equals(col)) c = i;
                    if (c < 0) throw new IllegalStateException("Column " + col + " missing in " + f.getName());
                    row.put(col, Double.parseDouble(p[c].trim()));
                }
                m.put(Integer.parseInt(p[cInst].trim()), row);
            }
        }
        return m;
    }

    // every algorithm must have exactly the same instances in every round (paired design)
    static void checkSameInstances() {
        for (int r : ROUNDS) {
            Set<Integer> ref = data.get(ALGOS[0]).get(r).keySet();
            for (String a : ALGOS)
                if (!data.get(a).get(r).keySet().equals(ref))
                    throw new IllegalStateException("Instances of " + a + " in AR" + r + " differ from " + ALGOS[0]);
            System.out.println("AR" + r + ": " + ref.size() + " paired instances for all " + ALGOS.length + " algorithms");
        }
        System.out.println();
    }

    static String fmtP(double p) { return p < 0.001 ? String.format(Locale.US, "%.2E", p) : String.format(Locale.US, "%.4f", p); }
}