package com.example.android.wouritv.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.example.android.wouritv.R;
import com.example.android.wouritv.config.HttpQuery;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SettingActivity1 extends Activity {

    private ProgressDialog pd;
    private static final String PREFS = "PREFS";
    private static final String PREFS_USER = "PREFS_USER";
    private static final String PREFS_NAME = "PREFS_NAME";
    private static final String PREFS_PINCODE = "PIN_CODE";
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sharedPreferences = getBaseContext().getSharedPreferences(PREFS, MODE_PRIVATE);

        Button button = (Button)findViewById(R.id.bouton);
        Button button2 = (Button)findViewById(R.id.bouton2);

        if (sharedPreferences.contains(PREFS_USER) ) {

            String iduser2 = sharedPreferences.getString(PREFS_USER, "");

            if(isConnected()) new GetCode().execute("getcode",iduser2);

            TextView textView = (TextView)findViewById(R.id.text);
            EditText code = (EditText)findViewById(R.id.code);
            button2.setText("RETOUR A L'ACCUEIL");
            button.setText("FERMER MA SESSION"); code.setVisibility(View.GONE);

            String iduser = sharedPreferences.getString(PREFS_NAME, "");

            textView.setText("Connecté en tant que "+iduser);
        }

        else button2.requestFocus();

    }

    public void CancelButton(View v){

        Intent intent = new Intent(SettingActivity1.this,MainActivity.class);

        startActivity(intent);
    }

    public void OnclickButton(View v){

        if (sharedPreferences.contains(PREFS_USER) ) {

            String iduser = sharedPreferences.getString(PREFS_USER, "");

            if (isConnected()) new BuildCode2().execute("closecode",iduser);
        }

        else {

            EditText code = (EditText) findViewById(R.id.code);

            String text = code.getText().toString();

            if (text.isEmpty())
                Toast.makeText(SettingActivity1.this, "Veuillez saisir un pin de connexion", Toast.LENGTH_LONG).show();

            else {

                String myDeviceModel = "Appareil : " + Build.MANUFACTURER + " \n Modele : " + Build.MODEL + " \n Android Version : " + android.os.Build.VERSION.RELEASE;

                sharedPreferences.edit().putString(PREFS_PINCODE, text).apply();

                if (isConnected()) new BuildCode().execute("updatecode", text, myDeviceModel);

            }

        }
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class BuildCode extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.getcode(args[0],args[1],args[2]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            if (pd!=null) { pd.dismiss(); }

            if(result != null) {

                Log.e("DATA",result);

                TextView  textView = (TextView)findViewById(R.id.text);
                Button button = (Button)findViewById(R.id.bouton);
                EditText code = (EditText)findViewById(R.id.code);

                try{

                    JSONObject jsonObject = new JSONObject(result);

                    if(jsonObject.getString("data").equals("nok1")) {

                        Toast.makeText(getApplicationContext(),"Le pin de connexion semble incorrect",Toast.LENGTH_LONG).show();
                    }

                    else  if(jsonObject.getString("data").equals("nok")) {

                        Toast.makeText(getApplicationContext(),"Une erreur est survenue lors du traiteemnt, veuillez recommencer",Toast.LENGTH_LONG).show();
                    }

                    else {

                        sharedPreferences.edit().putString(PREFS_USER, jsonObject.getString("iduser")).apply();

                        sharedPreferences.edit().putString(PREFS_NAME,jsonObject.getString("username") ).apply();

                        button.setText("FERMER MA SESSION"); code.setVisibility(View.GONE);

                        textView.setText("Connecte en tant que "+jsonObject.getString("username"));

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

            pd = new ProgressDialog(SettingActivity1.this);

            pd.setTitle("Traitement");

            pd.setMessage("Veuillez patienter");

            pd.setCancelable(false);

            pd.setIndeterminate(true);

            pd.show();
        }
    }

    private class BuildCode2 extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.getcode(args[0],args[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            if (pd!=null) { pd.dismiss(); }

            Log.e("DATA",result);

            try{

                JSONObject jsonObject = new JSONObject(result);

                if(jsonObject.getString("data").equals("ok")){

                    if (sharedPreferences.contains(PREFS_USER) ) {

                        sharedPreferences.edit().clear().commit();

                        Intent intent = new Intent(SettingActivity1.this,SettingActivity1.class);

                        startActivity(intent);
                    }

                }

                else {

                    Toast.makeText(getApplicationContext(),"Une erreur est survenue ",Toast.LENGTH_LONG).show();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

            pd = new ProgressDialog(SettingActivity1.this);

            pd.setTitle("Traitement");

            pd.setMessage("Veuillez patienter");

            pd.setCancelable(false);

            pd.setIndeterminate(true);

            pd.show();
        }
    }

    private class GetCode extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.getcode2(args[0],args[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            if (pd!=null) { pd.dismiss(); }

            if(result != null) {

                Log.e("DATA Serie","Le resultat de la recherche est : "+result);

                try {

                    JSONObject jsonObject = new JSONObject(result);

                    if(jsonObject.has("data")) {

                    }

                    else {

                        if(jsonObject.getString("status").equals("0")) {

                        }

                        else if(jsonObject.getString("status").equals("2")) {

                            sharedPreferences.edit().clear().commit();

                            Intent intent = new Intent(SettingActivity1.this,SettingActivity1.class);

                            startActivity(intent);
                        }

                        else {

                        }


                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

        }
    }
}

