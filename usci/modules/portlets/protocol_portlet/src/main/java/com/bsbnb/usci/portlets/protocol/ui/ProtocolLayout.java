package com.bsbnb.usci.portlets.protocol.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.bsbnb.usci.portlets.protocol.PortletEnvironmentFacade;
import com.bsbnb.usci.portlets.protocol.ProtocolApplication;
import com.bsbnb.usci.portlets.protocol.ProtocolPortletEnvironmentFacade;
import com.bsbnb.usci.portlets.protocol.ProtocolPortletResource;
import com.bsbnb.usci.portlets.protocol.data.InputInfoDisplayBean;
import com.bsbnb.usci.portlets.protocol.data.SharedDisplayBean;
import com.bsbnb.usci.portlets.protocol.export.XlsProtocolExporter;
import com.bsbnb.usci.portlets.protocol.export.XmlProtocolExporter;
import com.bsbnb.usci.portlets.protocol.export.ZippedProtocolExporter;
import com.bsbnb.vaadin.formattedtable.FormattedTable;
import com.bsbnb.vaadin.messagebox.MessageBoxButtons;
import com.bsbnb.vaadin.messagebox.MessageBoxType;
import com.bsbnb.vaadin.paged.table.control.PagedDataProvider;
import com.bsbnb.vaadin.paged.table.control.PagedTableControl;
import jxl.write.WriteException;
import com.bsbnb.usci.portlets.protocol.data.DataProvider;
import com.bsbnb.usci.portlets.protocol.data.ProtocolDisplayBean;
import com.bsbnb.usci.portlets.protocol.export.ExportException;
import com.bsbnb.usci.portlets.protocol.export.ProtocolExporter;
import com.bsbnb.usci.portlets.protocol.export.TxtProtocolNumbersExporter;
import com.bsbnb.vaadin.filterableselector.FilterableSelect;
import com.bsbnb.vaadin.filterableselector.SelectionCallback;
import com.bsbnb.vaadin.filterableselector.Selector;
import com.bsbnb.vaadin.messagebox.MessageBox;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.cr.model.InputInfo;
import kz.bsbnb.usci.cr.model.Protocol;
import kz.bsbnb.usci.cr.model.SubjectType;
import kz.bsbnb.usci.eav.util.BatchStatuses;
import org.apache.log4j.Logger;

/**
 *
 * @author Marat.Madybayev
 */
public class ProtocolLayout extends VerticalLayout {

    private static final int FILES_TABLE_PAGE_SIZE = 100;
    private static final String LOCALIZATION_PREFIX = "PROTOCOL-LAYOUT.";

    private boolean isProtocolGrouped = false;
    private Map<Object, List<ProtocolDisplayBean>> groupsMapProtocol;
    private List<ProtocolDisplayBean> listOfProtocols;
    private Set<String> prohibitedMessageTypes = new HashSet<String>();
    private DataProvider provider;
    private FilterableSelect<Creditor> creditorSelector;
    private DateField reportDateField;
    private BeanItemContainer<InputInfoDisplayBean> inputInfoContainer;
    private FormattedTable filesTable;
    private VerticalLayout filesTableLayout;
    private PagedTableControl<InputInfoDisplayBean> filesPagedControl;
    private HorizontalLayout typesOfProtocolLayout;
    private Tree groupsOfProtocolTree;
    private Panel groupsTreePanel;
    private BeanItemContainer<ProtocolDisplayBean> protocolsContainer;
    private FormattedTable tableProtocol;
    private VerticalLayout protocolLayout;
    private Label noProtocolsLabel;
    private VerticalLayout fileSignatureDisplayLayout;

    public final Logger logger = Logger.getLogger(ProtocolLayout.class);

    private List<Creditor> selectedCreditors = new ArrayList<Creditor>();

    private static final String[] FILES_TABLE_VISIBLE_COLUMNS = new String[]{
        "creditorName", "fileLink", "receiverDate", "startDate", "completionDate", "statusName", "actualCount", "successCount", "reportDate"};

    private static final String[] FILES_TABLE_VISIBLE_COLUMNS_FOR_BANK_USER = new String[]{
            "fileLink", "receiverDate", "startDate", "completionDate", "statusName", "reportDate"};

    private static final String[] FILES_TABLE_COLUMN_NAMES = new String[]{
        "creditorName", "fileLink", "fileName", "receiverDate", "completionDate", "statusName", "startDate", "actualCount", "successCount", "reportDate"};

