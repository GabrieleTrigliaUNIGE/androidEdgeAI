package com.gabri.edgeai.misc;

public class Constants {

    public static final int NUM_BYTE_PER_FLOAT = 4;

    public static final int INPUT_IMAGE_SIZE = 32;
    public static final int NUM_CHANNELS_RGB = 3;

    public static final String MODEL_CLASS__APPLE = "Apple";
    public static final String MODEL_CLASS__ORANGE = "Orange";
    public static final String MODEL_CLASS__BANANA = "Banana";

    public static final String[] MODEL_CLASSES = new String[] {
            MODEL_CLASS__APPLE,
            MODEL_CLASS__ORANGE,
            MODEL_CLASS__BANANA
    };


    public static final int[] NETWORK_SIZES = new int[] {1, 32, 32, 3};
}
