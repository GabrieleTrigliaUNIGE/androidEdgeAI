package com.gabri.edgeai;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.gabri.edgeai.interfaces.IRecognitionDone;
import com.gabri.edgeai.misc.Constants;
import com.gabri.edgeai.processing.ImageProcessing;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements IRecognitionDone {

    private final static String TAG = "MainActivity";

    private Button bttTakePicture;
    private Button bttFromGallery;

    private ImageView ivPicture;

    private TextView tvPrediction;

    private ActivityResultLauncher<Intent> arlTakePhoto;
    private ActivityResultLauncher<String> arlFromGallery;

    private ImageProcessing imageProcessing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bttTakePicture = findViewById(R.id.bttTakePicture);
        bttFromGallery = findViewById(R.id.bttFromGallery);

        ivPicture = findViewById(R.id.ivPicture);

        tvPrediction = findViewById(R.id.tvPrediction);

        arlTakePhoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.i(TAG, "");
                    if ((result.getResultCode() != RESULT_OK) || (result.getData() == null))
                        return;

                    Bundle bundle = result.getData().getExtras();
                    Bitmap bitmap = (Bitmap) bundle.get("data");

                    ivPicture.setImageBitmap(bitmap);
                    recognize(bitmap);
                }
        );

        arlFromGallery = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                PictureURI -> {
                    ContentResolver contentResolver = getContentResolver();
                    try {
                        Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, PictureURI),
                                (imageDecoder, imageInfo, source) -> imageDecoder.setMutableRequired(true));

                        ivPicture.setImageBitmap(bitmap);
                        recognize(bitmap);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );


        bttTakePicture.setOnClickListener(v -> {
            arlTakePhoto.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        });

        bttFromGallery.setOnClickListener(v -> {
            arlFromGallery.launch("image/*");
        });
    }

    private void recognize(Bitmap bitmap) {
        imageProcessing = new ImageProcessing(this, bitmap);
        imageProcessing.run();
    }

    @Override
    public void onRecognitionDone() {
        float maxConfidence = imageProcessing.getMaxConfidenceValue();
        int argMaxConfidence = imageProcessing.getMaxConfidenceIndex();

        String result = "Classification: " + Constants.MODEL_CLASSES[argMaxConfidence];
        result += "\n";
        result += "Confidence: " + maxConfidence;

        tvPrediction.setText(result);
    }
}