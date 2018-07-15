package kz.bsbnb.engine.aspects;

import kz.bsbnb.DataEntity;
import kz.bsbnb.SavingInfo;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
public class SavingInfoTestAspect {
    @Autowired
    SavingInfo savingInfo;

    @AfterReturning(pointcut = "execution(* kz.bsbnb.reader.test.ThreePartReader.read())", returning = "entity")
    public void read(DataEntity entity) {
        savingInfo.readFromEntity(entity);
    }
}
