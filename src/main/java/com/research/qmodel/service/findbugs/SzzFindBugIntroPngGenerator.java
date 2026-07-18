package com.research.qmodel.service.findbugs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Generates a slide-ready PNG that visualizes how findBugIntroducingCommits works.
 */
public class SzzFindBugIntroPngGenerator {

    private static final int W = 1920;
    private static final int H = 1080;

    private static final Color BG = new Color(246, 249, 253);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(210, 220, 232);
    private static final Color TITLE = new Color(31, 41, 55);
    private static final Color MUTED = new Color(107, 114, 128);

    private static final Color BLUE = new Color(59, 108, 255);
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color ORANGE = new Color(245, 158, 11);
    private static final Color PINK = new Color(236, 72, 153);
    private static final Color PURPLE = new Color(124, 58, 237);

    public static void main(String[] args) throws Exception {
        String output = args.length > 0
                ? args[0]
                : "docs/presentation/szz_findBugIntroducingCommits_presentation.png";

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            setup(g);
            paint(g);
        } finally {
            g.dispose();
        }

        Path out = Path.of(output);
        File parent = out.toFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create output directory: " + parent);
        }
        ImageIO.write(img, "png", out.toFile());
        System.out.println("Generated: " + out.toAbsolutePath());
    }

    private static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void paint(Graphics2D g) {
        GradientPaint gp = new GradientPaint(0, 0, BG, 0, H, new Color(241, 246, 252));
        g.setPaint(gp);
        g.fillRect(0, 0, W, H);

        // Header
        drawCard(g, 24, 20, W - 48, 90, 16);
        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        g.drawString("HOW findBugIntroducingCommits WORKS (SZZ)", 48, 62);
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Issue lookup -> changed-line extraction -> traceLineToCommit -> candidate bug-introducing commits", 48, 92);

        // Left column: method flow
        drawCard(g, 24, 130, 1160, 820, 18);
        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("MAIN FLOW IN BasicBugFinder.findBugIntroducingCommits", 46, 172);

        int x = 52;
        int y = 200;
        int w = 1102;
        int h = 84;
        int gap = 20;

        drawFlowStep(g, x, y + 0 * (h + gap), w, h, BLUE,
                "1) Load issue by id",
                "projectIssueRepository.findIssueById(repo, owner, issueId); throw if missing");
        drawFlowStep(g, x, y + 1 * (h + gap), w, h, GREEN,
                "2) Return cache if present",
                "if bugIntroducingCommits already exists -> return cached list");
        drawFlowStep(g, x, y + 2 * (h + gap), w, h, ORANGE,
                "3) Iterate fixing commits and files",
                "for each fixing commit -> for each FileChange -> parse changed lines from patch");
        drawFlowStep(g, x, y + 3 * (h + gap), w, h, PINK,
                "4) Trace each changed line",
                "traceLineToCommit(repoPath, fileName, line, currentCommitSha, depth)");
        drawFlowStep(g, x, y + 4 * (h + gap), w, h, PURPLE,
                "5) Aggregate candidates",
                "if non-empty trace result: candidateCommits.addAll(result)");
        drawFlowStep(g, x, y + 5 * (h + gap), w, h, BLUE,
                "6) Return candidate bug-introducing commits",
                "method returns List<Commit> candidateCommits");

        // arrows between steps
        for (int i = 0; i < 5; i++) {
            int y1 = y + i * (h + gap) + h;
            int y2 = y + (i + 1) * (h + gap);
            drawArrow(g, x + w / 2, y1 + 4, x + w / 2, y2 - 4, new Color(156, 168, 184), 4);
        }

        // right column: helper methods + stop conditions
        drawCard(g, 1210, 130, W - 1234, 820, 18);
        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("HELPERS + STOP CONDITIONS", 1230, 172);

        drawMiniCard(g, 1230, 200, 660, 170, "getChangedLineNumbers(patch)",
                new String[]{
                        "- Parses unified diff hunks: @@ -a,b +c,d @@",
                        "- Tracks new-file line cursor",
                        "- Collects added/modified line numbers"
                }, BLUE);

        drawMiniCard(g, 1230, 390, 660, 170, "traceLineToCommit(...)",
                new String[]{
                        "- Iterative tracing up to depth",
                        "- Stops if commit already visited",
                        "- Stops if blame returns null or same commit"
                }, GREEN);

        drawMiniCard(g, 1230, 580, 660, 170, "getBlamedCommit(...)",
                new String[]{
                        "- JGit blame on file + start commit",
                        "- Follow file renames",
                        "- Recovery path for orphaned/missing commits"
                }, ORANGE);

        // Footer
        drawCard(g, 24, 968, W - 48, 88, 16);
        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Presentation message:", 46, 1002);
        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("We start from fixing commits, isolate changed lines from patches, then trace those lines backward via blame", 230, 1002);
        g.drawString("to collect likely bug-introducing commits under depth and cycle safety constraints.", 230, 1028);
    }

    private static void drawCard(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(0, 0, 0, 20));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 6, w, h, arc, arc));
        g.setColor(CARD);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.3f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    private static void drawFlowStep(Graphics2D g, int x, int y, int w, int h, Color accent, String title, String subtitle) {
        g.setColor(new Color(252, 254, 255));
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 14, 14);

        g.setColor(accent);
        g.fillRoundRect(x + 4, y + 6, 10, h - 12, 6, 6);

        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, x + 24, y + 34);

        g.setColor(MUTED);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.drawString(subtitle, x + 24, y + 60);
    }

    private static void drawMiniCard(Graphics2D g, int x, int y, int w, int h, String title, String[] lines, Color accent) {
        g.setColor(new Color(251, 253, 255));
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 14, 14);

        g.setColor(accent);
        g.fillRoundRect(x + 6, y + 8, 9, h - 16, 6, 6);

        g.setColor(TITLE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(title, x + 24, y + 34);

        g.setColor(new Color(71, 85, 105));
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        int yy = y + 64;
        for (String line : lines) {
            g.drawString(line, x + 24, yy);
            yy += 28;
        }
    }

    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2, Color color, int width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2, y2);

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return;
        double ux = dx / len;
        double uy = dy / len;

        double ah = 12;
        double aw = 8;
        double bx = x2 - ux * ah;
        double by = y2 - uy * ah;
        double px = -uy;
        double py = ux;

        Path2D head = new Path2D.Double();
        head.moveTo(x2, y2);
        head.lineTo(bx + px * aw, by + py * aw);
        head.lineTo(bx - px * aw, by - py * aw);
        head.closePath();
        g.fill(head);
    }
}

