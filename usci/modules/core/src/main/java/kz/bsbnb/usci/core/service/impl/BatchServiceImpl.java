package kz.bsbnb.usci.core.service.impl;

import kz.bsbnb.usci.core.service.IBatchService;
import kz.bsbnb.usci.core.service.IGlobalService;
import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.BatchStatus;
import kz.bsbnb.usci.eav.model.EavGlobal;
import kz.bsbnb.usci.eav.model.EntityStatus;
import kz.bsbnb.usci.eav.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav.persistance.dao.IBatchStatusDao;
import kz.bsbnb.usci.eav.persistance.dao.IEntityStatusDao;
import kz.bsbnb.usci.eav.util.BatchStatuses;
import kz.bsbnb.usci.eav.util.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author k.tulbassiyev
 */
@Service
public class BatchServiceImpl implements IBatchService {
    @Autowired
    private IBatchDao batchDao;

    @Autowired
    private IBatchStatusDao batchStatusDao;

    @Autowired
    private IEntityStatusDao entityStatusDao;

    @Autowired
    private IGlobalService globalService;

    @Value("${batch.save.dir}")
    private String batchSaveDir;

    @Override
    public long save(Batch batch) {
        return batchDao.save(batch);
    }

    private static final Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);

    @Override
    public Batch getBatch(long batchId) {
        Batch batch = batchDao.load(batchId);

        if (batch.getRepDate() == null || batch.getCreditorId() == null || batch.getHash() == null) {
            return batch;
        }

        File file = new File(getFullFilePath(batch));

        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(file);
            batch.setContent(bytes);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return batch;
    }

    @Override
    public long uploadBatch(Batch batch) {
        setHash(batch);
        saveBatchFile(batch);
        return batchDao.save(batch);
    }

    private void saveBatchFile(Batch batch) {
        File repDateDir = new File(getCreditorDirPath(batch));

        if (!repDateDir.exists()) {
            repDateDir.mkdirs();
        }

        File outputFile = new File(getFullFilePath(batch));

        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }

            FileCopyUtils.copy(batch.getContent(), outputFile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFullFilePath(Batch batch) {
        if (batch.getRepDate() == null || batch.getCreditorId() == null || batch.getHash() == null) {
            throw new RuntimeException(Errors.compose(Errors.E233));
        }
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        return batchSaveDir + "/" + df.format(batch.getRepDate())
                + "/" + batch.getCreditorId() + "/" + batch.getHash() + ".zip";
    }

    private String getCreditorDirPath(Batch batch) {
        if (batch.getRepDate() == null || batch.getCreditorId() == null) {
            throw new RuntimeException(Errors.compose(Errors.E232));
        }
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        return batchSaveDir + "/" + df.format(batch.getRepDate())
                + "/" + batch.getCreditorId();
    }

    @Override
    public Long addBatchStatus(BatchStatus batchStatus) {
        if (batchStatus.getStatusId() < 1 && batchStatus.getStatus() != null) {
            EavGlobal status = globalService.getGlobal(batchStatus.getStatus());
            batchStatus.setStatusId(status.getId());
        }
        return batchStatusDao.insert(batchStatus);
    }

    @Override
    public void endBatch(long batchId) {
        EavGlobal statusCompleted = globalService.getGlobal(BatchStatuses.COMPLETED);

        List<BatchStatus> batchStatusList = getBatchStatusList(batchId);

        boolean hasError = false;

        for (BatchStatus batchStatus : batchStatusList) {
            if (BatchStatuses.ERROR.equals(batchStatus.getStatus())) {
                hasError = true;
                break;
            }
        }

        if (!hasError) {
            addBatchStatus(new BatchStatus()
                    .setBatchId(batchId)
                    .setStatusId(statusCompleted.getId())
                    .setReceiptDate(new Date()));
        }

        Batch batch = getBatch(batchId);

        try {
            File file = new File(batch.getFileName());
            if (!file.delete())
                logger.error("Не удалось удалить файл после завершения " + batch.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Long addEntityStatus(EntityStatus entityStatus) {
        if (entityStatus.getStatusId() < 1 && entityStatus.getStatus() != null) {
            EavGlobal status = globalService.getGlobal(entityStatus.getStatus());
            entityStatus.setStatusId(status.getId());
        }
        return entityStatusDao.insert(entityStatus);
    }

    @Override
    public List<EntityStatus> getEntityStatusList(long batchId) {
        return entityStatusDao.getList(batchId);
    }

    @Override
    public List<EntityStatus> getEntityStatusList(long batchId, int firstIndex, int count) {
        return entityStatusDao.getList(batchId, firstIndex, count);
    }

    @Override
    public List<BatchStatus> getBatchStatusList(long batchId) {
        return batchStatusDao.getList(batchId);
    }

    @Override
    public List<BatchStatus> getBatchStatuses(List<Long> batchIds) {
        return batchStatusDao.getStatuses(batchIds);
    }

    @Override
    public List<Batch> getPendingBatchList() {
        return batchDao.getPendingBatchList();
    }

    @Override
    public List<Batch> getBatchListToSign(long creditorId) {
        return batchDao.getBatchListToSign(creditorId);
    }

    @Override
    public void signBatch(long batchId, String sign, String signInfo, Date signTime) {
        Batch batch = batchDao.load(batchId);
        batch.setSign(sign);
        batch.setSignInfo(signInfo);
        batch.setSignTime(signTime);
        batchDao.save(batch);
    }

    @Override
    public List<Batch> getAll(Date repDate) {
        return batchDao.getAll(repDate);
    }

    @Override
    public List<Batch> getAll(Date repDate, List<Creditor> creditorsList) {
        return batchDao.getAll(repDate, creditorsList);
    }

    @Override
    public List<Batch> getAll(Date repDate, List<Creditor> creditorsList, int firstIndex, int count) {
        return batchDao.getAll(repDate, creditorsList, firstIndex, count);
    }

    private void setHash(Batch batch) {
        String hash = DigestUtils.md5DigestAsHex(batch.getContent());
        batch.setHash(hash);
    }

    @Override
    @Transactional
    public boolean incrementActualCounts(Map<Long, Long> batchesToUpdate) {
        try {
            for (Long batchId : batchesToUpdate.keySet()) {
                batchDao.incrementActualCount(batchId, batchesToUpdate.get(batchId));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean clearActualCount(long batchId){
        try {
            batchDao.clearActualCount(batchId);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public List<Batch> getMaintenanceBatches(Date reportDate) {
        return batchDao.getMaintenanceBatches(reportDate);
    }

    @Override
    public void approveMaintenance(List<Long> approvedBatchIds) {
        batchDao.approveMaintenance(approvedBatchIds);
    }

    @Override
    public void declineMaintenance(List<Long> declinedBatchIds) {
        batchDao.declineMaintenance(declinedBatchIds);
    }

    @Override
    public int getBatchCount(List<Creditor> creditors, Date reportDate) {
        return batchDao.getBatchCount(creditors, reportDate);
    }

    @Override
    public int getSuccessEntityCount(long batchId) {
        return entityStatusDao.getSuccessEntityCount(batchId);
    }

    @Override
    public int getErrorEntityStatusCount(Batch batch)
    {
        return entityStatusDao.getErrorCount(batch.getId());
    }

    @Override
    public String getSignatureInfo(long batchId) {
        return batchDao.getSignInfo(batchId);
    }
}
