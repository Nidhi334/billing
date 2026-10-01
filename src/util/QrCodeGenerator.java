package util;

import io.nayuki.qrcodegen.QrCode;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class QrCodeGenerator {

    /**
     * Builds an official NPCI UPI Intent URI with dynamic amount.
     * When scanned by GPay / PhonePe / Paytm / BHIM / Cred, the exact bill amount
     * will automatically appear prefilled on the customer's phone screen.
     *
     * @param upiId     Merchant UPI VPA (e.g. store@upi)
     * @param payeeName Merchant / Store display name
     * @param amount    Exact bill amount (dynamically generated from billing cart)
     * @param invoiceNo Unique invoice / bill reference number
     * @return Fully formatted, standard NPCI UPI URI string
     */
    public static String buildUpiUri(String upiId, String payeeName, double amount, String invoiceNo) {
        String cleanUpi = (upiId == null || upiId.trim().isEmpty()) ? "bazaarpoint@upi" : upiId.trim();
        String cleanName = (payeeName == null || payeeName.trim().isEmpty()) ? "SmartBilling Store" : payeeName.trim();
        String cleanInv = (invoiceNo == null || invoiceNo.trim().isEmpty()) ? "BILL-" + System.currentTimeMillis() : invoiceNo.trim();

        // NPCI UPI standard requires exact 2 decimal places (e.g., 250.00) using dot separator
        String formattedAmount = String.format(Locale.US, "%.2f", Math.max(0.01, amount));

        try {
            String encodedName = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            String encodedInv = URLEncoder.encode(cleanInv, StandardCharsets.UTF_8.name()).replace("+", "%20");
            String encodedUpi = URLEncoder.encode(cleanUpi, StandardCharsets.UTF_8.name()).replace("%40", "@");

            return String.format(Locale.US,
                    "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=Invoice%%20%s&tr=%s",
                    encodedUpi,
                    encodedName,
                    formattedAmount,
                    encodedInv,
                    encodedInv
            );
        } catch (Exception e) {
            return "upi://pay?pa=" + cleanUpi + "&pn=" + cleanName + "&am=" + formattedAmount + "&cu=INR&tn=Invoice%20" + cleanInv + "&tr=" + cleanInv;
        }
    }

    /**
     * Generates a high-resolution, scannable QR Code BufferedImage from text.
     * Uses Nayuki's pure Java QR encoder with fallback to native qrencode.
     *
     * @param text            The URI or payload text to encode
     * @param targetPixelSize Desired width/height of the QR image (e.g. 260px)
     * @return BufferedImage containing the rendered QR code with quiet zone
     */
    public static BufferedImage generateQrImage(String text, int targetPixelSize) {
        try {
            QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
            int border = 4; // Quiet zone: standard 4 modules
            int modules = qr.size + border * 2;
            int scale = Math.max(1, targetPixelSize / modules);
            int imgSize = modules * scale;

            BufferedImage image = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, imgSize, imgSize);
            g2.setColor(Color.BLACK);

            for (int y = 0; y < qr.size; y++) {
                for (int x = 0; x < qr.size; x++) {
                    if (qr.getModule(x, y)) {
                        g2.fillRect((x + border) * scale, (y + border) * scale, scale, scale);
                    }
                }
            }
            g2.dispose();
            return image;
        } catch (Throwable t) {
            // Fallback to system qrencode if available
            return generateQrWithQrencode(text, targetPixelSize);
        }
    }

    /**
     * Fallback generator using system 'qrencode' utility.
     */
    private static BufferedImage generateQrWithQrencode(String text, int targetPixelSize) {
        try {
            int moduleSize = Math.max(3, targetPixelSize / 45);
            ProcessBuilder pb = new ProcessBuilder("qrencode", "-s", String.valueOf(moduleSize), "-m", "4", "-l", "M", "-o", "-", text);
            Process p = pb.start();
            try (InputStream in = p.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                p.waitFor();
                return img;
            }
        } catch (Exception e) {
            System.err.println("QR generation fallback error: " + e.getMessage());
            return null;
        }
    }
}
