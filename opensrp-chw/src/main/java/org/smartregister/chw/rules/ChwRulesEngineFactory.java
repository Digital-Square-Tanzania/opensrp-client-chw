package org.smartregister.chw.rules;

import android.content.Context;

import com.vijay.jsonwizard.rules.RuleConstant;
import com.vijay.jsonwizard.rules.RulesEngineFactory;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;

import java.util.Map;

public class ChwRulesEngineFactory extends RulesEngineFactory {
    private Map<String, String> globalValues;
    private ChwRulesEngineHelper chwRulesEngineHelper;
    private String selectedRuleName;


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
}
