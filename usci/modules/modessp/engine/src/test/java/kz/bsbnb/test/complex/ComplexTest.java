package kz.bsbnb.test.complex;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.ISearchEntityDao;
import kz.bsbnb.dao.impl.StaticSearchEntityDao;
import kz.bsbnb.engine.RefEngine;
import kz.bsbnb.exception.RefNotFoundException;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.test.EngineTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComplexTest extends EngineTestBase {

    @Autowired
    ThreePartReader reader;

    @Autowired
    RefEngine refEngine;

    @Test(expected = RefNotFoundException.class)
    @Transactional
    public void shouldFailRefNotFound() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("complex/CurrencyCredit.xml"))
                .withMeta(metaCredit)
                .read();

        bootstrapEngine.process(entity);
        fail("should have thrown ref not found error");
    }

    @Test
    @Transactional
    @DirtiesContext
    public void shouldCreateHistory() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("complex/CurrencyCredit.xml"))
                .withMeta(metaCredit)
                .read();


        List<DataEntity> refs = reader.getRefs();
        refEngine.setSearchEntityDao(new StaticSearchEntityDao(refs));

        DataEntity appliedEntity = bootstrapEngine.process(entity);
        DataEntity currency = (DataEntity) appliedEntity.getBaseValue("currency").getValue();
        Assert.assertEquals(101, currency.getId());
        Assert.assertTrue(appliedEntity.getId() > 0);
    }


}
