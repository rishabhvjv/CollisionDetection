package com.techmantra.collisiondetection.Activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.techmantra.collisiondetection.Models.User;
import com.techmantra.collisiondetection.R;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser mUser;

    private EditText signUpEmail;
    private EditText displayName;
    private EditText signUpPassword1;
    private EditText signUpPassword2;
    private Button signUpBtn;
    private ProgressBar spinner ;

    private FirebaseDatabase mDB;
    private DatabaseReference mDBRef;

    public String userName ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        signUpEmail = (EditText) findViewById(R.id.emailSignUpField);
        displayName = (EditText) findViewById(R.id.displayNameFieldId);
        signUpPassword1 = (EditText) findViewById(R.id.passwordSignUpField1Id);
        signUpPassword2 = (EditText) findViewById(R.id.passwordSignUpField2Id);
        signUpBtn = (Button) findViewById(R.id.signUpBtnId);
        spinner = (ProgressBar) findViewById(R.id.progressSpinnerSignup);
        spinner.setVisibility(spinner.GONE);

        userName = displayName.getText().toString();
        mAuth = FirebaseAuth.getInstance();

        //handling sign up of a new user through firebase
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(signUpEmail.getText().toString()) && !TextUtils.isEmpty(signUpPassword1.getText().toString()) && !TextUtils.isEmpty(signUpPassword2.getText().toString())
                        && !TextUtils.isEmpty(displayName.getText().toString())){
                    String email = signUpEmail.getText().toString();
                    String password1 = signUpPassword1.getText().toString();
                    String password2 = signUpPassword2.getText().toString();
                    if(password1.equals(password2)){
                        signupNewUser(email , password1);
                        spinner.setVisibility(spinner.VISIBLE);

                    }
                    else {
                        Toast.makeText(SignUpActivity.this, "Passwords do not match , try again ", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(SignUpActivity.this,"please fill all the fields ",Toast.LENGTH_SHORT).show();
                }
            }

            //a function to handle sign up data on firebase and redirecting user to our MapsNavigation activity
            private void signupNewUser(String email, String password) {
                mAuth.createUserWithEmailAndPassword(email , password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            spinner.setVisibility(spinner.GONE);
                            Toast.makeText(SignUpActivity.this, "sign up failed , try again",Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_LONG).show();

                            mUser = mAuth.getCurrentUser();
                            mDB = FirebaseDatabase.getInstance();
                            mDBRef = mDB.getReference().child("UserDetails");
                            mDBRef.keepSynced(true);
                            User currUser = new User(mUser.getUid(),displayName.getText().toString(), mUser.getEmail());
                            mDBRef.child(mUser.getUid()).setValue(currUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                }
                            });
                            spinner.setVisibility(spinner.GONE);
                            startActivity(new Intent(SignUpActivity.this, MapsActivity.class));

                            }
                        }
                });
            }
        });

    }
}
