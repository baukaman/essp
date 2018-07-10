package kz.bsbnb.test.dao;

import com.google.common.base.Optional;
import junit.framework.Assert;
import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.engine.SavingInfo;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml"})
public class CreditPersistTest extends FunctionalTest {

    @Autowired
    DataEntityDao entityDao;

    ThreePartReader reader;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    DatabaseActivity databaseActivity;

    @Test
    @Transactional
    public void testInsert() throws Exception {
        reader = new ThreePartReader()
                .withSource(getInputStream("dao/SimpleValues.xml"))
                .withMeta(metaCredit);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        DataEntity entity = reader.read();
        entityDao.insert(entity);
        DataEntity loadedEntity = entityDao.load(entity.getId(), entity.getCreditorId(), entity.getReportDate());
        MetaClass meta = loadedEntity.getMeta();
        Assert.assertEquals("credit", meta.getClassName());
        Assert.assertEquals(entity.getEl("amount"), loadedEntity.getEl("amount"));
        Assert.assertEquals(entity.getEl("maturity_date"), loadedEntity.getEl("maturity_date"));
    }

    @Test
    @Transactional
    public void testLoadByMaxRD() throws Exception {
        reader = new ThreePartReader()
                .withSource(getInputStream("dao/SimpleValues.xml"))
                .withMeta(metaCredit);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        DataEntity entity = reader.read();
        entityDao.insert(entity);
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
