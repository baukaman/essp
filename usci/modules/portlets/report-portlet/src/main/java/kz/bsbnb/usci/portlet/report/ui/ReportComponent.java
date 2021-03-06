package kz.bsbnb.usci.portlet.report.ui;

import com.liferay.portal.model.User;
import kz.bsbnb.usci.portlet.report.dm.DatabaseConnect;
import kz.bsbnb.usci.portlet.report.export.AbstractReportExporter;
import kz.bsbnb.usci.portlet.report.dm.Report;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import kz.bsbnb.usci.portlet.report.export.ProtocolsTableReportExporter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Aidar.Myrzahanov
 */
public class ReportComponent extends VerticalLayout {

    private Report report;
    private DatabaseConnect connect;
    public Report getReport() {
        return report;
    }
    
    private ParametersComponent parametersComponent;
    private HorizontalLayout actionsLayout;
    private VerticalLayout outputLayout;
    
    public ReportComponent(Report report, DatabaseConnect connect) {
        this.connect = connect;
        this.report = report;
        parametersComponent = new ParametersComponent(report, connect);
        actionsLayout = new HorizontalLayout();
        actionsLayout.setSpacing(true);
        actionsLayout.setWidth("100%");
        outputLayout = new VerticalLayout();
        outputLayout.setWidth("100%");
        setSpacing(true);
        addComponent(parametersComponent);
        addComponent(actionsLayout);
        addComponent(outputLayout);
    }
    
    public void addReportExporter(AbstractReportExporter actionComponent) {
        actionComponent.setTargetReportComponent(this);
        actionsLayout.addComponent(actionComponent);
    }
    
    public void clearOutputComponents() {
        outputLayout.removeAllComponents();
    }
    
    public void addOutputComponent(Component outputComponent) {
        outputLayout.addComponent(outputComponent);
    }
    
    public CustomDataSource loadData(User user) throws SQLException {
        List<Object> parameterValues = getParameterValues();
        if(parameterValues==null) {
            return null;
        }
        if(!report.getName().equals("ProtocolsByRepDate") && !report.getName().equals("ProtocolsByTime"))
        return getConnect().getDataSourceFromStoredProcedure(report.getProcedureName(), parameterValues);
        else
        {
            return new CustomDataSource(new ProtocolsTableReportExporter(parameterValues,report.getName()).getData(user));
        }
    }

    public ResultSet getResultSet(int firstRowNum, int lastRowNum) throws SQLException {
        List<Object> parameterValues = getParameterValues();
        if(parameterValues==null) {
            return null;
        }
        parameterValues.add(0, lastRowNum);
        parameterValues.add(0, firstRowNum);
        return getConnect().getResultSetFromStoredProcedure(report.getProcedureName(), parameterValues);
    }
    
    public List<Object> getParameterValues() {
        return parametersComponent.getParameterValues();
    }
    
    public List<String> getParameterCaptions() {
        return parametersComponent.getParameterCaptions();
    }
    
    public List<String> getParameterNames() {
        return parametersComponent.getParameterNames();
    }
    
    public List<String> getParameterLocalizedNames() {
        return parametersComponent.getParameterLocalizedNames();
    }

    /**
     * @return the connect
     */
    public DatabaseConnect getConnect() {
        return connect;
    }

    public void setParameters(Properties properties) {
        parametersComponent.setParameterValues(properties);
    }
}
