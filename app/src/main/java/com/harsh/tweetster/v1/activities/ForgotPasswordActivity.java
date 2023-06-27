package com.harsh.tweetster.v1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.harsh.tweetster.databinding.ActivityForgotPasswordBinding;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity{
    private ActivityForgotPasswordBinding binding;
    private String verificationId;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth= FirebaseAuth.getInstance();
        setListeners();
    }
    private void setListeners(){
        binding.textSignIn.setOnClickListener(v->onBackPressed());
        binding.getOTP.setOnClickListener(v-> {
        if(binding.inputNumber.getText().toString().trim().isEmpty()){
            showToast("Enter Phone Number");
        }
        else if(!Patterns.PHONE.matcher(binding.inputNumber.getText().toString()).matches()){
            showToast("Enter valid phone number");
        }
        else if(binding.inputNumber.getText().toString().length()!=10){
            showToast("Phone Number must be of 10-digits only");
        }
        else{
            sendVerificationCode("+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString());
        }});
        binding.verifyOTP.setOnClickListener(v->
        {
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
        loading(true);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if(task.isSuccessful()){
                        loading(false);
                        Intent intent=new Intent(getApplicationContext(), ChangePasswordActivity.class);
                        intent.putExtra("phone", "+"+binding.countryCode.getSelectedCountryCode()+binding.inputNumber.getText().toString());
                        startActivity(intent);
                    }
                    else{
                        loading(false);
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
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.verifyOTP.setVisibility(View.INVISIBLE);
            binding.progressBar2.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar2.setVisibility(View.INVISIBLE);
            binding.verifyOTP.setVisibility(View.VISIBLE);
        }
    }
}
