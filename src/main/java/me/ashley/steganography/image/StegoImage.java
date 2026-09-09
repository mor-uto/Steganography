package me.ashley.steganography.image;

import me.ashley.steganography.util.Compressor;
import me.ashley.steganography.util.Crypto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class StegoImage {

    private static final String MAGIC = "ASHSTEG";

    public static void encode(File input, File output, String password, ImageMode mode, String posterText) throws Exception {

        byte[] fileBytes = Files.readAllBytes(input.toPath());
        byte[] compressed = Compressor.compress(fileBytes);
        byte[] encrypted = Crypto.encrypt(compressed, password);

        byte[] nameBytes = input.getName().getBytes(StandardCharsets.UTF_8);

        ByteBuffer inner = ByteBuffer.allocate(7 + 1 + 4 + nameBytes.length + 4 + encrypted.length);

        inner.put(MAGIC.getBytes(StandardCharsets.UTF_8));
        inner.put((byte) mode.ordinal());
        inner.putInt(nameBytes.length);
        inner.put(nameBytes);
        inner.putInt(encrypted.length);
        inner.put(encrypted);

        byte[] payload = inner.array();

        ByteBuffer outer = ByteBuffer.allocate(4 + payload.length);
        outer.putInt(payload.length);
        outer.put(payload);

        byte[] finalPayload = outer.array();

        BufferedImage img = renderPoster(posterText, 1200, 1200);

        printCapacityInfo(img.getWidth(), img.getHeight(), finalPayload.length);

        embedLSB(img, finalPayload);

        ImageIO.write(img, "png", output);
    }

    public static File decode(File imageFile, File outputDir, String password) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);

        byte[] raw = extractLSB(img);

        if (raw.length < 4) {
            throw new RuntimeException("Invalid stego image: missing payload length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(raw);

        int payloadLen = buffer.getInt();

        if (payloadLen <= 0 || payloadLen > buffer.remaining()) {
            throw new RuntimeException(
                    "Invalid payload length: " + payloadLen +
                            " (available: " + buffer.remaining() + ")"
            );
        }

        byte[] payload = new byte[payloadLen];
        buffer.get(payload);

        ByteBuffer p = ByteBuffer.wrap(payload);

        if (p.remaining() < 7) {
            throw new RuntimeException("Invalid stego payload: missing magic");
        }

        byte[] magic = new byte[7];
        p.get(magic);

        if (!MAGIC.equals(new String(magic, StandardCharsets.UTF_8))) {
            throw new RuntimeException("Invalid stego image");
        }

        if (p.remaining() < 5) {
            throw new RuntimeException("Invalid stego payload");
        }

        byte mode = p.get();
        int nameLen = p.getInt();

        if (nameLen < 0 || nameLen > p.remaining()) {
            throw new RuntimeException("Invalid filename length: " + nameLen);
        }

        byte[] nameBytes = new byte[nameLen];
        p.get(nameBytes);

        String filename = new String(nameBytes, StandardCharsets.UTF_8);

        if (p.remaining() < 4) {
            throw new RuntimeException("Invalid stego payload: missing encrypted length");
        }

        int encLen = p.getInt();

        if (encLen < 0 || encLen > p.remaining()) {
            throw new RuntimeException("Invalid encrypted data length: " + encLen);
        }

        byte[] encrypted = new byte[encLen];
        p.get(encrypted);

        byte[] decrypted = Crypto.decrypt(encrypted, password);
        byte[] fileBytes = Compressor.decompress(decrypted);

        File out = new File(outputDir, filename);
        Files.write(out.toPath(), fileBytes);

        return out;
    }

    private static BufferedImage renderPoster(String text, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {

            float t = (float) y / height;

            int r = (int)(18 + t * 25);
            int g = (int)(18 + t * 25);
            int b = (int)(25 + t * 35);

            int base = (r << 16) | (g << 8) | b;

            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, base);
            }
        }

        Graphics2D g2 = img.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font = new Font("Monospaced", Font.BOLD, width / 20);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();

        String[] lines = text.split("\n");

        int totalHeight = lines.length * fm.getHeight();
        int y = (height - totalHeight) / 2 + fm.getAscent();

        for (String line : lines) {
            int x = (width - fm.stringWidth(line)) / 2;

            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString(line, x + 3, y + 3);

            g2.setColor(new Color(235, 235, 235));
            g2.drawString(line, x, y);

            y += (int)(fm.getHeight() * 1.4);
        }

        g2.dispose();

        return img;
    }

    private static void embedLSB(BufferedImage img, byte[] data) {
        int bitIndex = 0;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                if (bitIndex >= data.length * 8) return;

                int rgb = img.getRGB(x, y);

                int byteIndex = bitIndex / 8;
                int bit = (data[byteIndex] >> (7 - (bitIndex % 8))) & 1;

                rgb = (rgb & 0xFFFFFFFE) | bit;

                img.setRGB(x, y, rgb);

                bitIndex++;
            }
        }
    }

    private static byte[] extractLSB(BufferedImage img) {
        byte[] buffer = new byte[img.getWidth() * img.getHeight() / 8];

        int current = 0;
        int bitCount = 0;
        int byteIndex = 0;

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {

                int bit = img.getRGB(x, y) & 1;

                current = (current << 1) | bit;
                bitCount++;

                if (bitCount == 8) {
                    buffer[byteIndex++] = (byte) current;
                    current = 0;
                    bitCount = 0;

                    if (byteIndex >= buffer.length) {
                        return buffer;
                    }
                }
            }
        }

        return buffer;
    }

    public static int getCapacityBytes(int width, int height) {
        return (width * height) / 8;
    }

    public static void printCapacityInfo(int width, int height, int payloadSize) {

        int cap = getCapacityBytes(width, height);

        System.out.println("Capacity: " + cap + " bytes");
        System.out.println("Payload:  " + payloadSize + " bytes");

        if (payloadSize > cap) {
            System.out.println("WARNING: payload too large");
        } else {
            System.out.println("OK: fits");
        }
    }
}