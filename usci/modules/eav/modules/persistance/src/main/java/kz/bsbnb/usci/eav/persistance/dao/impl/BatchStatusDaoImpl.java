package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.model.BatchStatus;
import kz.bsbnb.usci.eav.persistance.dao.IBatchStatusDao;
import kz.bsbnb.usci.eav.persistance.db.JDBCSupport;
import kz.bsbnb.usci.eav.util.BatchStatuses;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Insert;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static kz.bsbnb.eav.persistance.generated.Tables.EAV_BATCH_STATUSES;

@Repository
public class BatchStatusDaoImpl extends JDBCSupport implements IBatchStatusDao {

    @Autowired
    private DSLContext context;

    @Autowired
    private EavGlobalDaoImpl eavGlobalDao;

    @Override
    public Long insert(BatchStatus batchStatus) {
        Insert insert = context
                .insertInto(EAV_BATCH_STATUSES,
                    EAV_BATCH_STATUSES.BATCH_ID,
                    EAV_BATCH_STATUSES.STATUS_ID,
                    EAV_BATCH_STATUSES.RECEIPT_DATE,
                    EAV_BATCH_STATUSES.DESCRIPTION)
                .values(
                    batchStatus.getBatchId(),
                    batchStatus.getStatusId(),
                    DataUtils.convertToTimestamp(batchStatus.getReceiptDate()),
                    batchStatus.getDescription());

        return insertWithId(insert.getSQL(), insert.getBindValues().toArray());
    }

    @Override
    public List<BatchStatus> getList(long batchId) {
        Select select = context.selectFrom(
                EAV_BATCH_STATUSES)
                .where(EAV_BATCH_STATUSES.BATCH_ID.eq(batchId))
                .orderBy(EAV_BATCH_STATUSES.RECEIPT_DATE.desc(), EAV_BATCH_STATUSES.STATUS_ID.asc());

        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        List<BatchStatus> batchStatusList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            BatchStatus batchStatus = toBatchStatus(row);
            batchStatusList.add(batchStatus);
        }

        return batchStatusList;
    }

    @Override
    public List<BatchStatus> getStatuses(List<Long> batchIds) {
        Select select = context.selectFrom(
                EAV_BATCH_STATUSES)
                .where(EAV_BATCH_STATUSES.BATCH_ID.in(batchIds))
                .orderBy(EAV_BATCH_STATUSES.RECEIPT_DATE.desc(), EAV_BATCH_STATUSES.STATUS_ID.asc());

        List<Map<String, Object>> rows = queryForListWithStats(select.getSQL(), select.getBindValues().toArray());

        List<BatchStatus> batchStatusList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            BatchStatus batchStatus = toBatchStatus(row);
            batchStatusList.add(batchStatus);
        }

        return batchStatusList;
    }

    private BatchStatus toBatchStatus(Map<String, Object> row) {
        BatchStatus batchStatus = new BatchStatus();
        batchStatus.setId(((BigDecimal) row.get(EAV_BATCH_STATUSES.ID.getName())).longValue());
        batchStatus.setBatchId(((BigDecimal) row.get(EAV_BATCH_STATUSES.BATCH_ID.getName())).longValue());
        batchStatus.setStatusId(((BigDecimal) row.get(EAV_BATCH_STATUSES.STATUS_ID.getName())).longValue());
        batchStatus.setDescription((String) row.get(EAV_BATCH_STATUSES.DESCRIPTION.getName()));
        batchStatus.setReceiptDate(DataUtils.convert((Timestamp) row.get(EAV_BATCH_STATUSES.RECEIPT_DATE.getName())));
        batchStatus.setStatus(BatchStatuses.valueOf(eavGlobalDao.get(batchStatus.getStatusId()).getCode()));
        return batchStatus;
    }
}
