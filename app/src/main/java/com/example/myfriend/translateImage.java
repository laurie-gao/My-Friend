package com.example.myfriend;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient;
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionManager;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.Category;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.FaceDescription;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.ImageAnalysis;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.ImageCaption;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.ImageTag;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.LandmarksModel;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.VisualFeatureTypes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class translateImage extends AppCompatActivity {
    private Button cameraBtn, albumBtn;
    private ImageView blank;
    private ComputerVisionClient client;

    private String subscriptionKey = "d964c2e378b1498da6b7956d60f1415a";
    private String endpoint =  "https://laurie.cognitiveservices.azure.com/";

    static final int REQUEST_IMAGE_CAPTURE = 0;
    static final int IMAGE_GALLERY_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate_image);
        if (client == null) {
            client = ComputerVisionManager.authenticate(subscriptionKey).withEndpoint(endpoint);
        }

        cameraBtn = findViewById(R.id.cameraBtn);
        albumBtn = findViewById(R.id.albumBtn);
        blank = findViewById(R.id.blank);

        cameraBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        });

        albumBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK);
                File photoDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String photoDirectoryPath = photoDirectory.getPath();
                Uri data = Uri.parse(photoDirectoryPath);

                pickPhotoIntent.setDataAndType(data,"image/*");
                startActivityForResult(pickPhotoIntent, IMAGE_GALLERY_REQUEST);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE){
            super.onActivityResult(requestCode, resultCode, data);
            Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            blank.setImageBitmap(bitmap);
            MyBitMap cameraBitmap = new MyBitMap(bitmap, client);
            new analyzeImage(this).execute(cameraBitmap);
        }
        if (requestCode == IMAGE_GALLERY_REQUEST){
            Uri imageUri = data.getData();
            InputStream inputStream;
            try {
                inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                blank.setImageBitmap(bitmap);
                MyBitMap cameraBitmap = new MyBitMap(bitmap, client);
                new analyzeImage(this).execute(cameraBitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static class MyBitMap {
        Bitmap myBitmap;
        ComputerVisionClient comVisionClient;

        MyBitMap(Bitmap myBitmap, ComputerVisionClient comVisionClient) {
            this.myBitmap = myBitmap;
            this.comVisionClient = comVisionClient;
        }
    }

    public class analyzeImage extends AsyncTask<MyBitMap, String, String> {

        String result;
        private WeakReference<Context> contextRef;

        public analyzeImage(Context context) {
            contextRef = new WeakReference<>(context);
        }
        @Override
        protected String doInBackground(MyBitMap... myBitMaps) {
            List<VisualFeatureTypes> featuresToExtractFromLocalImage = new ArrayList<>();
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.DESCRIPTION);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.CATEGORIES);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.TAGS);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.FACES);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.ADULT);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.COLOR);
            featuresToExtractFromLocalImage.add(VisualFeatureTypes.IMAGE_TYPE);

            //convert photo taken by camera to drawable byte array
            Bitmap cameraBitmap = myBitMaps[0].myBitmap;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] cameraPhotoByteArray = outputStream.toByteArray();

            //call microsoft azure API
            ImageAnalysis analysis = myBitMaps[0].comVisionClient.computerVision().analyzeImageInStream()
                    .withImage(cameraPhotoByteArray)
                    .withVisualFeatures(featuresToExtractFromLocalImage)
                    .execute();

            StringBuffer englishResultBuffer = new StringBuffer();

            System.out.println("\nCaptions: ");
            for (ImageCaption caption : analysis.description().captions()) {
                System.out.printf("\'%s\' with confidence %f\n", caption.text(), caption.confidence());
                englishResultBuffer.append(caption.text());
            }
            result = englishResultBuffer.toString();
            System.out.println("--------------------------");
            System.out.println("english result => " + result);

            System.out.println("\nCategories: ");
            for (Category category : analysis.categories()) {
                System.out.printf("\'%s\' with confidence %f\n", category.name(), category.score());
            }

            System.out.println("\nTags: ");
            for (ImageTag tag : analysis.tags()) {
                System.out.printf("\'%s\' with confidence %f\n", tag.name(), tag.confidence());
            }

            System.out.println("\nFaces: ");
            for (FaceDescription face : analysis.faces()) {
                System.out.printf("\'%s\' of age %d at location (%d, %d), (%d, %d)\n", face.gender(), face.age(),
                        face.faceRectangle().left(), face.faceRectangle().top(),
                        face.faceRectangle().left() + face.faceRectangle().width(),
                        face.faceRectangle().top() + face.faceRectangle().height());
            }

            System.out.println("\nLandmarks: ");
            for (Category category : analysis.categories())
            {
                if (category.detail() != null && category.detail().landmarks() != null)
                {
                    for (LandmarksModel landmark : category.detail().landmarks())
                    {
                        System.out.printf("\'%s\' with confidence %f\n", landmark.name(), landmark.confidence());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute (String s){
            System.out.println("--------------------------");
            System.out.println("result: " + result);

            LinearLayout linearLayout = findViewById(R.id.resultLinearLayout);
            linearLayout.removeAllViews();
            Context myContext = contextRef.get();
            TextView textResult = new TextView(myContext);
            textResult.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            textResult.setText(result);
            textResult.setTextColor(Color.parseColor("#FFFFFF"));
            textResult.setTextSize(25);
            textResult.setGravity(Gravity.CENTER);
            linearLayout.addView(textResult);

        }
    }
}
