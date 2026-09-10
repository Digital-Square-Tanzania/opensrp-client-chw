package org.smartregister.chw.provider

import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.TextView
import org.smartregister.chw.R
import org.smartregister.chw.referral.fragment.BaseReferralRegisterFragment
import org.smartregister.chw.referral.provider.ReferralRegisterProvider
import org.smartregister.chw.referral.util.Constants
import org.smartregister.commonregistry.CommonPersonObjectClient
import org.smartregister.util.Utils
import org.smartregister.configurableviews.model.View as ConfigurableView
import org.smartregister.view.contract.SmartRegisterClient

/**
 * Referral register provider for the NACP flavour, used by `ReferralRegisterFragment`.
 *
 * It exists to re-attach the manual referral follow-up action to each register row. The
 * chw-referral library gates that action behind its own `ENABLE_REFERRAL_FOLLOWUP` BuildConfig
 * field, which stays off, so [ReferralRegisterProvider.getView] always leaves the follow-up
 * wrapper INVISIBLE. The follow-up is owned by the app, so the wrapper is bound here instead.
 *
 * @param followUpActionEnabled whether the follow-up action is offered at all. Supplied by
 * `ReferralRegisterFragment.isFollowUpActionEnabled()`, which reads the app's own
 * `BuildConfig.ENABLE_REFERRAL_FOLLOWUP` and can be overridden to false by registers that reuse
 * that fragment without a follow-up form. When false the wrapper is hidden outright.
 */
class NacpReferralRegisterProvider @JvmOverloads constructor(
    context: Context,
    paginationClickListener: View.OnClickListener,
    private val registerActionHandler: View.OnClickListener,
    visibleColumns: Set<ConfigurableView>,
    private val followUpActionEnabled: Boolean = true
) : ReferralRegisterProvider(context, paginationClickListener, registerActionHandler, visibleColumns) {

    override fun getView(cursor: Cursor, smartRegisterClient: SmartRegisterClient, registerViewHolder: RegisterViewHolder) {
        super.getView(cursor, smartRegisterClient, registerViewHolder)
        registerViewHolder.dueWrapper.visibility = View.VISIBLE

        val client = smartRegisterClient as? CommonPersonObjectClient
        val taskId = client?.let { Utils.getValue(it.columnmaps, Constants.Task.Key.TASK_ID, false) }

        // chw-referral keeps its own ENABLE_REFERRAL_FOLLOWUP flag switched off, so the wrapper always
        // comes back INVISIBLE from super.getView(). The manual referral follow-up is owned by the app,
        // so the action is re-attached here instead of through the library flag.
        if (!followUpActionEnabled || client == null || taskId.isNullOrBlank()) {
            registerViewHolder.followUpWrapper.visibility = View.GONE
            return
        }

        registerViewHolder.followUpWrapper.apply {
            visibility = View.VISIBLE
            setOnClickListener(registerActionHandler)
            tag = client
            setTag(R.id.VIEW_ID, BaseReferralRegisterFragment.LINKAGE_FOLLOWUP)
            findViewById<TextView>(R.id.text_view_follow_up)?.setText(R.string.referral_follow_up)
        }
    }
}
