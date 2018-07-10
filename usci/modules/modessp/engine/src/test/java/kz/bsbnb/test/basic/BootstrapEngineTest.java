package kz.bsbnb.test.basic;

import kz.bsbnb.DataEntity;
import kz.bsbnb.engine.BootstrapEngine;
import kz.bsbnb.engine.DatabaseActivity;
import kz.bsbnb.reader.test.ThreePartReader;
import kz.bsbnb.testing.FunctionalTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/applicationContextProp.xml","/applicationContextEngine.xml"})
public class BootstrapEngineTest extends FunctionalTest {

    @Autowired
    BootstrapEngine engine;

    ThreePartReader reader = new ThreePartReader();

    @Test
    public void shouldSaveEntity() throws Exception {
        InputStream inputStream = getInputStream("basic/A.xml");
        final DataEntity entity = reader.withSource(inputStream)
                .withMeta(metaCredit)
                .read();

        //DatabaseActivity activities = engine.getDatabaseActivity();

        //DataEntity savedEntity = engine.process(entity);
        final DataEntity[] entities = new DataEntity[20];
        for (int i = 0; i < 20; i++) {
            entities[i] = new DataEntity(metaCredit);
            entities[i].setCreditorId(4414L + i);
        }

        ExecutorService service = Executors.newFixedThreadPool(6);

        final Random r = new Random();
        for(int i=0;i<20;i++) {
            final int j = i;
            service.submit(new Runnable() {
                @Override
                public void run() {
                    engine.process(entities[j]);
                    try {
                        Thread.sleep(1000 + r.nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //System.out.println("finished");
                }
            });
        }

        service.shutdown();
        boolean b = service.awaitTermination(1000, TimeUnit.SECONDS);
        System.out.println(b);


        /*Assert.assertEquals(3, activities.numberOfSelects());
        Assert.assertEquals(4, activities.numberOfInserts());
        Assert.assertEquals(2, activities.numberOfUpdates());
        Assert.assertEquals(1, activities.noActions());
        Assert.assertTrue(savedEntity.getId() > 0);*/
    }

    @Test
    public void shouldManyUpdates() throws Exception {
        /*DataEntity entity1 = readEntity("dtest/credit_01_02_2018.xml");
        DataEntity entity2 = readEntity("dtest/credit_01_01_2018.xml");
        DatabaseActivity activities = engine.getDatabaseActivity();

        Assert.assertEquals(DataUtils.getDate("01.02.2018"), entity1.getReportDate());
        Assert.assertEquals(DataUtils.getDate("01.01.2018"), entity2.getReportDate());
        DataEntity processed1 = engine.process(entity1);
        activities.flush();

        DataEntity processed2 = engine.process(entity2);
        Assert.assertEquals(5, activities.numberOfUpdates());
        Assert.assertEquals(0, activities.numberOfInserts());*/
    }

    @Test
    public void shouldCreateNewHistory() throws Exception {

    }

    @Test
    public void shouldNotCreateNewHistory() throws Exception {

    }

    @Test
    public void historyMixedCreation() throws Exception {

    }

    @Test
    public void shouldNotTouchPledges() throws Exception {

    }

    @Test
    public void shouldClosePledgesNotSent() throws Exception {

    }

    @Test
    public void shouldDeleteOnSameRD() throws Exception {

    }

    @Test
    public void shouldNoActionOnSameRD() throws Exception {

    }
}