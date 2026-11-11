package GARVaverageEachRunResultEvaluation;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
public class ScalabilityGraphs {

    // ---- Settings to edit ------------------------------------------------------
    static String RESULTS_DIR = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Evaluation_Results";
    static String SCAL_DIR    = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Scalability_Results";
    static String GRAPH_DIR   = "G:\\My Drive\\PhD\\Contributions\\4. Contribution 3 (GARV)\\Revision (Continued)\\H\\Revision 4\\New Results\\Scalability_Results";
    static boolean ERROR_BARS = true;

    static final int[] ROUNDS = {1, 2, 3, 4, 5};
    static final int[] TASKS = {50, 100, 150, 200, 250};   // tasks per auction round

    // Font sizes in points at the final printed size (90 mm wide)
    static double TITLE_PT = 9.5, TICK_PT = 7.0, AXIS_TITLE_PT = 7.0;

    static final int DPI = EvaluationGraphs.DPI, W = EvaluationGraphs.W, H = EvaluationGraphs.H;
    static double pt(double p) { return EvaluationGraphs.pt(p); }
    static final Color LINE = EvaluationGraphs.BAR, TEXT = EvaluationGraphs.TEXT,
            AXIS = EvaluationGraphs.AXIS, BORDER = EvaluationGraphs.BORDER, ERR = EvaluationGraphs.ERR;

    public static void main(String[] args) throws Exception {
        new File(GRAPH_DIR).mkdirs();

        // ---- Scenario 1 ----------------------------------------------------------
        int n1 = ROUNDS.length;
        double[] x1 = new double[n1], m1 = new double[n1], lo1 = new double[n1], hi1 = new double[n1];
        for (int i = 0; i < n1; i++) {
            double[] v = EvaluationGraphs.need(EvaluationGraphs.readSummary("GARV", ROUNDS[i]), "GATimeSec", "GARV", ROUNDS[i]);
            x1[i] = TASKS[i]; m1[i] = v[0]; lo1[i] = v[1]; hi1[i] = v[2];
        }
        File f1 = new File(GRAPH_DIR, "Runtime_Tasks.png");
        drawLine("Runtime Trend by Varying Number of Tasks", "Number of Tasks", "Runtime (s)", x1, m1, lo1, hi1, f1);
        print("Scenario 1 (tasks, 100 VMs)", "Tasks", x1, m1, lo1, hi1, f1);

        // ---- Scenario 2 ----------------------------------------------------------
        java.util.List<double[]> rows = readScalability(new File(SCAL_DIR, "Scalability_VMs.csv"));
        int n2 = rows.size();
        double[] x2 = new double[n2], m2 = new double[n2], lo2 = new double[n2], hi2 = new double[n2];
        for (int i = 0; i < n2; i++) {
            double[] r = rows.get(i);
            x2[i] = r[0]; m2[i] = r[1]; lo2[i] = r[2]; hi2[i] = r[3];
        }
        File f2 = new File(GRAPH_DIR, "Runtime_VMs.png");
        drawLine("Runtime Trend by Varying Number of VMs", "Number of VMs", "Runtime (s)", x2, m2, lo2, hi2, f2);
        print("Scenario 2 (VMs, " + (n2 > 0 ? (int) rows.get(0)[4] : 0) + " tasks)", "VMs", x2, m2, lo2, hi2, f2);
    }

    // ============================================================================
    static void drawLine(String title, String xTitle, String yTitle, double[] x, double[] mean,
                         double[] low, double[] high, File out) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        String font = EvaluationGraphs.FONT;
        Font fTitle = new Font(font, Font.PLAIN, (int) Math.round(pt(TITLE_PT)));
        Font fTick = new Font(font, Font.PLAIN, (int) Math.round(pt(TICK_PT)));
        Font fAxis = new Font(font, Font.PLAIN, (int) Math.round(pt(AXIS_TITLE_PT)));
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

        // y scale from 0
        double maxV = 0;
        for (int i = 0; i < mean.length; i++) maxV = Math.max(maxV, ERROR_BARS ? Math.max(mean[i], high[i]) : mean[i]);
        double[] ticks = EvaluationGraphs.niceTicks(maxV);
        double yMax = ticks[ticks.length - 1];
        int dec = EvaluationGraphs.decimalsFor(ticks.length > 1 ? ticks[1] - ticks[0] : 1);

        g.setFont(fTick);
        FontMetrics fmK = g.getFontMetrics();
        double tickW = 0;
        for (double t : ticks) tickW = Math.max(tickW, fmK.stringWidth(EvaluationGraphs.fmt(t, dec)));
        g.setFont(fAxis);
        FontMetrics fmX = g.getFontMetrics();

        double plotLeft = pt(3) + fmX.getHeight() + pt(2) + tickW + pt(3);
        double plotRight = W - pt(8);
        double plotTop = titleBase + pt(6);
        double axisY = H - pt(3) - fmX.getHeight() - pt(2) - fmK.getHeight() - pt(2);
        double plotH = axisY - plotTop;

