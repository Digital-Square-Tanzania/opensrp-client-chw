package org.smartregister.chw.rules;

import android.content.Context;

import androidx.annotation.NonNull;

import com.vijay.jsonwizard.rules.RuleConstant;
import com.vijay.jsonwizard.rules.RulesEngineFactory;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

import timber.log.Timber;

public class ChwRulesEngineFactory extends RulesEngineFactory {
    private Map<String, String> globalValues;
    private ChwRulesEngineHelper chwRulesEngineHelper;
    private String selectedRuleName;
    private Rules diabetesRiskRules;
    private String RULE_FOLDER_PATH = "rule/";


    public ChwRulesEngineFactory(Context context, Map<String, String> globalValues) {
        super(context, globalValues);
        this.chwRulesEngineHelper = new ChwRulesEngineHelper(context);
        this.globalValues = globalValues;

    }

    @Override
    protected Facts initializeFacts(Facts facts) {
        if (globalValues != null) {
            for (Map.Entry<String, String> entry : globalValues.entrySet()) {
                facts.put(RuleConstant.PREFIX.GLOBAL + entry.getKey(), getValue(entry.getValue()));
            }
            facts.asMap().putAll(globalValues);
        }

        selectedRuleName = facts.get(RuleConstant.SELECTED_RULE);

        facts.put("helper", chwRulesEngineHelper);
        return facts;
    }

    @Override
    public boolean beforeEvaluate(Rule rule, Facts facts) {
        return selectedRuleName != null && selectedRuleName.equals(rule.getName());
    }

    @Override
    public String getCalculation(Facts calculationFact, String ruleFilename) {
        // Special handling for diabetes risk calculation rule (No need to format the calculation result)
        if (ruleFilename.equals("diabetes_hypertension_screening_calculation.yml")) {
            Facts facts = this.initializeFacts(calculationFact);
            facts.put("calculation", "");
            this.diabetesRiskRules = this.getRulesFromAsset(this.RULE_FOLDER_PATH + ruleFilename);
            this.processDefaultRules(this.diabetesRiskRules, facts);
            if (selectedRuleName != null && selectedRuleName.equals("step4_diabetes_risk_score_output"))
                return facts.get("calculation") != null ? facts.get("calculation").toString() : "0.0";
            return this.formatCalculationReturnValue(facts.get("calculation"));
        } else {
            return super.getCalculation(calculationFact, ruleFilename);
        }
    }

    private String formatCalculationReturnValue(Object rawValue) {
        String value = String.valueOf(rawValue).trim();
        if (value.isEmpty()) {
            return "";
        } else if (rawValue instanceof Map) {
            return (new JSONObject((Map)rawValue)).toString();
        } else {
            if (value.contains(".")) {
                try {
                    value = String.valueOf((float)Math.round(Float.valueOf(value) * 100.0F) / 100.0F);
                } catch (NumberFormatException e) {
                    Timber.e(e, "%s formatCalculationReturnValue", new Object[]{this.getClass().getCanonicalName()});
                }
            }

            return value;
        }
    }
}
