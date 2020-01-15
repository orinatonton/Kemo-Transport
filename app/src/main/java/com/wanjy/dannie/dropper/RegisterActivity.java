package com.wanjy.dannie.dropper;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.wanjy.dannie.dropper.R;

import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText mEmail, mPassword,mEditTextPhone, Firstname, SecondName,Username,PasswordAgain;
    private Button mRegistration;
    private RadioGroup mRadioGroup;
    private ProgressDialog mProgressDialog;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    private String mRole;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    if (mRole.equals("Express"))
                        intent = new Intent(RegisterActivity.this, CourierMapActivity.class);
                    else if(mRole.equals("Client"))
                        intent = new Intent(RegisterActivity.this, CustomerMapActivity.class);

                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        mEmail = (EditText) findViewById(R.id.email);

        mPassword = (EditText) findViewById(R.id.password);

        mEditTextPhone = (EditText) findViewById(R.id.login_edittext_phone);

        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);

        mRadioGroup.check(R.id.courier);


        mRegistration = (Button) findViewById(R.id.register);

        mProgressDialog = new ProgressDialog(this);

        Firstname = (EditText) findViewById(R.id.firstname);


        Username = (EditText) findViewById(R.id.username);

        PasswordAgain = (EditText) findViewById(R.id.conf_password);


        mRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                final String mPhone = mEditTextPhone.getText().toString().trim();

                final String firstname = Firstname.getText().toString();
                final String username = Username.getText().toString();
                final String PassAgain = PasswordAgain.getText().toString();

                String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

                int selectId = mRadioGroup.getCheckedRadioButtonId();
                final RadioButton radioButton = (RadioButton) findViewById(selectId);

                if(TextUtils.isEmpty(firstname)){
                    Firstname.setError("Firstname required");
                    Firstname.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Firstname required", Toast.LENGTH_SHORT).show();
                    return;
                }
//                if(TextUtils.isEmpty(secondname)){
//                    SecondName.setError("LastNmae required");
//                    SecondName.requestFocus();
//                    Toast.makeText(RegisterActivity.this, "Lastname required", Toast.LENGTH_SHORT).show();
//
//                    return;
//                }

                if(TextUtils.isEmpty(username)){
                    Username.setError("Username required");
                    Username.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Username required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(email)){
                    mEmail.setError("Email Required");
                    mEmail.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Email required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!email.matches(emailPattern)){
                    mEmail.setError("Enter valid Email ");
                    mEmail.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Invalid Email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(mPhone)){
                    mEditTextPhone.setError("EnterPhone");
                    mEditTextPhone.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Phone Number required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mPhone.length() <9||mPhone.length() ==9  ){
                    mEditTextPhone.setError("Phone Number Must be 10 digits");
                    mEditTextPhone.requestFocus();
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    mPassword.setError("Password required");
                    mPassword.requestFocus();
                    Toast.makeText(RegisterActivity.this, "Paasword required", Toast.LENGTH_SHORT).show();

                    return;
                }
                 if (password.length() < 6){
                    mPassword.setError("Password Require minimum of 6 characters");
                     mPassword.requestFocus();
                     Toast.makeText(RegisterActivity.this, "Password Require minimum of 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }


               if(password.length() != PassAgain.length()){
                    PasswordAgain.setError("Password Not Match");
                   PasswordAgain.requestFocus();
                   Toast.makeText(RegisterActivity.this, "Password Not Match", Toast.LENGTH_SHORT).show();
                    return;
                }


               if (radioButton.getText() == null){
                    return;
                }
                mProgressDialog.setMessage(getString(R.string.login_progressbar_register));
                mProgressDialog.show();

                mRole = radioButton.getText().toString();

                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(!task.isSuccessful()){
                            Toast.makeText(RegisterActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                            mProgressDialog.dismiss();
                        }else{
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("name");
                            DatabaseReference current_user_role = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("role");
                            DatabaseReference current_user_email = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("email");
                            DatabaseReference current_user_phone = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("phone");
                            DatabaseReference current_user_first = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("FirstName");
//                            DatabaseReference current_user_second = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id).child("second");


                            current_user_email.setValue(email);
                            current_user_phone.setValue(mPhone);
                            current_user_first.setValue(firstname);
//                            current_user_second.setValue(secondname);
                            current_user_db.setValue(username);
                            current_user_role.setValue(mRole);
                            mProgressDialog.dismiss();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
