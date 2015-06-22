package kz.bsbnb.usci.core.service;

import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.cr.model.InputInfo;
import kz.bsbnb.usci.eav.model.json.BatchFullJModel;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public interface InputInfoBeanRemoteBusiness {
    List<InputInfo> getAllInputInfos(List<Creditor> creditorsList, Date reportDate);
    List<InputInfo> getPendingBatches(List<Creditor> creditorsList);
    BatchFullJModel getBatchFullModel(BigInteger batchId);
}
