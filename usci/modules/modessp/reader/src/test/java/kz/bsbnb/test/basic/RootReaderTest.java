package kz.bsbnb.test.basic;

import kz.bsbnb.DataEntity;
import kz.bsbnb.reader.RootReader;
import kz.bsbnb.testing.FunctionalTest;
import kz.bsbnb.usci.eav.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;


public class RootReaderTest extends FunctionalTest {
    RootReader reader = new RootReader();

    @Test
    public void mustReadValues() throws Exception {
        InputStream inputStream = getInputStream("basic/credit.xml");
        reader.withSource(inputStream)
                .withMeta(metaCredit);
        DataEntity entity = reader.read();
        Assert.assertNotNull(entity);
        Assert.assertEquals(5000.0, entity.getEl("amount"));
        Assert.assertEquals(DataUtils.getDate("01.01.2018"), entity.getEl("maturity_date"));
        Assert.assertEquals("KZT-001", entity.getEl("primary_contract.no"));
        Assert.assertEquals(DataUtils.getDate("12.01.2018"), entity.getEl("primary_contract.date"));
        Assert.assertEquals(2, entity.getEls("{count}pledges"));
    }
}