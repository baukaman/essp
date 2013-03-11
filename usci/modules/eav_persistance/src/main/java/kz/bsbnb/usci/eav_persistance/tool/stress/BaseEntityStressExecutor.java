package kz.bsbnb.usci.eav_persistance.tool.stress;

import kz.bsbnb.usci.eav_model.model.Batch;
import kz.bsbnb.usci.eav_model.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav_model.util.SetUtils;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav_persistance.persistance.impl.db.JDBCSupport;
import kz.bsbnb.usci.eav_persistance.persistance.storage.IStorage;
import kz.bsbnb.usci.eav_persistance.stats.QueryEntry;
import kz.bsbnb.usci.eav_persistance.stats.SQLQueriesStats;
import kz.bsbnb.usci.eav_persistance.tool.generator.data.impl.BaseEntityGenerator;
import kz.bsbnb.usci.eav_persistance.tool.generator.data.impl.MetaClassGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BaseEntityStressExecutor
{
    private final static Logger logger = LoggerFactory.getLogger(BaseEntityStressExecutor.class);

    private final static int dataSize = 10;

    public static void main(String[] args)
    {
        System.out.println("Test started at: " + Calendar.getInstance().getTime());

        MetaClassGenerator metaClassGenerator = new MetaClassGenerator(25, 20, 2, 4);
        BaseEntityGenerator baseEntityGenerator = new BaseEntityGenerator();

        ClassPathXmlApplicationContext ctx
                = new ClassPathXmlApplicationContext("stressApplicationContext.xml");

        IStorage storage = ctx.getBean(IStorage.class);
        IMetaClassDao metaClassDao = ctx.getBean(IMetaClassDao.class);
        IBaseEntityDao baseEntityDao = ctx.getBean(IBaseEntityDao.class);
        IBatchDao batchDao = ctx.getBean(IBatchDao.class);
        SQLQueriesStats stats = ctx.getBean(SQLQueriesStats.class);

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

            System.out.println("Generation MetaClasses : ..........");
            System.out.print(  "Progress               : ");

            for(int i = 0; i < dataSize; i++)
            {
                double t1;
                double t2;

                t1 = System.nanoTime();
                MetaClass metaClass = metaClassGenerator.generateMetaClass();
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_META_CLASS_GENERATION", t2);

                t1 = System.nanoTime();
                long metaClassId = metaClassDao.save(metaClass);
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_META_CLASS_SAVE", t2);

                t1 = System.nanoTime();
                metaClass = metaClassDao.load(metaClassId);
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_META_CLASS_LOAD", t2);

                data.add(i, metaClass);

                //if(i % (dataSize / 10) == 0)
                    System.out.print(".");
            }

            System.out.println();

            // --------

            Batch batch = new Batch(new Timestamp(new Date().getTime()));

            long batchId = batchDao.save(batch);

            batch = batchDao.load(batchId);

            long index = 0L;

            System.out.println("Generation BaseEntities: ..........");
            System.out.print(  "Progress               : ");

            int i = 0;
            for (MetaClass metaClass : data)
            {
                double t1;
                double t2;

                t1 = System.nanoTime();
                BaseEntity baseEntityCreate = baseEntityGenerator.generateBaseEntity(batch, metaClass, ++index);
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_BASE_ENTITY_GENERATION", t2);

                t1 = System.nanoTime();
                long baseEntityId = baseEntityDao.save(baseEntityCreate);
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_BASE_ENTITY_SAVE", t2);

                t1 = System.nanoTime();
                BaseEntity baseEntityLoad = baseEntityDao.load(baseEntityId);
                t2 = (System.nanoTime() - t1) / 1000000;

                stats.put("_BASE_ENTITY_LOAD", t2);

                i++;
                //if(i % (dataSize / 10) == 0)
                    System.out.print(".");
            }
        }
        finally
        {
            metaClassGenerator.printStats();

            SQLQueriesStats sqlStats = ctx.getBean(SQLQueriesStats.class);
            storage.clear();

            if(sqlStats != null)
            {
                System.out.println();
                System.out.println("+---------+------------------+------------------------+");
                System.out.println("|  count  |        avg       |          total         |");
                System.out.println("+---------+------------------+------------------------+");

                List<String> queries = SetUtils.asSortedList(sqlStats.getStats().keySet());
                for (String query : queries)
                {
                    QueryEntry qe = sqlStats.getStats().get(query);

                    System.out.printf("| %7d | %16.6f | %22.6f | %s%n", qe.count,
                            qe.totalTime / qe.count, qe.totalTime, query);
                }

                System.out.println("+---------+------------------+------------------------+");
            }
            else
            {
                System.out.println("SQL stats off.");
            }

            storage.clear();
        }
    }
}