    private static final String[] FILES_TABLE_COLUMNS_TO_EXPORT = new String[]{
        "creditorName", "fileName", "receiverDate", "startDate", "completionDate", "statusName", "reportDate"};

    private static final String[] PROTOCOL_TABLE_COLUMNS = new String[]{
        "statusIcon", "message", "note", "link"};

    private static final String[] EXTENDED_PROTOCOL_TABLE_COLUMNS = new String[] {
        "statusIcon", "typeName", "primaryContractDate", "description", "message", "note", "link" };

    private static final String[] EXPORT_PROTOCOL_TABLE_COLUMNS = new String[] {
        "description", "primaryContractDate", "typeName", "messageType", "message", "note"};

    public ProtocolLayout(DataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void attach() {
        List<Creditor> creditorList = provider.getCreditorsList();

        if (creditorList == null || creditorList.isEmpty()) {
            Label errorMessageLabel = new Label(Localization.MESSAGE_NO_CREDITORS.getValue());
            addComponent(errorMessageLabel);
            return;
        }

        creditorSelector = new FilterableSelect<>(creditorList, new Selector<Creditor>() {
            public String getCaption(Creditor item) {
                return item.getName();
            }

            public Object getValue(Creditor item) {
                return item.getId();
            }

            public String getType(Creditor item) {
                SubjectType subjectType = item.getSubjectType();
                String s = PortletEnvironmentFacade.get().isLanguageKazakh() ?
                        subjectType.getNameKz() : subjectType.getNameRu();

                if(s == null || s.length() == 0)
                    return "";

                return s;
            }
        });

        creditorSelector.setImmediate(true);

        reportDateField = new DateField(Localization.REPORT_DATE_CAPTION.getValue());
        reportDateField.setDateFormat("dd.MM.yyyy");

        Button showProtocolButton = new Button(Localization.SHOW_BUTTON_CAPTION.getValue(),
                new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                loadCreditorInfo();
            }
        });

        showProtocolButton.setImmediate(true);

        Label headerOfFilesTableLabel = new Label("<h4>" + Localization.FILES_TABLE_CAPTION.getValue() + "</h4>",
                Label.CONTENT_XHTML);

