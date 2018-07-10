package kz.bsbnb.aspects;

import kz.bsbnb.DataEntity;
import kz.bsbnb.engine.SavingInfo;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@Aspect
public class SavingInfoAspect {

    @Autowired
    SavingInfo savingInfo;

    @Before("@annotation(kz.bsbnb.annotations.InfoBootstrap) && args(entity)")
    public void read(DataEntity entity) {
        savingInfo.setReportDate(entity.getReportDate());
        savingInfo.setCreditorId(entity.getCreditorId());
    }
}
