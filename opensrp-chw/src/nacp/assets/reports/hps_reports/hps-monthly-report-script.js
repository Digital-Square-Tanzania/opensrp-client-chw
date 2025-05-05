function loadData(reportKey, reportType) {
  const data = JSON.parse(Android.getData(reportKey));
  const keys = Object.keys(data.nameValuePairs);
  const reportPeriod = document.getElementById("report_period");
  const reportingFacility = document.getElementById("reporting_facility");
  keys.forEach((key) => {
    let element;
    if(reportType!== null && reportType === "pnc"){
      element = document.getElementById(key.replace("pnc-",""));
    }else{
      element = document.getElementById(key);
    }
    if (element !== null && typeof element !== "undefined") {
      element.innerHTML = data.nameValuePairs[key];
    }
  });
  reportPeriod.innerHTML = Android.getDataPeriod();
  reportingFacility.innerHTML = Android.getReportingChw();
}
