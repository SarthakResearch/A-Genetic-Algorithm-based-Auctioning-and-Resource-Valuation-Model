package GARVaverageEachRunResultEvaluation;

import java.io.*;
import java.util.*;

/*
 * ============================================================================
 * ANOVA OF THE THREE-STAGE DOE (Section 6.5)
 * ============================================================================
 * Input  (all in FOLDER):
 *   DOE_Stage1_Runs.csv   Stage I   (population x generations)
 *   DOE_Stage2_Runs.csv   Stage II  (crossover, mutation, tournament size)
 *   DOE_Stage3_Runs.csv   Stage III (omega)
 *
 * Unit of analysis
 *   The tuning instance is the independent unit. For every configuration the
 *   REPS seeded runs on the same instance are first averaged, giving one value
 *   per (configuration, instance). The instance is then included as a
 *   blocking factor, so that differences between instances are removed from
 *   the error term (randomized complete block design).
 *
 * Models (balanced design, so the sums of squares are exact)
 *   Stage I  : Y = Pop + Gen + Pop x Gen + Instance + Error
 *              (only the 5 x 5 grid; the extended 400-800 generation runs are
 *               a separate one-factor follow-up and are not part of the grid)
 *   Stage II : Y = Level + Instance + Error, separately for crossover rate,
 *              mutation rate and tournament size
 *   Stage III: Y = Omega + Instance + Error, for omega in [OMEGA_MIN, OMEGA_MAX]
 *              (levels above 100 produce identical allocations; see 6.4)
 *   Y = CSR and Y = PSR.
 *
 * Output
 *   Console tables (SS, df, MS, F, p-value, F-critical at alpha = 0.05) and
 *   level means, plus FOLDER/ANOVA_Results.csv and FOLDER/ANOVA_LevelMeans.csv.
 *   p-values use the F distribution, computed here with the regularized
 *   incomplete beta function (no external library needed).
 * ============================================================================
 */
public class AnovaAnalysis {

    // ---- Settings to edit ------------------------------------------------------
    static String FOLDER = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\ANOVA_Results";
    static String[] METRICS = {"CSR", "PSR"};
    static double ALPHA = 0.05;
    static double OMEGA_MIN = 5, OMEGA_MAX = 100;

    // ---- One row of a runs file -------------------------------------------------
    static class Row { Map<String, String> v = new HashMap<>(); }

    // ---- One line of an ANOVA table ---------------------------------------------
    static class Source {
        String name; double ss; int df; double ms, f = Double.NaN, p = Double.NaN, fCrit = Double.NaN;
        Source(String name, double ss, int df) { this.name = name; this.ss = ss; this.df = df; this.ms = df > 0 ? ss / df : Double.NaN; }
    }

    static PrintWriter out, means;

    // ============================================================================
    public static void main(String[] args) throws Exception {
        if (args.length > 0) FOLDER = args[0];
        out = new PrintWriter(new FileWriter(FOLDER + File.separator + "ANOVA_Results.csv"));
        means = new PrintWriter(new FileWriter(FOLDER + File.separator + "ANOVA_LevelMeans.csv"));
        out.println("Stage,Parameter,Metric,Source,SS,df,MS,F,pValue,Fcrit");
        means.println("Stage,Parameter,Level,Metric,Mean,SD_over_instances,Instances");

        // ---- Stage I ----------------------------------------------------------
        List<Row> s1 = read("DOE_Stage1_Runs.csv");
        List<Row> grid = filter(s1, r -> r.v.get("Block").equals("PopGen"));
        for (String m : METRICS) twoFactorBlocked("Stage I", grid, "Pop", "Gen", m);

        // ---- Stage II ---------------------------------------------------------
        List<Row> s2 = read("DOE_Stage2_Runs.csv");
        for (String p : new String[]{"Crossover", "Mutation", "Tournament"}) {
            List<Row> block = filter(s2, r -> r.v.get("Block").equals(p));
            for (String m : METRICS) oneFactorBlocked("Stage II", block, p, m);
        }

        // ---- Stage III --------------------------------------------------------
        List<Row> s3 = read("DOE_Stage3_Runs.csv");
        List<Row> om = filter(s3, r -> {
            double w = Double.parseDouble(r.v.get("Omega"));
            return w >= OMEGA_MIN - 1e-9 && w <= OMEGA_MAX + 1e-9;
        });
        for (String m : METRICS) oneFactorBlocked("Stage III", om, "Omega", m);

        out.close(); means.close();
        System.out.println("\nResults written to:");
        System.out.println("  " + new File(FOLDER, "ANOVA_Results.csv").getAbsolutePath());
        System.out.println("  " + new File(FOLDER, "ANOVA_LevelMeans.csv").getAbsolutePath());
    }

