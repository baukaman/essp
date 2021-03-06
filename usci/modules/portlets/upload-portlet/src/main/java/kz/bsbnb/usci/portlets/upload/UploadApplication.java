package kz.bsbnb.usci.portlets.upload;


import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.vaadin.ui.*;
import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.portlets.upload.ui.MainLayout;
import com.liferay.portal.util.PortalUtil;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.PortletApplicationContext2;
import com.vaadin.terminal.gwt.server.PortletApplicationContext2.PortletListener;
import org.apache.log4j.Logger;

import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadApplication extends Application {

    private static final long serialVersionUID = 2096197512742005243L;

    private final Logger logger = Logger.getLogger(UploadApplication.class);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");


    @Override
    public void init() {
        logger.info("upload portlet init");
        setMainWindow(new Window());

        if (getContext() instanceof PortletApplicationContext2) {
            PortletApplicationContext2 ctx =
                    (PortletApplicationContext2) getContext();

            ctx.addPortletListener(this, new SamplePortletListener());
        } else {
            getMainWindow().showNotification(Errors.getError(Errors.E287), Window.Notification.TYPE_ERROR_MESSAGE);
        }
    }

    private class SamplePortletListener implements PortletListener {

        private static final long serialVersionUID = -5984011853767129565L;

        private Label systemDatelabel;


        @Override
        public void handleRenderRequest(RenderRequest request, RenderResponse response, Window window) {
            try {

                boolean hasRights = false;
                boolean isNB = false;

                User user = PortalUtil.getUser(PortalUtil.getHttpServletRequest(request));
                if (user != null) {
                    for (Role role : user.getRoles()) {
                        if (role.getName().equals("Administrator") || role.getName().equals("BankUser")
                                || role.getName().equals("NationalBankEmployee")) {
                            hasRights = true;

                            if (role.getName().equals("NationalBankEmployee")) {
                                isNB = true;
                            }
                        }
                    }
                }

                if (!hasRights)
                    throw new AccessControlException(Errors.compose(Errors.E238));

                Window mainWindow = new Window();
                HorizontalLayout autoUpdateLayout = new HorizontalLayout();

                ProgressIndicator indicator = new ProgressIndicator();
                indicator.setPollingInterval(1000);
                indicator.setHeight(0);
                indicator.setWidth(0);
                autoUpdateLayout.addComponent(indicator);

                systemDatelabel = new Label("");
                autoUpdateLayout.addComponent(systemDatelabel);

                Thread updateThread = new Thread(new Runnable() {

                    public void run() {
                        while (true) {
                            systemDatelabel.setCaption("Системное время : "+TIME_FORMAT.format(new Date()));
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                logger.warn(null, ie);
                            }
                        }
                    }
                });
                updateThread.start();

                mainWindow.addComponent(autoUpdateLayout);
                mainWindow.addComponent(new MainLayout(
                        new UploadPortletEnvironmentFacade(PortalUtil.getUser(request), isNB)));
                setMainWindow(mainWindow);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                String exceptionMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                getMainWindow().showNotification(Errors.decompose(exceptionMessage), Window.Notification.TYPE_ERROR_MESSAGE);
            }
        }

        @Override
        public void handleActionRequest(ActionRequest request,
                ActionResponse response, Window window) {
        }

        @Override
        public void handleEventRequest(EventRequest request,
                EventResponse response, Window window) {
        }

        @Override
        public void handleResourceRequest(ResourceRequest request,
                ResourceResponse response, Window window) {
        }
    }
}
