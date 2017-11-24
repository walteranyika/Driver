package io.clone.dereva;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import io.clone.dereva.common.Common;
import io.clone.dereva.models.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LOGIN_ACTIVITY";
    RelativeLayout mRootLayout;
    Button btnSignIn, btnRegister;
    FirebaseAuth mFirebaseAuth;
    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mUsersRef;
    SpotsDialog progress_dialog;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath).build());

        mFirebaseAuth = FirebaseAuth.getInstance();
        if (mFirebaseAuth.getCurrentUser()!=null){
            Log.d(TAG, "User_ID: "+mFirebaseAuth.getCurrentUser().getUid());
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }
        setContentView(R.layout.activity_main);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersRef = mFirebaseDatabase.getReference(Common.USER_DRIVERS_TABLE);


        mRootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
        btnSignIn = (Button) findViewById(R.id.sign_in_btn);
        btnRegister = (Button) findViewById(R.id.register_btn);
        progress_dialog = new SpotsDialog(this);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });
    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Driver Login");
        dialog.setMessage("Please use your official email address");
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View login_layout = layoutInflater.inflate(R.layout.layout_login, null);

        final MaterialEditText inputEmail = (MaterialEditText) login_layout.findViewById(R.id.inputEmail);
        final MaterialEditText inputPassword = (MaterialEditText) login_layout.findViewById(R.id.inputPassword);
        dialog.setView(login_layout);

        dialog.setPositiveButton("SIGN IN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                validate(inputEmail);
                validate(inputPassword);
                final String email = inputEmail.getText().toString().trim();
                final String password = inputPassword.getText().toString().trim();
                progress_dialog.show();
                mFirebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Snackbar.make(mRootLayout, "Successfully logged in", Snackbar.LENGTH_LONG).show();
                                Log.d(TAG, "onSuccess: Logged in successfully");
                                progress_dialog.dismiss();
                                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onSuccess: Logged in successfully " + e.getMessage());
                        progress_dialog.dismiss();
                        Snackbar.make(mRootLayout, "Wrong username or password", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Driver Registration");
        dialog.setMessage("Please use your official email address");
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View register_layout = layoutInflater.inflate(R.layout.layout_register, null);

        final MaterialEditText inputEmail = (MaterialEditText) register_layout.findViewById(R.id.inputEmail);
        final MaterialEditText inputPassword = (MaterialEditText) register_layout.findViewById(R.id.inputPassword);
        final MaterialEditText inputPhone = (MaterialEditText) register_layout.findViewById(R.id.inputPhone);
        final MaterialEditText inputNames = (MaterialEditText) register_layout.findViewById(R.id.inputNames);

        dialog.setView(register_layout);

        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                validate(inputEmail);
                validate(inputPassword);
                validate(inputNames);
                validate(inputPhone);
                validatePassword(inputPassword.getText().toString().trim());
                validatePhone(inputPhone.getText().toString().trim());
                final String email = inputEmail.getText().toString().trim();
                final String password = inputPassword.getText().toString().trim();
                final String names = inputNames.getText().toString().trim();
                final String phone = inputPhone.getText().toString().trim();
                progress_dialog.show();
                mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                Log.d(TAG, "onSuccess: Successfully Registered User in Firebase");
                                User user = new User(names, email, password, phone);
                                String user_id = mFirebaseAuth.getCurrentUser().getUid();
                                mUsersRef.child(user_id).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(mRootLayout, "Registration successful", Snackbar.LENGTH_LONG).show();
                                        progress_dialog.dismiss();
                                        Log.d(TAG, "onSuccess: Added user to firebase database");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progress_dialog.dismiss();
                                        Log.d(TAG, "onSuccess: Could not add user to firebase database " + e.getMessage());
                                        Snackbar.make(mRootLayout, "Failed to register" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onSuccess: Could not register the user to firebase database " + e.getMessage());
                        Snackbar.make(mRootLayout, "Failed to sign up. Please try again", Snackbar.LENGTH_LONG).show();
                        progress_dialog.dismiss();

                    }
                });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void validate(MaterialEditText input) {
        if (input.getText().toString().isEmpty()) {
            Snackbar.make(mRootLayout, "Fill in all the fields", Snackbar.LENGTH_LONG).show();
            return;
        }
    }

    private void validatePhone(String phoneNumber) {
        if (phoneNumber.trim().length() < 8) {
            Snackbar.make(mRootLayout, "Invalid Phone Number", Snackbar.LENGTH_LONG).show();
            return;
        }

    }

    private void validatePassword(String password) {
        if (password.trim().length() < 6) {
            Snackbar.make(mRootLayout, "Your password is too short", Snackbar.LENGTH_LONG).show();
            return;
        }
    }
}
