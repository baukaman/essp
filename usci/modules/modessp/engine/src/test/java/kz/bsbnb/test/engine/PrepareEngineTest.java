package kz.bsbnb.test.engine;

import kz.bsbnb.DataDoubleValue;
import kz.bsbnb.DataEntity;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.engine.PrepareEngine;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml"})
public class PrepareEngineTest extends FunctionalTest {

    @Autowired
    PrepareEngine prepareEngine;

    @Autowired
    DatabaseActivity databaseActivity;

    @Before
    public void setUp() throws Exception {
        databaseActivity.reset();
    }

    @Test
    public void testNoCredit() throws Exception {
        DataEntity savingEntity = new ThreePartReader()
                .withSource(getInputStream("dao/SearchCredit.xml"))
                .withMeta(metaCredit).read();

        DataEntity preparedEntity = prepareEngine.process(savingEntity);
        Assert.assertEquals(0, preparedEntity.getId());
        Assert.assertEquals(1, databaseActivity.numberOfSelects());
    }
}