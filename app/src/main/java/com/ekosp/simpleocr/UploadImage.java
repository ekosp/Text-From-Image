package com.ekosp.simpleocr;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.karan.churi.PermissionManager.PermissionManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import id.zelory.compressor.Compressor;

public class UploadImage extends AppCompatActivity {

    private static final String TAG = UploadImage.class.getSimpleName();
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;

    private static final int REQUEST_TAKE_PHOTO = 1;
    String mCurrentPhotoPath;
    String mImagePathToUpload;
    File fotoFile;

    @BindView(R.id.imgPreview)
    ImageView imgPreview;
    @BindView(R.id.inputKeterangan)
    EditText keterangan;
//    @BindView(R.id.lokasiUser)
//    TextView lokasi;

    @OnClick(R.id.capturePicture)
    public void btnCapturePicture(View v) {
        isSampleImage = false;
        dispatchTakePictureIntent();
    }

    @OnClick(R.id.btnUpload)
    public void btnUpload(View v) {


        String OCRresult = null;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        // TextView tv_OCR_Result = (TextView) findViewById(R.id.tv_OCR_Result);
        // tv_OCR_Result.setText(OCRresult);
        Helper.showAlertDialog(this, OCRresult);

    }

    private Boolean isSampleImage = false;

    @OnClick(R.id.samplePicture)
    public void gambarSample(View v) {
        isSampleImage = true;

        dispatchTakePictureIntent();
    }

    long totalSize = 0;
    private HashMap datUser;

    private String TYPE;

    // private File actualImage;
    private File compressedImage;
    private ProgressDialog progressDialog;
    PermissionManager permission;

    Bitmap image;
    private TessBaseAPI mTess;
    String datapath = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);
        ButterKnife.bind(this);
        permission = new PermissionManager() {
        };

        permission.checkAndRequestPermissions(this);
        //lokasi.setText("adsasdad");

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                return;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = null;
                try {
                    photoURI = FileProvider.getUriForFile(UploadImage.this,
                            BuildConfig.APPLICATION_ID + ".provider",
                            createImageFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("file_uri", mCurrentPhotoPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentPhotoPath = savedInstanceState.getString("file_uri");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            // Show the thumbnail on ImageView
            Uri imageUri = Uri.parse(mCurrentPhotoPath);
            mImagePathToUpload = imageUri.getPath();

            fotoFile = new File(mImagePathToUpload);
            Log.e("Original Foto: ", String.format("Size : %s", getReadableFileSize(fotoFile.length())));
            Log.d("Original Foto", "Original image save in " + fotoFile.getPath());

            // TODO comment to include image compression
            imgPreview.setVisibility(View.VISIBLE);

            if (isSampleImage){
                image = BitmapFactory.decodeResource(getResources(), R.drawable.sample_ktp_2);
            } else {
                 image = BitmapFactory.decodeFile(fotoFile.getAbsolutePath());

            }

            imgPreview.setImageBitmap(image);

            // TODO uncomment to compress image
            // customCompressImage();
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    public void customCompressImage() {
        if (fotoFile == null) {
            showError("Please choose an image!");
        } else {
            // Compress image in main thread using custom Compressor
            try {
                compressedImage = new Compressor(this)
                        .setMaxWidth(640)
                        .setMaxHeight(480)
                        .setQuality(75)
                        .setCompressFormat(Bitmap.CompressFormat.WEBP)
                        .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES).getAbsolutePath())
                        .compressToFile(fotoFile);

                setCompressedImage();
            } catch (IOException e) {
                e.printStackTrace();
                showError(e.getMessage());
            }

        }
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    public String getReadableFileSize(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void setCompressedImage() {
        imgPreview.setVisibility(View.VISIBLE);
        imgPreview.setImageBitmap(BitmapFactory.decodeFile(compressedImage.getAbsolutePath()));
        Log.e("Compressed Foto :", String.format("Size : %s", getReadableFileSize(compressedImage.length())));
        Log.d("Compressed Foto", "Compressed image save in " + compressedImage.getPath());
    }

    private void deleteGeneratedFoto() {

        // delete original image
        if (fotoFile.exists()) {
            if (fotoFile.delete()) {
                Log.e("Foto Deleted :", fotoFile.getPath());
            }
        }

        // delete compresed image
        if (compressedImage.exists()) {
            if (compressedImage.delete()) {
                Log.e("Foto Deleted :", compressedImage.getPath());
            }
        }
    }


    private void checkFile(File dir) {
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}