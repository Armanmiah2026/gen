package com.alinaj.paidgen6;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LoginActivity extends AppCompatActivity {

    EditText user,pass;
    Button login;
    private SharedPreferences sp;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        mAuth = FirebaseAuth.getInstance();
        sp = getSharedPreferences("CloudGen",Context.MODE_PRIVATE);
        user = (EditText) findViewById(R.id.login_username);
        pass = (EditText) findViewById(R.id.login_password);
        login = (Button) findViewById(R.id.loginBtn);
        String u = sp.getString("login_user","");
        String p = sp.getString("login_pass","");
        user.setText(u);
        pass.setText(p);
        boolean isLogin = sp.getBoolean("isLogin",false);
        if(isLogin){
            String email = user.getText().toString();
            String password = pass.getText().toString();
            if(!email.isEmpty() || !password.isEmpty()){
                checkAuth(email,password);
             /*   ProgressDialog pd  = new ProgressDialog(LoginActivity.this);
                pd.setIndeterminate(true);
                pd.setMessage("Authenticating...");
                pd.show();
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(pd != null){
                                    pd.cancel();
                                }
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    sp.edit().putString("login_user",email).apply();
                                    sp.edit().putString("login_pass",password).apply();
                                    sp.edit().putBoolean("isLogin",true).apply();
                                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this,MainActivity.class));
                                    finish();
                                } else {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

              */
            }else{
                Toast.makeText(LoginActivity.this, "Email and Password is required!", Toast.LENGTH_SHORT).show();
            }
        }
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = user.getText().toString();
                String password = pass.getText().toString();
                if(!email.isEmpty() || !password.isEmpty()){

                    checkAuth(email,password);
                  /*  ProgressDialog pd  = new ProgressDialog(LoginActivity.this);
                    pd.setIndeterminate(true);
                    pd.setMessage("Authenticating...");
                    pd.show();
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(pd != null){
                                        pd.cancel();
                                    }
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "signInWithEmail:success");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        sp.edit().putString("login_user",email).apply();
                                        sp.edit().putString("login_pass",password).apply();
                                        sp.edit().putBoolean("isLogin",true).apply();
                                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this,MainActivity.class));
                                        finish();
                                    } else {
                                        FirebaseUser user = mAuth.getCurrentUser();

                                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                */
                }else{
                    Toast.makeText(LoginActivity.this, "Email and Password is required!", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    public void checkAuth(String user,String pass){
        ProgressDialog pd  = new ProgressDialog(LoginActivity.this);
        pd.setIndeterminate(true);
        pd.setMessage("Authenticating...");
        pd.show();
        String url = "https://tunnel.mtkapi.site/api/files/app?json=abe644f3e65168144adb";
        RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
        StringRequest req = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(pd != null){
                            pd.cancel();
                        }
                        try {
                            JSONObject jo = new JSONObject(response);
                            if(jo.has(user)){
                                JSONArray ja = jo.getJSONArray(user);
                                JSONObject jao = ja.getJSONObject(0);
                                if(pass.equals(jao.getString("password"))){
                                    if(jao.getString("status").equals("online")){
                                        sp.edit().putString("login_user",user).apply();
                                        sp.edit().putString("login_pass",pass).apply();
                                        sp.edit().putString("login_domain",jao.getString("domain")).apply();
                                        sp.edit().putString("login_hash",jao.getString("hash")).apply();
                                        if(jao.has("app_name")){
                                            sp.edit().putString("login_appname",jao.getString("app_name")).apply();
                                        }else{
                                           sp.edit().putString("login_appname",user).apply();
                                        }

                                        sp.edit().putBoolean("isLogin",true).apply();
                                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this,MainActivity.class));
                                        finish();
                                    }else{
                                        Toast.makeText(LoginActivity.this, "Unabled to login. Please contact your vpn provider.", Toast.LENGTH_SHORT).show();
                                    }
                                }else{
                                    Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                        requestQueue.getCache().clear();
                    }
                },   new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(pd != null){
                    pd.cancel();
                }
                requestQueue.getCache().clear();
            }

        });

        requestQueue.add(req);
    }
}
