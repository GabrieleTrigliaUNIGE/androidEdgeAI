package com.gabri.edgeai.processing;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.gabri.edgeai.interfaces.IRecognitionDone;
import com.gabri.edgeai.misc.Constants;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageProcessing {

    private final Context context;
    private IRecognitionDone iRecognitionDone;

    private final Bitmap theImageToProcess;

    private Bitmap squaredImage;

    private Model model;
    private TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(Constants.NETWORK_SIZES, DataType.FLOAT32);
    private ByteBuffer byteBuffer;

    private float[] modelConfidenceValues;
    private float maxConfidenceValue = 0.0f;
    private int maxConfidenceIndex = 0;

    public ImageProcessing(Context context, Bitmap theImageToProcess) {
        this.context = context;
        this.iRecognitionDone = (IRecognitionDone) context;
        this.theImageToProcess = theImageToProcess;

        try {
            model = Model.newInstance(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int nBytesPerImage = getNumBytePerImage();
        byteBuffer = ByteBuffer.allocateDirect(nBytesPerImage);
        byteBuffer.order(ByteOrder.nativeOrder());
    }

    public int getMaxConfidenceIndex() {
        return maxConfidenceIndex;
    }
    public float getMaxConfidenceValue() {
        return maxConfidenceValue;
    }
    public float[] getModelConfidenceValues() {
        return modelConfidenceValues;
    }

    // If the class extends Thread \\
//    @Override
//    public void run() {
//        preProcess();
//        process();
//        postProcess();
//    }

    public void run() {
        new Thread( () -> {
            preProcess();
            processing();
            postProcess();
        }).start();
    }

    private void preProcess() {
        byteBuffer.clear();
        squaredImage = Bitmap.createScaledBitmap(theImageToProcess, Constants.INPUT_IMAGE_SIZE, Constants.INPUT_IMAGE_SIZE, false);
        extractRGB_addToByteBuffer();
        inputFeature0.loadBuffer(byteBuffer);
    }

    private void processing() {
        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer out = outputs.getOutputFeature0AsTensorBuffer();

        modelConfidenceValues = out.getFloatArray();
    }

    private void postProcess() {
        getConfidenceMax_ArgMax();
        model.close();

        Handler h = new Handler(Looper.getMainLooper());
        h.post( () -> {
            iRecognitionDone.onRecognitionDone();
        });

    }

    private void getConfidenceMax_ArgMax() {
        for (int i = 0; i < modelConfidenceValues.length; i++)
            if (modelConfidenceValues[i] > maxConfidenceValue) {
                maxConfidenceIndex = i;
                maxConfidenceValue = modelConfidenceValues[i];
            }
    }

    private int getNumBytePerImage () {
        int nPixelsPerImage = Constants.INPUT_IMAGE_SIZE * Constants.INPUT_IMAGE_SIZE;
        int nFloatsPerImage = nPixelsPerImage * Constants.NUM_CHANNELS_RGB;
        int nBytesPerImage = nFloatsPerImage * Constants.NUM_BYTE_PER_FLOAT;

        return  nBytesPerImage;
    }

    private void extractRGB_addToByteBuffer() {
        for (int i = 0; i < Constants.INPUT_IMAGE_SIZE; i++) {
            for (int j = 0; j < Constants.INPUT_IMAGE_SIZE; j++) {
                int rgbPixelValue = squaredImage.getPixel(i, j); // RGB
                byteBuffer.putFloat( extractRed(rgbPixelValue) );
                byteBuffer.putFloat( extractGreen(rgbPixelValue) );
                byteBuffer.putFloat( extractBlue(rgbPixelValue) );
            }
        }
    }

    private int extractBlue(int rgbPixelValue) {
        return applyBitmask(rgbPixelValue);
    }
    private int extractGreen(int rgbPixelValue) {
        return applyBitmask(rgbPixelValue >> 8);
    }
    private int extractRed(int rgbPixelValue) {
        return applyBitmask(rgbPixelValue >> 16);
    }
    private int applyBitmask (int rgbPixelValue) {
        return rgbPixelValue & 0xFF;
    }
}
