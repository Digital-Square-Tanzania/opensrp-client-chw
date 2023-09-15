package org.smartregister.chw.dataloader;

import static org.smartregister.chw.malaria.util.Constants.EVENT_TYPE.MALARIA_CONFIRMATION;
import static org.smartregister.chw.util.Constants.Events.UPDATE_MALARIA_CONFIGURATION;

import org.smartregister.chw.core.form_data.NativeFormsDataLoader;

import java.util.ArrayList;
import java.util.List;

public class MalariaMemberDataLoader extends NativeFormsDataLoader {
    
    @Override
    protected List<String> getEventTypes() {
        List<String> res = new ArrayList<>();
        res.add(UPDATE_MALARIA_CONFIGURATION);
        res.add(MALARIA_CONFIRMATION);
        return res;
    }
}