    // ============================================================================
    // Stage I: two factors (A, B) with interaction, instance as block
    // ============================================================================
    static void twoFactorBlocked(String stage, List<Row> rows, String fa, String fb, String metric) {
        // cell[a][b][instance] = mean over reps
        Map<String, Map<String, Map<Integer, Double>>> cell = new TreeMap<>(NUM);
        Map<String, Map<String, Map<Integer, Integer>>> cnt = new TreeMap<>(NUM);
        for (Row r : rows) {
            String a = r.v.get(fa), b = r.v.get(fb);
            int inst = Integer.parseInt(r.v.get("Instance"));
            double y = Double.parseDouble(r.v.get(metric));
            cell.computeIfAbsent(a, k -> new TreeMap<>(NUM)).computeIfAbsent(b, k -> new TreeMap<>()).merge(inst, y, Double::sum);
            cnt.computeIfAbsent(a, k -> new TreeMap<>(NUM)).computeIfAbsent(b, k -> new TreeMap<>()).merge(inst, 1, Integer::sum);
        }
        List<String> A = new ArrayList<>(cell.keySet());
        List<String> B = new ArrayList<>(cell.get(A.get(0)).keySet());
        List<Integer> I = new ArrayList<>(cell.get(A.get(0)).get(B.get(0)).keySet());
        int a = A.size(), b = B.size(), n = I.size();

        double[][][] y = new double[a][b][n];
        for (int i = 0; i < a; i++) for (int j = 0; j < b; j++) for (int k = 0; k < n; k++) {
            Map<Integer, Double> c = cell.get(A.get(i)).get(B.get(j));
            if (c == null || !c.containsKey(I.get(k))) throw new IllegalStateException("Unbalanced data: " + A.get(i) + "/" + B.get(j) + "/" + I.get(k));
            y[i][j][k] = c.get(I.get(k)) / cnt.get(A.get(i)).get(B.get(j)).get(I.get(k));
        }

        double grand = 0; for (double[][] u : y) for (double[] v : u) for (double w : v) grand += w; grand /= (a * b * n);
        double[] mA = new double[a], mB = new double[b], mI = new double[n]; double[][] mAB = new double[a][b];
        for (int i = 0; i < a; i++) for (int j = 0; j < b; j++) for (int k = 0; k < n; k++) {
            mA[i] += y[i][j][k] / (b * n); mB[j] += y[i][j][k] / (a * n); mI[k] += y[i][j][k] / (a * b); mAB[i][j] += y[i][j][k] / n;
        }
        double ssT = 0, ssA = 0, ssB = 0, ssI = 0, ssAB = 0;
        for (int i = 0; i < a; i++) for (int j = 0; j < b; j++) for (int k = 0; k < n; k++) ssT += sq(y[i][j][k] - grand);
        for (int i = 0; i < a; i++) ssA += b * n * sq(mA[i] - grand);
        for (int j = 0; j < b; j++) ssB += a * n * sq(mB[j] - grand);
        for (int k = 0; k < n; k++) ssI += a * b * sq(mI[k] - grand);
        for (int i = 0; i < a; i++) for (int j = 0; j < b; j++) ssAB += n * sq(mAB[i][j] - mA[i] - mB[j] + grand);
        double ssE = ssT - ssA - ssB - ssI - ssAB;

        Source sE = new Source("Error", ssE, (a * b - 1) * (n - 1));
        List<Source> t = new ArrayList<>();
        t.add(new Source(label(fa), ssA, a - 1));
        t.add(new Source(label(fb), ssB, b - 1));
        t.add(new Source(label(fa) + " x " + label(fb), ssAB, (a - 1) * (b - 1)));
        t.add(new Source("Instance (block)", ssI, n - 1));
        finish(t, sE);
        t.add(sE);
        t.add(new Source("Total", ssT, a * b * n - 1));

        print(stage, label(fa) + " x " + label(fb), metric, t, n);
        for (int i = 0; i < a; i++) writeMean(stage, label(fa), A.get(i), metric, mA[i], sdOver(y, i, -1, b, n), n);
        for (int j = 0; j < b; j++) writeMean(stage, label(fb), B.get(j), metric, mB[j], sdOver(y, -1, j, a, n), n);
    }

