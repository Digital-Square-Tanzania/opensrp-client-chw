package org.smartregister.chw.activity;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.smartregister.chw.tbleprosy.R;


public class TbLeprosyContactRegister extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tbleprosy_register_contact);


        findViewById(R.id.newClient);
        findViewById(R.id.existingClient);

    }


}