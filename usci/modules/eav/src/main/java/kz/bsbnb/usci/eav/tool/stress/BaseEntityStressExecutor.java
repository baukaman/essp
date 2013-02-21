package kz.bsbnb.usci.eav.tool.stress;

import kz.bsbnb.usci.eav.model.BaseEntity;
import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.metadata.type.impl.MetaClass;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav.persistance.storage.IStorage;
import kz.bsbnb.usci.eav.tool.generator.data.impl.BaseEntityGenerator;
import kz.bsbnb.usci.eav.tool.generator.data.impl.MetaClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class BaseEntityStressExecutor
{
    private final static Logger logger = LoggerFactory.getLogger(BaseEntityStressExecutor.class);

    private final static int dataSize = 100;

    public static void main(String[] args)
    {
        System.out.println("Test started at: " + Calendar.getInstance().getTime());

        MetaClassGenerator metaClassGenerator = new MetaClassGenerator(25, 2);
        BaseEntityGenerator baseEntityGenerator = new BaseEntityGenerator();

        ClassPathXmlApplicationContext ctx
                = new ClassPathXmlApplicationContext("stressApplicationContext.xml");

        IStorage storage = ctx.getBean(IStorage.class);
        IMetaClassDao metaClassDao = ctx.getBean(IMetaClassDao.class);
        IBaseEntityDao baseEntityDao = ctx.getBean(IBaseEntityDao.class);
        IBatchDao batchDao = ctx.getBean(IBatchDao.class);

        ArrayList<MetaClass> data = new ArrayList<MetaClass>();

        try
        {
            if(!storage.testConnection())
            {
                logger.error("Can't connect to storage.");
                System.exit(1);
            }

            storage.clear();
            storage.initialize();

            System.out.println("Generation: ..........");
            System.out.print(  "Progress  : ");

            for(int i = 0; i < dataSize; i++)
            {
                MetaClass metaClass = metaClassGenerator.generateMetaClass(0);

                long metaClassId = metaClassDao.save(metaClass);

                metaClass = metaClassDao.load(metaClassId);

                data.add(i, metaClass);

                if(i % (dataSize / 10) == 0)
                    System.out.print(".");
            }

            System.out.println();

            // --------

            Batch batch = new Batch(new Timestamp(new Date().getTime()));

            long batchId = batchDao.save(batch);

            batch = batchDao.load(batchId);

            long index = 0L;

            for (MetaClass metaClass : data)
            {
                BaseEntity baseEntity = baseEntityGenerator.generateBaseEntity(batch, metaClass, ++index);
                baseEntityDao.save(baseEntity);
            }
        }
        finally
        {
            storage.clear();
        }
    }
}