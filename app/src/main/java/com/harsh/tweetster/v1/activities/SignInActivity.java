package com.harsh.tweetster.v1.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harsh.tweetster.R;
import com.harsh.tweetster.databinding.ActivitySignInBinding;
import com.harsh.tweetster.v1.utilities.Constants;
import com.harsh.tweetster.v1.utilities.PreferenceManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;
import java.util.List;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private ActivityResultLauncher<Intent> locationSettingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        binding= ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        locationSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Check if the user has enabled location services or not
                        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                        if (isLocationEnabled) {
                            // User has enabled location services
                            // Proceed with your app logic here
                            if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
                                Intent intent=new Intent(getApplicationContext(), MapActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // User has not enabled location services
                            Toast.makeText(SignInActivity.this, "Location services are still disabled.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        Dexter.withContext(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            preferenceManager.putBoolean(Constants.KEY_PERMISSION_FLAG, true);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).withErrorListener(error -> {
                    Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                }).onSameThread().check();

        if (preferenceManager.getBoolean(Constants.KEY_PERMISSION_FLAG)){
            checkLocation();
            setListeners();
        }
    }


    private void checkLocation(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isInternetAvailable()) {
            android.app.AlertDialog.Builder alertDialog1 = new android.app.AlertDialog.Builder(SignInActivity.this, R.style.CustomAlertDialogTheme);
            alertDialog1.setMessage("Internet access is required to use this application. The app will now close.");
            alertDialog1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Close the app
                    finish();
                }
            });
            alertDialog1.setCancelable(false); // Prevent the user from dismissing the dialog
            android.app.AlertDialog alertDialog2 = alertDialog1.create();
            alertDialog2.show();
        }
        else if (!isLocationEnabled) {
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
            alertDialogBuilder.setMessage("Please enable your location to use this application.");
            alertDialogBuilder.setNegativeButton("Enable Location", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Open location settings for the user to enable their location
                    Intent locationSettingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    locationSettingsLauncher.launch(locationSettingsIntent);
                }
            });
            alertDialogBuilder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Handle cancellation or any other action if needed
                    android.app.AlertDialog.Builder alertDialog1 = new android.app.AlertDialog.Builder(SignInActivity.this, R.style.CustomAlertDialogTheme);
                    alertDialog1.setMessage("Location access is required to use this application. The app will now close.");
                    alertDialog1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Close the app
                            finish();
                        }
                    });
                    alertDialog1.setCancelable(false); // Prevent the user from dismissing the dialog
                    android.app.AlertDialog alertDialog2 = alertDialog1.create();
                    alertDialog2.show();
                }
            });
            alertDialogBuilder.setCancelable(false);
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
                Intent intent=new Intent(getApplicationContext(), MapActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;

    }


        private void setListeners(){
        binding.textCreateNewAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.signIn.setOnClickListener(v->{
            if(binding.inputNumber.getText().toString().trim().isEmpty()){
                showToast("Enter Phone Number");
            }
            else if(!Patterns.PHONE.matcher(binding.inputNumber.getText().toString()).matches()){
                showToast("Enter valid phone number");
            }
            else if(binding.inputNumber.getText().toString().length()!=10){
                showToast("Phone Number must be of 10-digits only");
            }
            if(binding.inputPassword.getText().toString().trim().isEmpty()){
                showToast("Enter Password");
            }
            else{
                signIn();
            }
        });
        binding.textForgetPassword.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), ForgotPasswordActivity.class)));
    }


    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        try {
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .whereEqualTo(Constants.KEY_PHONE_NUMBER, "+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString())
                    .whereEqualTo(Constants.KEY_PASSWORD, AESCrypt.encrypt("gjagrijcvffgd" ,binding.inputPassword.getText().toString()))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                            preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_PHONE_NUMBER, documentSnapshot.getString(Constants.KEY_PHONE_NUMBER));
                            Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            loading(false);
                            binding.inputPassword.setText("");
                            showToast("Unable to Sign In");
                        }
                    });
        } catch (GeneralSecurityException e) {
            showToast("Encryption Error");
        }
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.signIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.signIn.setVisibility(View.VISIBLE);
        }
    }
}