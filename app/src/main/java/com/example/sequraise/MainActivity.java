package com.example.sequraise;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int REQUEST_IMAGE_CAPTURE_CODE = 2;
    private ImageView imageView;
    private TextView txtTimestamp, txtConnectivity, txtCharging, txtChargeValue, txtLocation, txtCaptureCount, txtFrequency;
    private Button btnRefresh;
    private int captureCount = 0;
    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {android.Manifest.permission.CAMERA};
    private long lastCaptureTimestamp;
    private Uri imageUri;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        captureImage();

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAllData();
                captureImage();
                getTimestamp();
                setInternetState();
                getBatteryStatus();
                retrieveLocation();
                txtCaptureCount.setText(String.valueOf(captureCount));
            }
        });
    }

    private String getFileExtension(Uri uri){
        ContentResolver resolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(resolver.getType(uri));
    }

    private void uploadAllData(){
        if(imageUri != null){
            StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        String downloadUrl;
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    downloadUrl = uri.toString();
                                    // Use the download URL of the image
                                }
                            });
                            UploadData uploadData = new UploadData(txtCaptureCount.getText().toString(),
                                    txtFrequency.getText().toString(), txtConnectivity.getText().toString(),
                                    txtCharging.getText().toString(), txtChargeValue.getText().toString(),
                                    txtLocation.getText().toString(), downloadUrl);
                            String uploadID = databaseReference.push().getKey();
                            databaseReference.child(uploadID).setValue(uploadData);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Upload Failed!!!!", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }


    //check permissions and capture image
    private void captureImage() {
        if (allPermissionsGranted()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            if (allPermissionsGranted()) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE_CODE);
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        lastCaptureTimestamp = System.currentTimeMillis();
        updateCaptureFrequency();
    }

    @Override
    protected void onStart() {
        super.onStart();

        getTimestamp();
        setInternetState();
        getBatteryStatus();
        retrieveLocation();
        txtCaptureCount.setText(String.valueOf(captureCount));
    }

    private void getTimestamp() {
        //Timestamp
        txtTimestamp.setText(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()) + " " + new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new Date()));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureCount = 0;
    }

    private void updateCaptureFrequency() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                long captureFrequency = calculateCaptureFrequency(currentTimeMillis);
                txtFrequency.setText(String.valueOf(captureFrequency));
            }
        });
    }

    private long calculateCaptureFrequency(long currentTimeMillis) {
        long timeDifference = (currentTimeMillis - lastCaptureTimestamp) / (1000 * 60);
        return timeDifference;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE_CODE) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            captureCount += 1;
            imageView.setImageBitmap(imageBitmap);
            imageUri = getImageUri(this, imageBitmap);
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void getBatteryStatus() {
        this.registerReceiver(this.batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    //Battery charging status
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            txtChargeValue.setText(String.valueOf(level) + "%");

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
            if (isCharging) {
                txtCharging.setText("ON");
            } else {
                txtCharging.setText("OFF");
            }

        }
    };


    //Internet connectivity status
    private void setInternetState(){
        if(getInternetState()){
            txtConnectivity.setText("ON");
        }else{
            txtConnectivity.setText("OFF");
        }
    }

    private boolean getInternetState() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }


    private void retrieveLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                retrieveLocation();
            } else {
                Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(location != null){
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

//            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//            List<Address> address = geocoder.getFromLocation(latitude,longitude,1);

            txtLocation.setText(String.format("%s, %s", latitude, longitude));

        }
    }

    private void initViews() {
        imageView = findViewById(R.id.image);
        txtTimestamp = findViewById(R.id.txtTimestamp);
        txtConnectivity = findViewById(R.id.txtConnectivity);
        txtCharging = findViewById(R.id.txtCharging);
        txtChargeValue = findViewById(R.id.txtChargeValue);
        txtCaptureCount = findViewById(R.id.txtCaptureCount);
        txtFrequency = findViewById(R.id.txtFrequency);
        txtLocation = findViewById(R.id.txtLocation);
        btnRefresh = findViewById(R.id.btnRefresh);

        storageReference = FirebaseStorage.getInstance().getReference("Data");
        databaseReference = FirebaseDatabase.getInstance().getReference("Data");
    }


    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

}




