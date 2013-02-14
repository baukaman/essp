package kz.bsbnb.usci.eav.relation;

import junit.framework.Assert;
import kz.bsbnb.usci.eav.model.BaseEntity;
import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.batchdata.impl.BatchValue;
import kz.bsbnb.usci.eav.model.metadata.DataTypes;
import kz.bsbnb.usci.eav.model.metadata.IMetaFactory;
import kz.bsbnb.usci.eav.model.metadata.impl.BasicMetaClassRepositoryImpl;
import kz.bsbnb.usci.eav.model.metadata.type.impl.MetaClass;
import kz.bsbnb.usci.eav.model.metadata.type.impl.MetaClassArray;
import kz.bsbnb.usci.eav.model.metadata.type.impl.MetaValue;
import kz.bsbnb.usci.eav.model.metadata.type.impl.MetaValueArray;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav.persistance.storage.IStorage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.util.Date;

/**
 * @author k.tulbassiyev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class RelationTest1
{
    private final Logger logger = LoggerFactory.getLogger(RelationTest1.class);

    @Autowired
    IStorage storage;

    @Autowired
    IMetaClassDao metaClassDao;

    @Autowired
    IBatchDao batchDao;

    @Autowired
    IBaseEntityDao baseEntityDao;

    @Autowired
    IMetaFactory metaFactory;

    @Test
    public void MetaClassBaseEntityRelation()
    {
        if(!storage.testConnection())
        {
            logger.error("Can't connect to storage.");
            System.exit(1);
        }

        storage.clear();
        storage.initialize();

        Batch batch = new Batch(new Timestamp(new Date().getTime()));
        batchDao.save(batch);

        MetaClass metaStreet = new MetaClass("street");
        metaStreet.setMemberType("lang", new MetaValue(DataTypes.STRING, false, true));
        metaStreet.setMemberType("value", new MetaValue(DataTypes.STRING, false, true));

        MetaClass metaHouse = new MetaClass("house");
        metaHouse.setMemberType("value", new MetaValueArray(DataTypes.INTEGER, false, true));

        MetaClass metaAddress = new MetaClass("address");
        metaAddress.setMemberType("country", new MetaValue(DataTypes.STRING, false, true));
        metaAddress.setMemberType("city", new MetaValue(DataTypes.STRING, false, true));
        metaAddress.setMemberType("house", metaHouse);
        metaAddress.setMemberType("street", metaStreet);

        MetaClass metaDocument = new MetaClass("document");
        metaDocument.setMemberType("type", new MetaValue(DataTypes.STRING, true, false));
        metaDocument.setMemberType("no", new MetaValue(DataTypes.STRING, true, false));

        MetaClass metaDocuments = new MetaClass("documents");
        metaDocuments.setMemberType("document", new MetaClassArray(metaDocument));

        MetaClass metaName = new MetaClass("name");
        metaName.setMemberType("firstname", new MetaValue(DataTypes.STRING, true, false));
        metaName.setMemberType("lastname", new MetaValue(DataTypes.STRING, true, false));

        MetaClass metaSubject = new MetaClass("subject");
        metaSubject.setMemberType("name", metaName);
        metaSubject.setMemberType("documents", metaDocuments);
        metaSubject.setMemberType("address", metaAddress);

        MetaClass metaContract = new MetaClass("contract");
        metaContract.setMemberType("no", new MetaValue(DataTypes.INTEGER, true, false));
        metaContract.setMemberType("subject", metaSubject);

        metaClassDao.save(metaContract);

        // ----------------------------------------------------------------------

        BaseEntity streetEntity = metaFactory.getBaseEntity("street", batch);
        streetEntity.set("lang", 1, "KAZ");
        streetEntity.set("value", 1, "ABAY");

        BaseEntity houseEntity = metaFactory.getBaseEntity("house", batch);
        houseEntity.getBatchValueArray("value").add(new BatchValue(batch, 2, 111));
        houseEntity.getBatchValueArray("value").add(new BatchValue(batch, 2, 222));
        houseEntity.getBatchValueArray("value").add(new BatchValue(batch, 2, 333));

        BaseEntity addressEntity = metaFactory.getBaseEntity("address", batch);
        addressEntity.set("country", 3, "KAZAKHSTAN");
        addressEntity.set("city", 3, "ALMATY");
        addressEntity.set("street", 3, streetEntity);
        addressEntity.set("house", 3, houseEntity);

        BaseEntity documentEntity1 = metaFactory.getBaseEntity("document", batch);
        documentEntity1.set("type", 4, "RNN");
        documentEntity1.set("no", 4, "1234567890");

        BaseEntity documentEntity2 = metaFactory.getBaseEntity("document", batch);
        documentEntity1.set("type", 4, "PASSPORT");
        documentEntity1.set("no", 4, "0987654321");

        BaseEntity documentsEntity = metaFactory.getBaseEntity("documents", batch);
        documentsEntity.getBatchValueArray("document").add(new BatchValue(batch, 5, documentEntity1));
        documentsEntity.getBatchValueArray("document").add(new BatchValue(batch, 5, documentEntity2));

        BaseEntity nameEntity = metaFactory.getBaseEntity("name", batch);
        nameEntity.set("firstname", 6, "KANAT");
        nameEntity.set("lastname", 6, "TULBASSIYEV");

        BaseEntity subjectEntity = metaFactory.getBaseEntity("subject", batch);
        subjectEntity.set("name", 7, nameEntity);
        subjectEntity.set("documents", 7, documentsEntity);
        subjectEntity.set("address", 7, addressEntity);

        BaseEntity contractEntity = metaFactory.getBaseEntity("contract", batch);
        contractEntity.set("no", 8, 12345);
        contractEntity.set("subject", 8, subjectEntity);

        long id = baseEntityDao.save(contractEntity);

        BaseEntity contractEntityTest = baseEntityDao.load(id);

        Assert.assertEquals(contractEntity, contractEntityTest);
    }
}