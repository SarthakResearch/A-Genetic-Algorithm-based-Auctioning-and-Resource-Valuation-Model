package GARVaverageEachRunResultEvaluation;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
//import java.util.List;
import org.w3c.dom.Node;

public class EvaluationGraphs {

    // ---- Settings to edit ------------------------------------------------------
    static String RESULTS_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Evaluation_Results";
    static String GRAPH_DIR   = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Evaluation_Graphs";
    static boolean IEI_ERROR_BARS = true;

    // Font sizes in points at the final printed size (90 mm wide)
    static double TITLE_PT = 9.5, TICK_PT = 7.0, ALGO_PT = 6.0, CATEGORY_PT = 6.5;

    static final String[] ALGOS = {"GARV", "Algo1", "Algo2", "Algo3"};   // bar order within each group
    static final int[] ROUNDS = {1, 2, 3, 4, 5};
    static final int[] TASKS = {50, 100, 150, 200, 250};                   // tasks per auction round

    // file name, title, column in the summary files ("IEI" is derived)
    static final String[][] GRAPHS = {
            {"CSR", "Combined Success Rate (CSR %)", "CSR"},
            {"PSR", "Preference Satisfaction Rate (PSR %)", "PSR"},
            {"IR", "Incentivization Rate (IR %)", "IR"},
            {"AIV", "Average Incentive per VM (AIV in cost unit)", "AIV"},
            {"IEI", "Incentivization Efficiency Index (IEI)", "IEI"},
            {"CVR", "Compensated Violation Rate (CVR %)", "CR"},
            {"ACC", "Average Compensation per Client (ACC in cost unit)", "ACC"},
            {"Makespan", "Makespan in Sec", "Makespan"},
            {"SW", "Social Welfare", "SW"},
    };

    // ---- Figure geometry: 90 mm x 56 mm at 600 dpi --------------------------------
    static final int DPI = 600;
    static final double WIDTH_MM = 90.0, HEIGHT_MM = 56.0;
    static final int W = (int) Math.round(WIDTH_MM / 25.4 * DPI);    // 2126 px
    static final int H = (int) Math.round(HEIGHT_MM / 25.4 * DPI);   // 1323 px
    static double pt(double points) { return points * DPI / 72.0; }  // points -> pixels

    // ---- Colours (as in the manuscript figures) ---------------------------------
    static final Color BAR = new Color(0x5B, 0x9B, 0xD5);
    static final Color TEXT = new Color(0x40, 0x40, 0x40);
    static final Color AXIS = new Color(0xA6, 0xA6, 0xA6);
    static final Color BORDER = new Color(0xD9, 0xD9, 0xD9);
    static final Color ERR = Color.BLACK;

    static String FONT = pickFont();

    // ============================================================================
    public static void main(String[] args) throws Exception {
        new File(GRAPH_DIR).mkdirs();

        // summary[algo][round] -> metric -> {mean, low, high}
        Map<String, Map<Integer, Map<String, double[]>>> data = new LinkedHashMap<>();
        for (String a : ALGOS) {
            Map<Integer, Map<String, double[]>> byRound = new LinkedHashMap<>();
            for (int r : ROUNDS) byRound.put(r, readSummary(a, r));
            data.put(a, byRound);
        }

        for (String[] g : GRAPHS) {
            double[][] mean = new double[ROUNDS.length][ALGOS.length];
            double[][] low = new double[ROUNDS.length][ALGOS.length];
            double[][] high = new double[ROUNDS.length][ALGOS.length];
            boolean bars = true;
            for (int ri = 0; ri < ROUNDS.length; ri++) {
                for (int ai = 0; ai < ALGOS.length; ai++) {
                    Map<String, double[]> s = data.get(ALGOS[ai]).get(ROUNDS[ri]);
                    double[] v;
                    if (g[2].equals("IEI")) {
                        double[] ir = need(s, "IR", ALGOS[ai], ROUNDS[ri]);
                        double[] aiv = need(s, "AIV", ALGOS[ai], ROUNDS[ri]);
                        v = new double[]{sqrt0(ir[0] * aiv[0]), sqrt0(ir[1] * aiv[1]), sqrt0(ir[2] * aiv[2])};
                        bars = IEI_ERROR_BARS;
                    } else {
                        v = need(s, g[2], ALGOS[ai], ROUNDS[ri]);
                    }
                    mean[ri][ai] = Math.max(0, v[0]);    // negative values drawn as 0
                    low[ri][ai] = Math.max(0, v[1]);
                    high[ri][ai] = Math.max(0, v[2]);
                }
            }
            File out = new File(GRAPH_DIR, g[0] + ".png");
            drawChart(g[1], mean, low, high, bars, out);
            System.out.println("Written: " + out.getAbsolutePath());
            printTable(g[0], mean, low, high);
        }
    }

