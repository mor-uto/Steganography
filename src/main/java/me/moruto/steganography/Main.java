package me.moruto.steganography;

import me.moruto.steganography.image.ImageMode;
import me.moruto.steganography.image.StegoImage;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        try {
            String password = "myStrongPassword123";

            File inputFile = new File("test/input_test.txt");
            File encodedImage = new File("test/secret.png");
            File outputDir = new File("test");

            outputDir.mkdirs();

            StegoImage.encode(
                    inputFile,
                    encodedImage,
                    password,
                    ImageMode.GRAYSCALE,
                    "Steganograph\n Curious about the contents?"
            );

            System.out.println("Encoded successfully!");

            File restored = StegoImage.decode(encodedImage, outputDir, password);

            System.out.println("Decoded file: " + restored.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}