        // x positions: equally spaced categories (the sizes are equally spaced)
        int n = x.length;
        double slotW = (plotRight - plotLeft) / n;
        double[] px = new double[n], py = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = plotLeft + slotW * (i + 0.5);
            py[i] = axisY - Math.max(0, mean[i]) / yMax * plotH;
        }

        // y tick labels (no gridlines)
        g.setFont(fTick);
        g.setColor(TEXT);
        for (double t : ticks) {
            double y = axisY - t / yMax * plotH;
            String s = EvaluationGraphs.fmt(t, dec);
            g.drawString(s, (float) (plotLeft - pt(3) - fmK.stringWidth(s)), (float) (y + fmK.getAscent() / 2.0 - pt(0.5)));
        }
        // x tick labels
        for (int i = 0; i < n; i++) {
            String s = String.valueOf((int) Math.round(x[i]));
            g.drawString(s, (float) (px[i] - fmK.stringWidth(s) / 2.0), (float) (axisY + pt(2) + fmK.getAscent()));
        }

        // axes
        g.setColor(AXIS);
        g.setStroke(new BasicStroke(thin));
        g.draw(new Line2D.Double(plotLeft, axisY, plotRight, axisY));
        g.draw(new Line2D.Double(plotLeft, axisY, plotLeft, plotTop));

        // axis titles
        g.setColor(TEXT);
        g.setFont(fAxis);
        g.drawString(xTitle, (float) ((plotLeft + plotRight) / 2.0 - fmX.stringWidth(xTitle) / 2.0),
                (float) (H - pt(3) - fmX.getDescent()));
        AffineTransform old = g.getTransform();
        g.translate(pt(3) + fmX.getAscent(), (plotTop + axisY) / 2.0 + fmX.stringWidth(yTitle) / 2.0);
        g.rotate(-Math.PI / 2);
        g.drawString(yTitle, 0f, 0f);
        g.setTransform(old);

        // line
        g.setColor(LINE);
        g.setStroke(new BasicStroke((float) pt(1.25), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D path = new Path2D.Double();
        for (int i = 0; i < n; i++) { if (i == 0) path.moveTo(px[i], py[i]); else path.lineTo(px[i], py[i]); }
        g.draw(path);

        // error bars
        if (ERROR_BARS) {
            g.setColor(ERR);
            g.setStroke(new BasicStroke((float) pt(0.5)));
            double cap = pt(3);
            for (int i = 0; i < n; i++) {
                if (!(high[i] > low[i])) continue;
                double yLo = axisY - Math.max(0, low[i]) / yMax * plotH;
                double yHi = axisY - Math.max(0, high[i]) / yMax * plotH;
                g.draw(new Line2D.Double(px[i], yLo, px[i], yHi));
                g.draw(new Line2D.Double(px[i] - cap / 2, yLo, px[i] + cap / 2, yLo));
                g.draw(new Line2D.Double(px[i] - cap / 2, yHi, px[i] + cap / 2, yHi));
            }
        }

        // markers
        double r = pt(1.8);
        for (int i = 0; i < n; i++) {
            g.setColor(LINE);
            g.fill(new Ellipse2D.Double(px[i] - r, py[i] - r, 2 * r, 2 * r));
        }
        g.dispose();
        EvaluationGraphs.writePng(img, out);
    }

    
    static java.util.List<double[]> readScalability(File f) throws IOException {
        if (!f.exists()) throw new FileNotFoundException("Missing file (run ScalabilityRunner first): " + f.getAbsolutePath());
        java.util.List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String[] h = br.readLine().split(",", -1);
            int cV = EvaluationGraphs.idx(h, "VMs"), cM = EvaluationGraphs.idx(h, "Mean"),
                    cL = EvaluationGraphs.idx(h, "CI95_Low"), cH = EvaluationGraphs.idx(h, "CI95_High"),
                    cT = EvaluationGraphs.idx(h, "Tasks");
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",", -1);
                rows.add(new double[]{Double.parseDouble(p[cV]), Double.parseDouble(p[cM]),
                        Double.parseDouble(p[cL]), Double.parseDouble(p[cH]), Double.parseDouble(p[cT])});
            }
        }
        rows.sort(Comparator.comparingDouble(a -> a[0]));
        return rows;
    }

    static void print(String label, String xName, double[] x, double[] m, double[] lo, double[] hi, File f) {
        System.out.println("Written: " + f.getAbsolutePath());
        System.out.println("   " + label + ":");
        for (int i = 0; i < x.length; i++)
            System.out.printf(Locale.US, "     %-5s %4d   mean %.4f s   95%% CI [%.4f, %.4f]%n", xName, (int) Math.round(x[i]), m[i], lo[i], hi[i]);
    }
}
