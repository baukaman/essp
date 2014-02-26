package com.bsbnb.creditregistry.portlets.upload;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.bsbnb.creditregistry.portlets.upload.ui.MainLayout;
import com.bsbnb.creditregistry.portlets.upload.ui.SingleUploadComponent;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.util.PortalUtil;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.PortletApplicationContext2;
import com.vaadin.terminal.gwt.server.PortletApplicationContext2.PortletListener;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

public class UploadApplication extends Application {

    private static final long serialVersionUID = 2096197512742005243L;
    public static final Logger log = Logger.getLogger(SingleUploadComponent.class.getCanonicalName());

    //TODO: Выдавать стандартную ошибку авторизации пользователя
    @Override
    public void init() {
        setMainWindow(new Window());

        if (getContext() instanceof PortletApplicationContext2) {
            PortletApplicationContext2 ctx =
                    (PortletApplicationContext2) getContext();

            ctx.addPortletListener(this, new SamplePortletListener());
        } else {
            getMainWindow().showNotification("Not inited via Portal!", Notification.TYPE_ERROR_MESSAGE);
        }

    }

    private class SamplePortletListener implements PortletListener {

        private static final long serialVersionUID = -5984011853767129565L;

        @Override
        public void handleRenderRequest(RenderRequest request, RenderResponse response, Window window) {
            try {
                Window mainWindow = new Window();
                mainWindow.addComponent(new MainLayout(new UploadPortletEnvironmentFacade(PortalUtil.getUser(request))));
                setMainWindow(mainWindow);
            } catch (PortalException pe) {
                log.log(Level.SEVERE, "", pe);
            } catch (SystemException se) {
                log.log(Level.SEVERE, "", se);
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