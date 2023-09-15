package org.smartregister.chw.activity;

import android.view.View;
import android.widget.ImageView;

import androidx.fragment.app.FragmentTransaction;

import org.smartregister.chw.R;
import org.smartregister.chw.fragment.AllMaleClientsRegisterFragment;
import org.smartregister.view.activity.SecuredActivity;

public class AllMaleClientsActivity extends SecuredActivity implements View.OnClickListener {
    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_all_male_clients);
        setupViews();
    }

    @Override
    protected void onResumption() {
        //overridden
    }

    public void setupViews() {
        ImageView closeImageView = findViewById(R.id.close);
        closeImageView.setOnClickListener(this);

        loadFragment();
    }

    private void loadFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.fragment_placeholder, new AllMaleClientsRegisterFragment());
        ft.commit();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close) {
            finish();
        }
    }
}