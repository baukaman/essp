package kz.bsbnb.usci.portlets.query;

import com.bsbnb.vaadin.base.portlet.BaseApplication;
import com.bsbnb.vaadin.base.portlet.PortletEnvironment;
import com.vaadin.ui.Window;
import kz.bsbnb.usci.eav.util.Errors;
import org.apache.log4j.Logger;

public class QueryApplication extends BaseApplication {

    private static final long serialVersionUID = 2096197512742005243L;
    private final Logger logger = Logger.getLogger(QueryApplication.class);

    @Override
    protected Window createWindow(PortletEnvironment env) {
        Window mainWindow = new Window();
        try{
            if (env.isUserAdmin()) {
                SqlExecutor executor = new SqlExecutor(new QuerySettings(env.getRequest()));
                mainWindow.addComponent(new QueryComponent(executor));
            }
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            String exceptionMessage = e.getMessage() != null ? e.getMessage() : e.toString();
            getMainWindow().showNotification(Errors.decompose(exceptionMessage), Window.Notification.TYPE_ERROR_MESSAGE);
        }
        return mainWindow;
    }

}
