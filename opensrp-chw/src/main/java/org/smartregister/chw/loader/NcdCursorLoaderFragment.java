package org.smartregister.chw.loader;

import org.smartregister.commonregistry.CommonRepository;

public interface NcdCursorLoaderFragment {

    void countExecute();

    String defaultFilterAndSortQuery();

    CommonRepository commonRepository();
}
