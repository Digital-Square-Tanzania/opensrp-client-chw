package org.smartregister.chw.dao;

import java.util.ArrayList;
import java.util.List;

public class FacilityDaoFlv extends DefaultChwPNCDaoFlv {


    public static List<String> getAllCHWFacilities(String chwUUID){
        String sql = "Select * from location inner join facility on chw='" + chwUUID + "'";

        DataMap<String> dataMap = c -> getCursorValue(c, "records");
        List<String> res = readData(sql, dataMap);
        return res==null?new ArrayList<>():res;
    }


}


