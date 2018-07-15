package kz.bsbnb;

import kz.bsbnb.DataEntity;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Scope(value = "thread", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SavingInfo {
    Date reportDate;
    Long creditorId;
    static AtomicLong counter = new AtomicLong();
    private long id;

    public SavingInfo(){
        //System.out.println("savingInfo created");
        this.id = counter.incrementAndGet();
    }

    public void readFromEntity(DataEntity entity){
        setReportDate(entity.getReportDate());
        setCreditorId(entity.getCreditorId());
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public Long getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Long creditorId) {
        this.creditorId = creditorId;
    }

    public long getId(){
        return this.id;
    }
}
