import java.io.*;
import java.util.*;

public class StabilityAnalysis {


    static String RESULTS_DIR = "Results\\Evaluation_Results";
    static String OUT_FILE = "Stability_Analysis.csv";   
    static final int[] ROUNDS = {1, 2, 3, 4, 5};

    public static void main(String[] args) throws Exception {
        File out = new File(RESULTS_DIR, OUT_FILE);
        try (PrintWriter w = new PrintWriter(new FileWriter(out))) {
            w.println("Round,Comparisons,Mean Similarity (%),Min Similarity (%),Max Similarity (%),Chance Level (%),CV of CSR (%),CV of PSR (%)");
            System.out.printf("%-6s %11s %10s %9s %9s %9s %10s %10s%n",
                    "Round", "Comparisons", "Mean Sim", "Min Sim", "Max Sim", "Chance", "CV CSR", "CV PSR");
            System.out.println("-".repeat(82));

            for (int r : ROUNDS) {
                // ---- outcome level + instance sizes from Runs.csv -------------------
                RunsData rd = readRuns(r);

                // ---- allocation level from Allocations.csv --------------------------
                // instance -> run -> set of "task:vm" pairs
                Map<Integer, Map<Integer, Set<String>>> alloc = readAllocations(r);

                double sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                long n = 0;
                double chanceSum = 0;
                for (Map.Entry<Integer, Map<Integer, Set<String>>> e : alloc.entrySet()) {
                    int inst = e.getKey();
                    List<Set<String>> runs = new ArrayList<>(e.getValue().values());
                    for (int a = 0; a < runs.size(); a++)
                        for (int b = a + 1; b < runs.size(); b++) {
                            Set<String> A = runs.get(a), B = runs.get(b);
                            int common = 0;
                            for (String p : A) if (B.contains(p)) common++;
                            double s = 100.0 * common / Math.max(A.size(), B.size());
                            sum += s; n++;
                            min = Math.min(min, s); max = Math.max(max, s);
                        }
                    int[] tv = rd.sizes.get(inst);
                    if (tv == null) throw new IllegalStateException("Instance " + inst + " of AR" + r + " not found in Runs.csv");
                    chanceSum += 100.0 / Math.max(tv[0], tv[1]);
                }
                if (n == 0) throw new IllegalStateException("No run pairs found for AR" + r);
                double meanSim = sum / n;
                double chance = chanceSum / alloc.size();

                double cvCsr = meanCv(rd.csr), cvPsr = meanCv(rd.psr);

                w.println(String.format(Locale.US, "AR%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                        r, n, meanSim, min, max, chance, cvCsr, cvPsr));
                System.out.printf(Locale.US, "%-6s %11d %10.2f %9.2f %9.2f %9.2f %10.2f %10.2f%n",
                        "AR" + r, n, meanSim, min, max, chance, cvCsr, cvPsr);
            }
        }
        System.out.println();
        System.out.println("Written: " + out.getAbsolutePath());
    }

    // ---- mean over instances of the CV (%) of the values of their runs ----------
    static double meanCv(Map<Integer, List<Double>> byInstance) {
        double s = 0;
        int k = 0;
        for (List<Double> v : byInstance.values()) {
            if (v.size() < 2) continue;
            double m = 0;
            for (double x : v) m += x;
            m /= v.size();
            double ss = 0;
            for (double x : v) ss += (x - m) * (x - m);
            double sd = Math.sqrt(ss / (v.size() - 1));
            if (m == 0) throw new IllegalStateException("Mean is zero for an instance; CV undefined");
            s += sd / m * 100.0;
            k++;
        }
        return s / k;
    }

    // ---- Readers ----------------------------------------------------------------
    static class RunsData {
        Map<Integer, int[]> sizes = new HashMap<>();              // instance -> {tasks, vms}
        Map<Integer, List<Double>> csr = new TreeMap<>(), psr = new TreeMap<>();
    }

    static RunsData readRuns(int round) throws IOException {
        File f = new File(RESULTS_DIR, "GARV_AR" + round + "_Runs.csv");
        if (!f.exists()) throw new FileNotFoundException("Missing file: " + f.getAbsolutePath());
        RunsData d = new RunsData();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cI = idx(h, "Instance"), cT = idx(h, "Tasks"), cV = idx(h, "VMs"),
                    cC = idx(h, "CSR"), cP = idx(h, "PSR");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                int inst = Integer.parseInt(p[cI].trim());
                d.sizes.put(inst, new int[]{Integer.parseInt(p[cT].trim()), Integer.parseInt(p[cV].trim())});
                d.csr.computeIfAbsent(inst, k -> new ArrayList<>()).add(Double.parseDouble(p[cC]));
                d.psr.computeIfAbsent(inst, k -> new ArrayList<>()).add(Double.parseDouble(p[cP]));
            }
        }
        return d;
    }

    static Map<Integer, Map<Integer, Set<String>>> readAllocations(int round) throws IOException {
        File f = new File(RESULTS_DIR, "GARV_AR" + round + "_Allocations.csv");
        if (!f.exists()) throw new FileNotFoundException("Missing file: " + f.getAbsolutePath());
        Map<Integer, Map<Integer, Set<String>>> m = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cI = idx(h, "Instance"), cR = idx(h, "Run"), cT = idx(h, "TaskID"), cV = idx(h, "VMID");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                int inst = Integer.parseInt(p[cI].trim()), run = Integer.parseInt(p[cR].trim());
                m.computeIfAbsent(inst, k -> new TreeMap<>())
                 .computeIfAbsent(run, k -> new HashSet<>())
                 .add(p[cT].trim() + ":" + p[cV].trim());
            }
        }
        return m;
    }

    static int idx(String[] h, String name) {
        for (int i = 0; i < h.length; i++) if (h[i].trim().equals(name)) return i;
        throw new IllegalStateException("Column " + name + " not found");
    }
}