    // ============================================================================
    // Stages II and III: one factor, instance as block
    // ============================================================================
    static void oneFactorBlocked(String stage, List<Row> rows, String f, String metric) {
        Map<String, Map<Integer, Double>> sum = new TreeMap<>(NUM);
        Map<String, Map<Integer, Integer>> cnt = new TreeMap<>(NUM);
        for (Row r : rows) {
            String l = r.v.get(f); int inst = Integer.parseInt(r.v.get("Instance"));
            sum.computeIfAbsent(l, k -> new TreeMap<>()).merge(inst, Double.parseDouble(r.v.get(metric)), Double::sum);
            cnt.computeIfAbsent(l, k -> new TreeMap<>()).merge(inst, 1, Integer::sum);
        }
        List<String> L = new ArrayList<>(sum.keySet());
        List<Integer> I = new ArrayList<>(sum.get(L.get(0)).keySet());
        int k = L.size(), n = I.size();
        double[][] y = new double[k][n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) {
            Map<Integer, Double> s = sum.get(L.get(i));
            if (!s.containsKey(I.get(j))) throw new IllegalStateException("Unbalanced data: " + L.get(i) + "/" + I.get(j));
            y[i][j] = s.get(I.get(j)) / cnt.get(L.get(i)).get(I.get(j));
        }
        double grand = 0; for (double[] u : y) for (double v : u) grand += v; grand /= (k * n);
        double[] mL = new double[k], mI = new double[n];
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) { mL[i] += y[i][j] / n; mI[j] += y[i][j] / k; }
        double ssT = 0, ssL = 0, ssI = 0;
        for (int i = 0; i < k; i++) for (int j = 0; j < n; j++) ssT += sq(y[i][j] - grand);
        for (int i = 0; i < k; i++) ssL += n * sq(mL[i] - grand);
        for (int j = 0; j < n; j++) ssI += k * sq(mI[j] - grand);
        double ssE = ssT - ssL - ssI;

        Source sE = new Source("Error", ssE, (k - 1) * (n - 1));
        List<Source> t = new ArrayList<>();
        t.add(new Source(label(f), ssL, k - 1));
        t.add(new Source("Instance (block)", ssI, n - 1));
        finish(t, sE);
        t.add(sE);
        t.add(new Source("Total", ssT, k * n - 1));

