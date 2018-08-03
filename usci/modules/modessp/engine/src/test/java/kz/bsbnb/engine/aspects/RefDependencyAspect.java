package kz.bsbnb.engine.aspects;

import kz.bsbnb.DataEntity;
import kz.bsbnb.SavingInfo;
import kz.bsbnb.dao.DataEntityDao;
import kz.bsbnb.engine.IRefEngine;
import kz.bsbnb.engine.StaticRefEngineImpl;
import kz.bsbnb.engine.impl.RefEngineImpl;
import kz.bsbnb.reader.test.ThreePartReader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Aspect
public class RefDependencyAspect {

    @Autowired
    DataEntityDao dataEntityDao;

    @Autowired
    SavingInfo savingInfo;

    @Autowired
    IRefEngine refEngine;

    @Around("execution(* kz.bsbnb.reader.test.ThreePartReader.read())")
    public Object advice(ProceedingJoinPoint joinPoint) throws Throwable {
        DataEntity ret = ((DataEntity) joinPoint.proceed());
        savingInfo.readFromEntity(ret);
        List<DataEntity> refs = ((ThreePartReader) joinPoint.getThis()).getRefs();
        if(refs != null) {
            ((StaticRefEngineImpl) refEngine).initCache(refs);
        }
        return ret;
    }
}
