package com.harsh.tweetster.v1.activities;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harsh.tweetster.databinding.ActivitySignUpBinding;
import com.harsh.tweetster.v1.utilities.Constants;
import com.harsh.tweetster.v1.utilities.PreferenceManager;
import com.scottyab.aescrypt.AESCrypt;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {
    private String verificationId;
    private FirebaseAuth mAuth;
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        binding=ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth= FirebaseAuth.getInstance();
        setListeners();
    }
    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.getOTP.setOnClickListener(v->{
            if(isValidSignUpDetails()){
                checkUser();
                sendVerificationCode("+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString());
            }
        });
        binding.signUp.setOnClickListener(v->{
            if(binding.inputOTP.getText().toString().trim().isEmpty()){
                showToast("Enter OTP");
            }
            else if(binding.inputOTP.getText().toString().length()!=6){
                showToast("Enter valid OTP");
            }
            else{
                verifyCode(binding.inputOTP.getText().toString());
            }
        });
    }
    private void sendVerificationCode(String phoneNumber){
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallBacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallBacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override

        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            verificationId = s;
        }
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            final String code=phoneAuthCredential.getSmsCode();
            if(code!=null){
                binding.inputOTP.setText(code);
            }
            verifyCode(code);
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            showToast(e.getMessage());
        }
    };
    private void verifyCode(String code){
        PhoneAuthCredential credential=PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }
    private void signInWithCredential(PhoneAuthCredential credential){
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()){
                        signUp();
                    }
                    else{
                        showToast(Objects.requireNonNull(task.getException()).getMessage());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            showToast("The verification code entered was invalid");
                        }
                    }
                });
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        loading(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        HashMap<String, Object> user=new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_LONGITUDE, 0);
        user.put(Constants.KEY_LATITUDE, 0);
        user.put(Constants.KEY_PHONE_NUMBER, "+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString());
        try {
            user.put(Constants.KEY_PASSWORD, AESCrypt.encrypt("gjagrijcvffgd" ,binding.inputPassword.getText().toString()));
            user.put(Constants.KEY_PIN, AESCrypt.encrypt("gjagrijcvffgd" ,binding.inputPin.getText().toString()));
        } catch (GeneralSecurityException e) {
            showToast("Encryption Error");
        }
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_PHONE_NUMBER, "+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString());
                    Intent intent=new Intent(getApplicationContext(), MapActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }

    private boolean isValidSignUpDetails(){
        if(binding.inputNumber.getText().toString().trim().isEmpty()){
            showToast("Enter Phone Number");
            return false;
        }
        else if(!Patterns.PHONE.matcher(binding.inputNumber.getText().toString()).matches()){
            showToast("Enter valid phone number");
            return false;
        }
        else if(binding.inputNumber.getText().toString().length()!=10){
            showToast("Phone Number must be of 10-digits only");
            return false;
        }
        else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Enter your Name");
            return false;
        }
        else if (binding.inputPassword.getText().toString().trim().isEmpty())
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
        else if(!isPinValid()){
            showToast("Enter valid PIN");
            return false;
        }
        else if(binding.inputConfirmPin.getText().toString().trim().isEmpty()){
            showToast("Confirm your PIN");
            return false;
        }
        else if (!binding.inputPin.getText().toString().equals(binding.inputConfirmPin.getText().toString()))
        {
            showToast("PIN and Confirm PIN must be same");
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
    private boolean isPinValid(){
        boolean flag=false;
        String regex = "[0-9]+";
        Pattern p=Pattern.compile(regex);
        Matcher m=p.matcher(binding.inputPin.getText().toString());
        if (binding.inputPin.getText().toString().length()==4){
            flag=true;
        }
        return (m.matches() && flag);
    }
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.signUp.setVisibility(View.INVISIBLE);
            binding.progressBar2.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar2.setVisibility(View.INVISIBLE);
            binding.signUp.setVisibility(View.VISIBLE);
        }
    }

    private void checkUser(){
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE_NUMBER, binding.inputNumber.getText().toString())
                .get()
                .addOnCompleteListener(task->{
                    if (task.isSuccessful() && task.getResult()!=null && task.getResult().size()>0){
                        showToast("You are already Registered");
                        Intent intent=new Intent(getApplicationContext(), SignInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
    }
}