package com.harsh.tweetster.v1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harsh.tweetster.databinding.ActivityChangePasswordBinding;
import com.harsh.tweetster.v1.utilities.Constants;
import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }
    private void setListeners(){
        binding.changePassword.setOnClickListener(v->{
            if(isValidDetails()){
                changePassword();
            }
        });
    }
    private void changePassword(){
        Intent intent=getIntent();
        String phone=intent.getStringExtra("phone");
        final String[] userId = new String[1];
        loading(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE_NUMBER, phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> snapshot = queryDocumentSnapshots.getDocuments();
                    for (DocumentSnapshot snapshot1:snapshot){
                        userId[0] = snapshot1.getId();
                    }
                    try {
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                        .document(userId[0])
                                                .update(Constants.KEY_PASSWORD, AESCrypt.encrypt("gjagrijcvffgd" ,binding.inputPassword.getText().toString()))
                                                        .addOnSuccessListener(v->{
                                                            loading(false);
                                                            Intent i = new Intent(getApplicationContext(), SignInActivity.class);
                                                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(i);
                                                                }
                                                        ).addOnFailureListener(exception -> {
                                    loading(false);
                                    showToast(exception.getMessage());
                                });
                    } catch (GeneralSecurityException e) {
                        showToast("Encryption Error");
                    }
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }
    private boolean isValidDetails(){
        if (binding.inputPassword.getText().toString().trim().isEmpty())
        {
            showToast("Enter Password");
            return false;
        }
        else if(!isPasswordValid()){
            showToast("Enter valid Password");
            return false;
        }
        else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("Confirm your Password");
            return false;
        }
        else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString()))
        {
            showToast("Password and Confirm Password must be same");
            return false;
        }
        else{
            return true;
        }
    }
    private boolean isPasswordValid(){
        String regex = "^(?=.*[0-9])" + "(?=.*[a-z])(?=.*[A-Z])" + "(?=.*[*@#$%^&+=])" + "(?=\\S+$).{8,20}$";
        Pattern p=Pattern.compile(regex);
        Matcher m=p.matcher(binding.inputPassword.getText().toString());
        return m.matches();
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.changePassword.setVisibility(View.INVISIBLE);
            binding.progressBar2.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar2.setVisibility(View.INVISIBLE);
            binding.changePassword.setVisibility(View.VISIBLE);
        }
    }
}