    // ============================================================================
    // Drawing
    // ============================================================================
    static void drawChart(String title, double[][] mean, double[][] low, double[][] high,
                          boolean errorBars, File out) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        Font fTitle = new Font(FONT, Font.PLAIN, (int) Math.round(pt(TITLE_PT)));
        Font fTick = new Font(FONT, Font.PLAIN, (int) Math.round(pt(TICK_PT)));
        Font fAlgo = new Font(FONT, Font.PLAIN, (int) Math.round(pt(ALGO_PT)));
        Font fCat = new Font(FONT, Font.PLAIN, (int) Math.round(pt(CATEGORY_PT)));
        float thin = (float) pt(0.5);

        // outer border
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(thin));
        g.draw(new Rectangle2D.Double(thin, thin, W - 2 * thin, H - 2 * thin));

        // title
        g.setColor(TEXT);
        g.setFont(fTitle);
        FontMetrics fmT = g.getFontMetrics();
        double titleBase = pt(4) + fmT.getAscent();
        g.drawString(title, (float) ((W - fmT.stringWidth(title)) / 2.0), (float) titleBase);

        // y scale
        double maxV = 0;
        for (int r = 0; r < mean.length; r++)
            for (int a = 0; a < mean[r].length; a++)
                maxV = Math.max(maxV, errorBars ? Math.max(mean[r][a], high[r][a]) : mean[r][a]);
        double[] ticks = niceTicks(maxV);
        double yMax = ticks[ticks.length - 1];
        int decimals = decimalsFor(ticks.length > 1 ? ticks[1] - ticks[0] : 1);

        // label areas below the axis
        g.setFont(fAlgo);
        FontMetrics fmA = g.getFontMetrics();
        double algoBand = 0;
        for (String a : ALGOS) algoBand = Math.max(algoBand, fmA.stringWidth(a));
        algoBand += pt(4);
        g.setFont(fCat);
        FontMetrics fmC = g.getFontMetrics();
        double catRow = fmC.getHeight() + pt(3);

        g.setFont(fTick);
        FontMetrics fmK = g.getFontMetrics();
        double tickW = 0;
        for (double t : ticks) tickW = Math.max(tickW, fmK.stringWidth(fmt(t, decimals)));

        double plotLeft = pt(4) + tickW + pt(3);
        double plotRight = W - pt(6);
        double plotTop = titleBase + pt(6);
        double axisY = H - pt(3) - 2 * catRow - algoBand;
        double plotH = axisY - plotTop;

        // y tick labels (no gridlines)
        g.setColor(TEXT);
        for (double t : ticks) {
            double y = axisY - t / yMax * plotH;
            String s = fmt(t, decimals);
            g.drawString(s, (float) (plotLeft - pt(3) - fmK.stringWidth(s)), (float) (y + fmK.getAscent() / 2.0 - pt(0.5)));
        }

        // bars, error bars, labels
        int nG = mean.length, nA = ALGOS.length;
        double groupW = (plotRight - plotLeft) / nG;
        double slot = groupW / (nA + 0.6);
        double barW = slot * 0.55;
        for (int r = 0; r < nG; r++) {
            double gx = plotLeft + r * groupW;
            for (int a = 0; a < nA; a++) {
                double cx = gx + slot * 0.3 + slot * (a + 0.5);
                double hBar = mean[r][a] / yMax * plotH;
                g.setColor(BAR);
                g.fill(new Rectangle2D.Double(cx - barW / 2, axisY - hBar, barW, hBar));

                if (errorBars && high[r][a] > low[r][a]) {
                    double yLo = axisY - low[r][a] / yMax * plotH;
                    double yHi = axisY - high[r][a] / yMax * plotH;
                    double cap = barW * 0.5;
                    g.setColor(ERR);
                    g.setStroke(new BasicStroke((float) pt(0.5)));
                    g.draw(new Line2D.Double(cx, yLo, cx, yHi));
                    g.draw(new Line2D.Double(cx - cap / 2, yLo, cx + cap / 2, yLo));
                    g.draw(new Line2D.Double(cx - cap / 2, yHi, cx + cap / 2, yHi));
                }

                // algorithm name, rotated, reading upwards
                g.setColor(TEXT);
                g.setFont(fAlgo);
                AffineTransform old = g.getTransform();
                g.translate(cx + fmA.getAscent() / 2.0 - pt(0.4), axisY + pt(2) + fmA.stringWidth(ALGOS[a]));
                g.rotate(-Math.PI / 2);
                g.drawString(ALGOS[a], 0f, 0f);
                g.setTransform(old);
            }
            // "Auction n" and "n Tasks" rows
            g.setFont(fCat);
            String l1 = "Auction " + ROUNDS[r], l2 = TASKS[r] + " Tasks";
            double mid = gx + groupW / 2;
            g.drawString(l1, (float) (mid - fmC.stringWidth(l1) / 2.0), (float) (axisY + algoBand + fmC.getAscent() + pt(1)));
            g.drawString(l2, (float) (mid - fmC.stringWidth(l2) / 2.0), (float) (axisY + algoBand + catRow + fmC.getAscent() + pt(1)));
        }

        // category axis: horizontal line at 0 and vertical separators between groups
        g.setColor(AXIS);
        g.setStroke(new BasicStroke(thin));
        g.draw(new Line2D.Double(plotLeft, axisY, plotRight, axisY));
        double sepBottom = axisY + algoBand + 2 * catRow;
        for (int r = 0; r <= nG; r++) {
            double x = plotLeft + r * groupW;
            g.draw(new Line2D.Double(x, axisY, x, sepBottom));
        }
        g.dispose();
        writePng(img, out);
    }

    // ---- "nice" y-axis ticks from 0 to just above max ---------------------------
    static double[] niceTicks(double max) {
        if (max <= 0) max = 1;
        double raw = max / 8;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double[] steps = {1, 2, 2.5, 5, 10};
        double step = 10 * mag;
        for (double s : steps) if (s * mag >= raw) { step = s * mag; break; }
        int n = (int) Math.ceil(max / step - 1e-9);
        if (n * step < max * 1.0001) n++;   // leave a little headroom above the tallest bar
        double[] t = new double[n + 1];
        for (int i = 0; i <= n; i++) t[i] = i * step;
        return t;
    }

    static int decimalsFor(double step) {
        if (step >= 1 && Math.abs(step - Math.rint(step)) < 1e-9) return 0;
        if (step * 10 >= 1 && Math.abs(step * 10 - Math.rint(step * 10)) < 1e-9) return 1;
        return 2;
    }

    static String fmt(double v, int dec) { return String.format(Locale.US, "%." + dec + "f", v); }

    // ---- PNG with 600 dpi stored in the file (so it opens at 90 mm wide) ---------
    static void writePng(BufferedImage img, File out) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(img);
        IIOMetadata meta = writer.getDefaultImageMetadata(type, param);
        String fmt = "javax_imageio_png_1.0";
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(fmt);
        IIOMetadataNode phys = new IIOMetadataNode("pHYs");
        String ppm = Integer.toString((int) Math.round(DPI / 0.0254));
        phys.setAttribute("pixelsPerUnitXAxis", ppm);
        phys.setAttribute("pixelsPerUnitYAxis", ppm);
        phys.setAttribute("unitSpecifier", "meter");
        Node existing = null;
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling())
            if (n.getNodeName().equals("pHYs")) existing = n;
        if (existing != null) root.removeChild(existing);
        root.appendChild(phys);
        meta.mergeTree(fmt, root);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(new IIOImage(img, null, meta));
        } finally {
            writer.dispose();
        }
    }

    // ============================================================================
    // Reading the summary files
    // ============================================================================
    static Map<String, double[]> readSummary(String algo, int round) throws IOException {
        File f = new File(RESULTS_DIR, algo + "_AR" + round + "_Summary.csv");
        if (!f.exists()) throw new FileNotFoundException("Missing summary file: " + f.getAbsolutePath());
        Map<String, double[]> m = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cM = idx(h, "Metric"), cMean = idx(h, "Mean"), cLo = idx(h, "CI95_Low"), cHi = idx(h, "CI95_High");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p[cMean].trim().isEmpty()) continue;   // metric not applicable (blank)
                m.put(p[cM].trim(), new double[]{Double.parseDouble(p[cMean]), Double.parseDouble(p[cLo]), Double.parseDouble(p[cHi])});
            }
        }
        return m;
    }

    static double[] need(Map<String, double[]> s, String metric, String algo, int round) {
        double[] v = s.get(metric);
        if (v == null) throw new IllegalStateException("Metric " + metric + " not found in " + algo + "_AR" + round + "_Summary.csv");
        return v;
    }

    static int idx(String[] h, String name) {
        for (int i = 0; i < h.length; i++) if (h[i].trim().equals(name)) return i;
        throw new IllegalStateException("Column " + name + " not found in summary file");
    }

    static double sqrt0(double x) { return x > 0 ? Math.sqrt(x) : 0; }

    static void printTable(String name, double[][] mean, double[][] low, double[][] high) {
        StringBuilder b = new StringBuilder("   " + name + ":");
        for (int r = 0; r < mean.length; r++) {
            b.append("  AR").append(ROUNDS[r]).append(" [");
            for (int a = 0; a < mean[r].length; a++)
                b.append(a > 0 ? ", " : "").append(String.format(Locale.US, "%.2f", mean[r][a]));
            b.append("]");
        }
        System.out.println(b);
    }

    // Calibri (as in the manuscript figures) if installed, otherwise a sans-serif font
    static String pickFont() {
        Set<String> fonts = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Calibri", "Carlito", "Arial", "Liberation Sans", "DejaVu Sans"})
            if (fonts.contains(f)) return f;
        return Font.SANS_SERIF;
    }
}
