package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * High-DPI Vector SVG Icons for Sidebar Navigation.
 * Renders crisp, scalable vector graphics with anti-aliasing and round stroke joins.
 */
public class SidebarIcon implements Icon {
    private final String name;
    private final int size;
    private Color overrideColor;

    public SidebarIcon(String name, int size) {
        this(name, size, null);
    }

    public SidebarIcon(String name, int size, Color overrideColor) {
        this.name = name;
        this.size = size;
        this.overrideColor = overrideColor;
    }

    public void setOverrideColor(Color color) {
        this.overrideColor = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color paintColor = (overrideColor != null) ? overrideColor : (c != null ? c.getForeground() : Color.BLACK);
        g2.setColor(paintColor);

        // Standard coordinate scale (18x18 base coordinate system)
        float scale = size / 18.0f;
        g2.translate(x, y);
        g2.scale(scale, scale);

        drawIcon(g2, name);
        g2.dispose();
    }

    private static void drawIcon(Graphics2D g2, String name) {
        float strokeWidth = 1.65f;
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case "dashboard": {
                // 4 modern rounded grid tiles (Lucide layout-dashboard)
                g2.draw(new RoundRectangle2D.Float(2f, 2f, 5.5f, 5.5f, 2.5f, 2.5f));
                g2.draw(new RoundRectangle2D.Float(10.5f, 2f, 5.5f, 5.5f, 2.5f, 2.5f));
                g2.draw(new RoundRectangle2D.Float(2f, 10.5f, 5.5f, 5.5f, 2.5f, 2.5f));
                g2.draw(new RoundRectangle2D.Float(10.5f, 10.5f, 5.5f, 5.5f, 2.5f, 2.5f));
                break;
            }
            case "billing": {
                // Receipt / ticket outline with dashed items
                Path2D p = new Path2D.Float();
                p.moveTo(3f, 2f);
                p.lineTo(15f, 2f);
                p.lineTo(15f, 16f);
                p.lineTo(13f, 14.5f);
                p.lineTo(11f, 16f);
                p.lineTo(9f, 14.5f);
                p.lineTo(7f, 16f);
                p.lineTo(5f, 14.5f);
                p.lineTo(3f, 16f);
                p.closePath();
                g2.draw(p);

                // Receipt lines
                g2.draw(new Line2D.Float(6f, 6f, 12f, 6f));
                g2.draw(new Line2D.Float(6f, 9.5f, 12f, 9.5f));
                g2.draw(new Line2D.Float(6f, 12.5f, 9.5f, 12.5f));
                break;
            }
            case "cart":
            case "self_checkout": {
                // Shopping cart
                Path2D p = new Path2D.Float();
                p.moveTo(2f, 3f);
                p.lineTo(4.5f, 3f);
                p.lineTo(6.2f, 11f);
                p.lineTo(14f, 11f);
                p.lineTo(15.5f, 5.5f);
                p.lineTo(5.2f, 5.5f);
                g2.draw(p);

                // Cart wheels
                g2.fill(new Ellipse2D.Float(6.2f, 13.5f, 2.3f, 2.3f));
                g2.fill(new Ellipse2D.Float(12.7f, 13.5f, 2.3f, 2.3f));
                break;
            }
            case "orders":
            case "bill_history": {
                // Circular clock history
                g2.draw(new Ellipse2D.Float(2f, 2f, 14f, 14f));
                Path2D hands = new Path2D.Float();
                hands.moveTo(9f, 5.5f);
                hands.lineTo(9f, 9f);
                hands.lineTo(12f, 9f);
                g2.draw(hands);
                break;
            }
            case "customers": {
                // User silhouettes (Lucide users)
                // Main user head
                g2.draw(new Ellipse2D.Float(6f, 2.5f, 5f, 5f));
                // Main user torso
                Path2D torso = new Path2D.Float();
                torso.moveTo(3f, 15.5f);
                torso.curveTo(3f, 11.8f, 5.2f, 10.5f, 8.5f, 10.5f);
                torso.curveTo(11.8f, 10.5f, 14f, 11.8f, 14f, 15.5f);
                g2.draw(torso);

                // Secondary user arc
                Path2D sec = new Path2D.Float();
                sec.moveTo(13.5f, 9.5f);
                sec.curveTo(15f, 10.2f, 16f, 11.5f, 16f, 14f);
                g2.draw(sec);
                break;
            }
            case "products": {
                // Isometric package box (Lucide package)
                Path2D box = new Path2D.Float();
                box.moveTo(9f, 2f);
                box.lineTo(15.5f, 5.5f);
                box.lineTo(15.5f, 12.5f);
                box.lineTo(9f, 16f);
                box.lineTo(2.5f, 12.5f);
                box.lineTo(2.5f, 5.5f);
                box.closePath();
                g2.draw(box);

                // Center edges
                g2.draw(new Line2D.Float(9f, 2f, 9f, 9f));
                g2.draw(new Line2D.Float(9f, 9f, 15.5f, 5.5f));
                g2.draw(new Line2D.Float(9f, 9f, 2.5f, 5.5f));
                g2.draw(new Line2D.Float(9f, 9f, 9f, 16f));
                break;
            }
            case "inventory": {
                // Stacked inventory layers (Lucide layers)
                Path2D top = new Path2D.Float();
                top.moveTo(9f, 2f);
                top.lineTo(15.5f, 5.5f);
                top.lineTo(9f, 9f);
                top.lineTo(2.5f, 5.5f);
                top.closePath();
                g2.draw(top);

                Path2D mid = new Path2D.Float();
                mid.moveTo(2.5f, 9f);
                mid.lineTo(9f, 12.5f);
                mid.lineTo(15.5f, 9f);
                g2.draw(mid);

                Path2D bot = new Path2D.Float();
                bot.moveTo(2.5f, 12.5f);
                bot.lineTo(9f, 16f);
                bot.lineTo(15.5f, 12.5f);
                g2.draw(bot);
                break;
            }
            case "suppliers": {
                // Commercial delivery truck (Lucide truck)
                // Cargo bed
                g2.draw(new RoundRectangle2D.Float(2f, 4.5f, 8.5f, 7.5f, 2f, 2f));
                // Cabin
                Path2D cab = new Path2D.Float();
                cab.moveTo(10.5f, 7.5f);
                cab.lineTo(13.5f, 7.5f);
                cab.lineTo(15.8f, 10f);
                cab.lineTo(15.8f, 12f);
                cab.lineTo(10.5f, 12f);
                g2.draw(cab);

                // Wheels
                g2.draw(new Ellipse2D.Float(4.5f, 11f, 2.5f, 2.5f));
                g2.draw(new Ellipse2D.Float(12.5f, 11f, 2.5f, 2.5f));
                break;
            }
            case "reports": {
                // Growth trendline with upward arrow (Lucide trending-up)
                Path2D trend = new Path2D.Float();
                trend.moveTo(2.5f, 13.5f);
                trend.lineTo(7.5f, 8.5f);
                trend.lineTo(11f, 11f);
                trend.lineTo(15.5f, 5.5f);
                g2.draw(trend);

                // Arrow head
                Path2D arr = new Path2D.Float();
                arr.moveTo(11.5f, 5.5f);
                arr.lineTo(15.5f, 5.5f);
                arr.lineTo(15.5f, 9.5f);
                g2.draw(arr);
                break;
            }
            case "settings": {
                // Clean gear / cog (Lucide settings)
                g2.draw(new Ellipse2D.Float(6.5f, 6.5f, 5f, 5f));
                Path2D gear = new Path2D.Float();
                gear.moveTo(9f, 2f);   gear.lineTo(9f, 4f);
                gear.moveTo(9f, 14f);  gear.lineTo(9f, 16f);
                gear.moveTo(2f, 9f);   gear.lineTo(4f, 9f);
                gear.moveTo(14f, 9f);  gear.lineTo(16f, 9f);
                gear.moveTo(4f, 4f);   gear.lineTo(5.5f, 5.5f);
                gear.moveTo(12.5f, 12.5f); gear.lineTo(14f, 14f);
                gear.moveTo(14f, 4f);  gear.lineTo(12.5f, 5.5f);
                gear.moveTo(5.5f, 12.5f); gear.lineTo(4f, 14f);
                g2.draw(gear);
                break;
            }
            case "database": {
                // Lucide database cylinder
                g2.draw(new Ellipse2D.Float(3f, 2.5f, 12f, 4f));
                g2.draw(new Arc2D.Float(3f, 6.5f, 12f, 4f, 180f, 180f, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(3f, 10.5f, 12f, 4f, 180f, 180f, Arc2D.OPEN));
                g2.draw(new Line2D.Float(3f, 4.5f, 3f, 12.5f));
                g2.draw(new Line2D.Float(15f, 4.5f, 15f, 12.5f));
                break;
            }
            case "panel_left_close": {
                // Sidebar panel with left collapse arrow
                g2.draw(new RoundRectangle2D.Float(2f, 2.5f, 14f, 13f, 3f, 3f));
                g2.draw(new Line2D.Float(7f, 2.5f, 7f, 15.5f));
                // Chevron pointing left
                Path2D chev = new Path2D.Float();
                chev.moveTo(13f, 7f);
                chev.lineTo(11f, 9f);
                chev.lineTo(13f, 11f);
                g2.draw(chev);
                break;
            }
            case "panel_left_open": {
                // Sidebar panel with right expand arrow
                g2.draw(new RoundRectangle2D.Float(2f, 2.5f, 14f, 13f, 3f, 3f));
                g2.draw(new Line2D.Float(7f, 2.5f, 7f, 15.5f));
                // Chevron pointing right
                Path2D chev = new Path2D.Float();
                chev.moveTo(11f, 7f);
                chev.lineTo(13f, 9f);
                chev.lineTo(11f, 11f);
                g2.draw(chev);
                break;
            }
            case "logout": {
                // Exit door with arrow (Lucide log-out)
                Path2D door = new Path2D.Float();
                door.moveTo(7.5f, 3f);
                door.lineTo(3.5f, 3f);
                door.lineTo(3.5f, 15f);
                door.lineTo(7.5f, 15f);
                g2.draw(door);

                // Arrow pointing right
                g2.draw(new Line2D.Float(7.5f, 9f, 15.5f, 9f));
                Path2D arr = new Path2D.Float();
                arr.moveTo(12.5f, 6f);
                arr.lineTo(15.5f, 9f);
                arr.lineTo(12.5f, 12f);
                g2.draw(arr);
                break;
            }
            case "refresh": {
                // Official dual-arrow refresh icon (Lucide refresh-cw)
                // Top clockwise arc & arrowhead
                Arc2D topArc = new Arc2D.Float(2.5f, 2.5f, 13f, 13f, 180f, -145f, Arc2D.OPEN);
                g2.draw(topArc);
                Path2D arr1 = new Path2D.Float();
                arr1.moveTo(10.5f, 5.2f);
                arr1.lineTo(14.8f, 5.2f);
                arr1.lineTo(14.8f, 1.2f);
                g2.draw(arr1);

                // Bottom clockwise arc & arrowhead
                Arc2D botArc = new Arc2D.Float(2.5f, 2.5f, 13f, 13f, 0f, -145f, Arc2D.OPEN);
                g2.draw(botArc);
                Path2D arr2 = new Path2D.Float();
                arr2.moveTo(7.5f, 12.8f);
                arr2.lineTo(3.2f, 12.8f);
                arr2.lineTo(3.2f, 16.8f);
                g2.draw(arr2);
                break;
            }
            case "zap": {
                // Lightning bolt (Lucide zap)
                Path2D bolt = new Path2D.Float();
                bolt.moveTo(10.5f, 2f);
                bolt.lineTo(4f, 9.5f);
                bolt.lineTo(9f, 9.5f);
                bolt.lineTo(7.5f, 16f);
                bolt.lineTo(14f, 8.5f);
                bolt.lineTo(9f, 8.5f);
                bolt.closePath();
                g2.draw(bolt);
                break;
            }
            case "calendar": {
                // Modern calendar box
                g2.draw(new RoundRectangle2D.Float(2.5f, 3.5f, 13f, 12f, 2.5f, 2.5f));
                g2.draw(new Line2D.Float(2.5f, 7.5f, 15.5f, 7.5f));
                g2.draw(new Line2D.Float(6f, 2f, 6f, 4.5f));
                g2.draw(new Line2D.Float(12f, 2f, 12f, 4.5f));
                break;
            }
            case "wallet":
            case "coins": {
                // Wallet card outline
                g2.draw(new RoundRectangle2D.Float(2f, 3.5f, 14f, 11f, 2.5f, 2.5f));
                Path2D flap = new Path2D.Float();
                flap.moveTo(10f, 7f);
                flap.lineTo(16f, 7f);
                flap.lineTo(16f, 11f);
                flap.lineTo(10f, 11f);
                flap.closePath();
                g2.draw(flap);
                g2.fill(new Ellipse2D.Float(12f, 8.5f, 1.5f, 1.5f));
                break;
            }
            case "alert_triangle": {
                // Alert warning triangle
                Path2D tri = new Path2D.Float();
                tri.moveTo(9f, 2.5f);
                tri.lineTo(15.8f, 14.5f);
                tri.lineTo(2.2f, 14.5f);
                tri.closePath();
                g2.draw(tri);
                g2.draw(new Line2D.Float(9f, 6.5f, 9f, 10.5f));
                g2.fill(new Ellipse2D.Float(8.25f, 12.2f, 1.5f, 1.5f));
                break;
            }
            case "boxes":
            case "stock": {
                // Inventory stacked boxes
                g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, 5.5f, 5.5f, 1.5f, 1.5f));
                g2.draw(new RoundRectangle2D.Float(10f, 2.5f, 5.5f, 5.5f, 1.5f, 1.5f));
                g2.draw(new RoundRectangle2D.Float(6.25f, 10f, 5.5f, 5.5f, 1.5f, 1.5f));
                break;
            }
            case "building": {
                // Vendor office building
                g2.draw(new RoundRectangle2D.Float(3f, 2.5f, 12f, 13f, 2f, 2f));
                g2.draw(new Line2D.Float(6f, 6f, 7.5f, 6f));
                g2.draw(new Line2D.Float(10.5f, 6f, 12f, 6f));
                g2.draw(new Line2D.Float(6f, 9.5f, 7.5f, 9.5f));
                g2.draw(new Line2D.Float(10.5f, 9.5f, 12f, 9.5f));
                g2.draw(new Line2D.Float(7.5f, 15.5f, 7.5f, 12.5f));
                g2.draw(new Line2D.Float(7.5f, 12.5f, 10.5f, 12.5f));
                g2.draw(new Line2D.Float(10.5f, 12.5f, 10.5f, 15.5f));
                break;
            }
            case "check_circle": {
                // Verified check circle
                g2.draw(new Ellipse2D.Float(2f, 2f, 14f, 14f));
                Path2D chk = new Path2D.Float();
                chk.moveTo(5.5f, 9.5f);
                chk.lineTo(8f, 12f);
                chk.lineTo(12.5f, 6.5f);
                g2.draw(chk);
                break;
            }
            case "pie_chart":
            case "pie": {
                // Circular pie chart with slice (Lucide pie-chart)
                g2.draw(new Ellipse2D.Float(2f, 2f, 14f, 14f));
                g2.draw(new Line2D.Float(9f, 2f, 9f, 9f));
                g2.draw(new Line2D.Float(9f, 9f, 15f, 12f));
                break;
            }
            case "trending_up": {
                // Lucide trending-up
                Path2D p = new Path2D.Float();
                p.moveTo(2f, 13.5f);
                p.lineTo(7f, 8.5f);
                p.lineTo(11f, 12.5f);
                p.lineTo(16f, 5.5f);
                g2.draw(p);
                Path2D arr = new Path2D.Float();
                arr.moveTo(11.5f, 5.5f);
                arr.lineTo(16f, 5.5f);
                arr.lineTo(16f, 10f);
                g2.draw(arr);
                break;
            }
            case "trophy": {
                // Lucide trophy
                Path2D cup = new Path2D.Float();
                cup.moveTo(4.5f, 3f);
                cup.lineTo(13.5f, 3f);
                cup.lineTo(13.5f, 8f);
                cup.curveTo(13.5f, 11f, 11f, 11.8f, 9f, 11.8f);
                cup.curveTo(7f, 11.8f, 4.5f, 11f, 4.5f, 8f);
                cup.closePath();
                g2.draw(cup);
                g2.draw(new Line2D.Float(9f, 11.8f, 9f, 14.8f));
                g2.draw(new Line2D.Float(5.5f, 14.8f, 12.5f, 14.8f));
                Path2D h1 = new Path2D.Float();
                h1.moveTo(4.5f, 4.5f);
                h1.curveTo(2.2f, 4.5f, 2.2f, 7.5f, 4.5f, 8f);
                g2.draw(h1);
                Path2D h2 = new Path2D.Float();
                h2.moveTo(13.5f, 4.5f);
                h2.curveTo(15.8f, 4.5f, 15.8f, 7.5f, 13.5f, 8f);
                g2.draw(h2);
                break;
            }
            case "search": {
                g2.draw(new Ellipse2D.Float(3f, 3f, 8.5f, 8.5f));
                g2.draw(new Line2D.Float(9.2f, 9.2f, 15f, 15f));
                break;
            }
            case "plus": {
                g2.draw(new Line2D.Float(9f, 3.5f, 9f, 14.5f));
                g2.draw(new Line2D.Float(3.5f, 9f, 14.5f, 9f));
                break;
            }
            case "minus": {
                g2.draw(new Line2D.Float(3.5f, 9f, 14.5f, 9f));
                break;
            }
            case "trash": {
                g2.draw(new Line2D.Float(3f, 5.5f, 15f, 5.5f));
                g2.draw(new Line2D.Float(7f, 3.5f, 11f, 3.5f));
                Path2D can = new Path2D.Float();
                can.moveTo(4.5f, 5.5f);
                can.lineTo(5.5f, 15f);
                can.curveTo(5.5f, 15.5f, 6f, 16f, 6.5f, 16f);
                can.lineTo(11.5f, 16f);
                can.curveTo(12f, 16f, 12.5f, 15.5f, 12.5f, 15f);
                can.lineTo(13.5f, 5.5f);
                g2.draw(can);
                g2.draw(new Line2D.Float(7.5f, 8f, 7.5f, 13f));
                g2.draw(new Line2D.Float(10.5f, 8f, 10.5f, 13f));
                break;
            }
            case "barcode": {
                g2.draw(new Line2D.Float(3f, 4.5f, 3f, 13.5f));
                g2.draw(new Line2D.Float(5.5f, 4.5f, 5.5f, 13.5f));
                g2.draw(new Line2D.Float(7.5f, 4.5f, 7.5f, 13.5f));
                g2.draw(new Line2D.Float(10.5f, 4.5f, 10.5f, 13.5f));
                g2.draw(new Line2D.Float(12.5f, 4.5f, 12.5f, 13.5f));
                g2.draw(new Line2D.Float(15f, 4.5f, 15f, 13.5f));
                break;
            }
            case "stop": {
                g2.draw(new RoundRectangle2D.Float(3.5f, 3.5f, 11f, 11f, 3f, 3f));
                break;
            }
            case "camera": {
                Path2D cam = new Path2D.Float();
                cam.moveTo(2.5f, 6.5f);
                cam.lineTo(5f, 6.5f);
                cam.lineTo(6.5f, 4f);
                cam.lineTo(11.5f, 4f);
                cam.lineTo(13f, 6.5f);
                cam.lineTo(15.5f, 6.5f);
                cam.curveTo(16f, 6.5f, 16.5f, 7f, 16.5f, 7.5f);
                cam.lineTo(16.5f, 14.5f);
                cam.curveTo(16.5f, 15f, 16f, 15.5f, 15.5f, 15.5f);
                cam.lineTo(2.5f, 15.5f);
                cam.curveTo(2f, 15.5f, 1.5f, 15f, 1.5f, 14.5f);
                cam.lineTo(1.5f, 7.5f);
                cam.curveTo(1.5f, 7.5f, 2f, 6.5f, 2.5f, 6.5f);
                g2.draw(cam);
                g2.draw(new Ellipse2D.Float(6.2f, 7.8f, 5.6f, 5.6f));
                break;
            }
            case "printer": {
                Path2D top = new Path2D.Float();
                top.moveTo(5f, 6f);
                top.lineTo(5f, 2.5f);
                top.lineTo(13f, 2.5f);
                top.lineTo(13f, 6f);
                g2.draw(top);
                g2.draw(new RoundRectangle2D.Float(2f, 6f, 14f, 6.5f, 2f, 2f));
                Path2D bot = new Path2D.Float();
                bot.moveTo(5f, 10f);
                bot.lineTo(5f, 15.5f);
                bot.lineTo(13f, 15.5f);
                bot.lineTo(13f, 10f);
                g2.draw(bot);
                g2.fill(new Ellipse2D.Float(12.5f, 8f, 1.2f, 1.2f));
                break;
            }
            case "credit_card": {
                g2.draw(new RoundRectangle2D.Float(2f, 4f, 14f, 10f, 2.5f, 2.5f));
                g2.draw(new Line2D.Float(2f, 7.5f, 16f, 7.5f));
                g2.draw(new Line2D.Float(4.5f, 11f, 7.5f, 11f));
                break;
            }
            case "smartphone":
            case "upi": {
                g2.draw(new RoundRectangle2D.Float(4.5f, 2f, 9f, 14f, 2.2f, 2.2f));
                g2.draw(new Line2D.Float(8f, 13.5f, 10f, 13.5f));
                break;
            }
            case "file_text": {
                Path2D doc = new Path2D.Float();
                doc.moveTo(3.5f, 2.5f);
                doc.lineTo(11f, 2.5f);
                doc.lineTo(14.5f, 6f);
                doc.lineTo(14.5f, 15.5f);
                doc.lineTo(3.5f, 15.5f);
                doc.closePath();
                g2.draw(doc);
                g2.draw(new Line2D.Float(11f, 2.5f, 11f, 6f));
                g2.draw(new Line2D.Float(11f, 6f, 14.5f, 6f));
                g2.draw(new Line2D.Float(6f, 9.5f, 12f, 9.5f));
                g2.draw(new Line2D.Float(6f, 12.5f, 10f, 12.5f));
                break;
            }
            case "user": {
                g2.draw(new Ellipse2D.Float(6.5f, 2.5f, 5f, 5f));
                Path2D torso = new Path2D.Float();
                torso.moveTo(3f, 15.5f);
                torso.curveTo(3f, 11.5f, 5.5f, 10.5f, 9f, 10.5f);
                torso.curveTo(12.5f, 10.5f, 15f, 11.5f, 15f, 15.5f);
                g2.draw(torso);
                break;
            }
            case "check": {
                Path2D chk = new Path2D.Float();
                chk.moveTo(3.5f, 9f);
                chk.lineTo(7.5f, 13f);
                chk.lineTo(14.5f, 5f);
                g2.draw(chk);
                break;
            }
            case "arrow_right": {
                g2.draw(new Line2D.Float(3f, 9f, 15f, 9f));
                Path2D arr = new Path2D.Float();
                arr.moveTo(11f, 5f);
                arr.lineTo(15f, 9f);
                arr.lineTo(11f, 13f);
                g2.draw(arr);
                break;
            }
            case "download": {
                // Lucide download tray and downward arrow
                Path2D tray = new Path2D.Float();
                tray.moveTo(3.5f, 11f);
                tray.lineTo(3.5f, 14.5f);
                tray.lineTo(14.5f, 14.5f);
                tray.lineTo(14.5f, 11f);
                g2.draw(tray);

                g2.draw(new Line2D.Float(9f, 2.5f, 9f, 11f));
                Path2D arr = new Path2D.Float();
                arr.moveTo(5.5f, 7.5f);
                arr.lineTo(9f, 11f);
                arr.lineTo(12.5f, 7.5f);
                g2.draw(arr);
                break;
            }
            case "filter": {
                Path2D fun = new Path2D.Float();
                fun.moveTo(2.5f, 3.5f);
                fun.lineTo(15.5f, 3.5f);
                fun.lineTo(10.5f, 9.5f);
                fun.lineTo(10.5f, 14.5f);
                fun.lineTo(7.5f, 13f);
                fun.lineTo(7.5f, 9.5f);
                fun.closePath();
                g2.draw(fun);
                break;
            }
            case "eye": {
                Path2D eye = new Path2D.Float();
                eye.moveTo(2f, 9f);
                eye.quadTo(9f, 2.5f, 16f, 9f);
                eye.quadTo(9f, 15.5f, 2f, 9f);
                eye.closePath();
                g2.draw(eye);
                g2.draw(new Ellipse2D.Float(7f, 7f, 4f, 4f));
                break;
            }
            case "shield": {
                Path2D sh = new Path2D.Float();
                sh.moveTo(9f, 2f);
                sh.lineTo(15.5f, 4.5f);
                sh.lineTo(15.5f, 9.5f);
                sh.quadTo(15.5f, 14.5f, 9f, 16.5f);
                sh.quadTo(2.5f, 14.5f, 2.5f, 9.5f);
                sh.lineTo(2.5f, 4.5f);
                sh.closePath();
                g2.draw(sh);
                break;
            }
            case "package":
            case "box": {
                Path2D box = new Path2D.Float();
                box.moveTo(9f, 2f);
                box.lineTo(15.5f, 5.5f);
                box.lineTo(15.5f, 12.5f);
                box.lineTo(9f, 16f);
                box.lineTo(2.5f, 12.5f);
                box.lineTo(2.5f, 5.5f);
                box.closePath();
                g2.draw(box);
                g2.draw(new Line2D.Float(9f, 2f, 9f, 9f));
                g2.draw(new Line2D.Float(2.5f, 5.5f, 9f, 9f));
                g2.draw(new Line2D.Float(15.5f, 5.5f, 9f, 9f));
                break;
            }
            case "image": {
                // Lucide image: rounded photo card with sun circle and mountain peak
                g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, 13f, 13f, 3f, 3f));
                g2.draw(new Ellipse2D.Float(5.8f, 5.8f, 2.4f, 2.4f));
                Path2D m = new Path2D.Float();
                m.moveTo(15.5f, 12f);
                m.lineTo(11.5f, 8f);
                m.lineTo(7.5f, 12f);
                m.lineTo(5.5f, 10f);
                m.lineTo(2.5f, 13f);
                g2.draw(m);
                break;
            }
            case "tag":
            case "category": {
                // Lucide tag icon: angled tag with hole
                Path2D tag = new Path2D.Float();
                tag.moveTo(2.5f, 9.5f);
                tag.lineTo(2.5f, 4f);
                tag.curveTo(2.5f, 3f, 3f, 2.5f, 4f, 2.5f);
                tag.lineTo(9.5f, 2.5f);
                tag.lineTo(15.5f, 8.5f);
                tag.curveTo(16f, 9f, 16f, 10f, 15.5f, 10.5f);
                tag.lineTo(10.5f, 15.5f);
                tag.curveTo(10f, 16f, 9f, 16f, 8.5f, 15.5f);
                tag.closePath();
                g2.draw(tag);
                // Tag hole
                g2.draw(new Ellipse2D.Float(5f, 5f, 2.2f, 2.2f));
                break;
            }
            case "accessories":
            case "headphones": {
                // Lucide headphones: curved headband with cushioned earcups
                Path2D arc = new Path2D.Float();
                arc.moveTo(2.5f, 10.5f);
                arc.curveTo(2.5f, 3.2f, 15.5f, 3.2f, 15.5f, 10.5f);
                g2.draw(arc);
                g2.draw(new RoundRectangle2D.Float(1.5f, 9f, 3.2f, 6f, 2f, 2f));
                g2.draw(new RoundRectangle2D.Float(13.3f, 9f, 3.2f, 6f, 2f, 2f));
                break;
            }
            case "electronics":
            case "monitor": {
                // Lucide monitor: crisp rounded display screen with stand and base
                g2.draw(new RoundRectangle2D.Float(2f, 2.5f, 14f, 9.5f, 2.2f, 2.2f));
                g2.draw(new Line2D.Float(9f, 12f, 9f, 15.5f));
                g2.draw(new Line2D.Float(5.5f, 15.5f, 12.5f, 15.5f));
                break;
            }
            case "stationery":
            case "pencil":
            case "pen": {
                // Lucide pencil: angled pencil with lead tip and eraser collar
                Path2D pen = new Path2D.Float();
                pen.moveTo(13.5f, 2.5f);
                pen.lineTo(15.5f, 4.5f);
                pen.lineTo(7f, 13f);
                pen.lineTo(2.5f, 15.5f);
                pen.lineTo(5f, 11f);
                pen.closePath();
                g2.draw(pen);
                g2.draw(new Line2D.Float(5f, 11f, 7f, 13f));
                g2.draw(new Line2D.Float(12f, 4f, 14f, 6f));
                break;
            }
            case "storage_networking":
            case "network": {
                // Lucide network: master router node linked to two client terminal nodes
                g2.draw(new RoundRectangle2D.Float(6.5f, 2f, 5f, 4f, 1.5f, 1.5f));
                g2.draw(new RoundRectangle2D.Float(2f, 12f, 5f, 4f, 1.5f, 1.5f));
                g2.draw(new RoundRectangle2D.Float(11f, 12f, 5f, 4f, 1.5f, 1.5f));
                g2.draw(new Line2D.Float(9f, 6f, 9f, 9f));
                g2.draw(new Line2D.Float(4.5f, 9f, 13.5f, 9f));
                g2.draw(new Line2D.Float(4.5f, 9f, 4.5f, 12f));
                g2.draw(new Line2D.Float(13.5f, 9f, 13.5f, 12f));
                break;
            }
            case "hard_drive":
            case "storage":
            case "usb": {
                // Lucide hard-drive: storage enclosure with indicator dot
                g2.draw(new RoundRectangle2D.Float(2f, 4f, 14f, 10f, 2.5f, 2.5f));
                g2.draw(new Line2D.Float(2f, 10f, 16f, 10f));
                g2.draw(new Line2D.Float(13f, 12f, 14f, 12f));
                break;
            }
            case "grocery":
            case "food":
            case "fresh": {
                // Lucide shopping basket: sturdy basket with curved handle
                Path2D basket = new Path2D.Float();
                basket.moveTo(2.5f, 7.5f);
                basket.lineTo(15.5f, 7.5f);
                basket.lineTo(14f, 15.5f);
                basket.lineTo(4f, 15.5f);
                basket.closePath();
                g2.draw(basket);
                Path2D handle = new Path2D.Float();
                handle.moveTo(5.5f, 7.5f);
                handle.curveTo(5.5f, 3f, 12.5f, 3f, 12.5f, 7.5f);
                g2.draw(handle);
                g2.draw(new Line2D.Float(9f, 7.5f, 9f, 15.5f));
                break;
            }
            case "dairy":
            case "milk": {
                // Milk bottle outline with label band
                Path2D bottle = new Path2D.Float();
                bottle.moveTo(7f, 2.5f);
                bottle.lineTo(11f, 2.5f);
                bottle.lineTo(11f, 5f);
                bottle.lineTo(13f, 7.5f);
                bottle.lineTo(13f, 15.5f);
                bottle.lineTo(5f, 15.5f);
                bottle.lineTo(5f, 7.5f);
                bottle.lineTo(7f, 5f);
                bottle.closePath();
                g2.draw(bottle);
                g2.draw(new Line2D.Float(5f, 11.5f, 13f, 11.5f));
                break;
            }
            case "clothing":
            case "apparel":
            case "fashion": {
                // Lucide shirt: short sleeve top with round neck
                Path2D shirt = new Path2D.Float();
                shirt.moveTo(6.5f, 3.5f);
                shirt.lineTo(2.5f, 6f);
                shirt.lineTo(4.5f, 9f);
                shirt.lineTo(6f, 8f);
                shirt.lineTo(6f, 15.5f);
                shirt.lineTo(12f, 15.5f);
                shirt.lineTo(12f, 8f);
                shirt.lineTo(13.5f, 9f);
                shirt.lineTo(15.5f, 6f);
                shirt.lineTo(11.5f, 3.5f);
                shirt.quadTo(9f, 6f, 6.5f, 3.5f);
                shirt.closePath();
                g2.draw(shirt);
                break;
            }
            case "book":
            case "books": {
                // Lucide book-open: spread open book pages
                Path2D book = new Path2D.Float();
                book.moveTo(9f, 5f);
                book.quadTo(5.5f, 3f, 2f, 4f);
                book.lineTo(2f, 14.5f);
                book.quadTo(5.5f, 13.5f, 9f, 15.5f);
                book.quadTo(12.5f, 13.5f, 16f, 14.5f);
                book.lineTo(16f, 4f);
                book.quadTo(12.5f, 3f, 9f, 5f);
                book.closePath();
                g2.draw(book);
                g2.draw(new Line2D.Float(9f, 5f, 9f, 15.5f));
                break;
            }
            case "arrow_left":
            case "back": {
                // Lucide arrow-left
                g2.draw(new Line2D.Float(14.5f, 9f, 3.5f, 9f));
                Path2D arr = new Path2D.Float();
                arr.moveTo(8f, 4.5f);
                arr.lineTo(3.5f, 9f);
                arr.lineTo(8f, 13.5f);
                g2.draw(arr);
                break;
            }
            default: {
                g2.draw(new Ellipse2D.Float(3f, 3f, 12f, 12f));
                break;
            }
        }
    }
}
