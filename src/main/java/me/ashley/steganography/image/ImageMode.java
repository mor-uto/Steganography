package me.ashley.steganography.image;

public enum ImageMode {
    GRAYSCALE(1),
    RGB(3);

    public final int channels;

    ImageMode(int c) {
        this.channels = c;
    }
}