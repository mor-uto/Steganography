package me.moruto.steganography.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Compressor {
    public static byte[] compress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(data);
        }
        return baos.toByteArray();
    }

    public static byte[] decompress(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InflaterInputStream iis = new InflaterInputStream(bais);

        byte[] buffer = new byte[4096];
        int len;

        while ((len = iis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        iis.close();

        return baos.toByteArray();
    }
}