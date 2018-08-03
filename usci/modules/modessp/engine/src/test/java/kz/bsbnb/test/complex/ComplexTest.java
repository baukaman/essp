package kz.bsbnb.test.complex;

import kz.bsbnb.DataComplexValue;
import kz.bsbnb.DataEntity;
import kz.bsbnb.DataStringValue;
import kz.bsbnb.SavingInfo;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.IRefEngine;
import kz.bsbnb.exception.RefNotFoundException;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.test.EngineTestBase;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.fail;

public class ComplexTest extends EngineTestBase {

    @Autowired
    ThreePartReader reader;

    @Autowired
    IRefEngine refEngine;

    @Autowired
    SavingInfo savingInfo;

    @Test(expected = RefNotFoundException.class)
    @Transactional
    public void shouldFailRefNotFound() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("complex/CurrencyCredit.xml"))
                .withMeta(metaCredit)
                .read();

        refEngine.reloadCache();
        bootstrapEngine.process(entity);
        fail("should have thrown ref not found error");
    }

    @Test
    @Transactional
    public void shouldSaveNewCredit() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("complex/CurrencyCredit.xml"))
                .withMeta(metaCredit)
                .read();


        //List<DataEntity> refs = reader.getRefs();
        //refEngine.setSearchEntityDao(new StaticSearchEntityDao(refs));

        DataEntity appliedEntity = bootstrapEngine.process(entity);
        DataEntity currency = (DataEntity) appliedEntity.getBaseValue("currency").getValue();
        Assert.assertEquals(101, currency.getId());
        Assert.assertTrue(appliedEntity.getId() > 0);
    }

    @Test
    @Transactional
    public void shouldCreateNewHistory() throws Exception {
        DataEntity savingEntity = reader.withSource(getInputStream("complex/CurrencyCredit.xml"))
                .withMeta(metaCredit)
                .read();

        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));

        DataEntity appliedEntity = bootstrapEngine.process(savingEntity);
        DataEntity newCurrency = new DataEntity(((MetaClass) metaCredit.getEl("currency")));
        newCurrency.setDataValue("short_name",new DataStringValue("EUR"));
        savingEntity.setDataValue("currency", new DataComplexValue(newCurrency));
        savingEntity.setReportDate(DataUtils.getDate("01.02.2018"));
        databaseActivity.reset();
        bootstrapEngine.process(savingEntity);
        Assert.assertEquals(1, databaseActivity.numberOfInserts());
    }
}
