package kz.bsbnb.test.dao;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.dao.ISearchEntityDao;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml","/applicationContextEngineTest.xml"})
public class SearchEntityDaoTest extends FunctionalTest {

    @Autowired
    ThreePartReader reader;

    @Autowired
    DataEntityDao entityDao;

    @Autowired
    ISearchEntityDao searchEntityDao;

    @Autowired
    DatabaseActivity databaseActivity;

    @Before
    public void setUp() throws Exception {
        databaseActivity.reset();
    }

    @Test
    @Transactional
    public void testPrimaryContractSearch() throws Exception {

        reader.withSource(getInputStream("dao/SimplePrimaryContract.xml")).withMeta(metaPrimaryContract);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaPrimaryContract));
        DataEntity primaryContract = reader.read();
        entityDao.insertNewEntity(primaryContract);

        long id = primaryContract.getId();
        primaryContract.setId(0);
        long searchId = searchEntityDao.search(primaryContract);
        Assert.assertEquals(id, searchId);
    }

    @Test
    @Transactional
    public void testCreditSearch() throws Exception {
        reader.withSource(getInputStream("dao/SearchCredit.xml"))
                .withMeta(metaCredit);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaCredit));
        DataEntity credit = reader.read();
        DataEntity primaryContract = (DataEntity) credit.getBaseValue("primary_contract").getValue();
        primaryContract.setReportDate(credit.getReportDate());
        primaryContract.setCreditorId(credit.getCreditorId());
        entityDao.insertNewEntity(primaryContract);
        entityDao.insertNewEntity(credit);

        long savedId = credit.getId();
        credit.setId(0);
        long searchId = searchEntityDao.search(credit);
        Assert.assertEquals(savedId, searchId);
        Assert.assertEquals(1, databaseActivity.numberOfSelects());
    }

    @Test
    @Transactional
    public void testNoCredit() throws Exception {
        reader.withSource(getInputStream("dao/SearchCredit.xml")).withMeta(metaCredit);

        long searchId = searchEntityDao.search(reader.read());
        Assert.assertEquals(0, searchId);

    }
}