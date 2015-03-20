package com.bsbnb.usci.portlets.crosscheck.ui;

import com.bsbnb.usci.portlets.crosscheck.PortletEnvironmentFacade;

/**
 *
 * @author Aidar.Myrzahanov
 */
public enum Localization {
    SELECT_BANK_COMBOBOX_CAPTION("BANK-COMBOBOX-CAPTION"),
    SELECT_ALL_BANKS_CHECKBOX_CAPTION("SELECT-ALL-BANKS-CHECKBOX-CAPTION"),
    DATE_FIELD_CAPTION("DATE-FIELD-CAPTION"),
    SHOW_BUTTON_CAPTION("SHOW-BUTTON-CAPTION"),
    RUN_CROSS_CHECK_BUTTON_CAPTION("RUN-CROSS-CHECK-BUTTON-CAPTION"),
    INFO_FILE_TABLE_CAPTION("INFO-FILE-TABLE-CAPTION"),
    PACKAGE_ERRORS_TABLE_CAPTION("PACKAGE-ERRORS-TABLE-CAPTION"),
    MESSAGE_CROSS_CHECK_FAILED("MESSAGE-CROSS-CHECK-FAILED"),
    MESSAGE_DATE_NOT_SELECTED("MESSAGE-DATE-NOT-SELECTED"),
    MESSAGE_NO_DATA_FOUND("MESSAGE-NO-DATA-FOUND"),
    MESSAGE_FAILED_TO_LOAD_CROSS_CHECK("MESSAGE-FAILED-TO-LOAD-CROSS-CHECK"),
    CROSS_CHECK_MISMATCH_NOTE("CROSS-CHECK-MISMATCH-NOTE"),
    CROSS_CHECK_MATCH_NOTE("CROSS-CHECK-MATCH-NOTE"),
    EXPORT_TABLE_TO_XLS("EXPORT-TABLE-TO-XLS"),
    MARK_ALL("MARK-ALL"),
    UNMARK_ALL("UNMARK-ALL"),
    CROSS_CHECK_EXCEL_CAPTION("CROSS-CHECK_EXCEL_CAPTION"),
    EXCEL_HEADER_ORGANIZATION("EXCEL-HEADER-ORGANIZATION"),
    EXCEL_HEADER_REPORT_DATE("EXCEL-HEADER-REPORT-DATE"),
    CROSS_CHECK_TABLE_COLUMNS("CROSS-CHECK-TABLE-COLUMNS"),
    CROSS_CHECK_TABLE_HEADERS("CROSS-CHECK-TABLE-HEADERS"),
    CROSS_CHECK_MESSAGE_TABLE_COLUMNS("CROSS-CHECK-MESSAGE-TABLE-COLUMNS"),
    CROSS_CHECK_MESSAGE_TABLE_HEADERS("CROSS-CHECK-MESSAGE-TABLE-HEADERS"), 
    XLS_SHEET_HEADER("XLS-SHEET-HEADER"),
    BUSINESS_RULES("BUSINESS-RULES"),
    BATCH_EXPORT_TO_EXCEL("BATCH-EXPORT-TO-EXCEL"),
    EMPTY_DATE_FIELD("EMPTY-DATE-FIELD"),
    XLS_BATCH_MESSAGES_EXPORT_FILENAME_PREFIX("XLS-BATCH-MESSAGES-EXPORT-FILENAME-PREFIX"),
    XLS_MESSAGES_EXPORT_FILENAME_PREFIX("XLS-MESSAGES-EXPORT-FILENAME-PREFIX"),
    DEVELOPMENT_MESSAGE("DEVELOPMENT-MESSAGE");

    private String key;

    private Localization(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return PortletEnvironmentFacade.get().getResourceString(key);
    }
    
    public String getKey() {
        return key;
    }
}
