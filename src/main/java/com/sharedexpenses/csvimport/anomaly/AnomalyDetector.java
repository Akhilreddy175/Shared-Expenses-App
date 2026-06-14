package com.sharedexpenses.csvimport.anomaly;

import com.sharedexpenses.csvimport.ImportRow;

import java.util.List;


public interface AnomalyDetector {

    
    List<AnomalyReport> detect(ImportRow row, AnomalyContext context);

    
    String getName();
}
