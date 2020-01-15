/*
 *
 *  * Copyright (C) 2017 Safaricom, Ltd.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.wanjy.dannie.dropper.MPESA;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bdhobare.mpesa.interfaces.MpesaListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.wanjy.dannie.dropper.CustomerMapActivity;
import com.wanjy.dannie.dropper.MPESA.rest_api.ApiClient;
import com.wanjy.dannie.dropper.MPESA.rest_api.model.AccessToken;
import com.wanjy.dannie.dropper.MPESA.rest_api.model.STKPush;
import com.wanjy.dannie.dropper.MPESA.utils.AppConstants;
import com.wanjy.dannie.dropper.MPESA.utils.NotificationUtils;
import com.wanjy.dannie.dropper.MPESA.utils.SharedPrefsUtil;
import com.wanjy.dannie.dropper.MPESA.utils.Utils;
import com.wanjy.dannie.dropper.MainActivity;
import com.wanjy.dannie.dropper.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.BUSINESS_SHORT_CODE;
import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.PARTYB;
import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.PASSKEY;
import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.PUSH_NOTIFICATION;
import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.REGISTRATION_COMPLETE;
import static com.wanjy.dannie.dropper.MPESA.utils.AppConstants.TRANSACTION_TYPE;
import static com.wanjy.dannie.dropper.app.Config.TOPIC_GLOBAL;

public class GateWay extends AppCompatActivity implements MpesaListener {

    private String regId;

    private String mFireBaseRegId;
    private FirebaseAuth mAuth;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressDialog mProgressDialog, mProgress;
    private SharedPrefsUtil mSharedPrefsUtil;
    private ApiClient mApiClient;

    private boolean Execute;


    Button pay, send, btn_back;
    ProgressDialog dialog;
    EditText phone, mpesa_code;
    ConstraintLayout constraintLayout, constraintLayout2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpesaexpress);
        Execute = false;
        phone = findViewById(R.id.editTextPhoneNumber);
        pay = findViewById(R.id.sendButton);
        send = findViewById(R.id.send);
        mpesa_code = findViewById(R.id.mpesa_code);
        btn_back = findViewById(R.id.btn_back);
        constraintLayout = findViewById(R.id.view1);

        constraintLayout2 = findViewById(R.id.view2);
        mProgress = new ProgressDialog(GateWay.this);
        mProgress.setMessage("Please wait");
        mProgress.setCancelable(true);

        mProgressDialog = new ProgressDialog(this);
        mSharedPrefsUtil = new SharedPrefsUtil(this);
        mApiClient = new ApiClient();
        mApiClient.setIsDebug(true); //Set True to enable logging, false to disable.

        getAccessToken();
        getFirebaseRegId();


        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(REGISTRATION_COMPLETE)) {
                    // fcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_GLOBAL);
                    getFirebaseRegId();

                } else if (intent.getAction().equals(PUSH_NOTIFICATION)) {
                    String title = intent.getStringExtra("title");
                    String message = intent.getStringExtra("message");

                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();

                    showResultDialog();
                }
            }
        };

        btn_back.setOnClickListener(view -> {
            constraintLayout.setVisibility(View.VISIBLE);
            constraintLayout2.setVisibility(View.GONE);

        });

        pay.setOnClickListener(view -> {
            String p = phone.getText().toString();

            if (p.isEmpty()) {
                phone.setError("Enter phone.");
                return;
            }

            phone.setText("");
            performSTKPush(p);
        });

        send.setOnClickListener(view -> {
            String transaction = mpesa_code.getText().toString();
            int length = mpesa_code.getText().length();
            if (transaction.isEmpty()) {
                mpesa_code.setError("Transaction code Require");
            } else if (length < 10 || length > 10) {
                mpesa_code.setError("Code not Valid");
            }
            else {
                enterTransaction(transaction);
            }
            });
    }

    private void enterTransaction(String transaction) {
        mProgress.show();

        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("Tranasactions");

        Query dbRefFirstTimeCheck = db.orderByChild("TransactionCode").equalTo(transaction);

        dbRefFirstTimeCheck.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Map codeMap = new HashMap();
                    codeMap.put("TransactionCode", transaction);
                    codeMap.put("dateTransaction", getDate());
                    db.push().setValue(codeMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mpesa_code.setText("");
                            Intent intent = new Intent(GateWay.this, CustomerMapActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(GateWay.this, "Payment Successful", Toast.LENGTH_SHORT).show();
                            mProgress.dismiss();
//                            startActivity(new Intent(GateWay.this, StartConsultation.class));
                        } else {

                            Toast.makeText(GateWay.this, "Error! Please check your connection.", Toast.LENGTH_LONG).show();
                        }

                    });
                }else {
                    dbRefFirstTimeCheck.removeEventListener(this);
                    erroe();
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                throw databaseError.toException();
            }
    });
    }

    void erroe(){
        mProgress.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(GateWay.this);
        // Set the Alert Dialog Message
        builder.setMessage(Html.fromHtml("<font color = '#e20719'>Invalid code try again!!</font>"));
        builder.setCancelable(false);
        builder.setPositiveButton(Html.fromHtml("<font color = '#118626'>OK</font>"),
                (dialog, id) -> {
                    // Restart the Activity
                    dialog.dismiss();
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public String getDate() {
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("dd-MM-yyyy");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);//Menu Resource, Menu
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);


    }


    @Override
    protected void onResume() {
        super.onResume();
        if (Execute) {
            constraintLayout.setVisibility(View.GONE);
            constraintLayout2.setVisibility(View.VISIBLE);
        } else {
            Execute = true;
        }

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(REGISTRATION_COMPLETE));
        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
    }

    public void getAccessToken() {

        SharedPreferences pref = getApplicationContext().getSharedPreferences(AppConstants.SHARED_PREF, 0);
        regId = pref.getString("regId", null);
        mApiClient.setGetAccessToken(true);
        mApiClient.mpesaService().getAccessToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(@NonNull Call<AccessToken> call, @NonNull Response<AccessToken> response) {

                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String token = response.body().accessToken;
                    mApiClient.setAuthToken(response.body().accessToken);

                }
            }

            @Override
            public void onFailure(@NonNull Call<AccessToken> call, @NonNull Throwable t) {

            }
        });
    }

    public void performSTKPush(String p) {
        mProgressDialog.setMessage("Processing....please wait");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
        String timestamp = Utils.getTimestamp();


        STKPush stkPush = new STKPush(
                BUSINESS_SHORT_CODE,
                Utils.getPassword(BUSINESS_SHORT_CODE, PASSKEY, timestamp),
                timestamp,
                TRANSACTION_TYPE,
                "1000",
                Utils.sanitizePhoneNumber(p),
                PARTYB,
                Utils.sanitizePhoneNumber(p),
                "http://amazontronics.co.ke/firebase/index.php?title=bayo&message=result&push_type=individual&regId=" + mFireBaseRegId,
                "test", //The account reference
                "test"  //The transaction description
        );


        mApiClient.setGetAccessToken(false);

        mApiClient.mpesaService().sendPush(stkPush).enqueue(new Callback<STKPush>() {
            @Override
            public void onResponse(@NonNull Call<STKPush> call, @NonNull Response<STKPush> response) {
                mProgressDialog.dismiss();
                try {
                    if (response.isSuccessful()) {
                        Toast.makeText(GateWay.this, "Send, processing...", Toast.LENGTH_SHORT).show();
                        Timber.d("post submitted to API. %s", response.body());
                    } else {
                        Timber.e("Response %s", response.errorBody().string());
                        Toast.makeText(GateWay.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<STKPush> call, @NonNull Throwable t) {
                mProgressDialog.dismiss();
                Toast.makeText(GateWay.this, "Check Your Internet!", Toast.LENGTH_SHORT).show();
                Timber.e(t);
            }
        });
    }


    private void getFirebaseRegId() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences(AppConstants.SHARED_PREF, 0);
        mFireBaseRegId = pref.getString("regId", null);
//        mFireBaseRegId = mSharedPrefsUtil.getFirebaseRegistrationID();

        if (!TextUtils.isEmpty(mFireBaseRegId)) {
            mSharedPrefsUtil.saveFirebaseRegistrationID(mFireBaseRegId);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    //
    public void showResultDialog() {
//        MaterialDialog dialog = new MaterialDialog.Builder(this)
//                .title("Successful")
//                .titleGravity(CENTER)
//                .customView(R.layout.success_dialog, true)
//                .positiveText("OK")
//                .cancelable(true)
//                .widgetColorRes(R.color.colorPrimary)
//                .callback(new MaterialDialog.ButtonCallback() {
//                    @Override
//                    public void onPositive(MaterialDialog dialog) {
//                        super.onPositive(dialog);
//                     dialog.dismiss();
//                    }
//                })
//                .build();
//        View view = dialog.getCustomView();
//        TextView messageText = (TextView) view.findViewById(R.id.message);
//        ImageView imageView = (ImageView) view.findViewById(R.id.success);
//        dialog.show();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        startActivity(new Intent(GateWay.this, MainActivity.class));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
//            startActivity(new Intent(GateWay.this, MainActivity.class));

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMpesaError(com.bdhobare.mpesa.utils.Pair<Integer, String> result) {
        dialog.hide();
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMpesaSuccess(String MerchantRequestID, String CheckoutRequestID, String CustomerMessage) {
        dialog.hide();
        Toast.makeText(this, CustomerMessage, Toast.LENGTH_SHORT).show();
    }


}
