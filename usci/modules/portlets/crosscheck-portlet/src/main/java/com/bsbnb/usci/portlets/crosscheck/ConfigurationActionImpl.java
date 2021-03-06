package com.bsbnb.usci.portlets.crosscheck;

import java.util.logging.Level;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import org.apache.log4j.Logger;

/**
 *
 * @author Aidar.Myrzahanov
 */
public class ConfigurationActionImpl implements ConfigurationAction {
    
    public static final String BUSINESS_RULES_URL_KEY = "business_rules_url";
    private final Logger logger = Logger.getLogger(ConfigurationActionImpl.class);

    public void processAction(PortletConfig pc, ActionRequest ar, ActionResponse ar1) throws Exception {
        String portletResource = ParamUtil.getString(ar, "portletResource");

        PortletPreferences prefs = PortletPreferencesFactoryUtil.getPortletSetup(ar, portletResource);
        String typeValue = ar.getParameter("type");
        logger.info(typeValue);
        if(typeValue!=null) {
            logger.info("Setting: "+typeValue);
            prefs.setValue("type", typeValue);
        }
        String businessRulesUrl = ar.getParameter(BUSINESS_RULES_URL_KEY);
        if(businessRulesUrl!=null) {
            prefs.setValue(BUSINESS_RULES_URL_KEY, businessRulesUrl);
        }

        prefs.store();
        // todo: add function support
        SessionMessages.add(ar, pc.getPortletName() + ".doConfigure");
    }

    public String render(PortletConfig pc, RenderRequest rr, RenderResponse rr1) throws Exception {
        return "/configuration.jsp";
    }
}
