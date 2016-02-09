package kz.bsbnb.usci.portlets.upload.ui;

/**
 *
 * @author Aidar.Myrzahanov
 */
public enum Localization {
    SINGLE_UPLOAD_TAB_CAPTION("SINGLE-UPLOAD-TAB-CAPTION"),
    MULTIPLE_UPLOAD_TAB_CAPTION("MULTIPLE-UPLOAD-TAB-CAPTION"),
    DIGITAL_SIGN_TAB_CAPTION("DIGITAL-SIGN-TAB-CAPTION"),
    MULTI_UPLOAD_AREA_TEXT("MULTI-UPLOAD-AREA-TEXT"),
    UPLOAD_BUTTON_CAPTION("UPLOAD-BUTTON-CAPTION"),
    MULTI_UPLOAD_DESCRIPTION("MULTI-UPLOAD-DESCRIPTION"),
    FILE_TOO_LARGE_MESSAGE("FILE-TOO-LARGE-MESSAGE"),
    NOT_A_ZIP_FILE_MESSAGE("NOT-A-ZIP-FILE-MESSAGE"),
    USER_UNKNOWN_MESSAGE("USER-UNKNOWN-MESSAGE"),
    UPLOAD_HAVE_STARTED_MESSAGE("UPLOAD-HAVE-STARTED-MESSAGE"),
    USER_DOES_NOT_HAVE_ACCESS_TO_CREDITORS_MESSAGE("USER-DOES-NOT-HAVE-ACCESS-TO-CREDITORS-MESSAGE"),
    USER_HAS_MORE_THAN_ONE_CREDITOR_MESSAGE("USER-HAS-MORE-THAN-ONE-CREDITOR-MESSAGE"),
    UPLOAD_SUCCEDED_MESSAGE("UPLOAD-SUCCEEDED-MESSAGE"),
    UPLOAD_PROGRESS_MESSAGE("UPLOAD-PROGRESS-MESSAGE"),
    ORGANIZATION_APPROVED_DATA_MESSAGE("ORGANIZATION-APPROVED-DATA-MESSAGE"),
    REPORT_STATUS_NOT_FOUND_MESSAGE("REPORT-STATUS-NOT-FOUND-MESSAGE"), 
    SEND_USING_DIGITAL_SIGNATURE("SEND-USING-DIGITAL-SIGNATURE"),
    DIGITAL_SIGNING_ORGANIZATIONS_SELECT_CAPTION("DIGITAL-SIGNING-ORGANIZATIONS-SELECT-CAPTION"),
    CONFIGURATION_SAVED_MESSAGE("CONFIGURATION-SAVED-MESSAGE"),
    SAVE_CONFIGURATION_BUTTON_CAPTION("SAVE-CONFIGURATION-BUTTON-CAPTION"),
    CONFIGURATION_TAB_CAPTION("CONFIGURATION-TAB-CAPTION");
    private String key;
    private Localization(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
}
