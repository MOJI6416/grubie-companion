import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IconAtlas {
    private static final int[] SIZES = {12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48};
    private static final Map<String, List<String>> ICONS = new LinkedHashMap<>();

    static {
        icon("users", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "circle 9 7 4", "M22 21v-2a4 4 0 0 0-3-3.87", "M16 3.13a4 4 0 0 1 0 7.75");
        icon("user-plus", "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "circle 9 7 4", "M19 8v6", "M22 11h-6");
        icon("mic", "rect 9 2 6 12 3", "M19 10v1a7 7 0 0 1-14 0v-1", "M12 18v4");
        icon("mic-off", "M2 2l20 20", "M18.89 13.23A7.12 7.12 0 0 0 19 12v-2", "M5 10v2a7 7 0 0 0 12 5", "M15 9.34V5a3 3 0 0 0-5.68-1.33", "M9 9v3a3 3 0 0 0 5.12 2.12", "M12 19v3");
        icon("headphones", "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3");
        icon("headphones-off", "M21 14h-1.343", "M9.128 3.47A9 9 0 0 1 21 12v3.343", "M2 2l20 20", "M20.414 20.414A2 2 0 0 1 19 21h-1a2 2 0 0 1-2-2v-3", "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 2.636-6.364");
        icon("x", "M18 6 6 18", "M6 6l12 12");
        icon("check", "M20 6 9 17l-5-5");
        icon("send", "M22 2l-7 20-4-9-9-4Z", "M22 2 11 13");
        icon("camera", "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z", "circle 12 13 3");
        icon("trophy", "M6 9H4.5a2.5 2.5 0 0 1 0-5H6", "M18 9h1.5a2.5 2.5 0 0 0 0-5H18", "M4 22h16", "M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22", "M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22", "M18 2H6v7a6 6 0 0 0 12 0V2Z");
        icon("radio", "M4.9 19.1C1 15.2 1 8.8 4.9 4.9", "M7.8 16.2c-2.3-2.3-2.3-6.1 0-8.5", "circle 12 12 2", "M16.2 7.8c2.3 2.3 2.3 6.1 0 8.5", "M19.1 4.9C23 8.8 23 15.1 19.1 19");
        icon("server", "rect 2 2 20 8 2", "rect 2 14 20 8 2", "M6 6h.01", "M6 18h.01");
        icon("signal", "M2 20h.01", "M7 20v-4", "M12 20v-8", "M17 20V8");
        icon("chevron-right", "M9 18l6-6-6-6");
        icon("chevron-down", "M6 9l6 6 6-6");
        icon("chevron-left", "M15 18l-6-6 6-6");
        icon("copy", "rect 9 9 13 13 2", "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1");
        icon("message", "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z");
        icon("log-out", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9");
        icon("search", "circle 11 11 8", "M21 21l-4.3-4.3");
        icon("globe", "circle 12 12 10", "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20");
        icon("alert", "M21.73 18l-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01");
        icon("gamepad", "M6 12h4", "M8 10v4", "M15 13h.01", "M18 11h.01", "rect 2 6 20 12 2");
        icon("link", "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71", "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71");
        icon("stop", "rect 6 6 12 12 2");
        icon("sliders", "M21 4H14", "M10 4H3", "M21 12H12", "M8 12H3", "M21 20H16", "M12 20H3", "M14 2V6", "M8 10V14", "M16 18V22");
    }

    private static void icon(String name, String... parts) {
        ICONS.put(name, List.of(parts));
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        int cell = 0;
        int width = 0;
        for (int size : SIZES) width = Math.max(width, ICONS.size() * (size + 2));
        int height = 0;
        for (int size : SIZES) height += size + 2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(java.awt.Color.WHITE);
        StringBuilder json = new StringBuilder("{\"sizes\":[");
        for (int i = 0; i < SIZES.length; i++) json.append(i == 0 ? "" : ",").append(SIZES[i]);
        json.append("],\"names\":[");
        int n = 0;
        for (String name : ICONS.keySet()) json.append(n++ == 0 ? "" : ",").append('"').append(name).append('"');
        json.append("],\"rows\":[");
        int y = 0;
        for (int s = 0; s < SIZES.length; s++) {
            int size = SIZES[s];
            json.append(s == 0 ? "" : ",").append(y);
            int x = 0;
            for (List<String> parts : ICONS.values()) {
                Graphics2D cellG = (Graphics2D) g.create();
                cellG.translate(x + 1, y + 1);
                double scale = size / 24.0;
                cellG.scale(scale, scale);
                float stroke = (float) Math.max(2.0, 1.35 / scale);
                cellG.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (String part : parts) cellG.draw(shape(part));
                cellG.dispose();
                x += size + 2;
            }
            y += size + 2;
            cell++;
        }
        json.append("]}");
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
        Files.writeString(new File(out.getParentFile(), "icons.json").toPath(), json.toString(), StandardCharsets.UTF_8);
        System.out.println("icons " + ICONS.size() + " x " + SIZES.length + " -> " + out + " " + width + "x" + height);
    }

    private static Shape shape(String spec) {
        String[] p = spec.split(" ");
        if (p[0].equals("circle")) {
            double cx = Double.parseDouble(p[1]);
            double cy = Double.parseDouble(p[2]);
            double r = Double.parseDouble(p[3]);
            return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        }
        if (p[0].equals("rect")) {
            double x = Double.parseDouble(p[1]);
            double y = Double.parseDouble(p[2]);
            double w = Double.parseDouble(p[3]);
            double h = Double.parseDouble(p[4]);
            double r = Double.parseDouble(p[5]);
            return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
        }
        return path(spec);
    }

    private static Path2D path(String d) {
        Path2D.Double path = new Path2D.Double();
        List<String> tokens = tokenize(d);
        int i = 0;
        char cmd = 0;
        double x = 0, y = 0, sx = 0, sy = 0, cx = 0, cy = 0;
        while (i < tokens.size()) {
            String t = tokens.get(i);
            if (Character.isLetter(t.charAt(0))) {
                cmd = t.charAt(0);
                i++;
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    x = num(tokens, i++) + (rel ? x : 0);
                    y = num(tokens, i++) + (rel ? y : 0);
                    path.moveTo(x, y);
                    sx = x;
                    sy = y;
                    cx = x;
                    cy = y;
                    cmd = rel ? 'l' : 'L';
                }
                case 'L' -> {
                    x = num(tokens, i++) + (rel ? x : 0);
                    y = num(tokens, i++) + (rel ? y : 0);
                    path.lineTo(x, y);
                    cx = x;
                    cy = y;
                }
                case 'H' -> {
                    x = num(tokens, i++) + (rel ? x : 0);
                    path.lineTo(x, y);
                    cx = x;
                    cy = y;
                }
                case 'V' -> {
                    y = num(tokens, i++) + (rel ? y : 0);
                    path.lineTo(x, y);
                    cx = x;
                    cy = y;
                }
                case 'C' -> {
                    double x1 = num(tokens, i++) + (rel ? x : 0);
                    double y1 = num(tokens, i++) + (rel ? y : 0);
                    double x2 = num(tokens, i++) + (rel ? x : 0);
                    double y2 = num(tokens, i++) + (rel ? y : 0);
                    double ex = num(tokens, i++) + (rel ? x : 0);
                    double ey = num(tokens, i++) + (rel ? y : 0);
                    path.curveTo(x1, y1, x2, y2, ex, ey);
                    cx = x2;
                    cy = y2;
                    x = ex;
                    y = ey;
                }
                case 'S' -> {
                    double x1 = 2 * x - cx;
                    double y1 = 2 * y - cy;
                    double x2 = num(tokens, i++) + (rel ? x : 0);
                    double y2 = num(tokens, i++) + (rel ? y : 0);
                    double ex = num(tokens, i++) + (rel ? x : 0);
                    double ey = num(tokens, i++) + (rel ? y : 0);
                    path.curveTo(x1, y1, x2, y2, ex, ey);
                    cx = x2;
                    cy = y2;
                    x = ex;
                    y = ey;
                }
                case 'Q' -> {
                    double x1 = num(tokens, i++) + (rel ? x : 0);
                    double y1 = num(tokens, i++) + (rel ? y : 0);
                    double ex = num(tokens, i++) + (rel ? x : 0);
                    double ey = num(tokens, i++) + (rel ? y : 0);
                    path.quadTo(x1, y1, ex, ey);
                    cx = x1;
                    cy = y1;
                    x = ex;
                    y = ey;
                }
                case 'A' -> {
                    double rx = num(tokens, i++);
                    double ry = num(tokens, i++);
                    double rot = num(tokens, i++);
                    boolean large = num(tokens, i++) != 0;
                    boolean sweep = num(tokens, i++) != 0;
                    double ex = num(tokens, i++) + (rel ? x : 0);
                    double ey = num(tokens, i++) + (rel ? y : 0);
                    arc(path, x, y, rx, ry, rot, large, sweep, ex, ey);
                    x = ex;
                    y = ey;
                    cx = x;
                    cy = y;
                }
                case 'Z' -> {
                    path.closePath();
                    x = sx;
                    y = sy;
                    cx = x;
                    cy = y;
                }
                default -> throw new IllegalArgumentException("Unsupported path command " + cmd + " in " + d);
            }
        }
        return path;
    }

    private static void arc(Path2D path, double x0, double y0, double rx, double ry, double angle, boolean large, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) {
            path.lineTo(x, y);
            return;
        }
        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double rad = Math.toRadians(angle % 360.0);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x1 = cos * dx2 + sin * dy2;
        double y1 = -sin * dx2 + cos * dy2;
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double check = (x1 * x1) / (rx * rx) + (y1 * y1) / (ry * ry);
        if (check > 1) {
            rx = Math.sqrt(check) * rx;
            ry = Math.sqrt(check) * ry;
        }
        double sign = (large == sweep) ? -1 : 1;
        double sq = ((rx * rx * ry * ry) - (rx * rx * y1 * y1) - (ry * ry * x1 * x1)) / ((rx * rx * y1 * y1) + (ry * ry * x1 * x1));
        sq = sq < 0 ? 0 : sq;
        double coef = sign * Math.sqrt(sq);
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);
        double cx = (x0 + x) / 2.0 + (cos * cx1 - sin * cy1);
        double cy = (y0 + y) / 2.0 + (sin * cx1 + cos * cy1);
        double ux = (x1 - cx1) / rx;
        double uy = (y1 - cy1) / ry;
        double vx = (-x1 - cx1) / rx;
        double vy = (-y1 - cy1) / ry;
        double start = Math.toDegrees(angleOf(1, 0, ux, uy));
        double extent = Math.toDegrees(angleOf(ux, uy, vx, vy));
        if (!sweep && extent > 0) extent -= 360;
        else if (sweep && extent < 0) extent += 360;
        extent %= 360;
        start %= 360;
        Arc2D.Double arc = new Arc2D.Double(cx - rx, cy - ry, rx * 2, ry * 2, -start, -extent, Arc2D.OPEN);
        AffineTransform t = AffineTransform.getRotateInstance(rad, cx, cy);
        Point2D current = path.getCurrentPoint();
        path.append(t.createTransformedShape(arc), current != null);
    }

    private static double angleOf(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return (ux * vy - uy * vx) < 0 ? -a : a;
    }

    private static double num(List<String> tokens, int i) {
        return Double.parseDouble(tokens.get(i));
    }

    private static List<String> tokenize(String d) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (Character.isLetter(c) && c != 'e') {
                flush(tokens, current);
                tokens.add(String.valueOf(c));
            } else if (c == ' ' || c == ',') {
                flush(tokens, current);
            } else if (c == '-' && current.length() > 0 && current.charAt(current.length() - 1) != 'e') {
                flush(tokens, current);
                current.append(c);
            } else if (c == '.' && current.indexOf(".") >= 0) {
                flush(tokens, current);
                current.append(c);
            } else {
                current.append(c);
            }
        }
        flush(tokens, current);
        return tokens;
    }

    private static void flush(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }
}
