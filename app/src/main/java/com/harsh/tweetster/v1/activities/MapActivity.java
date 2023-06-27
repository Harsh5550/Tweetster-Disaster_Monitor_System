package com.harsh.tweetster.v1.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.harsh.tweetster.R;
import com.harsh.tweetster.databinding.ActivityMapsBinding;
import com.harsh.tweetster.v1.models.Disaster;
import com.harsh.tweetster.v1.utilities.Constants;
import com.harsh.tweetster.v1.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private List <Address> addresses;
    private float distance;
    private StringBuilder str;
    private PreferenceManager preferenceManager;
    private Disaster disaster;
    private Toolbar toolbar;
    private FirebaseFirestore database;
    private double lat, lon;

    public MapActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        toolbar=binding.toolbar;
        setActionBar(toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.grey));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.grey));
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle("Welcome, "+preferenceManager.getString(Constants.KEY_NAME));
        }


        getToken();
        database=FirebaseFirestore.getInstance();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);
    }
    private void listener(){
        binding.reportDisaster.setOnClickListener(v->{
            Intent intent=new Intent(MapActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void updateUserLocation(){
        HashMap<String, Object> map=new HashMap<>();
        map.put(Constants.KEY_LATITUDE, lat);
        map.put(Constants.KEY_LONGITUDE, lon);
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(map)
                .addOnSuccessListener(v->{
                    //
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database=FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(Throwable::printStackTrace);
    }



    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (getIntent()!=null){
            Intent intent=getIntent();
            mMap.addMarker(new MarkerOptions().position(new LatLng(intent.getDoubleExtra("lat", 0), intent.getDoubleExtra("lon", 0))).title(preferenceManager.getString(Constants.KEY_NAME)+": "+intent.getStringExtra("tweet")));
        }

        //To fetch current location
        FusedLocationProviderClient fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(MapActivity.this);
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location!=null){
                            try {
                                Geocoder geocoder = new Geocoder(MapActivity.this, Locale.getDefault());
                                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                lat= (addresses.get(0).getLatitude());
                                lon= (addresses.get(0).getLongitude());
                                toolbar.setSubtitle(addresses.get(0).getLocality()+", "+addresses.get(0).getCountryName());
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude())));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude()), 16f));
                                updateUserLocation();
                                listener();
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


//        //For showcasing existing Disasters
//        database.collection(Constants.KEY_COLLECTION_CLUSTER)
//                .whereGreaterThanOrEqualTo(Constants.KEY_COUNT, 3)
//                .get()
//                .addOnCompleteListener(v->{
//                    if (v.isSuccessful() && v.getResult()!=null && v.getResult().getDocuments().size()>0){
//                        for (DocumentSnapshot documentSnapshot: v.getResult().getDocuments()){
//                            clusterIdList.add(documentSnapshot.getId());
//                            mMap.addCircle(new CircleOptions()
//                                    .center(new LatLng(Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString(Constants.KEY_LATITUDE))),
//                                            Double.parseDouble(Objects.requireNonNull(documentSnapshot.getString(Constants.KEY_LONGITUDE)))))
//                                    .radius(200)
//                                    .fillColor(Color.argb((float) 0.25, 1, 0, 0))
//                                    .strokeColor(Color.RED));
//                        }
//                        database.collection(Constants.KEY_COLLECTION_DISASTERS)
//                                .get()
//                                .addOnCompleteListener(task-> {
//                                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0) {
//                                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
//                                            if (clusterIdList.contains(documentSnapshot.getString(Constants.KEY_CLUSTER_ID))) {
//                                                disaster=new Disaster();
//                                                disaster.name=documentSnapshot.getString(Constants.KEY_NAME);
//                                                disaster.number=documentSnapshot.getString(Constants.KEY_PHONE_NUMBER);
//                                                disaster.userId=documentSnapshot.getString(Constants.KEY_USER_ID);
//                                                disaster.address=documentSnapshot.getString(Constants.KEY_ADDRESS);
//                                                disaster.latitude= documentSnapshot.getString(Constants.KEY_LATITUDE);
//                                                disaster.longitude=documentSnapshot.getString(Constants.KEY_LONGITUDE);
//                                                disaster.clusterId=documentSnapshot.getString(Constants.KEY_CLUSTER_ID);
//                                                disaster.timeStamp=documentSnapshot.getString(Constants.KEY_TIMESTAMP);
//                                                disaster.tweet=documentSnapshot.getString(Constants.KEY_TWEET);
//                                                disasterList.add(disaster);
//
//                                                mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(disaster.latitude), Double.parseDouble(disaster.longitude))).title(disaster.name+": "+disaster.tweet));
//                                            }
//                                        }
//                                    }
//                                });
//                    }
//                });


        //For showcasing new Disasters
        database.collection(Constants.KEY_COLLECTION_CLUSTER)
                .whereGreaterThanOrEqualTo(Constants.KEY_COUNT, 3)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if(error==null && value!=null) {
                            for (DocumentChange documentChange : value.getDocumentChanges()) {
                                String id = documentChange.getDocument().getId();
                                if (documentChange.getType()==DocumentChange.Type.ADDED){
                                    mMap.addCircle(new CircleOptions()
                                            .center(new LatLng(Double.parseDouble(Objects.requireNonNull(documentChange.getDocument().getString(Constants.KEY_LATITUDE))),
                                                    Double.parseDouble(Objects.requireNonNull(documentChange.getDocument().getString(Constants.KEY_LONGITUDE)))))
                                            .radius(200)
                                            .fillColor(Color.argb((float) 0.15, 1, 0, 0))
                                            .strokeColor(Color.RED));

                                    database.collection(Constants.KEY_COLLECTION_DISASTERS)
                                            .get()
                                            .addOnSuccessListener(task -> {
                                                if (task.getDocuments().size() > 0) {
                                                    for (DocumentSnapshot documentSnapshot : task.getDocuments()) {
                                                        if (Objects.equals(documentSnapshot.getString(Constants.KEY_CLUSTER_ID), id)) {
                                                            disaster = new Disaster();
                                                            disaster.name = documentSnapshot.getString(Constants.KEY_NAME);
                                                            disaster.number = documentSnapshot.getString(Constants.KEY_PHONE_NUMBER);
                                                            disaster.userId = documentSnapshot.getString(Constants.KEY_USER_ID);
                                                            disaster.address = documentSnapshot.getString(Constants.KEY_ADDRESS);
                                                            disaster.latitude = documentSnapshot.getString(Constants.KEY_LATITUDE);
                                                            disaster.longitude = documentSnapshot.getString(Constants.KEY_LONGITUDE);
                                                            disaster.clusterId = documentSnapshot.getString(Constants.KEY_CLUSTER_ID);
                                                            disaster.timeStamp = documentSnapshot.getString(Constants.KEY_TIMESTAMP);
                                                            disaster.tweet = documentSnapshot.getString(Constants.KEY_TWEET);
                                                            mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(disaster.latitude), Double.parseDouble(disaster.longitude))).title(disaster.name+": "+disaster.tweet));
                                                        }
                                                    }
                                                }
                                            });
                                }
                                else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                                    database.collection(Constants.KEY_COLLECTION_DISASTERS)
                                            .get()
                                            .addOnSuccessListener(task -> {
                                                if (task.getDocuments().size() > 0) {
                                                    for (DocumentSnapshot documentSnapshot : task.getDocuments()) {
                                                        if (Objects.equals(documentSnapshot.getString(Constants.KEY_CLUSTER_ID), id) && Objects.equals(documentSnapshot.getString(Constants.KEY_DISASTER_NUMBER), String.valueOf(documentChange.getDocument().get(Constants.KEY_COUNT))))
                                                        {
                                                            disaster = new Disaster();
                                                            disaster.name = documentSnapshot.getString(Constants.KEY_NAME);
                                                            disaster.number = documentSnapshot.getString(Constants.KEY_PHONE_NUMBER);
                                                            disaster.userId = documentSnapshot.getString(Constants.KEY_USER_ID);
                                                            disaster.address = documentSnapshot.getString(Constants.KEY_ADDRESS);
                                                            disaster.latitude = documentSnapshot.getString(Constants.KEY_LATITUDE);
                                                            disaster.longitude = documentSnapshot.getString(Constants.KEY_LONGITUDE);
                                                            disaster.clusterId = documentSnapshot.getString(Constants.KEY_CLUSTER_ID);
                                                            disaster.timeStamp = documentSnapshot.getString(Constants.KEY_TIMESTAMP);
                                                            disaster.tweet = documentSnapshot.getString(Constants.KEY_TWEET);
                                                            mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(disaster.latitude), Double.parseDouble(disaster.longitude))).title(disaster.name+": "+disaster.tweet));
                                                        }
                                                    }
                                                }
                                            });

                                }
                            }
                        }
                    }
                });
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
        return super.onOptionsItemSelected(item);
    }
}