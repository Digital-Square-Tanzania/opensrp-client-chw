package org.smartregister.chw.domain;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;

import java.util.Date;

public class KvpReportObject extends ReportObject {


    private final String[] kvpQuestionsGroups = new String[]{"1","2","3","4","5","6","7","8","9","10","11"};
    private final String[] kvpAgeGroups = new String[]{
            "<10","10-14","15-19","20-24","25-29","30-34","35-39","40-44","45-49",">50"
    };
    private final String[] kvpGroups = new String[]{
            "pwid","pwud","fsw","msm","agyw","other_vulnerable_population"
    };
    private final String[] kvpGenderGroups = new String[]{
            "ME","KE"
    };

    private final Date reportDate;
    private JSONObject jsonObject ;

    public KvpReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        for (String questionGroup : kvpQuestionsGroups) {   //rows
            for (String ageGroup : kvpAgeGroups) {  //columns
                for (String kvpGroup : kvpGroups) {
                    for (String genderGroup : kvpGenderGroups) {
                        jsonObject.put("kvp" + "-" + questionGroup + "-" + ageGroup + "-" + kvpGroup + "-" + genderGroup,
                                ReportDao.getReportPerIndicatorCode("kvp" + "-" + questionGroup + "-" + ageGroup +"-" + kvpGroup + "-" + genderGroup, reportDate));
                    }
                }
            }
        }
        // get total of all Male & Female in Qn 2 & 7
        //and the whole total for both of them
        funcGetTotal();

        return jsonObject;
    }

    private int getTotalPerEachIndicator(String question,String kvpgroup) throws JSONException {
        int  totalOfGenderGiven = 0;
        int returnedValue = 0;
        for (String age: kvpAgeGroups){
                totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode("kvp" + "-"
                        + question + "-" + age + "-" + kvpgroup + "-" + "ME", reportDate)
                +ReportDao.getReportPerIndicatorCode("kvp" + "-"
                        + question + "-" + age + "-" + kvpgroup + "-" + "KE", reportDate));
            jsonObject.put("kvp"+"-"+question+"-"+kvpgroup+"-jumla-both-ME-KE",totalOfGenderGiven);  //display the total for both gender
            returnedValue = totalOfGenderGiven;
        }
        return returnedValue;
    }


    private void funcGetTotal() throws JSONException {
        int totalofthewholekvpgroup = 0;
        for (String question: kvpQuestionsGroups) {
                for (String kvpGroup : kvpGroups) {
                    totalofthewholekvpgroup+=getTotalPerEachIndicator(question,kvpGroup);
                    jsonObject.put("kvp"+"-"+question+"-jumla-kuu",totalofthewholekvpgroup); //total for all kvp groups
                }
            totalofthewholekvpgroup = 0;
        }
    }

}
