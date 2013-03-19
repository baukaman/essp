package kz.bsbnb.usci.eav.test.relation;

import junit.framework.Assert;
import kz.bsbnb.usci.eav.test.GenericTestCase;
import kz.bsbnb.usci.eav.comparator.IBaseEntityComparator;
import kz.bsbnb.usci.eav.comparator.impl.BasicBaseEntityComparator;
import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.base.impl.BaseValue;
import kz.bsbnb.usci.eav.model.type.DataTypes;
import kz.bsbnb.usci.eav.factory.IMetaFactory;
import kz.bsbnb.usci.eav.model.meta.impl.MetaAttribute;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav.persistance.dao.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.Date;

import static junit.framework.Assert.fail;

/**
 * @author k.tulbassiyev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class RelationTest1  extends GenericTestCase
{
    private final Logger logger = LoggerFactory.getLogger(RelationTest1.class);

    @Autowired
    IMetaClassDao metaClassDao;

    @Autowired
    IBatchDao batchDao;

    @Autowired
    IBaseEntityDao baseEntityDao;

    @Autowired
    IMetaFactory metaFactory;

    @Autowired
    IBaseEntitySearcherPool baseEntitySearcherPool;

    private MetaClass generateMetaClass()
    {
        MetaClass metaStreetHolder = new MetaClass("street");
        metaStreetHolder.setMetaAttribute("lang",
                new MetaAttribute(false, false, new MetaValue(DataTypes.STRING)));
        metaStreetHolder.setMetaAttribute("value",
                new MetaAttribute(false, false, new MetaValue(DataTypes.STRING)));

        MetaClass metaHouseHolder = new MetaClass("house");
        metaHouseHolder.setMetaAttribute("value",
                new MetaAttribute(false, true, new MetaSet(new MetaValue(DataTypes.INTEGER))));

        MetaClass metaAddressHolder = new MetaClass("address");
        metaAddressHolder.setMetaAttribute("country",
                new MetaAttribute(false, true, new MetaValue(DataTypes.STRING)));
        metaAddressHolder.setMetaAttribute("city",
                new MetaAttribute(false, true, new MetaValue(DataTypes.STRING)));
        metaAddressHolder.setMetaAttribute("house",
                new MetaAttribute(metaHouseHolder));
        metaAddressHolder.setMetaAttribute("street",
                new MetaAttribute(metaStreetHolder));

        MetaClass metaDocumentHolder = new MetaClass("document");
        metaDocumentHolder.setMetaAttribute("type",
                new MetaAttribute(true, false, new MetaValue(DataTypes.STRING)));
        metaDocumentHolder.setMetaAttribute("no",
                new MetaAttribute(true, false, new MetaValue(DataTypes.STRING)));

        MetaClass metaDocumentsHolder = new MetaClass("documents");
        metaDocumentsHolder.setMetaAttribute("document",
                new MetaAttribute(new MetaSet(metaDocumentHolder)));

        MetaClass metaNameHolder = new MetaClass("name");
        metaNameHolder.setMetaAttribute("firstname",
                new MetaAttribute(true, false, new MetaValue(DataTypes.STRING)));
        metaNameHolder.setMetaAttribute("lastname",
                new MetaAttribute(true, false, new MetaValue(DataTypes.STRING)));

        MetaClass metaSubjectHolder = new MetaClass("subject");
        metaSubjectHolder.setMetaAttribute("name",
                new MetaAttribute(true, false, metaNameHolder));
        metaSubjectHolder.setMetaAttribute("documents",
                new MetaAttribute(metaDocumentsHolder));
        metaSubjectHolder.setMetaAttribute("address",
                new MetaAttribute(metaAddressHolder));

        MetaClass metaContractHolder = new MetaClass("contract");
        metaContractHolder.setMetaAttribute("no",
                new MetaAttribute(true, false, new MetaValue(DataTypes.INTEGER)));
        metaContractHolder.setMetaAttribute("subject",
                new MetaAttribute(true, false, metaSubjectHolder));

        return metaContractHolder;
    }

    private BaseEntity generateBaseEntity(Batch batch)
    {
        BaseEntity streetEntity = metaFactory.getBaseEntity("street");
        streetEntity.put("lang", new BaseValue(batch, 1, "KAZ"));
        streetEntity.put("value", new BaseValue(batch, 1, "ABAY"));

        BaseEntity houseEntity = metaFactory.getBaseEntity("house");
        BaseSet houseSet = new BaseSet(((MetaSet)(houseEntity.getMemberType("value"))).getMemberType());
        houseSet.put(new BaseValue(batch, 2, 111));
        houseSet.put(new BaseValue(batch, 2, 222));
        houseSet.put(new BaseValue(batch, 2, 333));
        houseEntity.put("value", new BaseValue(batch, 2, houseSet));

        BaseEntity addressEntity = metaFactory.getBaseEntity("address");
        addressEntity.put("country", new BaseValue(batch, 3, "KAZAKHSTAN"));
        addressEntity.put("city", new BaseValue(batch, 3, "ALMATY"));
        addressEntity.put("street", new BaseValue(batch, 3, streetEntity));
        addressEntity.put("house", new BaseValue(batch, 3, houseEntity));

        BaseEntity documentEntity1 = metaFactory.getBaseEntity("document");
        documentEntity1.put("type", new BaseValue(batch, 4, "RNN"));
        documentEntity1.put("no", new BaseValue(batch, 4, "1234567890"));

        BaseEntity documentEntity2 = metaFactory.getBaseEntity("document");
        documentEntity2.put("type", new BaseValue(batch, 4, "PASSPORT"));
        documentEntity2.put("no", new BaseValue(batch, 4, "0987654321"));

        BaseEntity documentsEntity = metaFactory.getBaseEntity("documents");
        BaseSet documentsSet = new BaseSet(((MetaSet)(documentsEntity.getMemberType("document"))).getMemberType());
        documentsSet.put(new BaseValue(batch, 5, documentEntity1));
        documentsSet.put(new BaseValue(batch, 5, documentEntity2));
        documentsEntity.put("document", new BaseValue(batch, 5, documentsSet));

        BaseEntity nameEntity = metaFactory.getBaseEntity("name");
        nameEntity.put("firstname", new BaseValue(batch, 6, "KANAT"));
        nameEntity.put("lastname", new BaseValue(batch, 6, "TULBASSIYEV"));

        BaseEntity subjectEntity = metaFactory.getBaseEntity("subject");
        subjectEntity.put("name", new BaseValue(batch, 7, nameEntity));
        subjectEntity.put("documents", new BaseValue(batch, 7, documentsEntity));
        subjectEntity.put("address", new BaseValue(batch, 7, addressEntity));

        BaseEntity contractEntity = metaFactory.getBaseEntity("contract");
        contractEntity.put("no", new BaseValue(batch, 8, 12345));
        contractEntity.put("subject", new BaseValue(batch, 8, subjectEntity));

        return contractEntity;
    }

    @Test
    public void MetaClassBaseEntityRelation()
    {
        Batch batch = new Batch(new Timestamp(new Date().getTime()), new java.sql.Date(new Date().getTime()));
        batchDao.save(batch);

        metaClassDao.save(generateMetaClass());

        BaseEntity contractEntity = generateBaseEntity(batch);

        long id = baseEntityDao.save(contractEntity);

        BaseEntity contractEntityTest = baseEntityDao.load(id);

        //TODO: fix this
        Assert.assertTrue(contractEntity.equals(contractEntityTest));
    }

    @Test
    public void equalsTest()
    {
        Batch batch = new Batch(new Timestamp(new Date().getTime()), new java.sql.Date(new Date().getTime()));
        batchDao.save(batch);

        metaClassDao.save(generateMetaClass());

        BaseEntity contractEntity = generateBaseEntity(batch);
        BaseEntity contractEntity1 = generateBaseEntity(batch);

        Assert.assertEquals(contractEntity, contractEntity1);

        BaseEntity subject = (BaseEntity)contractEntity1.getBaseValue("subject").getValue();

        BaseEntity nameEntity = (BaseEntity)subject.getBaseValue("name").getValue();

        nameEntity.put("firstname", new BaseValue(batch, 6, "KANAT_some_fix"));

        Assert.assertFalse(contractEntity.equals(contractEntity1));
    }

    @Test
    public void compareTest()
    {
        if (baseEntitySearcherPool == null)
        {
            fail("No base entity searcher found in spring config!");
        }
        Batch batch = new Batch(new Timestamp(new Date().getTime()), new java.sql.Date(new Date().getTime()));
        batchDao.save(batch);

        metaClassDao.save(generateMetaClass());

        BaseEntity contractEntity = generateBaseEntity(batch);
        BaseEntity contractEntity1 = generateBaseEntity(batch);

        //IBaseEntitySearcher baseEntitySearcher = baseEntitySearcherPool.
          //      getSearcher(contractEntity.getMeta().getClassName());

        IBaseEntityComparator baseEntityComparator = new BasicBaseEntityComparator();

        logger.debug("Trying same objects");
        Assert.assertTrue(baseEntityComparator.compare(contractEntity, contractEntity1));

        BaseEntity subject = (BaseEntity)contractEntity1.getBaseValue("subject").getValue();

        BaseEntity nameEntity = (BaseEntity)subject.getBaseValue("name").getValue();

        nameEntity.put("firstname", new BaseValue(batch, 6, "KANAT_some_fix"));

        logger.debug("Trying changed first name objects");
        Assert.assertFalse(baseEntityComparator.compare(contractEntity, contractEntity1));
    }
}
