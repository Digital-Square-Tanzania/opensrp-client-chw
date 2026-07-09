package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import org.smartregister.chw.anc.activity.BaseAncHomeVisitActivity;

/**
 * Activity for NCD (Diabetes & Hypertension) Home Visit workflow
 */
public class NcdHomeVisitActivity extends BaseAncHomeVisitActivity {

    public static final String BASE_ENTITY_ID = "base_entity_id";

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, NcdHomeVisitActivity.class);
        intent.putExtra(BASE_ENTITY_ID, baseEntityID);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Set up the workflow UI and logic for diabetes and hypertension screening
    }
}
