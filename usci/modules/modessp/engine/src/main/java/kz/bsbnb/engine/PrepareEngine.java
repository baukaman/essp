package kz.bsbnb.engine;

import kz.bsbnb.DataEntity;
import kz.bsbnb.dao.SearchEntityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrepareEngine {

    @Autowired
    DatabaseActivity activity;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    NewEntityProcessDecision newEntityProcessDecision;

    @Autowired
    SearchEntityDao searchEntityDao;

    public DataEntity process(DataEntity entity) {
        long searchId = searchEntityDao.search(entity);
        return null;
    }

    public PrepareEngine() {
        //System.out.println("const");
    }
}
