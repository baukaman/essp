package kz.bsbnb.usci.receiver.monitor;

import kz.bsbnb.usci.core.service.PortalUserBeanRemoteBusiness;
import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.cr.model.PortalUser;
import kz.bsbnb.usci.cr.model.Report;
import kz.bsbnb.usci.eav.StaticRouter;
import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.BatchStatus;
import kz.bsbnb.usci.eav.model.EavGlobal;
import kz.bsbnb.usci.eav.model.json.BatchInfo;
import kz.bsbnb.usci.eav.util.BatchStatuses;
import kz.bsbnb.usci.eav.util.DataUtils;
import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.eav.util.ReportStatus;
import kz.bsbnb.usci.receiver.queue.JobInfo;
import kz.bsbnb.usci.receiver.queue.JobLauncherQueue;
import kz.bsbnb.usci.receiver.reader.impl.InfoReader;
import kz.bsbnb.usci.receiver.reader.impl.ManifestReader;
import kz.bsbnb.usci.receiver.reader.impl.beans.InfoData;
import kz.bsbnb.usci.receiver.reader.impl.beans.ManifestData;
import kz.bsbnb.usci.receiver.repository.IServiceRepository;
import kz.bsbnb.usci.sync.service.IBatchService;
import kz.bsbnb.usci.sync.service.IEntityService;
import kz.bsbnb.usci.sync.service.ReportBeanRemoteBusiness;
import kz.bsbnb.usci.tool.status.ReceiverStatusSingleton;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import java.io.*;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFilesMonitor {
    private final Logger logger = LoggerFactory.getLogger(ZipFilesMonitor.class);

    @Autowired
    private IServiceRepository serviceFactory;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ReceiverStatusSingleton receiverStatusSingleton;

    @Autowired
    private JobLauncherQueue jobLauncherQueue;

    private IBatchService batchService;

    private Map<String, Job> jobs;

    private List<Creditor> creditors;

    SenderThread sender;

    public static final int ZIP_BUFFER_SIZE = 1024;
    public static final int MAX_SYNC_QUEUE_SIZE = 2048;

    private static final String DIGITAL_SIGNING_SETTINGS = "DIGITAL_SIGNING_SETTINGS";
    private static final String DIGITAL_SIGNING_ORGANIZATIONS_IDS_CONFIG_CODE = "DIGITAL_SIGNING_ORGANIZATIONS_IDS";
    private static final String ORG_FIRST_DATE_SETTING = "ORG_FIRST_DATE_SETTING";
    private static final String CREDITOR_DATES = "CREDITOR_DATES";
    private static final String DEFAULT_DATE_VALUE = "DEFAULT_DATE_VALUE";
    private static final String WAITING_FOR_SIGNATURE = "WAITING_FOR_SIGNATURE";

    private static final long WAIT_TIMEOUT = 360; //in 10 sec units

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    public ZipFilesMonitor(Map<String, Job> jobs) {
        this.jobs = jobs;
    }


    public void сancelBatch(long batchId) {
        batchService.addBatchStatus(new BatchStatus()
                .setBatchId(batchId)
                .setStatus(BatchStatuses.CANCELLED)
                .setReceiptDate(new Date()));
    }

    public void declineMaintenanceBatch(long batchId) {
        batchService.addBatchStatus(new BatchStatus()
                .setBatchId(batchId)
                .setStatus(BatchStatuses.MAINTENANCE_DECLINED)
                .setReceiptDate(new Date()));
    }

    public boolean restartBatch(long batchId) {
        try {
            Batch batch = batchService.getBatch(batchId);
            BatchInfo batchInfo = new BatchInfo(batch);

            if(batch.isMaintenance() && !batch.isMaintenanceApproved()) {
                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.MAINTENANCE_REQUEST)
                        .setReceiptDate(new Date())
                );

                List<Creditor> cList = serviceFactory.getUserService().getPortalUserCreditorList(batchInfo.getUserId());

                if(cList.size() != 1) {
                    logger.error("Неправильное количество кредиторов ожидаемое = 1, " +
                            "получено = " + cList.size() + ", пользователь = " + batchInfo.getUserId());
                } else {
                    batch.setCreditor(cList.get(0));
                    serviceFactory.getMailMessageBeanCommonBusiness().notifyNBMaintenance(batch);
                }
                return false;
            }
           // EavGlobal signGlobal = serviceFactory.getGlobalService().getGlobal(batch.getStatusId());

            //if(signGlobal.getValue().equals(WAITING_FOR_SIGNATURE)) {

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.WAITING)
                        .setReceiptDate(new Date()));
            //}
            System.out.println("Перезагрузка батча : " + batch.getId() + " - " + batch.getFileName());

            jobLauncherQueue.addJob(batchId, batchInfo);
            receiverStatusSingleton.batchReceived();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @PostConstruct
    public void init() {
        batchService = serviceFactory.getBatchService();
        sender = new SenderThread();
        sender.start();
        sender.setReceiverStatusSingleton(receiverStatusSingleton);
        creditors = serviceFactory.getRemoteCreditorBusiness().findMainOfficeCreditors();
        System.out.println("Найдено " + creditors.size() + " кредиторов;");

        IBatchService batchService = serviceFactory.getBatchService();

        List<Batch> pendingBatchList = batchService.getPendingBatchList();

        filterUnsignedBatches(pendingBatchList);

        if (pendingBatchList.size() > 0) {
            System.out.println("Найдены не законченные батчи: " + pendingBatchList.size());

            System.out.println("-------------------------------------------------------------------------");

            for (Batch b : pendingBatchList)
                System.out.println(b.getId() + ", " + b.getFileName() + ", " + dateFormat.format(b.getRepDate()));

            System.out.println("-------------------------------------------------------------------------");

            for (Batch batch : pendingBatchList) {
                try {
                    jobLauncherQueue.addJob(batch.getId(), new BatchInfo(batch));
                    receiverStatusSingleton.batchReceived();
                    System.out.println("Перезагрузка батча : " + batch.getId() + " - " + batch.getFileName());
                } catch (Exception e) {
                    System.out.println("Error in pending batches view: " + e.getMessage());
                    System.out.println("Retrying...");
                }
            }
        }
    }

    private class SenderThread extends Thread {
        private ReceiverStatusSingleton receiverStatusSingleton;

        public ReceiverStatusSingleton getReceiverStatusSingleton() {
            return receiverStatusSingleton;
        }

        public void setReceiverStatusSingleton(ReceiverStatusSingleton receiverStatusSingleton) {
            this.receiverStatusSingleton = receiverStatusSingleton;
        }

        public void run() {
            //noinspection InfiniteLoopStatement
            while (true) {
                JobInfo nextJob;

                if (serviceFactory != null && serviceFactory.getEntityService().getQueueSize() > MAX_SYNC_QUEUE_SIZE) {
                    try {
                        sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if(serviceFactory != null) {
                    Set<Long> finishedBatches = serviceFactory.getEntityService().getFinishedBatches();

                    if(finishedBatches.size() == 0) {
                        try {
                            sleep(100L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    for (Long batchId : finishedBatches) {
                        jobLauncherQueue.jobFinished(batchId);
                    }
                }

                if ((nextJob = jobLauncherQueue.getNextJob()) != null) {
                    System.out.println("Отправка батча на обработку : " + nextJob.getBatchId());

                    try {
                        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

                        jobParametersBuilder.addParameter("creditorId", new JobParameter(nextJob.getBatchInfo().getCreditorId()));

                        jobParametersBuilder.addParameter("batchId", new JobParameter(nextJob.getBatchId()));

                        jobParametersBuilder.addParameter("userId", new JobParameter(nextJob.getBatchInfo().getUserId()));

                        jobParametersBuilder.addParameter("reportId", new JobParameter(nextJob.getBatchInfo().getReportId()));

                        jobParametersBuilder.addParameter("actualCount", new JobParameter(nextJob.getBatchInfo().getActualCount()));

                        Job job = jobs.get(nextJob.getBatchInfo().getBatchType());

                        if (job != null) {
                            jobLauncher.run(job, jobParametersBuilder.toJobParameters());
                            receiverStatusSingleton.batchStarted();
                            batchService.clearActualCount(nextJob.getBatchId());
                            batchService.addBatchStatus(new BatchStatus()
                                    .setBatchId(nextJob.getBatchId())
                                    .setStatus(BatchStatuses.PROCESSING)
                                    .setReceiptDate(new Date()));
                        } else {
                            logger.error("Неивестный тип батч файла: " + nextJob.getBatchInfo().getBatchType() +
                                    " ID: " + nextJob.getBatchId());

                            batchService.addBatchStatus(new BatchStatus()
                                    .setBatchId(nextJob.getBatchId())
                                    .setStatus(BatchStatuses.ERROR)
                                    .setDescription("Неивестный тип батч файла: " +
                                            nextJob.getBatchInfo().getBatchType())
                                    .setReceiptDate(new Date()));
                        }

                        sleep(1000);
                    } catch (Exception e) {
                        jobLauncherQueue.jobFinished(nextJob.getBatchId());
                        e.printStackTrace();
                    }
                } else {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logger.debug("Нет файлов для отправки");
                }
            }
        }
    }

    public long parseCreditorId(byte[] bytes) {

        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(bytes));
            InfoReader infoReader = new InfoReader();
            infoReader.parse(eventReader);
            InfoData infoData = infoReader.getInfoData();
            Date repDate = infoData.getReportDate();
            DataUtils.toBeginningOfTheMonth(repDate);
            DataUtils.toBeginningOfTheDay(repDate);
            BatchInfo batchInfo = new BatchInfo();
            batchInfo.setRepDate(repDate);
            batchInfo.setSize(infoData.getActualCreditCount());
            batchInfo.setActualCount(infoData.getActualCreditCount());
            batchInfo.setTotalCount(0);
            batchInfo.setMaintenance(infoData.isMaintenance());
            String code = infoData.getCode();
            if (code != null && code.length() > 0) {
                batchInfo.addParam("CODE", code.trim());
            } else {
                String docType = infoData.getDocType();
                String docValue = infoData.getDocValue();

                if (docType != null && docValue != null &&
                        docType.length() > 0 && docValue.length() > 0) {
                    batchInfo.addParam("DOC_TYPE", docType.trim());
                    batchInfo.addParam("DOC_VALUE", docValue.trim());
                }
            }

            return getCreditor(batchInfo, creditors).getId();
        } catch (Exception e) {
            e.printStackTrace();
            return -1L;
        }
    }

    public void saveData(BatchInfo batchInfo, String filename, byte[] bytes, boolean isNB) {
        receiverStatusSingleton.batchReceived();

        Batch batch = new Batch();
        batch.setId(batchInfo.getBatchId());
        batch.setUserId(batchInfo.getUserId());
        batch.setFileName(filename);
        batch.setContent(bytes);
        batch.setRepDate(batchInfo.getRepDate());
        batch.setReceiptDate(new Date());
        batch.setBatchType(batchInfo.getBatchType());
        batch.setTotalCount(batchInfo.getSize());

        long batchId = batchService.save(batch);
        batch.setId(batchId);

        Long cId;
        boolean haveError = false;

        List<Creditor> cList = serviceFactory.getUserService().getPortalUserCreditorList(batchInfo.getUserId());

        if (batchInfo.getUserId() != 100500L) {
            if (cList.size() > 0) {
                batch.setCreditor(getCreditor(batchInfo, cList));
                cId = batch.getCreditor().getId();

                if (cId <= 0) {
                    String docType = batchInfo.getAdditionalParams().get("DOC_TYPE");
                    String docValue = batchInfo.getAdditionalParams().get("DOC_VALUE");

                    if (docType == null) docType = "";
                    if (docValue == null) docValue = "";

                    logger.error("Несоответствие кредитора пользователю портала: " + docType + ", " + docValue);

                    failFast(batchId, "Несоответствие кредитора пользователю портала");
                    haveError = true;

                    cId = cList.get(0).getId();
                }
            } else {
                cId = -1L;

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.ERROR)
                        .setDescription("Пользователь не имеет доступа к кредиторам: " + batchInfo.getUserId())
                        .setReceiptDate(new Date()));

                haveError = true;
            }
        } else {
            cId = getCreditor(batchInfo, creditors).getId();
            if (cId <= 0) {
                String docType = batchInfo.getAdditionalParams().get("DOC_TYPE");
                String docValue = batchInfo.getAdditionalParams().get("DOC_VALUE");

                if (docType == null) docType = "";
                if (docValue == null) docValue = "";

                logger.error("Кредитор не найден: " + docType + ", " + docValue);

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.ERROR)
                        .setDescription("Кредитор с документами (" + docType + ", " + docValue + ")не найден")
                        .setReceiptDate(new Date()));
                haveError = true;
            }
        }


        if (!haveError && !isNB && !StaticRouter.isInMode(filename) && StaticRouter.isSignatureEnabled() && !checkAndFillEavReport(cId, batchInfo, batchId))
            haveError = true;

        batch.setCreditorId(isNB ? 0 : cId);
        batch.setReportId(batchInfo.getReportId());
        batch.setMaintenance(batchInfo.isMaintenance());
        batchService.uploadBatch(batch);

        if (!haveError) {
            if (!waitForSignature(filename, batch, batchInfo)) {
                if(batchInfo.isMaintenance()) {
                    batchService.addBatchStatus(new BatchStatus()
                            .setBatchId(batchId)
                            .setStatus(BatchStatuses.MAINTENANCE_REQUEST)
                            .setReceiptDate(new Date())
                    );

                    serviceFactory.getMailMessageBeanCommonBusiness().notifyNBMaintenance(batch);
                    return;
                }

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.WAITING)
                        .setReceiptDate(new Date()));

                batchInfo.setContentSize(batch.getContent().length);
                batchInfo.setCreditorId(batch.getCreditorId());
                batchInfo.setReceiptDate(batch.getReceiptDate());

                jobLauncherQueue.addJob(batchId, batchInfo);
            }
        }
    }

    boolean waitForSignature(String filename, Batch batch, BatchInfo batchInfo) {
        if (StaticRouter.isInMode(filename))
            return false;

        if(!StaticRouter.isSignatureEnabled())
            return false;

        String digitalSignArguments = serviceFactory.getGlobalService().getValue(DIGITAL_SIGNING_SETTINGS,
        DIGITAL_SIGNING_ORGANIZATIONS_IDS_CONFIG_CODE);

        String[] orgIds = digitalSignArguments.split(",");
        if (batch.getCreditorId() > 0 && Arrays.asList(orgIds).contains(String.valueOf(batch.getCreditorId()))) {
            batchService.addBatchStatus(new BatchStatus()
                    .setBatchId(batch.getId())
                    .setStatus(BatchStatuses.WAITING_FOR_SIGNATURE)
                    .setReceiptDate(new Date()));

            batchInfo.setContentSize(batch.getContent().length);
            batchInfo.setCreditorId(batch.getCreditorId());
            batchInfo.setReceiptDate(batch.getReceiptDate());

            return true;
        }
        return false;
    }

    private void filterUnsignedBatches(List<Batch> pendingBatchList) {
        String digitalSignOrgs = serviceFactory.getGlobalService().getValue(DIGITAL_SIGNING_SETTINGS,
                DIGITAL_SIGNING_ORGANIZATIONS_IDS_CONFIG_CODE);

        String[] orgIds = digitalSignOrgs.split(",");
        Iterator<Batch> it = pendingBatchList.iterator();
        while (it.hasNext()) {
            Batch batch = it.next();
            EavGlobal signGlobal = serviceFactory.getGlobalService().getGlobal(batch.getStatusId());
            if (signGlobal.getValue().equals(WAITING_FOR_SIGNATURE) && batch.getSign() == null && batch.getCreditorId() > 0
                    && Arrays.asList(orgIds).contains(batch.getCreditorId() + ""))
                it.remove();
        }
    }

    private void failFast(Long batchId, String error) {
        batchService.addBatchStatus(new BatchStatus()
                .setBatchId(batchId)
                .setStatus(BatchStatuses.ERROR)
                .setDescription(error)
                .setReceiptDate(new Date()));

        batchService.endBatch(batchId);
    }

    private Creditor getCreditor(BatchInfo batchInfo, List<Creditor> creditors) {
        Creditor ret = new Creditor();
        ret.setId(-1L);

        if (batchInfo.getAdditionalParams() != null && batchInfo.getAdditionalParams().size() > 0) {
            String docType = batchInfo.getAdditionalParams().get("DOC_TYPE");
            String docValue = batchInfo.getAdditionalParams().get("DOC_VALUE");

            String code = batchInfo.getAdditionalParams().get("CODE");
            String bin = batchInfo.getAdditionalParams().get("BIN");
            String bik = batchInfo.getAdditionalParams().get("BIK");
            String rnn = batchInfo.getAdditionalParams().get("RNN");

            if (docType == null) docType = "";
            if (docValue == null) docValue = "";

            for (Creditor creditor : creditors) {
                if (creditor.getBIK() != null && docType.equals("15") && creditor.getBIK().equals(docValue)) {
                    ret = creditor;
                    break;
                }

                if (creditor.getBIN() != null && docType.equals("07") && creditor.getBIN().equals(docValue)) {
                    ret = creditor;
                    break;
                }

                if (creditor.getRNN() != null && docType.equals("11") && creditor.getRNN().equals(docValue)) {
                    ret = creditor;
                    break;
                }

                if (code != null && code.length() > 0 && creditor.getCode() != null
                        && creditor.getCode().length() > 0 && code.equals(creditor.getCode())) {
                    ret = creditor;
                    break;
                }

                if (bin != null && bin.length() > 0 && creditor.getBIN() != null
                        && creditor.getBIN().length() > 0 && bin.equals(creditor.getBIN())) {
                    ret = creditor;
                    break;
                }

                if (bik != null && bik.length() > 0 && creditor.getBIK() != null
                        && creditor.getBIK().length() > 0 && bik.equals(creditor.getBIK())) {
                    ret = creditor;
                    break;
                }

                if (rnn != null && rnn.length() > 0 && creditor.getRNN() != null
                        && creditor.getRNN().length() > 0 && rnn.equals(creditor.getRNN())) {
                    ret = creditor;
                    break;
                }
            }
        }
        return ret;
    }

    private boolean checkAndFillEavReport(long creditorId, BatchInfo batchInfo, long batchId) {
        ReportBeanRemoteBusiness reportBeanRemoteBusiness = serviceFactory.getReportBeanRemoteBusinessService();

        if (!StaticRouter.isDevMode()) {
            String errMsg = null;
            Date lastApprovedDate = reportBeanRemoteBusiness.getLastApprovedDate(creditorId);

            Date mustDate = null;

            //never approved
            if(lastApprovedDate == null) {
                String creditorDates = serviceFactory.getGlobalService().getValue(ORG_FIRST_DATE_SETTING, CREDITOR_DATES);
                String creditorFirstDate = serviceFactory.getGlobalService().getValue(ORG_FIRST_DATE_SETTING, DEFAULT_DATE_VALUE);

                String[] pairs = creditorDates.split(",");
                for(String pair: pairs) {
                    String[] record = pair.split("=");
                    Long cId = Long.parseLong(record[0]);
                    String date = record[1];
                    if(creditorId == cId) {
                        creditorFirstDate = date;
                        break;
                    }
                }

                try {
                    mustDate = dateFormat.parse(creditorFirstDate);
                } catch (ParseException e) {
                    logger.error("Ошибка при парсинге певой даты для организаций creditorId = "
                            + creditorId + ", batchId = " + batchId + ", creditorFirstDate = " + creditorFirstDate);
                    errMsg = "Неправильная настройка первой отчетной даты для организации";
                }

                if(mustDate != null && !mustDate.equals(batchInfo.getRepDate())) {
                    errMsg = "Ошибка отчетной даты. Первая отчетная дата для организации = "
                            + dateFormat.format(mustDate) + ", заявлено = " + dateFormat.format(batchInfo.getRepDate());
                }

            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(lastApprovedDate);
                Integer reportPeriodDurationMonths = null;

                for (Creditor creditor : creditors) {
                    if(creditor.getId() == creditorId) {
                        reportPeriodDurationMonths = creditor.getSubjectType().getReportPeriodDurationMonths();
                        break;
                    }
                }
                cal.add(Calendar.MONTH, reportPeriodDurationMonths == null ? 1 : reportPeriodDurationMonths);
                mustDate = cal.getTime();

                if(batchInfo.isMaintenance()) {
                    if(mustDate.compareTo(batchInfo.getRepDate()) < 0) {
                        errMsg = "Ошибка запроса на изменение за утвержденный период. Последняя утвержденная дата = " +
                                dateFormat.format(lastApprovedDate) + ", " + " заявлено = " + dateFormat.format(batchInfo.getRepDate())
                                + ", шаг отчетности = " + reportPeriodDurationMonths;
                    } else {
                        //otherwise breaks status_id in eav_report
                        return true;
                    }
                } else {
                    if(!mustDate.equals(batchInfo.getRepDate())) {
                        errMsg = "Ошибка отчетной даты. Последняя утвержденная дата = " + dateFormat.format(lastApprovedDate) + ", " +
                                " заявлено = " + dateFormat.format(batchInfo.getRepDate()) + ", шаг отчетности = " + reportPeriodDurationMonths;
                        logger.error(errMsg);
                    }
                }
            }



            /*
            creditors.get(0).getSubjectType().getReportPeriodDurationMonths()

            if (existing != null) {
                if (ReportStatus.COMPLETED.code().equals(existing.getStatus().getCode())
                        || ReportStatus.ORGANIZATION_APPROVED.code().equals(existing.getStatus().getCode())
                        || ReportStatus.ORGANIZATION_APPROVING.code().equals(existing.getStatus().getCode())) {
                    errMsg = "Данные на указанную отчетную дату утверждены организацией = "
                            + creditorId + ", отчетная дата = " + dateFormat.format(batchInfo.getRepDate());
                }
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTime(batchInfo.getRepDate());
                cal.add(Calendar.MONTH, -1);
                Date prevMonth = cal.getTime();

                Report prevMonthReport = reportBeanRemoteBusiness.getReport(creditorId, prevMonth);


                if(prevMonthReport != null) {
                    if(!(ReportStatus.COMPLETED.code().equals(prevMonthReport.getStatus().getCode())
                            || ReportStatus.ORGANIZATION_APPROVING.code().equals(prevMonthReport.getStatus().getCode())
                            || ReportStatus.ORGANIZATION_APPROVED.code().equals(prevMonthReport.getStatus().getCode())))
                        errMsg = "Необходимо утвердить данные за отчетный периюд : " + dateFormat.format(prevMonthReport.getReportDate());
                } else {
                    Report firstReport = reportBeanRemoteBusiness.getFirstReport(creditorId);
                    String creditorFirstDate = serviceFactory.getGlobalService().getValue(ORG_FIRST_DATE_SETTING, DEFAULT_DATE_VALUE);

                    //Первый отчет для новой организации
                    if(firstReport == null) {
                        String creditorDates = serviceFactory.getGlobalService().getValue(ORG_FIRST_DATE_SETTING, CREDITOR_DATES);
                        String[] pairs = creditorDates.split(",");
                        for(String pair: pairs) {
                            String[] record = pair.split("=");
                            Long cId = Long.parseLong(record[0]);
                            String date = record[1];
                            if(creditorId == cId) {
                                creditorFirstDate = date;
                                break;
                            }
                        }

                        try {
                            if (!batchInfo.getRepDate().equals(dateFormat.parse(creditorFirstDate)))
                                errMsg = "Настройками организации установлена первая отчетная дата: " + creditorFirstDate;
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            errMsg = e.getMessage();
                        }
                    }
                    //Утверждены не все отчетные даты
                    else {
                        errMsg = "История отчетности неккоректна";
                    }
                }
            }*/

            if (errMsg != null) {
                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.WAITING)
                        .setReceiptDate(new Date()));

                batchService.addBatchStatus(new BatchStatus()
                        .setBatchId(batchId)
                        .setStatus(BatchStatuses.PROCESSING)
                        .setReceiptDate(new Date()));

                logger.error(errMsg);
                failFast(batchId, errMsg);
                return false;
            }
        }

        EavGlobal inProgress = serviceFactory.getGlobalService().getGlobal(ReportStatus.IN_PROGRESS);
        Report existing = reportBeanRemoteBusiness.getReport(creditorId, batchInfo.getRepDate());

        if (existing != null) {
            existing.setStatusId(inProgress.getId());
            existing.setTotalCount(batchInfo.getTotalCount());
            existing.setActualCount(batchInfo.getActualCount());
            existing.setEndDate(new Date());

            PortalUserBeanRemoteBusiness userService = serviceFactory.getUserService();
            PortalUser portalUser = userService.getUser(batchInfo.getUserId());
            if (portalUser != null)
                reportBeanRemoteBusiness.updateReport(existing, portalUser.getScreenName());
            else
                reportBeanRemoteBusiness.updateReport(existing, "Неивестный");

            batchInfo.setReportId(existing.getId());
        } else {
            Report report = new Report();
            {
                Creditor creditor = new Creditor();
                creditor.setId(creditorId);
                report.setCreditor(creditor);
            }
            report.setStatusId(inProgress.getId());
            report.setTotalCount(batchInfo.getTotalCount());
            report.setActualCount(batchInfo.getActualCount());
            report.setReportDate(batchInfo.getRepDate());
            report.setBeginningDate(new Date());
            report.setEndDate(new Date());

            PortalUserBeanRemoteBusiness userService = serviceFactory.getUserService();
            PortalUser portalUser = userService.getUser(batchInfo.getUserId());
            Long reportId;
            if (portalUser != null)
                reportId = reportBeanRemoteBusiness.insert(report, portalUser.getScreenName());
            else
                reportId = reportBeanRemoteBusiness.insert(report, "Неизвестный");
            batchInfo.setReportId(reportId);
        }

        return true;
    }

    public byte[] inputStreamToByte(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        in.close();

        return buffer.toByteArray();
    }

    public void readFiles(String filename) {
        readFiles(filename, null);
    }

    public void readFiles(String filename, Long userId) {
        readFiles(filename, userId, false);
    }

    public void readFiles(String filename, Long userId, boolean isNB) {
        Batch batch = new Batch();
        batch.setUserId(userId);
        batch.setFileName(filename);
        batch.setReceiptDate(new Date());
        batch.setCreditorId(0L);

        if (userId != null && userId > 0 && !isNB) {
            List<Creditor> cList = serviceFactory.getUserService().getPortalUserCreditorList(userId);

            if (cList.size() == 0) {
                batch.setId(batchService.save(batch));
                failFast(batch.getId(), "Нет доступных кредиторов для " + userId);
                return;
            }

            if (cList.size() == 1) {
                batch.setCreditorId(cList.get(0).getId());
            } else {
                batch.setId(batchService.save(batch));
                failFast(batch.getId(), "Доступно больше одного кредитора для " + userId);
                return;
            }
        }

        batch.setId(batchService.save(batch));

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(batch.getId());

        ZipFile zipFile = null;

        try {
            zipFile = new ZipFile(filename);
            ZipEntry manifestEntry = zipFile.getEntry("manifest.xml");

            if (manifestEntry == null) { // credit-registry
                ZipArchiveInputStream zis = null;
                byte[] extractedBytes = null;

                try {
                    zis = new ZipArchiveInputStream(new FileInputStream(filename));

                    while (zis.getNextZipEntry() != null) {
                        ByteArrayOutputStream byteArrayOutputStream = null;
                        try {
                            int size;
                            byte[] buffer = new byte[ZIP_BUFFER_SIZE];

                            byteArrayOutputStream = new ByteArrayOutputStream();

                            while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                                byteArrayOutputStream.write(buffer, 0, size);
                            }
                            extractedBytes = byteArrayOutputStream.toByteArray();
                        } finally {
                            if (byteArrayOutputStream != null) {
                                byteArrayOutputStream.flush();
                                byteArrayOutputStream.close();
                            }
                        }
                    }
                } finally {
                    if (zis != null) {
                        zis.close();
                    }
                }

                if (extractedBytes == null)
                    throw new IOException(Errors.compose(Errors.E191));

                if (userId == null)
                    userId = 100500L;

                batchInfo.setBatchType("2");
                batchInfo.setBatchName(parseFileNameFromPath(filename));
                batchInfo.setUserId(userId);

                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(extractedBytes));

                InfoReader infoReader = new InfoReader();
                infoReader.parse(eventReader);
                InfoData infoData = infoReader.getInfoData();

                Date repDate = infoData.getReportDate();
                DataUtils.toBeginningOfTheMonth(repDate);
                DataUtils.toBeginningOfTheDay(repDate);
                batchInfo.setRepDate(repDate);

                batchInfo.setSize(infoData.getActualCreditCount());
                batchInfo.setActualCount(infoData.getActualCreditCount());
                batchInfo.setTotalCount(0);
                batchInfo.setMaintenance(infoData.isMaintenance());

                String code = infoData.getCode();
                if (code != null && code.length() > 0) {
                    batchInfo.addParam("CODE", code.trim());
                } else {
                    String docType = infoData.getDocType();
                    String docValue = infoData.getDocValue();

                    if (docType != null && docValue != null &&
                            docType.length() > 0 && docValue.length() > 0) {
                        batchInfo.addParam("DOC_TYPE", docType.trim());
                        batchInfo.addParam("DOC_VALUE", docValue.trim());
                    }
                }


                saveData(batchInfo, filename, inputStreamToByte(new FileInputStream(filename)), isNB);
            } else { // usci
                InputStream inManifest = zipFile.getInputStream(manifestEntry);

                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = inputFactory.createXMLEventReader(inManifest);

                ManifestReader manifestReader = new ManifestReader();
                manifestReader.parse(eventReader);
                ManifestData manifestData = manifestReader.getManifestData();

                batchInfo.setBatchType(manifestData.getType().trim());

                batchInfo.setBatchName(parseFileNameFromPath(filename));
                batchInfo.setMaintenance(manifestData.isMaintenance());

                batchInfo.setUserId(userId == null ? manifestData.getUserId() : userId);

                int actualCreditCount = manifestData.getSize();

                batchInfo.setSize((long) actualCreditCount);
                batchInfo.setActualCount(actualCreditCount);
                batchInfo.setTotalCount(0);

                Date date = manifestData.getReportDate();

                DataUtils.toBeginningOfTheMonth(date);
                DataUtils.toBeginningOfTheDay(date);
                batchInfo.setRepDate(date);

                batchInfo.setAdditionalParams(manifestData.getAdditionalParams());

                saveData(batchInfo, filename, inputStreamToByte(new FileInputStream(filename)), isNB);
            }
        } catch (Exception e) {
            if(e instanceof IOException) {
                failFast(batch.getId(), "Ошибка I/O: " + e.getMessage() );
            } else {
                failFast(batch.getId(), "Не корректный XML файл");
            }
        } finally {
            try {
                if (zipFile != null) zipFile.close();
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    public void monitor(Path path) throws InterruptedException, IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,StandardWatchEventKinds.ENTRY_DELETE);

        IEntityService entityService = serviceFactory.getEntityService();

        boolean valid;
        long sleepCounter = 0;
        do {
            while (entityService.getQueueSize() > MAX_SYNC_QUEUE_SIZE) {
                Thread.sleep(1000);

                sleepCounter++;

                if (sleepCounter > WAIT_TIMEOUT)
                    logger.error("Sync timeout in reader.");
            }
            sleepCounter = 0;

            WatchKey watchKey = watchService.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                String fileName = event.context().toString();

                if (!StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind()) && fileName.contains(".lock")) {
                    continue;
                }

                if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind())) {
                    System.out.println("Поступил батч : " + fileName);
                } else if(StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())){
                } else if(StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind()) && event.context().toString().contains(".lock")){
                    readFiles(path + "/" + fileName.substring(0,fileName.length()-5));
                }

            }
            valid = watchKey.reset();

        } while (valid);

    }

    private String parseFileNameFromPath(String fileName) {
        return fileName.substring(fileName.lastIndexOf('/') + 1);

    }

    public JobLauncherQueue getJobLauncherQueue() {
        return jobLauncherQueue;
    }
}