        Button filesTableExportToXLSButton = new Button(Localization.EXPORT_TO_XLS_CAPTION.getValue(),
                new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                    filesTable.downloadXls("files.xls", FILES_TABLE_COLUMNS_TO_EXPORT,
                            getResourceStrings(FILES_TABLE_COLUMNS_TO_EXPORT));
            }
        });

        filesTableExportToXLSButton.setIcon(ProtocolPortletResource.EXCEL_ICON);

        HorizontalLayout filesTableHeaderLayout = new HorizontalLayout();
        filesTableHeaderLayout.addComponent(headerOfFilesTableLabel);
        filesTableHeaderLayout.setComponentAlignment(headerOfFilesTableLabel, Alignment.MIDDLE_LEFT);
        filesTableHeaderLayout.addComponent(filesTableExportToXLSButton);
        filesTableHeaderLayout.setComponentAlignment(filesTableExportToXLSButton, Alignment.MIDDLE_RIGHT);
        filesTableHeaderLayout.setWidth("100%");

        filesPagedControl = new PagedTableControl<InputInfoDisplayBean>(InputInfoDisplayBean.class, FILES_TABLE_PAGE_SIZE, new PagedDataProvider<InputInfoDisplayBean>() {
            @Override
            public int getCount() {
                return provider.countFiles(selectedCreditors, (Date) reportDateField.getValue());
            }

            @Override
            public List<InputInfoDisplayBean> getRecords(int firstIndex, int count) {
                List<InputInfoDisplayBean> files = provider.loadFiles(selectedCreditors, (Date) reportDateField.getValue(), firstIndex, count);
                if (!files.isEmpty()) {
                    filesTableLayout.setVisible(true);
                } else {
                    MessageBox.Show(Localization.MESSAGE_NO_DATA.getValue(), getWindow());
                }
                return files;
            }
        });

        filesTable = filesPagedControl.getTable();
        filesTable.setStyleName("wordwrap-table");
        filesTable.setVisibleColumns(FILES_TABLE_COLUMN_NAMES);
        filesTable.setColumnHeaders(getResourceStrings(FILES_TABLE_COLUMN_NAMES));
        filesTable.setVisibleColumns(FILES_TABLE_VISIBLE_COLUMNS);
        filesTable.setWidth("100%");
        filesTable.setHeight("400px");
        filesTable.setSelectable(true);
        filesTable.setMultiSelect(false);
        filesTable.setImmediate(true);
        filesTable.addFormat("receiverDate", "dd/MM/yyyy HH:mm:ss");
        filesTable.addFormat("reportDate", "dd/MM/yyyy");
        filesTable.addFormat("completionDate", "dd/MM/yyyy HH:mm:ss");
        filesTable.addFormat("startDate", "dd/MM/yyyy HH:mm:ss");

        filesTable.addListener(new Property.ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    if(((InputInfoDisplayBean) event.getProperty().getValue()).getInputInfo().getUserId() == 100500L
                        && !provider.isNb())
                        return;
                    showProtocol((InputInfoDisplayBean) event.getProperty().getValue());
                }
            }
        });

        filesTableLayout = new VerticalLayout();
        filesTableLayout.addComponent(filesTableHeaderLayout);
        filesTableLayout.addComponent(filesPagedControl);
        filesTableLayout.setSpacing(false);
        filesTableLayout.setWidth("100%");
        filesTableLayout.setVisible(false);

        Label headerProtocolLabel = new Label("<h4>" + Localization.PROTOCOL_TABLE_CAPTION.getValue() + "</h4>",
                Label.CONTENT_XHTML);

        typesOfProtocolLayout = new HorizontalLayout();
        typesOfProtocolLayout.setImmediate(true);

        Button exportProtocolToXLSButton = new Button(Localization.EXPORT_TO_XLS_CAPTION.getValue(),
                new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                ProtocolExporter exporter = new XlsProtocolExporter(EXPORT_PROTOCOL_TABLE_COLUMNS,
                        getResourceStrings(EXPORT_PROTOCOL_TABLE_COLUMNS));

                List<ProtocolDisplayBean> visibleProtocols = Arrays.asList(tableProtocol.getItemIds().
                        toArray(new ProtocolDisplayBean[0]));

                exportProtocolData(exporter, visibleProtocols);
            }
        });

        exportProtocolToXLSButton.setIcon(ProtocolPortletResource.EXCEL_ICON);

        Button exportProtocolToTxtButton = new Button(Localization.EXPORT_NUMBERS.getValue(),
                new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                exportProtocolData(new TxtProtocolNumbersExporter());
            }
        });

        exportProtocolToTxtButton.setIcon(ProtocolPortletResource.TXT_ICON);

        Button exportProtocolToXMLButton = new Button(Localization.EXPORT_TO_XML.getValue(),
                new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                exportProtocolData(new XmlProtocolExporter());
            }
        });

        exportProtocolToXMLButton.setIcon(ProtocolPortletResource.XML_ICON);

        final Button showGroupsToggleButton = new Button(Localization.GROUP_PROTOCOL_RECORDS_BUTTON_CAPTION.
                getValue());

        showGroupsToggleButton.setIcon(ProtocolPortletResource.TREE_ICON);
        showGroupsToggleButton.setImmediate(true);

        showGroupsToggleButton.addListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                isProtocolGrouped = !isProtocolGrouped;
                if (isProtocolGrouped) {
                    showGroupsToggleButton.setCaption(Localization.UNGROUP_PROTOCOL_RECORDS_BUTTON_CAPTION.
                            getValue());

                    showGroupsToggleButton.setStyleName("v-button v-pressed");
                } else {
                    showGroupsToggleButton.setCaption(Localization.GROUP_PROTOCOL_RECORDS_BUTTON_CAPTION.getValue());
                    showGroupsToggleButton.setStyleName("v-button");
                }

                InputInfoDisplayBean inputInfo = (InputInfoDisplayBean) filesTable.getValue();

                if (inputInfo == null)
                    return;

                showProtocol(inputInfo);
            }
        });

        CssLayout headerProtocolLayout = new CssLayout() {
            @Override
            protected String getCss(Component c) {
                if (c instanceof HorizontalLayout)
                    return "float: left";

                if (c instanceof Button)
                    return "float: right";

                return null;
            }
        };
        headerProtocolLayout.addComponent(typesOfProtocolLayout);
        headerProtocolLayout.addComponent(showGroupsToggleButton);
        headerProtocolLayout.addComponent(exportProtocolToXLSButton);
        headerProtocolLayout.addComponent(exportProtocolToTxtButton);
        headerProtocolLayout.addComponent(exportProtocolToXMLButton);
        headerProtocolLayout.setWidth("100%");

        groupsOfProtocolTree = new Tree();
        groupsOfProtocolTree.setHeight("100%");
        groupsOfProtocolTree.setWidth("200px");
        groupsOfProtocolTree.setImmediate(true);
        groupsOfProtocolTree.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                if (event.getProperty() == null || event.getProperty().getValue() == null) {
                    return;
                }
                Object itemId = event.getProperty().getValue();
                logger.info("Clicked item id: "+ itemId);
                List<ProtocolDisplayBean> protocols = groupsMapProtocol.get(itemId);
                if (protocols == null) {
                    return;
                }
                protocolsContainer.removeAllItems();
                protocolsContainer.addAll(protocols);
            }
        });

        groupsTreePanel = new Panel();
        groupsTreePanel.addComponent(groupsOfProtocolTree);
        groupsTreePanel.setScrollable(true);
        groupsTreePanel.setWidth("300px");
        groupsTreePanel.setHeight("100%");


        tableProtocol = new FormattedTable();
        tableProtocol.setStyleName("wordwrap-table");
        protocolsContainer = new BeanItemContainer<>(ProtocolDisplayBean.class);
        tableProtocol.setContainerDataSource(protocolsContainer);
        tableProtocol.addFormat("primaryContractDate", "dd.MM.yyyy");
        tableProtocol.setImmediate(true);
        tableProtocol.setSizeFull();

        HorizontalLayout layoutOfProtocolTable = new HorizontalLayout();
        layoutOfProtocolTable.addComponent(groupsTreePanel);
        layoutOfProtocolTable.addComponent(tableProtocol);
        layoutOfProtocolTable.setExpandRatio(tableProtocol, 1.0f);
        layoutOfProtocolTable.setWidth("100%");
        layoutOfProtocolTable.setImmediate(true);

        noProtocolsLabel = new Label(Localization.NO_PROTOCOLS_BY_INPUT_INFO_MESSAGE.getValue(), Label.CONTENT_XHTML);
        noProtocolsLabel.setVisible(false);
        noProtocolsLabel.setImmediate(true);

        fileSignatureDisplayLayout = new VerticalLayout();

        protocolLayout = new VerticalLayout();
        protocolLayout.addComponent(fileSignatureDisplayLayout);
        protocolLayout.addComponent(headerProtocolLabel);
        protocolLayout.addComponent(headerProtocolLayout);
        protocolLayout.addComponent(layoutOfProtocolTable);
        protocolLayout.setSpacing(false);
        protocolLayout.setWidth("100%");
        protocolLayout.setVisible(false);
        protocolLayout.setImmediate(true);

        setSpacing(true);
        addComponent(creditorSelector);

        if (creditorList.size() == 1) {
            loadCreditorInfo(creditorList);
        } else {
            addComponent(reportDateField);
            addComponent(showProtocolButton);
        }

        addComponent(filesTableLayout);
        addComponent(noProtocolsLabel);
        addComponent(protocolLayout);
        setWidth("100%");
    }

    private void exportProtocolData(ProtocolExporter exporter, List<ProtocolDisplayBean> data) {
        ZippedProtocolExporter zippedExporter = new ZippedProtocolExporter(exporter);
        zippedExporter.setProtocols(data);
        zippedExporter.setApplication(getApplication());
        InputInfoDisplayBean selectedInputInfo = (InputInfoDisplayBean) filesTable.getValue();
        if (selectedInputInfo != null) {
            String filename = selectedInputInfo.getFileName();
            if (filename.endsWith(".zip")) {
                filename = filename.substring(0, filename.length() - 4);
            }
            zippedExporter.setFilenamePrefix(filename);
        }
        try {
            Resource resource = zippedExporter.getResource();
            (getWindow()).open(resource);
        } catch (ExportException ee) {
            logger.info("Failed to export", ee);
            MessageBox.Show(Localization.MESSAGE_EXPORT_FAILED.getValue(), getWindow());
        }
    }

    private String[] getResourceStrings(String[] keys) {
        String[] result = new String[keys.length];
        for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
            result[keyIndex] = ProtocolPortletEnvironmentFacade.get().getResourceString(LOCALIZATION_PREFIX + keys[keyIndex]);
        }
        return result;
    }

    private void setProtocolColumns(String[] columns) {
        tableProtocol.setVisibleColumns(columns);
        tableProtocol.setColumnHeaders(getResourceStrings(columns));
        tableProtocol.setColumnWidth("note", 300);
        tableProtocol.setColumnWidth("message", 300);
    }

    private void showProtocol(InputInfoDisplayBean ii) throws UnsupportedOperationException {
        String signatureInformation = provider.getSignatureInformation(ii);
        String signatureCaption;
        if (signatureInformation == null) {
            signatureCaption = Localization.NO_SIGNATURE_CAPTION.getValue();
        } else {
            String signatureCaptionTemplate = Localization.SIGNATURE_CAPTION.getValue();
            signatureCaption = String.format(signatureCaptionTemplate, signatureInformation);
        }
        Label signatureInfoLabel = new Label(signatureCaption, Label.CONTENT_XHTML);
        fileSignatureDisplayLayout.removeAllComponents();
        fileSignatureDisplayLayout.addComponent(signatureInfoLabel);

        groupsOfProtocolTree.removeAllItems();
        protocolsContainer.removeAllItems();
        typesOfProtocolLayout.removeAllComponents();
        groupsMapProtocol = new HashMap<>();
        if (isProtocolGrouped) {
            showGroupedProtocol(ii);
        } else {
            showProtocolTable(ii);
        }
    }

    private void showProtocolTable(InputInfoDisplayBean ii) throws UnsupportedOperationException {
        typesOfProtocolLayout.setVisible(true);
        groupsTreePanel.setVisible(false);

        prohibitedMessageTypes.clear();
        listOfProtocols = provider.getProtocolsByInputInfo(ii);
        Set<String> messageTypeCodes = new HashSet<>();

        if (listOfProtocols.isEmpty()) {
            Map<String, Long> weightsByErrorCode = new HashMap<>();
            weightsByErrorCode.put(BatchStatuses.ERROR.code(), 1000L);
            weightsByErrorCode.put(BatchStatuses.COMPLETED.code(), 999L);
            weightsByErrorCode.put(BatchStatuses.MAINTENANCE_REQUEST.code(), 20L);
            weightsByErrorCode.put(BatchStatuses.WAITING.code(), 10L);
            weightsByErrorCode.put(BatchStatuses.WAITING_FOR_SIGNATURE.code(), 1L);

            long maxWeight = 0;
            Protocol p = null;

            for (Protocol batchStatus : ii.getInputInfo().getBatchStatuses()) {
                Long weight = weightsByErrorCode.get(batchStatus.getMessageType().getCode());
                if (weight == null)
                    weight = 0L;

                if (maxWeight < weight) {
                    maxWeight = weight;
                    p = batchStatus;
                }
            }

            if (p != null)
                listOfProtocols.add(new ProtocolDisplayBean(p));
        }else{
            List<ProtocolDisplayBean> protocolStatistics = provider.getProtocolStatisticsByInputInfo(ii);
            listOfProtocols.addAll(protocolStatistics);
        }

        if (listOfProtocols.isEmpty()) {
            noProtocolsLabel.setVisible(true);
            protocolLayout.setVisible(false);
        } else {
            for (ProtocolDisplayBean protocolDisplayBean : listOfProtocols) {
                addProtocol(protocolDisplayBean, messageTypeCodes);
            }
            setProtocolColumns(EXTENDED_PROTOCOL_TABLE_COLUMNS);
            noProtocolsLabel.setVisible(false);
            protocolLayout.setVisible(true);
        }
    }

    private void addProtocol(final ProtocolDisplayBean protocol, Set<String> messageTypes) {
        tableProtocol.addItem(protocol);
        if (!messageTypes.contains(protocol.getMessageType())) {
            messageTypes.add(protocol.getMessageType());
            Button filterButton = new Button(null, new Button.ClickListener() {

                public void buttonClick(ClickEvent event) {
                    Button button = event.getButton();
                    String styleName = button.getStyleName();
                    if ("v-button v-pressed".equals(styleName)) {
                        button.setStyleName("v-button");
                        prohibitedMessageTypes.add(protocol.getMessageType());
                    } else {
                        button.setStyleName("v-button v-pressed");
                        prohibitedMessageTypes.remove(protocol.getMessageType());
                    }
                    updateProtocolTable();
                }
            });
            filterButton.setStyleName("v-button v-pressed");
            filterButton.setIcon(protocol.getStatusIcon().getSource());
            filterButton.setDescription(protocol.getMessageType());
            filterButton.setImmediate(true);
            typesOfProtocolLayout.addComponent(filterButton);
        }
    }

    private void showGroupedProtocol(InputInfoDisplayBean ii) throws UnsupportedOperationException {
        typesOfProtocolLayout.setVisible(false);
        Map<SharedDisplayBean, Map<String, List<ProtocolDisplayBean>>> protocolMap = provider.getProtocolsByInputInfoGrouped(ii);
        Object firstItemId = null;
        for (Entry<SharedDisplayBean, Map<String, List<ProtocolDisplayBean>>> typeEntry : protocolMap.entrySet()) {
            Object typeItemId = groupsOfProtocolTree.addItem();
            if (firstItemId == null) {
                firstItemId = typeItemId;
            }
            if ("CREDIT".equals(typeEntry.getKey().getCode())) {
                groupsOfProtocolTree.setItemIcon(typeItemId, ProtocolPortletResource.CREDIT_CARD_ICON);
            } else if ("PORTFOLIO".equals(typeEntry.getKey().getCode())) {
                groupsOfProtocolTree.setItemIcon(typeItemId, ProtocolPortletResource.BRIEFCASE_ICON);
            }
            groupsOfProtocolTree.setItemCaption(typeItemId, typeEntry.getKey().getName());
            groupsOfProtocolTree.setChildrenAllowed(typeItemId, false);
            for (Entry<String, List<ProtocolDisplayBean>> entry : typeEntry.getValue().entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty()) {
                    groupsMapProtocol.put(typeItemId, entry.getValue());
                } else {
                    groupsOfProtocolTree.setChildrenAllowed(typeItemId, true);
                    Object keyItemId = groupsOfProtocolTree.addItem();
                    groupsOfProtocolTree.setItemCaption(keyItemId, entry.getKey());
                    groupsOfProtocolTree.setParent(keyItemId, typeItemId);
                    groupsOfProtocolTree.setChildrenAllowed(keyItemId, false);
                    groupsMapProtocol.put(keyItemId, entry.getValue());
                }

            }
            groupsOfProtocolTree.expandItem(typeItemId);
            if (!groupsOfProtocolTree.hasChildren(typeItemId) && !groupsMapProtocol.containsKey(typeItemId)) {
                groupsOfProtocolTree.removeItem(typeItemId);
            }
        }
        int groupItemsCount = groupsOfProtocolTree.getItemIds().size();
        logger.info("Group items count: "+ groupItemsCount);
        if (groupItemsCount == 0) {
            noProtocolsLabel.setVisible(true);
            protocolLayout.setVisible(false);
            groupsTreePanel.setVisible(false);
        } else {
            // TODO check batchStatuses

            noProtocolsLabel.setVisible(false);
            protocolLayout.setVisible(true);
            groupsTreePanel.setVisible(true);
            groupsOfProtocolTree.select(firstItemId);
        }
        setProtocolColumns(PROTOCOL_TABLE_COLUMNS);
    }

    private void updateProtocolTable() {
        protocolsContainer.removeAllItems();
        List<ProtocolDisplayBean> filteredProtocols = new ArrayList<>();
        for (ProtocolDisplayBean protocol : listOfProtocols) {
            if (!prohibitedMessageTypes.contains(protocol.getMessageType())) {
                filteredProtocols.add(protocol);
            }
        }
        protocolsContainer.addAll(filteredProtocols);
    }

    private void loadCreditorInfo() {
        creditorSelector.getSelectedElements(new SelectionCallback<Creditor>() {

            public void selected(List<Creditor> creditors) {
                loadCreditorInfo(Arrays.asList(creditors.toArray(new Creditor[0])));
            }
        });
    }

    private void loadCreditorInfo(List<Creditor> creditors) {
        List<Creditor> newList = new ArrayList<>(creditors);
        if (PortletEnvironmentFacade.get().isNB()) {
            Creditor nb = new Creditor();
            nb.setId(0L);
            nb.setName("НБРК");
            nb.setShortName("НБРК");
            newList.add(nb);
        }
        selectedCreditors = newList;
        filesTableLayout.setVisible(false);
        protocolLayout.setVisible(false);
        filesPagedControl.reload();
    }

    private void exportProtocolData(ProtocolExporter exporter) {
        List<ProtocolDisplayBean> data = provider.getProtocolsByInputInfo((InputInfoDisplayBean) filesTable.getValue());
        exportProtocolData(exporter, data);
    }
}
