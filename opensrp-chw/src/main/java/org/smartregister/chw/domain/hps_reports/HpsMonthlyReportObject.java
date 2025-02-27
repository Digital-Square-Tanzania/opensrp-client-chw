package org.smartregister.chw.domain.hps_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.ReportObject;

import java.util.Date;

public class HpsMonthlyReportObject extends ReportObject {


    private final String[] hpsQuestionsGroups = new String[]{"1a","1b","1c","1A1","1A2","1A3","1A4","1A5","1A6","1A7","1A8",
            "1B1","1B2","1B3","1B4","1B5","1B6","1B7","1B8","1B9","1B10","1B11","1B12","1B13","1B14",
            "1C1","1C2","1C3","1C4","1C5","1C6","1C7","1C8","1C9","1C10","1C11","1C12","1C13","1C14","1C15","1C16","1C17","1C18","1C19","1C20","1C21","1C22","1C23","1C24","1C25","1C26","1C27","1C28","1C29",
            "1D1","1D2","1D3","1D4","1D5","1D6","1D7","1D8","1D9","1D10","1D11","1D12","1D13","1D14","1D15","1D16","1D17","1D18","1D19","1D20","1D21",
            "1F1","1F2","1F3","1F4","1F5","1F6","1F7","1F8",
            "11","12","13","14","15","16","17","18","19","110","111",
            "1G1","1G2","1G3","1G4","1G5","1G6",
            "1H1","1H2","1H3","1H4","1H5","1H6","1H7","1H8","1H9","1H10","1H11","1H12","1H13","1H14","1H15","1H16","1H17","1H18","1H19","1H20","1H21",
            "4J1","4J2","4J3","4J4","4j5",
    };

    private final String[] hpsQuestionsGroupsWithOnlyTotal = new String[]{
            "1I1","1I2","1I3","1I4",
            "2a","2b","2c","2C1","2C2","2C3","2C4","2C5","2C6","2C7","2C8","2C9","2C10","2C11","2C12","2C13","2C14","2C15","2C16","2C17","2C18","2C19","2C20","2C21","2C22","2C23","2C24","2C25","2C26","2C27","2C28","2C29",
            "2D1","2D10","2D11","2D13","2D14","2D19","2D21",
            "3K1","3k2","3k3","3K4","3k5","3k6","3L1","3L2","3L3","3L4","3L5","3L6","3L7","3L8",
            "3C1","3C2","3C3","3C4","3C5","3C6","3C7","3C8","3C9","3C10","3C11","3C12","3C13","3C14","3C15","3C16","3C17","3C18","3C19","3C20","3C21","3C22","3C23","3C24","3C25","3C26","3C27","3C28","3C29",
            "5M1","5M2","5M3","5M4",
    };

    private final String[] hpsQuestionsGroupsWithGenderTotalAndTotal = new String[]{
            "3C1","3C2","3C3","3C4","3C5","3C6","3C7","3C8","3C9","3C10","3C11","3C12","3C13","3C14","3C15","3C16",
            "3C17","3C18","3C19","3C20","3C21","3C22","3C23","3C24","3C25","3C26","3C27","3C28","3C29",
    };

    private final String[] hpsGenderGroups = new String[]{
            "ME","KE"
    };
    private final String[] hpsAgeGroups = new String[]{
            "under-month","1-11-months","12-23-months","2-4","5-9","10-14","15-19","20-24","25-59","60+"
    };

    private final Date reportDate;
    private JSONObject jsonObject ;

    public HpsMonthlyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        for (String questionGroup : hpsQuestionsGroups) {   //rows
            for (String genderGroup : hpsGenderGroups) {
                    for (String ageGroup : hpsAgeGroups) {
                        jsonObject.put("hps" + "-" + questionGroup + "-" + genderGroup + "-" + ageGroup,
                                ReportDao.getReportPerIndicatorCode("hps" + "-" + questionGroup + "-" + genderGroup + "-" + ageGroup, reportDate));
                    }
            }
            for (String qns : hpsQuestionsGroupsWithOnlyTotal){
                jsonObject.put("hps" + "-" + qns + "-grand-total",
                        ReportDao.getReportPerIndicatorCode("hps" + "-" + qns + "-grand-total", reportDate));
            }
        }
        funcGetGenderIndicatorTotal();
        getTotalPerIndicator3C();

        return jsonObject;
    }

    private void funcGetGenderIndicatorTotal() throws JSONException {
        int totalofthewholehpsgroup = 0;
        for (String question: hpsQuestionsGroups) {
            for (String hpsGenderGroups : hpsGenderGroups) {
                totalofthewholehpsgroup+=getTotalPerEachIndicator(question,hpsGenderGroups);
                jsonObject.put("hps"+"-"+question+"-grand-total",totalofthewholehpsgroup); //total for all hps groups
            }
            totalofthewholehpsgroup = 0;
        }
    }
    private int getTotalPerEachIndicator(String question,String hpsgenderGroup) throws JSONException {
        int  totalOfGenderGiven = 0;
        int returnedValue = 0;
        for (String age: hpsAgeGroups){
                totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode(
                        "hps" + "-" + question + "-" + hpsgenderGroup + "-" + age, reportDate));
            jsonObject.put("hps"+"-"+question+"-"+hpsgenderGroup+"-total",totalOfGenderGiven);  //display the total for both gender
            returnedValue = totalOfGenderGiven;
        }
        return returnedValue;
    }

    private void getTotalPerIndicator3C() throws JSONException {
        int totalofthewholehpsgroup = 0;
        for (String question: hpsQuestionsGroupsWithGenderTotalAndTotal) {
            for (String hpsGenderGroups : hpsGenderGroups) {
                totalofthewholehpsgroup+=ReportDao.getReportPerIndicatorCode(
                        "hps" + "-" + question + "-" + hpsGenderGroups + "-total", reportDate);
                jsonObject.put("hps"+"-"+question+"-grand-total",totalofthewholehpsgroup); //total for all hps groups
            }
            totalofthewholehpsgroup = 0;
        }
    }

}
