package kz.bsbnb.test;

import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.engine.BootstrapEngine;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml","/applicationContextEngineTest.xml"})
public class EngineTestBase extends FunctionalTest {
    @Autowired
    protected DatabaseActivity databaseActivity;

    @Before
    public void setUp() throws Exception {
        databaseActivity.reset();
    }

    @Autowired
    protected DataEntityDao dataEntityDao;

    @Autowired
    protected BootstrapEngine bootstrapEngine;

    @Autowired
    protected ThreePartReader reader;
}
