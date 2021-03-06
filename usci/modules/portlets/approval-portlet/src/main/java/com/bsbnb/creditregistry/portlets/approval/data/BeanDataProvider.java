package com.bsbnb.creditregistry.portlets.approval.data;

import kz.bsbnb.usci.core.service.IGlobalService;
import kz.bsbnb.usci.core.service.MailMessageBeanCommonBusiness;
import kz.bsbnb.usci.core.service.PortalUserBeanRemoteBusiness;
import kz.bsbnb.usci.core.service.ReportBeanRemoteBusiness;
import kz.bsbnb.usci.cr.model.*;
import kz.bsbnb.usci.eav.StaticRouter;
import kz.bsbnb.usci.eav.model.EavGlobal;
import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.eav.util.ReportStatus;
import org.apache.log4j.Logger;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Aidar.Myrzahanov
 */
public class BeanDataProvider implements DataProvider {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private final Logger logger = Logger.getLogger(BeanDataProvider.class);

    private PortalUserBeanRemoteBusiness portalUserBusiness;
    private ReportBeanRemoteBusiness reportBusiness;
    private MailMessageBeanCommonBusiness mailMessageBusiness;
    private IGlobalService globalService;

    public BeanDataProvider() {
        try {
            // portalUserBeanRemoteBusiness
            RmiProxyFactoryBean portalUserBeanRemoteBusinessFactoryBean = new RmiProxyFactoryBean();
            portalUserBeanRemoteBusinessFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP()
                    + ":1099/portalUserBeanRemoteBusiness");
            portalUserBeanRemoteBusinessFactoryBean.setServiceInterface(PortalUserBeanRemoteBusiness.class);
            portalUserBeanRemoteBusinessFactoryBean.afterPropertiesSet();
            portalUserBusiness = (PortalUserBeanRemoteBusiness) portalUserBeanRemoteBusinessFactoryBean.getObject();

            // reportBeanRemoteBusiness
            RmiProxyFactoryBean reportBusinessFactoryBean = new RmiProxyFactoryBean();
            reportBusinessFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP()
                    + ":1099/reportBeanRemoteBusiness");
            reportBusinessFactoryBean.setServiceInterface(ReportBeanRemoteBusiness.class);
            reportBusinessFactoryBean.afterPropertiesSet();
            reportBusiness = (ReportBeanRemoteBusiness) reportBusinessFactoryBean.getObject();

            RmiProxyFactoryBean mailBusinessFactoryBean = new RmiProxyFactoryBean();
            mailBusinessFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP() + ":1099/mailRemoteBusiness");
            mailBusinessFactoryBean.setServiceInterface(MailMessageBeanCommonBusiness.class);
            mailBusinessFactoryBean.afterPropertiesSet();
            mailMessageBusiness = (MailMessageBeanCommonBusiness) mailBusinessFactoryBean.getObject();

            RmiProxyFactoryBean globalServiceFactoryBean = new RmiProxyFactoryBean();
            globalServiceFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP() + ":1099/globalService");
            globalServiceFactoryBean.setServiceInterface(IGlobalService.class);
            globalServiceFactoryBean.afterPropertiesSet();
            globalService = (IGlobalService) globalServiceFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(Errors.getError(Errors.E286));
        }
    }

    @Override
    public List<Creditor> getCreditorsList(long userId) {
        return portalUserBusiness.getMainCreditorsInAlphabeticalOrder(userId);
    }

    @Override
    public List<ReportDisplayBean> getReportsForDate(List<Creditor> accessibleCreditors, Date reportDate) {
        List<Report> reports = reportBusiness.getReportsByReportDateAndCreditors(reportDate, accessibleCreditors);
        List<ReportDisplayBean> displayBeans = new ArrayList<ReportDisplayBean>(reports.size());
        int rownum = 1;
        for (Report report : reports) {
            ReportDisplayBean displayBean = new ReportDisplayBean(report);
            displayBean.setRownum(rownum);
            displayBeans.add(displayBean);
            rownum++;
        }
        return displayBeans;
    }

    @Override
    public Date getCurrentReportDate(Creditor creditor) {
        return reportBusiness.getReportDate(creditor.getId());
    }

    @Override
    public List<ReportMessage> getReportMessages(Report report) {
        return reportBusiness.getMessagesByReport(report);
    }

    @Override
    public void addNewMessage(ReportMessage message, Report report, List<ReportMessageAttachment> attachments) {
        reportBusiness.addNewMessage(message, report, attachments);
    }

    @Override
    public Date getInitialReportDate() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            return dateFormat.parse(Report.INITIAL_REPORT_DATE_STR);
        } catch (ParseException pe) {
            throw new RuntimeException(Errors.compose(Errors.E205));
        }
    }

    @Override
    public Report getReport(Creditor creditor, Date reportDate) {
        List<Creditor> creditors = new ArrayList<Creditor>();
        creditors.add(creditor);
        List<Report> reports = reportBusiness.getReportsByReportDateAndCreditors(reportDate, creditors);
        if (reports.size() > 1) {
            throw new RuntimeException(Errors.compose(Errors.E205));
        }
        if (reports.isEmpty()) {
            return null;
        }

        return reports.get(0);
    }

    @Override
    public void updateReportStatus(Report report, ReportStatus status) {
        EavGlobal eavGlobal = globalService.getGlobal(status);
        report.setStatusId(eavGlobal.getId());
        reportBusiness.updateReport(report);
    }

    @Override
    public List<ReportMessageAttachment> getReportAttachments(Report report) {
        return reportBusiness.getAttachmentsByReport(report);
    }

    @Override
    public void sendApprovalNotifications(Creditor creditor, Report report, String username, Date sendDate,
                                          String text) {
        List<PortalUser> notificationRecipients = portalUserBusiness.getPortalUsersHavingAccessToCreditor(creditor);
        Properties mailMessageParameters = new Properties();
        mailMessageParameters.setProperty("CREDITOR", creditor.getName());
        mailMessageParameters.setProperty("STATUS", report.getStatus().getNameRu());
        mailMessageParameters.setProperty("USERNAME", username);
        mailMessageParameters.setProperty("REPORT_DATE", DATE_FORMAT.format(report.getReportDate()));
        mailMessageParameters.setProperty("UPDATE_TIME", TIME_FORMAT.format(sendDate));
        mailMessageParameters.setProperty("TEXT", text);
        for (PortalUser portalUser : notificationRecipients) {
            mailMessageBusiness.sendMailMessage("APPROVAL_UPDATE", portalUser.getUserId(), mailMessageParameters);
        }
    }

    @Override
    public Date getLastReportDate(Creditor creditor) {
        return reportBusiness.getLastReportDate(creditor.getId());
    }

    @Override
    public void updateLastManualEditDate(Report report) {
        report.setLastManualEditDate(new Date());
        reportBusiness.updateReport(report);
    }

    @Override
    public EavGlobal getGlobal(long globalId) {
        return globalService.getGlobal(globalId);
    }
}
