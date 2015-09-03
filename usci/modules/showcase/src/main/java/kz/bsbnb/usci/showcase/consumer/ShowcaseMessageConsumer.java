package kz.bsbnb.usci.showcase.consumer;

import kz.bsbnb.usci.eav.model.base.IBaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.OperationType;
import kz.bsbnb.usci.eav.showcase.QueueEntry;
import kz.bsbnb.usci.eav.stats.SQLQueriesStats;
import kz.bsbnb.usci.showcase.ShowcaseHolder;
import kz.bsbnb.usci.showcase.dao.ShowcaseDao;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class ShowcaseMessageConsumer implements MessageListener {
    final static Logger logger = Logger.getLogger(ShowcaseMessageConsumer.class);

    @Autowired
    SQLQueriesStats stats;

    @Autowired
    ShowcaseDao showcaseDao;

    private ExecutorService exec = Executors.newCachedThreadPool();

    @Override
    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            ObjectMessage om = (ObjectMessage) message;
            QueueEntry queueEntry;

            try {
                queueEntry = (QueueEntry) om.getObject();
            } catch(JMSException jms) {
                jms.printStackTrace();
                return;
            }

            if (queueEntry.getBaseEntityApplied() == null) {
                System.err.println("Переданный объект пустой;");
                return;
            }

            Long scId = queueEntry.getScId();

            try {
                ArrayList<Future> futures = new ArrayList<>();
                List<ShowcaseHolder> holders = showcaseDao.getHolders();

                if (holders.size() == 0)
                    throw new IllegalStateException("Необходимо создать витрины;");

                OperationType operationType;

                if (queueEntry.getBaseEntityApplied().getOperation() != null) {
                    operationType = queueEntry.getBaseEntityApplied().getOperation();
                } else {
                    operationType = OperationType.INSERT;
                }

                if (operationType == OperationType.DELETE) {
                    ShowcaseHolder h = showcaseDao.getHolderByClassName(
                            queueEntry.getBaseEntityApplied().getMeta().getClassName());

                    showcaseDao.deleteById(h, queueEntry.getBaseEntityApplied());
                } else if (operationType == OperationType.NEW) {
                    throw new UnsupportedOperationException("Operation new not supported in showcase");
                } else if (operationType == OperationType.CLOSE) {
                    showcaseDao.closeEntities(scId, queueEntry.getBaseEntityApplied(), holders);
                } else {
                    boolean found = false;

                    for (ShowcaseHolder holder : holders) {
                        if (!holder.getShowCaseMeta().getMeta().getClassName()
                                .equals(queueEntry.getBaseEntityApplied().getMeta().getClassName()))
                            continue;

                        if (scId == null || scId == 0L || scId == holder.getShowCaseMeta().getId()) {
                            Future future = exec.submit(new CarteageGenerator(queueEntry.getBaseEntityApplied(),
                                    holder));

                            futures.add(future);

                            found = true;
                        }
                    }

                    if(!found)
                        System.err.println("Для мета класа  " +
                                queueEntry.getBaseEntityApplied().getMeta().getClassName() +
                                " нет существующих витрин;");

                    for (Future f : futures)
                        f.get();

                    futures.removeAll(futures);
                }
            } catch (Exception e) {
                e.printStackTrace();

                logger.error(e.getMessage());

                StringBuilder sb = new StringBuilder();

                for(StackTraceElement s : e.getStackTrace())
                    sb.append(s.toString());

                showcaseDao.insertBadEntity(queueEntry.getBaseEntityApplied().getId(), scId,
                        queueEntry.getBaseEntityApplied().getReportDate(), sb.toString(), e.getMessage());
            }
        }
    }

    private class CarteageGenerator implements Runnable {
        private IBaseEntity entity;
        private ShowcaseHolder holder;

        public CarteageGenerator(IBaseEntity entity, ShowcaseHolder holder) {
            this.entity = entity;
            this.holder = holder;
        }

        @Override
        public void run() {
            showcaseDao.generate(entity, holder);
        }
    }

    private class CloseGenerator implements Runnable {
        private IBaseEntity entity;
        private ShowcaseHolder holder;

        public CloseGenerator(IBaseEntity entity, ShowcaseHolder holder) {
            this.entity = entity;
            this.holder = holder;
        }

        @Override
        public void run() {

        }
    }
}
