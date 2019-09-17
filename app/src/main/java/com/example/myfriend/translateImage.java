package com.example.myfriend;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;

import org.w3c.dom.Text;

public class translateImage extends AppCompatActivity {
    private Button cameraBtn, albumBtn;
    private ImageView blank;
    private TextView textResult;

    private String subscriptionKey = System.getenv("COMPUTER_VISION_SUBSCRIPTION_KEY");
    private String endpoint =  ("COMPUTER_VISION_ENDPOINT");

    //Declare Vision Client
    VisionServiceClient visionServiceClient = new VisionServiceRestClient(subscriptionKey, endpoint);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate_image);

        cameraBtn = findViewById(R.id.cameraBtn);
        albumBtn = findViewById(R.id.albumBtn);
        blank = findViewById(R.id.blank);

        cameraBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent,0);
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = (Bitmap)data.getExtras().get("data");
        blank.setImageBitmap(bitmap);
    }

    public class analyzeImage{
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.camera);
    }
}
