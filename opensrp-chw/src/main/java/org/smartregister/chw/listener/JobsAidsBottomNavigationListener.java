package org.smartregister.chw.listener;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import org.smartregister.AllConstants;
import org.smartregister.chw.activity.FamilyRegisterActivity;
import org.smartregister.family.R;
import org.smartregister.listener.BottomNavigationListener;
import org.smartregister.util.PermissionUtils;
import org.smartregister.util.Utils;
import org.smartregister.view.activity.BarcodeScanActivity;

import timber.log.Timber;

public class JobsAidsBottomNavigationListener extends BottomNavigationListener {
    private Activity context;

    public JobsAidsBottomNavigationListener(Activity context) {
        super(context);
        this.context = context;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//        super.onNavigationItemSelected(item);

        if (item.getItemId() == R.id.action_scan_qr) {

            if (PermissionUtils.isPermissionGranted(context, Manifest.permission.CAMERA, PermissionUtils.CAMERA_PERMISSION_REQUEST_CODE)) {
                try {
                    Intent intent = new Intent(context, BarcodeScanActivity.class);
                    context.startActivityForResult(intent, AllConstants.BARCODE.BARCODE_REQUEST_CODE);
                } catch (SecurityException e) {
                    Utils.showToast(context, context.getString(org.smartregister.R.string.allow_camera_management));
                }
            }

        } else if (item.getItemId() == R.id.action_register) {

            try {
                ((FamilyRegisterActivity) context).startRegistration();
            } catch (Exception e) {
                Timber.e(e);
            }

        }

        context.finish();
        return true;
    }
}