        print(stage, label(f), metric, t, n);
        for (int i = 0; i < k; i++) {
            double s = 0; for (int j = 0; j < n; j++) s += sq(y[i][j] - mL[i]);
            writeMean(stage, label(f), L.get(i), metric, mL[i], Math.sqrt(s / (n - 1)), n);
        }
    }

    // ---- F, p-value and critical value for each source ------------------------
    static void finish(List<Source> t, Source err) {
        for (Source s : t) {
            s.f = s.ms / err.ms;
            s.p = fUpperTail(s.f, s.df, err.df);
            s.fCrit = fCritical(ALPHA, s.df, err.df);
        }
    }

    // ---- Output -------------------------------------------------------------
    static void print(String stage, String param, String metric, List<Source> t, int n) {
        System.out.println();
        System.out.printf("=== %s: %s, response = %s (instances = %d, seeds averaged within instance) ===%n", stage, param, metric, n);
        System.out.printf("%-26s %12s %5s %12s %10s %12s %9s%n", "Source", "SS", "df", "MS", "F", "p-value", "F-crit");
        System.out.println("-".repeat(92));
        for (Source s : t) {
            if (Double.isNaN(s.f))
                System.out.printf(Locale.US, "%-26s %12.4f %5d %12s %10s %12s %9s%n", s.name, s.ss, s.df,
                        s.name.equals("Total") ? "" : String.format(Locale.US, "%.4f", s.ms), "", "", "");
            else
                System.out.printf(Locale.US, "%-26s %12.4f %5d %12.4f %10.4f %12s %9.4f%n", s.name, s.ss, s.df, s.ms, s.f, fmtP(s.p), s.fCrit);
            out.printf(Locale.US, "%s,%s,%s,%s,%.6f,%d,%s,%s,%s,%s%n", stage, param, metric, s.name, s.ss, s.df,
                    s.name.equals("Total") ? "" : String.format(Locale.US, "%.6f", s.ms),
                    Double.isNaN(s.f) ? "" : String.format(Locale.US, "%.6f", s.f),
                    Double.isNaN(s.p) ? "" : String.format(Locale.US, "%.6g", s.p),
                    Double.isNaN(s.fCrit) ? "" : String.format(Locale.US, "%.6f", s.fCrit));
        }
        out.flush();
    }

    static void writeMean(String stage, String param, String level, String metric, double mean, double sd, int n) {
        means.printf(Locale.US, "%s,%s,%s,%s,%.4f,%.4f,%d%n", stage, param, level, metric, mean, sd, n);
        means.flush();
    }

    static String fmtP(double p) { return p < 1e-4 ? String.format(Locale.US, "%.3E", p) : String.format(Locale.US, "%.4f", p); }

    // ============================================================================
    // F distribution (no external library)
    // ============================================================================
    // P(F > f) for F(d1, d2) = I_x(d2/2, d1/2) with x = d2 / (d2 + d1 f)
    static double fUpperTail(double f, int d1, int d2) {
        if (f <= 0) return 1.0;
        double x = d2 / (d2 + d1 * f);
        return regIncBeta(x, d2 / 2.0, d1 / 2.0);
    }

    // Critical value: smallest f with P(F > f) = alpha (bisection)
    static double fCritical(double alpha, int d1, int d2) {
        double lo = 0, hi = 1;
        while (fUpperTail(hi, d1, d2) > alpha) hi *= 2;
        for (int it = 0; it < 200; it++) {
            double mid = 0.5 * (lo + hi);
            if (fUpperTail(mid, d1, d2) > alpha) lo = mid; else hi = mid;
        }
        return 0.5 * (lo + hi);
    }

    // Regularized incomplete beta I_x(a, b) (continued fraction, Numerical Recipes)
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

    // Lanczos approximation of ln(Gamma(x))
    static double logGamma(double x) {
        double[] g = {676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
                12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
        if (x < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1 - x);
        x -= 1; double a = 0.99999999999980993; double t = x + 7.5;
        for (int i = 0; i < 8; i++) a += g[i] / (x + i + 1);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    // ============================================================================
    // helpers
    // ============================================================================
    static List<Row> read(String file) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FOLDER + File.separator + file))) {
            String[] h = br.readLine().split(",");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                Row r = new Row();
                for (int i = 0; i < h.length; i++) r.v.put(h[i].trim(), p[i].trim());
                rows.add(r);
            }
        }
        return rows;
    }

    interface Pred { boolean test(Row r); }
    static List<Row> filter(List<Row> rows, Pred p) { List<Row> o = new ArrayList<>(); for (Row r : rows) if (p.test(r)) o.add(r); return o; }

    static String label(String f) {
        switch (f) {
            case "Pop": return "Population size";
            case "Gen": return "Generations";
            case "Tournament": return "Tournament size";
            case "Crossover": return "Crossover rate";
            case "Mutation": return "Mutation rate";
            default: return f;
        }
    }

    static double sq(double x) { return x * x; }

    // SD over instances of the factor-level means (for the level-means file)
    static double sdOver(double[][][] y, int i, int j, int other, int n) {
        double[] v = new double[n]; double m = 0;
        for (int k = 0; k < n; k++) {
            double s = 0;
            for (int o = 0; o < other; o++) s += (i >= 0) ? y[i][o][k] : y[o][j][k];
            v[k] = s / other; m += v[k] / n;
        }
        double ss = 0; for (double w : v) ss += sq(w - m);
        return Math.sqrt(ss / (n - 1));
    }

    // Sort factor levels numerically ("25" < "100")
    static final Comparator<String> NUM = (s, t) -> Double.compare(Double.parseDouble(s), Double.parseDouble(t));
}