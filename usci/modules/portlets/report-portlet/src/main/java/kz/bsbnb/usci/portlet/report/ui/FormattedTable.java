package kz.bsbnb.usci.portlet.report.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import jxl.CellView;
import jxl.Workbook;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.write.DateFormat;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import com.vaadin.data.Property;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.StreamResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 *
 * @author Marat.Madybayev
 */
public class FormattedTable extends Table {

    /** Java сериализация UID. */
    private static final long serialVersionUID = 804740605642799781L;

    public FormattedTable() {
        alwaysRecalculateColumnWidths = true;
    }

    public FormattedTable(String caption) {
        super(caption);
        alwaysRecalculateColumnWidths = true;
    }
    private HashMap<Object, String> formatData = new HashMap<Object, String>();

    public void addFormat(Object propertyId, String formatString) {
        formatData.put(propertyId, formatString);
    }

    @Override
    protected String formatPropertyValue(Object rowId, Object colId, Property property) {
        if (property == null) {
            return "";
        }
        Object value = property.getValue();
        if (value == null) {
            return "";
        }
        if (formatData.containsKey(colId) && Date.class.isAssignableFrom(property.getType())) {
            String formatString = formatData.get(colId);
            SimpleDateFormat sdf = new SimpleDateFormat(formatString);
            return sdf.format((Date) value);
        }
        return super.formatPropertyValue(rowId, colId, property);
    }

    public byte[] exportToXLS(Object[] columnIDs) throws WriteException, IOException {
        WritableWorkbook workbook = null;
        WritableFont times12font = new WritableFont(WritableFont.TIMES, 12);
        WritableFont times12fontBold = new WritableFont(WritableFont.TIMES, 12, WritableFont.BOLD);
        WritableCellFormat times12format = new WritableCellFormat(times12font);
        WritableCellFormat times12formatBold = new WritableCellFormat(times12fontBold);
        CellView cellView = new CellView();
        cellView.setSize(10000);

        times12formatBold.setAlignment(jxl.format.Alignment.CENTRE);
        times12formatBold.setBorder(Border.ALL, BorderLineStyle.THIN);
        times12format.setBorder(Border.ALL, BorderLineStyle.THIN);
        ByteArrayOutputStream baos = null;
        baos = new ByteArrayOutputStream();
        workbook = Workbook.createWorkbook(baos);
        WritableSheet sheet = workbook.createSheet("Sheet1", 0);

        WritableCellFormat[] dateFormats = new WritableCellFormat[columnIDs.length];

        int rowCounter = 0;
        for (int columnIndex = 0; columnIndex < columnIDs.length; columnIndex++) {
            Object columnID = columnIDs[columnIndex];
            sheet.addCell(new Label(columnIndex, rowCounter, getColumnHeader(columnID), times12formatBold));
            String formatString = "dd.MM.yyyy";
            if (formatData.containsKey(columnID)) {
                formatString = formatData.get(columnID);
            }
            dateFormats[columnIndex] = new WritableCellFormat(new DateFormat(formatString));
            dateFormats[columnIndex].setBorder(Border.ALL, BorderLineStyle.THIN);
        }
        rowCounter++;



        for (Object itemID : getItemIds()) {
            for (int columnIndex = 0; columnIndex < columnIDs.length; columnIndex++) {
                Object columnID = columnIDs[columnIndex];
                Object value = getContainerProperty(itemID, columnIDs[columnIndex]).getValue();

                if (value != null) {
                    if (value instanceof String) {
                        sheet.addCell(new jxl.write.Label(columnIndex, rowCounter, value.toString(), times12format));

                    } else if (value instanceof Number) {
                        jxl.write.Number number = new jxl.write.Number(columnIndex, rowCounter, Integer.parseInt(value.toString()), times12format);
                        sheet.addCell(number);
                    } else if (value instanceof Date) {
                        sheet.addCell(new jxl.write.DateTime(columnIndex, rowCounter, (Date) value, dateFormats[columnIndex]));
                    } else if (value instanceof Property) {
                        Object propertyValue = ((Property) value).getValue();
                        sheet.addCell(new Label(columnIndex, rowCounter, propertyValue == null ? "" : propertyValue.toString(), times12format));
                    } else if (value instanceof Component) {
                        sheet.addCell(new Label(columnIndex, rowCounter, ((Component) value).getCaption(), times12format));
                    }
                }
                sheet.setColumnView(columnIndex, cellView);
            }
            rowCounter++;
        }

        workbook.write();

        workbook.close();


        return baos.toByteArray();
    }

    public void downloadXls(String filename) throws WriteException, IOException {
        final byte[] bytes = exportToXLS(getVisibleColumns());
        StreamResource.StreamSource streamSource = new StreamResource.StreamSource() {

            public InputStream getStream() {
                return new ByteArrayInputStream(bytes);
            }
        };
        StreamResource resource = new StreamResource(streamSource, filename, getApplication()) {

            @Override
            public DownloadStream getStream() {
                DownloadStream downloadStream = super.getStream();
                downloadStream.setParameter("Content-Disposition", "attachment;filename=" + getFilename());
                downloadStream.setContentType("application/vnd.ms-excel");
                downloadStream.setCacheTime(0);
                return downloadStream;
            }
        };
        getWindow().open(resource, "_blank");
    }
}