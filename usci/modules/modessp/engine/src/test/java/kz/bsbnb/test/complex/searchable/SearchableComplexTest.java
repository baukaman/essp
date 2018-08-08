package kz.bsbnb.test.complex.searchable;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.test.EngineTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

public class SearchableComplexTest extends EngineTestBase {

    @Test
    @Transactional
    public void shouldSaveComplex() throws Exception {
        DataEntity entity = reader.withSource(getInputStream("complex/searchable/SubjectCredit.xml"))
                .withMeta(metaCredit)
                .read();

        dataEntityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));


        DataEntity appliedEntity = bootstrapEngine.process(entity);
        DataEntity currency = (DataEntity) appliedEntity.getDataValue("currency").getValue();
        //Assert.assertEquals(101, currency.getId());
        Assert.assertTrue(appliedEntity.getId() > 0);
        DataEntity loaded = dataEntityDao.load(appliedEntity.getId(), appliedEntity.getCreditorId(), appliedEntity.getReportDate());
        Assert.assertEquals(2, loaded.getEls("{count}subject.docs"));
    }
}
