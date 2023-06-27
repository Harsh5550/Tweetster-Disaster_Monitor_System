package com.harsh.tweetster.v1.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.harsh.tweetster.R;
import com.harsh.tweetster.databinding.ActivityMainBinding;
import com.harsh.tweetster.v1.models.User;
import com.harsh.tweetster.v1.utilities.Constants;
import com.harsh.tweetster.v1.utilities.PreferenceManager;
import com.scottyab.aescrypt.AESCrypt;

import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Toolbar toolbar;
    private final String url="https://disastermonitor.el.r.appspot.com/tweet";

    private final String modelUrl="https://disastermonitor.el.r.appspot.com/add";
    private List<Address> addresses;
    private PreferenceManager preferenceManager;
    private StringBuilder strAddress;
    private String lat, lon, city;

//    private List<User> users;
    @SuppressLint({"SetTextI18n", "RestrictedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        preferenceManager=new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        //Toolbar
        toolbar=binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Welcome, "+preferenceManager.getString(Constants.KEY_NAME));
        }

        //Ping to wake server
        ping();

//        getUsers();
        getLocation();
        listener();
    }

    //For Option Management
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId=item.getItemId();

        if(itemId==R.id.signOut){
            showToast("Signing Out.......");
            FirebaseFirestore database=FirebaseFirestore.getInstance();
            DocumentReference documentReference=
                    database.collection(Constants.KEY_COLLECTION_USERS).document(
                            preferenceManager.getString(Constants.KEY_USER_ID)
                    );
            HashMap <String, Object> updates=new HashMap<>();
            updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
            documentReference.update(updates)
                    .addOnSuccessListener(unused->{
                        preferenceManager.clear();
                        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e->showToast("Unable to Sign Out"));
        }
        else if(itemId==R.id.deRegister){
            AlertDialog.Builder delDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
            delDialog.setTitle("Deregister?");
            delDialog.setIcon(R.drawable.ic_warning);
            delDialog.setMessage("Are you sure you want to deregister yourself?");
            delDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferenceManager.clear();
                    Intent intent=new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    deRegister();
                }
            });
            delDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            delDialog.show();
        }
        else if (itemId==R.id.aboutUs) {
            // Define the URL you want to open
            String url = "http://disastermonitor.live/about.html";

            // Create an intent with the ACTION_VIEW action and the URL as the data
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        else if(itemId==android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void listener() {
        binding.submit.setOnClickListener(v -> {
            if (binding.inputTweet.getText().toString().trim().isEmpty()) {
                showToast("Enter your Tweet");
            } else if (binding.inputPin.getText().toString().trim().isEmpty()) {
                showToast("Enter your PIN");
            }
            else if (binding.inputPin.getText().toString().length()!=4) {
                showToast("PIN must be of 4-digit only");
            }
            else {
                FirebaseFirestore database=FirebaseFirestore.getInstance();
                try {
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .whereEqualTo(Constants.KEY_PIN, AESCrypt.encrypt("gjagrijcvffgd", binding.inputPin.getText().toString()))
                            .whereEqualTo(Constants.KEY_PHONE_NUMBER, preferenceManager.getString(Constants.KEY_PHONE_NUMBER))
                            .get()
                            .addOnCompleteListener(task->{
                                if (task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0){
                                    volley();
                                }
                                else {
                                    loading(false);
                                    binding.inputPin.setText("");
                                    showToast("Incorrect PIN");
                                }
                            });
                } catch (GeneralSecurityException e) {
                    showToast("Encryption Error");
                }
            }
        });
        binding.textForgetPassword.setOnClickListener(v->{
            AlertDialog.Builder delDialog = new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
            delDialog.setTitle("Re-Register?");
            delDialog.setIcon(R.drawable.ic_warning);
            delDialog.setMessage("You will be required to Re-Register yourself. Proceed?");
            delDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferenceManager.clear();
                    Intent intent=new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    deRegister();
                }
            });
            delDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //for no option
                }
            });
            delDialog.show();
        });
    }
    @SuppressLint("MissingPermission")
    private void getLocation(){
        FusedLocationProviderClient fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location!=null){
                            try {
                                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                int i = 0;
                                strAddress = new StringBuilder();
                                while (addresses.get(0).getAddressLine(i) != null) {
                                    strAddress.append(addresses.get(0).getAddressLine(i));
                                    i++;
                                }
                                lat= String.valueOf(addresses.get(0).getLatitude());
                                lon= String.valueOf(addresses.get(0).getLongitude());
                                city=String.valueOf(addresses.get(0).getLocality());
                            }
                            catch (Exception e){
                                showToast("Network connection is slow. Please try again later");
                                e.printStackTrace();
                            }
                        }
                        else {
                            showToast("Network connection is slow. Please try again later");
                        }
                    }
                })
                .addOnFailureListener(v->{
                    showToast("Network connection is slow. Please try again later");
                });
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.submit.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.submit.setVisibility(View.VISIBLE);
        }
    }

    private void ping(){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            //No response handling required for Ping purpose
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }){
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tweet", "Ping");
                return params;
            }
        };
        RequestQueue queue= Volley.newRequestQueue(MainActivity.this);
        queue.add(stringRequest);
    }

    private void sendToModel() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, modelUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //No response required...
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tweet", binding.inputTweet.getText().toString());
                params.put("lat", lat);
                params.put("long", lon);
                params.put("city", city);

                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(stringRequest);
    }

    private void databaseManager(){
        HashMap<String, Object> cluster=new HashMap<>();
        cluster.put(Constants.KEY_LATITUDE, lat);
        cluster.put(Constants.KEY_LONGITUDE, lon);
        cluster.put(Constants.KEY_COUNT, 1);
        cluster.put(Constants.KEY_TIMESTAMP, FieldValue.serverTimestamp());

        HashMap<String, String> disaster=new HashMap<>();
        disaster.put(Constants.KEY_LATITUDE, String.valueOf(lat));
        disaster.put(Constants.KEY_LONGITUDE, String.valueOf(lon));
        disaster.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
        disaster.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        disaster.put(Constants.KEY_PHONE_NUMBER, preferenceManager.getString(Constants.KEY_PHONE_NUMBER));
        disaster.put(Constants.KEY_TWEET, binding.inputTweet.getText().toString());
        disaster.put(Constants.KEY_ADDRESS, String.valueOf(strAddress));
        disaster.put(Constants.KEY_TIMESTAMP, getCurrentDateTime());
        preferenceManager.putString(Constants.KEY_TIMESTAMP, getCurrentDateTime());
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        final boolean[] flag = {false};
        database.collection(Constants.KEY_COLLECTION_CLUSTER)
                .get()
                .addOnCompleteListener(v-> {
                    loading(false);
                    if (v.isSuccessful() && v.getResult() != null && v.getResult().getDocuments().size()>0) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : v.getResult()) {
                            LatLng destination=new LatLng(Double.parseDouble(String.valueOf(queryDocumentSnapshot.get(Constants.KEY_LATITUDE))), Double.parseDouble(String.valueOf(queryDocumentSnapshot.get(Constants.KEY_LONGITUDE))));
                            if (distanceValid(destination)){
                                flag[0] =true;
                                long count= (long) Objects.requireNonNull(queryDocumentSnapshot.get(Constants.KEY_COUNT)) +1;

                                HashMap<String, Object> map=new HashMap<>();
                                map.put(Constants.KEY_COUNT, count);
                                map.put(Constants.KEY_TIMESTAMP, FieldValue.serverTimestamp());
                                disaster.put(Constants.KEY_CLUSTER_ID, queryDocumentSnapshot.getId());
                                disaster.put(Constants.KEY_DISASTER_NUMBER, String.valueOf(count));
                                database.collection(Constants.KEY_COLLECTION_CLUSTER)
                                        .document(queryDocumentSnapshot.getId())
                                        .update(map);
                                addDisaster(disaster);
                            }
                        }
                        if (!flag[0]){
                            database.collection(Constants.KEY_COLLECTION_CLUSTER)
                                    .add(cluster)
                                    .addOnSuccessListener(task->{
                                        disaster.put(Constants.KEY_CLUSTER_ID, task.getId());
                                        disaster.put(Constants.KEY_DISASTER_NUMBER, "1");
                                        addDisaster(disaster);
                                    })
                                    .addOnFailureListener(Throwable::printStackTrace);
                        }
                    }
                    else {
                        database.collection(Constants.KEY_COLLECTION_CLUSTER)
                                .add(cluster)
                                .addOnSuccessListener(task->{
                                    disaster.put(Constants.KEY_CLUSTER_ID, task.getId());
                                    disaster.put(Constants.KEY_DISASTER_NUMBER, "1");
                                    addDisaster(disaster);
                                })
                                .addOnFailureListener(Throwable::printStackTrace);

                    }
                });
    }

    private boolean distanceValid(LatLng destination){
            Location locationA = new Location("");
            locationA.setLatitude(addresses.get(0).getLatitude());
            locationA.setLongitude(addresses.get(0).getLongitude());
            Location locationB = new Location("");
            locationB.setLatitude(destination.latitude);
            locationB.setLongitude(destination.longitude);
            float distance = (locationA.distanceTo(locationB));
            return distance < 200;
    }


    public static String getCurrentDateTime() {
        // Create a SimpleDateFormat object to format the date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Get the current date and time
        Date now = new Date();

        // Format the date and time as a string
        String currentDateTime = dateFormat.format(now);

        return currentDateTime;
    }

    private void volley(){
        loading(true);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String data = jsonObject.getString("prediction");
                            if (data.equals("1")) {
                                loading(false);
                                databaseManager();
                                sendToModel();
//                                send();
                                showToast("Thanks for Reporting");
                                Intent intent=new Intent(MainActivity.this, MapActivity.class);
                                intent.putExtra("lat", Double.parseDouble(lat));
                                intent.putExtra("lon", Double.parseDouble(lon));
                                intent.putExtra("tweet", binding.inputTweet.getText().toString());
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                loading(false);
                                showToast("Goli Beta... Masti Nai... \uD83D\uDE0E");
                                Intent intent=new Intent(MainActivity.this, MapActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        loading(false);
                        showToast("Oops.....Server Down");
                    }
                }){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params=new HashMap<String, String>();
                params.put("tweet", binding.inputTweet.getText().toString());
                return params;
            }
        };
        RequestQueue newRequestQueue= Volley.newRequestQueue(MainActivity.this);
        newRequestQueue.add(stringRequest);
    }

    private void addDisaster(HashMap<String, String> disaster){
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_DISASTERS)
                .add(disaster)
                .addOnSuccessListener(task->{
                    //
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void deRegister(){
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .delete()
                .addOnSuccessListener(v->{

                })
                .addOnFailureListener(v->{
                    showToast("Unable to Deregister");
                });
    }


//    private void getUsers(){
//        loading(true);
//        FirebaseFirestore database=FirebaseFirestore.getInstance();
//        database.collection(Constants.KEY_COLLECTION_USERS)
//                .get()
//                .addOnCompleteListener(task->{
//                    loading(false);
//                    String currentUserId=preferenceManager.getString(Constants.KEY_USER_ID);
//                    if(task.isSuccessful() && task.getResult() != null){
//                        users= new ArrayList<>();
//                        for(QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
//                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
//                                continue;
//                            }
//                            User user=new User();
//                            user.name=queryDocumentSnapshot.getString(Constants.KEY_NAME);
//                            user.number=queryDocumentSnapshot.getString(Constants.KEY_PHONE_NUMBER);
//                            user.token=queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
//                            user.lon=queryDocumentSnapshot.getDouble(Constants.KEY_LONGITUDE);
//                            user.lat=queryDocumentSnapshot.getDouble(Constants.KEY_LATITUDE);
//                            user.id=queryDocumentSnapshot.getId();
//                            users.add(user);
//                        }
//                    }
//                    else{
//                        showToast("Unable to Fetch User List");
//                    }
//                });
//    }
//
//
//    private void sendNotification(String messageBody){
//        ApiClient.getClient().create(ApiService.class).sendMessage(
//                Constants.getRemoteMsgHeaders(),
//                messageBody
//        ).enqueue(new Callback<String>() {
//            @Override
//            public void onResponse(@org.checkerframework.checker.nullness.qual.NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
//                if(response.isSuccessful()){
//                    try {
//                        if(response.body()!=null){
//                            JSONObject responseJson=new JSONObject(response.body());
//                            JSONArray results=responseJson.getJSONArray("results");
//                            if(responseJson.getInt("failure")==1){
//                                JSONObject error=(JSONObject) results.get(0);
//                                showToast(error.getString("error"));
//                                return;
//                            }
//                        }
//                    }
//                    catch (JSONException e){
//                        e.printStackTrace();
//                    }
//                }
//                else {
//                    showToast("Error"+response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(@org.checkerframework.checker.nullness.qual.NonNull Call<String> call, @org.checkerframework.checker.nullness.qual.NonNull Throwable t) {
//                showToast(t.getMessage());
//            }
//        });
//    }
//
//    private void send(){
//        try {
//            JSONArray tokens = new JSONArray();
//            for (User user: users){
//                if (distanceValid(new LatLng(user.lat, user.lon))){
//                    tokens.put(user.token);
//                }
//            }
//
//            JSONObject data = new JSONObject();
//            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
//            data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
//
//            JSONObject body = new JSONObject();
//            body.put(Constants.REMOTE_MSG_DATA, data);
//            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
//
//            if (tokens.length()>0){
//                sendNotification(body.toString());
//            }
//        } catch (Exception exception) {
//            showToast(exception.getMessage());
//        }
//    }
}