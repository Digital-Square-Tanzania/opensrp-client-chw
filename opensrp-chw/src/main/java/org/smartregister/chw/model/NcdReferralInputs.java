package org.smartregister.chw.model;

import androidx.annotation.Nullable;

/**
 * Immutable holder for the emergency-case and treatment-supporter values captured on the
 * post-visit NCD referral prompt ({@code NcdReferralPromptDialog}) and carried into
 * {@code NcdReferralTaskHelper.createReferralIfNeeded(...)} so they can be emitted as obs on
 * the auto-generated Referral Registration event.
 *
 * <p>The supporter values are edited <em>for this referral only</em>; they are never written
 * back to the client's registration record. {@link #hasTreatmentSupporter} acts as the gate:
 * when it is not {@code "Yes"}, the name/phone/relationship fields are ignored by the event
 * builder.
 *
 * <p>Values use the same {@code "Yes"}/{@code "No"} option keys as the spinners on
 * {@code ncd_referral_form.json}.
 */
public class NcdReferralInputs {

    private final String isEmergencyCase;
    private final String hasTreatmentSupporter;
    private final String supporterName;
    private final String supporterPhone;
    private final String supporterRelationship;
    private final String referralFacilityId;
    private final String referralFacilityName;

    public NcdReferralInputs(@Nullable String isEmergencyCase,
                             @Nullable String hasTreatmentSupporter,
                             @Nullable String supporterName,
                             @Nullable String supporterPhone,
                             @Nullable String supporterRelationship) {
        this(isEmergencyCase, hasTreatmentSupporter, supporterName, supporterPhone,
                supporterRelationship, null, null);
    }

    public NcdReferralInputs(@Nullable String isEmergencyCase,
                             @Nullable String hasTreatmentSupporter,
                             @Nullable String supporterName,
                             @Nullable String supporterPhone,
                             @Nullable String supporterRelationship,
                             @Nullable String referralFacilityId,
                             @Nullable String referralFacilityName) {
        this.isEmergencyCase = isEmergencyCase;
        this.hasTreatmentSupporter = hasTreatmentSupporter;
        this.supporterName = supporterName;
        this.supporterPhone = supporterPhone;
        this.supporterRelationship = supporterRelationship;
        this.referralFacilityId = referralFacilityId;
        this.referralFacilityName = referralFacilityName;
    }

    /** @return {@code "Yes"} / {@code "No"}, or {@code null} if not captured. */
    @Nullable
    public String getIsEmergencyCase() {
        return isEmergencyCase;
    }

    /** @return the gate value {@code "Yes"} / {@code "No"}, or {@code null} if not captured. */
    @Nullable
    public String getHasTreatmentSupporter() {
        return hasTreatmentSupporter;
    }

    @Nullable
    public String getSupporterName() {
        return supporterName;
    }

    @Nullable
    public String getSupporterPhone() {
        return supporterPhone;
    }

    @Nullable
    public String getSupporterRelationship() {
        return supporterRelationship;
    }

    /** @return whether the treatment-supporter gate is "Yes" (case-insensitive). */
    public boolean isTreatmentSupporterGateYes() {
        return "Yes".equalsIgnoreCase(hasTreatmentSupporter);
    }

    /** @return the selected referral facility's location id, or {@code null} if none was chosen. */
    @Nullable
    public String getReferralFacilityId() {
        return referralFacilityId;
    }

    /**
     * @return the selected referral facility's display name, or {@code null} if none was chosen.
     */
    @Nullable
    public String getReferralFacilityName() {
        return referralFacilityName;
    }
}
