package org.smartregister.chw.domain;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.smartregister.chw.dao.ReportDao;
import org.smartregister.chw.domain.iccm_reports.IccmClientsReportObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IccmClientsReportObjectTest {

    private Date reportDate;

    @Before
    public void setUp() throws ParseException {
        reportDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2026-04-01");
    }

    @Test
    public void testGetIndicatorDataDerivesIccmAgeAndSexTotals() throws JSONException {
        try (MockedStatic<ReportDao> reportDao = Mockito.mockStatic(ReportDao.class)) {
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-1-less-than-1-month-ME", reportDate))
                    .thenReturn(2);
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-1-less-than-1-month-KE", reportDate))
                    .thenReturn(3);
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-2-less-than-1-month-ME", reportDate))
                    .thenReturn(4);
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-2-less-than-1-month-KE", reportDate))
                    .thenReturn(6);
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-3-less-than-1-month-ME", reportDate))
                    .thenReturn(1);
            reportDao.when(() -> ReportDao.getReportPerIndicatorCode("iccm-3-less-than-1-month-KE", reportDate))
                    .thenReturn(2);

            JSONObject indicatorData = new IccmClientsReportObject(reportDate).getIndicatorData();

            assertEquals(5, indicatorData.getInt("iccm-1-less-than-1-month-jumla"));
            assertEquals(2, indicatorData.getInt("iccm-1-ME-jumla"));
            assertEquals(3, indicatorData.getInt("iccm-1-KE-jumla"));
            assertEquals(5, indicatorData.getInt("iccm-1-jumla"));

            assertEquals(5, indicatorData.getInt("iccm-2+3-less-than-1-month-ME"));
            assertEquals(8, indicatorData.getInt("iccm-2+3-less-than-1-month-KE"));
            assertEquals(13, indicatorData.getInt("iccm-2+3-less-than-1-month-jumla"));
            assertEquals(13, indicatorData.getInt("iccm-2+3-jumla"));
        }
    }
}
