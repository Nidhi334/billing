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
            default: {
                g2.draw(new Ellipse2D.Float(3f, 3f, 12f, 12f));
                break;
            }
        }
    }
}
