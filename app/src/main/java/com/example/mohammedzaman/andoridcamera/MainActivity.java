package com.example.mohammedzaman.andoridcamera;


import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Vertex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity  {

    private ImageSurfaceView mPreview;

    static final int REQUEST_GALLERY_IMAGE = 100;
    static final int REQUEST_CODE_PICK_ACCOUNT = 101;
    static final int REQUEST_ACCOUNT_AUTHORIZATION = 102;
    static final int REQUEST_PERMISSIONS = 13;
    private static String accessToken;
    private final String TAG = "FYP IndoorNav";

    private Account mAccount;
    private TextView facesDetected;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPreview = (ImageSurfaceView) findViewById(R.id.c_preview);
        facesDetected = findViewById(R.id.textView2);
        mScrollView = (ScrollView) findViewById(R.id.myScoll);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_PERMISSIONS);



    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getAuthToken();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK && data != null) {


        } else if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                AccountManager am = AccountManager.get(this);
                Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
                for (Account account : accounts) {
                    if (account.name.equals(email)) {
                        mAccount = account;
                        break;
                    }
                }
                getAuthToken();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "No Account Selected", Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == REQUEST_ACCOUNT_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                Bundle extra = data.getExtras();
                onTokenReceived(extra.getString("authtoken"));
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Authorization Failed", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    public void performCloudVisionRequest(Bitmap b) {
        if (b != null) {
            callCloudVision(resizeBitmap(b));
        }
    }




    @SuppressLint("StaticFieldLeak")
    private synchronized void callCloudVision(final Bitmap bitmap) {


        new AsyncTask<Object, Void, BatchAnnotateImagesResponse>() {
            @Override
            protected BatchAnnotateImagesResponse doInBackground(Object... params) {
                try {
                    GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder
                            (httpTransport, jsonFactory, credential);
                    Vision vision = builder.build();

                    List<Feature> featureList = new ArrayList<>();

                    Feature labelDetection = new Feature();
                    labelDetection.setType("LABEL_DETECTION");
                    labelDetection.setMaxResults(1);
                    featureList.add(labelDetection);



                    List<AnnotateImageRequest> imageList = new ArrayList<>();
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                    Image base64EncodedImage = getBase64EncodedJpeg(bitmap);
                    annotateImageRequest.setImage(base64EncodedImage);
                    annotateImageRequest.setFeatures(featureList);
                    imageList.add(annotateImageRequest);

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(imageList);
                    Log.d("imageSent",bitmap.toString() );

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "Sending request to Google Cloud");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return response;

                } catch (GoogleJsonResponseException e) {
                    Log.e(TAG, "Request error: " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "Request error: " + e.getMessage());
                }
                return null;
            }

            protected void onPostExecute(BatchAnnotateImagesResponse response) {
                facesDetected.append(getDetectedLabels(response));


            }

        }.execute();
    }

    private String getDetectedLabels(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message.append(String.format(Locale.getDefault(), "%.3f: %s",
                        label.getScore(), label.getDescription()));
                message.append("\n");
                message.append("\n");


            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private String getDetectedFaceNumber(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        int faceNum = 0;
        if (faces != null) {
            for (FaceAnnotation face : faces) {
                faceNum++;

            }
        } else {
            message.append("nothing\n");
        }


        return Integer.toString(faceNum);
    }


    private String getDetectedFacesAnger(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        if (faces != null) {
            for (FaceAnnotation face : faces) {
                message.append(face.getAngerLikelihood());
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private String getDetectedLandmark(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<EntityAnnotation> landmarks = response.getResponses().get(0).getLandmarkAnnotations();
        if (landmarks != null) {
            for (EntityAnnotation landmark : landmarks) {
                message.append(landmark.getDescription());
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private String getDetectedFacesJoy(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        if (faces != null) {
            for (FaceAnnotation face : faces) {
                message.append(face.getJoyLikelihood());
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private String getDetectedFacesBoundBox(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
        if (faces != null) {
            for (FaceAnnotation face : faces) {
                message.append("\n");
                message.append(face.getBoundingPoly().getVertices());
                message.append("\n");
                setBoundingBox(face);
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }

    private void setBoundingBox(FaceAnnotation face) {

        List<Point> squareVertices = new ArrayList<>();


        for (Vertex vertex : face.getFdBoundingPoly().getVertices()) {
            Point point = new Point(vertex.getX(), vertex.getY());
            squareVertices.add(point);
        }



    }


    private String getDetectedTexts(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder("");
        List<EntityAnnotation> texts = response.getResponses().get(0)
                .getTextAnnotations();
        if (texts != null) {
            for (EntityAnnotation text : texts) {
                message.append(String.format(Locale.getDefault(), "%s: %s",
                        text.getLocale(), text.getDescription()));
                message.append("\n");
            }
        } else {
            message.append("nothing\n");
        }

        return message.toString();
    }



    public Bitmap resizeBitmap(Bitmap bitmap) {

        int maxDimension = 1024;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private void getAuthToken() {
        String SCOPE = "oauth2:https://www.googleapis.com/auth/cloud-platform";
        if (mAccount == null) {
            pickUserAccount();
        } else {
            new GetOAuthToken(MainActivity.this, mAccount, SCOPE, REQUEST_ACCOUNT_AUTHORIZATION)
                    .execute();
        }
    }
    public void onTokenReceived(String token) {
        accessToken = token;
       // launchImagePicker();
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                performCloudVisionRequest(mPreview.getS());
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 0, 3000);

    }





}
