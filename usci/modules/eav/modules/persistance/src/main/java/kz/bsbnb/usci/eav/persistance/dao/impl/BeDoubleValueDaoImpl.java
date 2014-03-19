package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.persistable.IPersistable;
import kz.bsbnb.usci.eav.persistance.dao.IBeDoubleValueDao;
import kz.bsbnb.usci.eav.persistance.impl.db.JDBCSupport;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.jooq.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;

import static kz.bsbnb.eav.persistance.generated.Tables.EAV_BE_DOUBLE_VALUES;

/**
 *
 */
@Repository
public class BeDoubleValueDaoImpl extends JDBCSupport implements IBeDoubleValueDao
{

    private final Logger logger = LoggerFactory.getLogger(BeDoubleValueDaoImpl.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Override
    public long insert(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue)persistable;
        long baseValueId = insert(
                baseValue.getBaseContainer().getId(),
                baseValue.getBatch().getId(),
                baseValue.getMetaAttribute().getId(),
                baseValue.getIndex(),
                baseValue.getRepDate(),
                baseValue.getValue(),
                baseValue.isClosed(),
                baseValue.isLast());
        baseValue.setId(baseValueId);

        return baseValueId;
    }

    protected long insert(long baseEntityId, long batchId, long metaAttributeId, long index,
                          Date reportDate, Object value, boolean closed, boolean last)
    {
        Insert insert = context
                .insertInto(EAV_BE_DOUBLE_VALUES)
                .set(EAV_BE_DOUBLE_VALUES.ENTITY_ID, baseEntityId)
                .set(EAV_BE_DOUBLE_VALUES.BATCH_ID, batchId)
                .set(EAV_BE_DOUBLE_VALUES.ATTRIBUTE_ID, metaAttributeId)
                .set(EAV_BE_DOUBLE_VALUES.INDEX_, index)
                .set(EAV_BE_DOUBLE_VALUES.REPORT_DATE, DataUtils.convert(reportDate))
                .set(EAV_BE_DOUBLE_VALUES.VALUE, (Double)value)
                .set(EAV_BE_DOUBLE_VALUES.IS_CLOSED, DataUtils.convert(closed))
                .set(EAV_BE_DOUBLE_VALUES.IS_LAST, DataUtils.convert(last));

        logger.debug(insert.toString());
        return insertWithId(insert.getSQL(), insert.getBindValues().toArray());
    }

    @Override
    public void update(IPersistable persistable) {
        IBaseValue baseValue = (IBaseValue)persistable;
        update(baseValue.getId(), baseValue.getBaseContainer().getId(), baseValue.getBatch().getId(),
                baseValue.getMetaAttribute().getId(), baseValue.getIndex(), baseValue.getRepDate(),
                baseValue.getValue(), baseValue.isClosed(), baseValue.isLast());
    }

    protected void update(long id, long baseEntityId, long batchId, long metaAttributeId, long index,
                          Date reportDate, Object value, boolean closed, boolean last)
    {
        String tableAlias = "dv";
        Update update = context
                .update(EAV_BE_DOUBLE_VALUES.as(tableAlias))
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).ENTITY_ID, baseEntityId)
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).BATCH_ID, batchId)
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).ATTRIBUTE_ID, metaAttributeId)
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).INDEX_, index)
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).REPORT_DATE, DataUtils.convert(reportDate))
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).VALUE, (Double)value)
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).IS_CLOSED, DataUtils.convert(closed))
                .set(EAV_BE_DOUBLE_VALUES.as(tableAlias).IS_LAST, DataUtils.convert(last))
                .where(EAV_BE_DOUBLE_VALUES.as(tableAlias).ID.equal(id));

        logger.debug(update.toString());
        int count = updateWithStats(update.getSQL(), update.getBindValues().toArray());
        if (count != 1)
        {
            throw new RuntimeException("UPDATE operation should be update only one record.");
        }
    }

    @Override
    public void delete(IPersistable persistable) {
        delete(persistable.getId());
    }

    protected void delete(long id) {
        String tableAlias = "dv";
        Delete delete = context
                .delete(EAV_BE_DOUBLE_VALUES.as(tableAlias))
                .where(EAV_BE_DOUBLE_VALUES.as(tableAlias).ID.equal(id));

        logger.debug(delete.toString());
        int count = updateWithStats(delete.getSQL(), delete.getBindValues().toArray());
        if (count != 1)
        {
            throw new RuntimeException("DELETE operation should be delete only one record.");
        }
    }

}
