package com.synchronoss.androidDev.contactcreaterapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    protected static final String MAIN_ACTIVITY = "com.synchronoss.androidDev.contactcreaterapp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        importContactsOntoPhone();
        Log.d("CONTACT_HELPER", "IMPORTED");
    }

    public void importContactsOntoPhone() {
        CreateAndAddContacts.startActionImport(this);
    }

}
