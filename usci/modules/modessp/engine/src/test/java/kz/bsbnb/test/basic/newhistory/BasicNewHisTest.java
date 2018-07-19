package kz.bsbnb.test.basic.newhistory;

import kz.bsbnb.DataDoubleValue;
import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.BootstrapEngine;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.test.EngineTestBase;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class BasicNewHisTest extends EngineTestBase {

    @Autowired
    DataEntityDao dataEntityDao;

    @Autowired
    BootstrapEngine bootstrapEngine;

    @Autowired
    ThreePartReader reader;

    @Test
    @Transactional
    public void shouldCreateHistory() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        savingEntity.setDataValue("amount", new DataDoubleValue(6000.0));
        savingEntity.setReportDate(DataUtils.getDate("01.02.2018"));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(6000.0, newEntity.getEl("amount"));
        Assert.assertEquals(DataUtils.getDate("01.02.2018"), newEntity.getReportDate());
        Assert.assertEquals(1, databaseActivity.numberOfInserts());
    }

    @Test
    @Transactional
    public void shouldNotCreateHistory() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        savingEntity.setReportDate(DataUtils.getDate("01.02.2018"));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(0, databaseActivity.numberOfInserts());
    }

    @Test
    @Transactional
    public void shouldUpdateHistory() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        savingEntity.setDataValue("amount", new DataDoubleValue(6000.0));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(6000.0, newEntity.getEl("amount"));
        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(1, databaseActivity.numberOfUpdates());

        DataEntity loadedEntity = dataEntityDao.load(newEntity.getId(), savingEntity.getCreditorId(), newEntity.getReportDate());
        Assert.assertEquals(6000.0, loadedEntity.getEl("amount"));
        Assert.assertEquals(newEntity.getEl("primary_contract.no"), loadedEntity.getEl("primary_contract.no"));
        Assert.assertEquals(newEntity.getEl("primary_contract.date"), loadedEntity.getEl("primary_contract.date"));
    }

    @Test
    @Transactional
    public void shouldNotUpdateHistory() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(0, databaseActivity.numberOfUpdates());

        DataEntity loadedEntity = dataEntityDao.load(newEntity.getId(), savingEntity.getCreditorId(), newEntity.getReportDate());
        Assert.assertEquals(5000.0, loadedEntity.getEl("amount"));
        Assert.assertEquals(newEntity.getEl("primary_contract.no"), loadedEntity.getEl("primary_contract.no"));
        Assert.assertEquals(newEntity.getEl("primary_contract.date"), loadedEntity.getEl("primary_contract.date"));
    }

    @Test
    @Transactional
    public void shouldCreateHistoryRDLess() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        savingEntity.setDataValue("amount", new DataDoubleValue(6000.0));
        savingEntity.setReportDate(DataUtils.getDate("01.12.2017"));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(6000.0, newEntity.getEl("amount"));
        Assert.assertEquals(DataUtils.getDate("01.12.2017"), newEntity.getReportDate());
        Assert.assertEquals(1, databaseActivity.numberOfInserts());
    }

    @Test
    @Transactional
    public void shouldNotHistoryButUpdateRDLess() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        savingEntity.setReportDate(DataUtils.getDate("01.12.2017"));

        databaseActivity.reset();
        DataEntity newEntity = bootstrapEngine.process(savingEntity);

        Assert.assertEquals(5000.0, newEntity.getEl("amount"));
        Assert.assertEquals(DataUtils.getDate("01.12.2017"), newEntity.getReportDate());
        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(1, databaseActivity.numberOfUpdates());
    }

    @Test
    @Transactional
    public void shouldCheckSubsetOf1Class() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));
        DataEntity savedEntity = entity.clone();
        savedEntity.setDataValue("interest_rate_yearly", new DataDoubleValue(12.0));

        DataEntity appliedEntity = bootstrapEngine.process(savedEntity);
        databaseActivity.reset();

        entity.setReportDate(DataUtils.getDate("01.02.2018"));
        bootstrapEngine.process(entity);

        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(0, databaseActivity.numberOfUpdates());
    }

    @Test
    @Transactional
    public void shouldCheckSubsetOf2Class() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));
        DataEntity savedEntity = entity.clone();
        savedEntity.setDataValue("interest_rate_yearly", new DataDoubleValue(12.0));

        DataEntity appliedEntity = bootstrapEngine.process(savedEntity);
        databaseActivity.reset();

        bootstrapEngine.process(entity);

        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(0, databaseActivity.numberOfUpdates());
    }

    @Test
    @Transactional
    public void shouldCheckSubsetOf3Class() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("basic/newhistory/AmountedSCredit.xml"))
                .withMeta(metaCredit)
                .read();

        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));
        DataEntity savedEntity = entity.clone();
        savedEntity.setDataValue("interest_rate_yearly", new DataDoubleValue(12.0));

        DataEntity appliedEntity = bootstrapEngine.process(savedEntity);
        databaseActivity.reset();

        entity.setReportDate(DataUtils.getDate("01.12.2017"));
        bootstrapEngine.process(entity);

        Assert.assertEquals(0, databaseActivity.numberOfInserts());
        Assert.assertEquals(1, databaseActivity.numberOfUpdates());
    }




}
