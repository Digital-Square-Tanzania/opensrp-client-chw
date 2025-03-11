function loadData(reportKey, reportType) {
  const data = JSON.parse(Android.getData(reportKey));
  const tableBody = document.getElementById("table-body");
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
    if(typeof data!== undefined && data !== ""){
      const reportData = data.nameValuePairs.reportData.values;

      reportData.forEach((dataPoint) => {
          //append to the table body a row with data
          const row = document.createElement("tr");
          const dataPointKeys = Object.keys(dataPoint.nameValuePairs);
          dataPointKeys.forEach((key) => {
              const cell = document.createElement("td");
              cell.innerHTML = dataPoint.nameValuePairs[key];
              row.appendChild(cell);
          }
          );
          tableBody.appendChild(row);
      });
    }
  reportPeriod.innerHTML = Android.getDataPeriod(reportKey);
  reportingFacility.innerHTML = Android.getReportingFacility();
}
