package kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom;

import kz.bsbnb.usci.eav_model.model.Batch;
import kz.bsbnb.usci.eav_model.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav_persistance.persistance.storage.IStorage;
import kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom.data.AttributeTree;
import kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom.data.BaseEntityGenerator;
import kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom.data.MetaClassGenerator;
import kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom.xml.impl.BaseEntityXmlGenerator;
import kz.bsbnb.usci.eav_persistance.tool.generator.nonrandom.helper.TreeGenerator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author abukabayev
 */
public class GenerateInsertXml
{
    private static final int DATA_SIZE = 1;   // Number of entities to generate
    private static String OS = System.getProperty("os.name").toLowerCase();
    private final static String FILE_PATH_UNIX = "/opt/xmls/test.xml";
    private final static String FILE_PATH_WINDOWS = "D:/DevTemp/test.xml";

    public static void main(String args[])
    {
        BaseEntityXmlGenerator baseEntityXmlGenerator = new BaseEntityXmlGenerator();

        MetaClassGenerator metaClassGenerator = new MetaClassGenerator();
        BaseEntityGenerator baseEntityGenerator = new BaseEntityGenerator();

        ArrayList<MetaClass> data = new ArrayList<MetaClass>();
        ArrayList<BaseEntity> entities = new ArrayList<BaseEntity>();

        ClassPathXmlApplicationContext ctx
                = new ClassPathXmlApplicationContext("stressApplicationContext.xml");

        IStorage storage = ctx.getBean(IStorage.class);
        IMetaClassDao metaClassDao = ctx.getBean(IMetaClassDao.class);
        IBaseEntityDao baseEntityDao = ctx.getBean(IBaseEntityDao.class);
        IBatchDao batchDao = ctx.getBean(IBatchDao.class);

        AttributeTree tree = new AttributeTree("entities",null);
        TreeGenerator helper = new TreeGenerator();

        if(!storage.testConnection())
        {
            //logger.error("Can't connect to storage.");
            System.exit(1);
        }

        storage.clear();
        storage.initialize();

        try {

            tree = helper.generateTree(tree);
            tree = tree.getChildren().get(0);

            for (int i=1; i<=DATA_SIZE; i++){

                MetaClass metaClass = metaClassGenerator.generateMetaClass(tree,i);

                Long id = metaClassDao.save(metaClass);
                data.add(metaClassDao.load(id));

            }

            for (MetaClass m:metaClassGenerator.getMetaClasses()){
                   metaClassDao.save(m);
            }

            Batch batch = new Batch(new Timestamp(new Date().getTime()), new java.sql.Date(new Date().getTime()));
            long batchId = batchDao.save(batch);

            batch = batchDao.load(batchId);
            int index=0;
            for (MetaClass aData : data) {
                BaseEntity baseEntity = baseEntityGenerator.generateBaseEntity(batch, aData, ++index);
                entities.add(baseEntity);
               Long id = baseEntityDao.save(baseEntity);
               BaseEntity bb = baseEntityDao.load(id);
            }

            Document document = baseEntityXmlGenerator.getGeneratedDocument(entities);

            String filePath;
            if (isWindows()) {
                filePath = FILE_PATH_WINDOWS;
            } else if (isMac()) {
                throw new RuntimeException("OS is not support.");
            } else if (isUnix()) {
                filePath = FILE_PATH_UNIX;
            } else if (isSolaris()) {
                throw new RuntimeException("OS is not support.");
            } else {
                throw new RuntimeException("OS is not support.");
            }

            baseEntityXmlGenerator.writeToXml(document, filePath);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMac() {
        return (OS.contains("mac"));
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0 );
    }

    public static boolean isSolaris() {
        return (OS.contains("sunos"));
    }
}