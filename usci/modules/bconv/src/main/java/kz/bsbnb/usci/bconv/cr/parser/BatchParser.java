package kz.bsbnb.usci.bconv.cr.parser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import kz.bsbnb.usci.eav.model.Batch;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.persistance.dao.IRefProcessorDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseEntityLoadDao;
import kz.bsbnb.usci.eav.repository.IMetaClassRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public abstract class BatchParser {
    protected XMLEventReader xmlReader;
    protected DateFormat dateFormat = new SimpleDateFormat(Const.DATE_FORMAT);

    private Logger logger = Logger.getLogger(BatchParser.class);

    protected Batch batch;

    protected BaseEntity currentBaseEntity = null;

    protected boolean hasMore = false;

    protected long index;

    protected long creditorId;

    @Autowired
    protected IMetaClassRepository metaClassRepository;

    @Autowired
    protected IBaseEntityLoadDao baseEntityLoadDao;

    @Autowired
    protected IRefProcessorDao refProcessorDao;

    public BatchParser() {
        super();
        dateFormat.setLenient(false);
    }

    public void parse(XMLEventReader xmlReader, Batch batch, long index, long creditorId) throws SAXException {
        this.batch = batch;
        this.xmlReader = xmlReader;
        this.index = index;
        this.creditorId = creditorId;

        init();

        while (xmlReader.hasNext()) {
            XMLEvent event = (XMLEvent) xmlReader.next();

            if (event.isStartDocument()) {
                logger.info("start document");
            } else if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                String localName = startElement.getName().getLocalPart();

                if (startElement(event, startElement, localName)) break;
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                String localName = endElement.getName().getLocalPart();

                if (endElement(localName)) break;
            } else if (event.isEndDocument()) {
                logger.info("end document");
            } else {
                logger.info(event.toString());
            }
        }
    }

    public abstract boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException;

    public abstract boolean endElement(String localName) throws SAXException;
    public void init() {}

    public BaseEntity getCurrentBaseEntity() {
        return currentBaseEntity;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public long getIndex() {
        return index;
    }

    public String trim(String data) {
        return data.trim();
    }

    public void setCreditorId(long creditorId) {
        this.creditorId = creditorId;
    }
}
