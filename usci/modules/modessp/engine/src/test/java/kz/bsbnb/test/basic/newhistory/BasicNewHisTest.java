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

        //Assert.assertEquals(5000.0, appliedEntity.getEl("amount"));
        Assert.assertEquals(1, databaseActivity.numberOfInserts());
    }

    @Test
    @Transactional
    public void shouldNotCreateHisotry() throws Exception {
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
}
