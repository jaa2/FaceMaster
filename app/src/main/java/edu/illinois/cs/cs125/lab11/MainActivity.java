package edu.illinois.cs.cs125.lab11;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Main class for our UI design lab.
 */

public final class MainActivity extends AppCompatActivity {
    /**Code to get image from file explorer. */
    private static final int IMAGE_CAPTURE_REQUEST_CODE = 1;
    /**Code to read a file from the file system. */
    private static final int READ_REQUEST_CODE = 42;
    /** Code to take a picture. **/
    private static final int CAMERA_REQUEST_CODE = 100;

    /** Base64 encoded image to submit to API. */
    private String encodedString;
    /**Uri of current selected photo. */
    private Uri currentPhotoURI;
    /** Current file that we are using for our image request. */
    private boolean photoRequestActive = false;

    /** Whether a current photo request is being processed. */
    private File currentPhotoFile = null;
    /** Default logging tag for messages from the main activity. */
    private static final String TAG = "Lab11:Main";

    /** Request queue for our API requests. */
    private static RequestQueue requestQueue;

    /**
     * View for our text.
     */
    private TextView responseTextView;


    /**
     * Run when this activity comes to the foreground.
     *
     * @param savedInstanceState unused
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Set up the queue for our API requests
        requestQueue = Volley.newRequestQueue(this);

        setContentView(R.layout.activity_main);

        final Button getResult = findViewById(R.id.get_result);
        getResult.setOnClickListener(v -> {
            Log.d(TAG, "Open file button clicked");
            new CallAPI(MainActivity.this).execute("https://api-us.faceplusplus.com/facepp/v3/detect",
                    "gender,age,emotion,ethnicity,beauty", encodedString);
        });
        final Button openFile = findViewById(R.id.openFile);
        openFile.setOnClickListener(v -> {
            startOpenFile();
            responseTextView.setText("Image loaded");
        });

        final Button buttonOpenCamera = findViewById(R.id.buttonOpenCamera);
        buttonOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        });

        // Make sure the camera exists on the hardware
        if (!getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            ViewGroup layout = (ViewGroup) buttonOpenCamera.getParent();
            layout.removeView(buttonOpenCamera);
        }

        responseTextView = findViewById(R.id.textView_response);
    }


    /**
     * Run when this activity is no longer visible.
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /** Start the process of opening a file.
     */
    private void startOpenFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }
    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                final Intent resultData) {
        if (resultCode != Activity.RESULT_OK) {
            Log.w(TAG, "onActivityResult with code " + requestCode + " failed");
            if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
                photoRequestActive = false;
            }
            return;
        }
        if (requestCode == READ_REQUEST_CODE) {
            currentPhotoURI = resultData.getData();
        } else if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
            currentPhotoURI = Uri.fromFile(currentPhotoFile);
            photoRequestActive = false;
        } else {
            Log.w(TAG, "Unhandled activityResult with code " + requestCode);
            return;
        }
        Log.d(TAG, "Photo selection produced URI " + currentPhotoURI);
        if (currentPhotoURI != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentPhotoURI);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
