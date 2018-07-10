package kz.bsbnb.test.dao;

import kz.bsbnb.DataEntity;
import kz.bsbnb.DataStringValue;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.dao.SearchEntityDao;
import kz.bsbnb.dao.impl.StaticMetaClassDaoImpl;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml"})
public class SearchEntityDaoTest extends FunctionalTest {

    ThreePartReader reader;

    @Autowired
    DataEntityDao entityDao;

    @Autowired
    SearchEntityDao searchEntityDao;

    @Test
    @Transactional
    public void testPrimaryContractSearch() throws Exception {

        reader = new ThreePartReader()
                .withSource(getInputStream("dao/SimplePrimaryContract.xml"))
                .withMeta(metaPrimaryContract);

        entityDao.setMetaSource(new StaticMetaClassDaoImpl(metaPrimaryContract));
        DataEntity primaryContract = reader.read();
        entityDao.insert(primaryContract);

        long id = primaryContract.getId();
        primaryContract.setId(0);
        long searchId = searchEntityDao.search(primaryContract);
        Assert.assertEquals(id, searchId);
    }
}