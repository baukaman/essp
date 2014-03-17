package kz.bsbnb.usci.eav.persistance.dao.impl;

import kz.bsbnb.usci.eav.model.base.IBaseValue;
import kz.bsbnb.usci.eav.model.persistable.IPersistable;
import kz.bsbnb.usci.eav.persistance.dao.IBeDateValueDao;
import kz.bsbnb.usci.eav.persistance.impl.db.JDBCSupport;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.jooq.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Map;

import static kz.bsbnb.eav.persistance.generated.Tables.EAV_BE_DATE_VALUES;

/**
 *
 */
@Repository
public class BeDateValueDaoImpl extends JDBCSupport implements IBeDateValueDao
{

    private final Logger logger = LoggerFactory.getLogger(BeDateValueDaoImpl.class);

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private DSLContext context;

    @Override
    public void insert(IPersistable persistable) {
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
    }

    protected long insert(long baseEntityId, long batchId, long metaAttributeId, long index,
                          Date reportDate, Object value, boolean closed, boolean last)
    {
        Insert insert = context
                .insertInto(EAV_BE_DATE_VALUES)
                .set(EAV_BE_DATE_VALUES.ENTITY_ID, baseEntityId)
                .set(EAV_BE_DATE_VALUES.BATCH_ID, batchId)
                .set(EAV_BE_DATE_VALUES.ATTRIBUTE_ID, metaAttributeId)
                .set(EAV_BE_DATE_VALUES.INDEX_, index)
                .set(EAV_BE_DATE_VALUES.REPORT_DATE, DataUtils.convert(reportDate))
                .set(EAV_BE_DATE_VALUES.VALUE, DataUtils.convert((Date) value))
                .set(EAV_BE_DATE_VALUES.IS_CLOSED, DataUtils.convert(closed))
                .set(EAV_BE_DATE_VALUES.IS_LAST, DataUtils.convert(last));

        logger.debug(insert.toString());
        return insertWithId(insert.getSQL(), insert.getBindValues().toArray());
    }

    @Override
    public void update(IPersistable persistable) {

    }

    @Override
    public void delete(IPersistable persistable) {

    }
}
