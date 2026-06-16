package org.smartregister.chw.provider

import android.content.Context
import android.database.Cursor
import android.view.View
import org.smartregister.chw.referral.provider.ReferralRegisterProvider
import org.smartregister.configurableviews.model.View as ConfigurableView
import org.smartregister.view.contract.SmartRegisterClient

class NacpReferralRegisterProvider(
    context: Context,
    paginationClickListener: View.OnClickListener,
    onClickListener: View.OnClickListener,
    visibleColumns: Set<ConfigurableView>
) : ReferralRegisterProvider(context, paginationClickListener, onClickListener, visibleColumns) {

    override fun getView(cursor: Cursor, smartRegisterClient: SmartRegisterClient, registerViewHolder: RegisterViewHolder) {
        super.getView(cursor, smartRegisterClient, registerViewHolder)
        registerViewHolder.followUpWrapper.visibility = View.GONE
        registerViewHolder.dueWrapper.visibility = View.VISIBLE
    }
}
