package kz.bsbnb.usci.tool.status;

import org.springframework.stereotype.Component;

@Component
public class SyncStatusSingleton {
    private SyncStatus status = new SyncStatus();

    public void put(long queueSize, long threadsCount, double avgTime) {
        status.setQueueSize(queueSize);
        status.setThreadsCount(threadsCount);
        status.setAvgTime(avgTime);
    }

    public void setExecutorStat(long executorCnt, double avgExecutor){
        status.setExectuorStat(executorCnt, avgExecutor);

    }

    public SyncStatus getStatus() {
        return status;
    }
}
