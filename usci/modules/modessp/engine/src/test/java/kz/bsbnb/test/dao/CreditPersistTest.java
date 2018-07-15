package kz.bsbnb.test.dao;

import com.google.common.base.Optional;
import junit.framework.Assert;
import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.SavingInfo;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.test.EngineTestBase;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class CreditPersistTest extends EngineTestBase {

    @Autowired
    DataEntityDao entityDao;

    @Autowired
    ThreePartReader reader;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    DatabaseActivity databaseActivity;

    @Test
    @Transactional
    public void testInsert() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("dao/SimpleValues.xml"))
                .withMeta(metaCredit).read();

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        entityDao.insertNewEntity(entity);
        DataEntity loadedEntity = entityDao.load(entity.getId(), entity.getCreditorId(), entity.getReportDate());
        MetaClass meta = loadedEntity.getMeta();
        Assert.assertEquals("credit", meta.getClassName());
        Assert.assertEquals(entity.getEl("amount"), loadedEntity.getEl("amount"));
        Assert.assertEquals(entity.getEl("maturity_date"), loadedEntity.getEl("maturity_date"));
    }

    @Test
    @Transactional
    public void testLoadByMaxRD() throws Exception {
        reader.withSource(getInputStream("dao/SimpleValues.xml"))
                .withMeta(metaCredit);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        DataEntity entity = reader.read();
        entityDao.insertNewEntity(entity);
        databaseActivity.reset();

        DataEntity testEntity = entity.clone();
        testEntity.setReportDate(DataUtils.getDate("01.02.2018"));

        Optional<DataEntity> dataEntityOptional = entityDao.loadByMaxReportDate(testEntity);
        Assert.assertTrue(dataEntityOptional.isPresent());
        DataEntity loadedEntity = dataEntityOptional.get();
        Assert.assertEquals(loadedEntity.getReportDate(), DataUtils.getDate("01.01.2018"));
        Assert.assertEquals(loadedEntity.getEl("amount"), entity.getEl("amount"));
        Assert.assertEquals(loadedEntity.getEl("maturity_date"), entity.getEl("maturity_date"));
        Assert.assertEquals(3, databaseActivity.numberOfSelects());

    }
}
