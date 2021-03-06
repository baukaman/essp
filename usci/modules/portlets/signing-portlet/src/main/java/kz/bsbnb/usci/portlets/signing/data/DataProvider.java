package kz.bsbnb.usci.portlets.signing.data;

import kz.bsbnb.usci.cr.model.Creditor;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Aidar.Myrzahanov
 */
public interface DataProvider {
   List<Creditor> getCreditorsList(long userId);
    
    List<FileSignatureRecord> getFilesToSign(long creditorId);

    String getBaseUrl();
    
    void addInputFileToQueue(FileSignatureRecord record);

    void signFile(long fileId, String sign, String signInfo, Date signTime);

    public String getCreditorsBinNumber(Creditor creditor);

    public void cancelFile(FileSignatureRecord file);
    
    public String getOcspServiceUrl();


}
