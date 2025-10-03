package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.ncd.activity.BaseNcdVisitActivity;
import org.smartregister.chw.ncd.util.Constants;
import org.smartregister.chw.presenter.NcdVisitPresenter;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class NcdVisitActivity extends BaseNcdVisitActivity {

    public static void startMe(Activity activity, String baseEntityId, Boolean isEditMode) {
        Intent intent = new Intent(activity, NcdVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.SKELETON_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void registerPresenter() {
        presenter = new NcdVisitPresenter(memberObject, this);
    }

    @Override
    public void setUpView() {
        super.setUpView();

        RecyclerView recyclerView = findViewById(org.smartregister.chw.ncd.R.id.recyclerView);
        if (recyclerView == null) {
            return;
        }

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                restyleSubtitle(view.findViewById(org.smartregister.chw.ncd.R.id.customFontTextViewDetails));
                restyleSubtitle(view.findViewById(org.smartregister.chw.ncd.R.id.customFontTextViewInvalid));
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // no-op
            }
        });

        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                applyStylesToAllChildren((RecyclerView) v));

        applyStylesToAllChildren(recyclerView);

        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter != null) {
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                private void refresh() {
                    applyStylesToAllChildren(recyclerView);
                }

                @Override
                public void onChanged() {
                    refresh();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    refresh();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    refresh();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    refresh();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    refresh();
                }
            });
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setActionBarBackground(R.color.family_actionbar);
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    private void applyStylesToAllChildren(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }

        for (int index = 0; index < recyclerView.getChildCount(); index++) {
            View child = recyclerView.getChildAt(index);
            if (child == null) {
                continue;
            }
            restyleSubtitle(child.findViewById(org.smartregister.chw.ncd.R.id.customFontTextViewDetails));
            restyleSubtitle(child.findViewById(org.smartregister.chw.ncd.R.id.customFontTextViewInvalid));
        }
    }

    private void restyleSubtitle(View view) {
        if (!(view instanceof TextView)) {
            return;
        }

        TextView textView = (TextView) view;
        textView.setIncludeFontPadding(false);
        textView.setPaintFlags(textView.getPaintFlags() | Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textView.setLetterSpacing(0f);
        }

        int urgentColor = ContextCompat.getColor(textView.getContext(), org.smartregister.R.color.alert_urgent_red);
        if (textView.getCurrentTextColor() == urgentColor) {
            textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.ncd_visit_subtitle_overdue));
        }
    }
}
