package kz.bsbnb.reader.test;

import kz.bsbnb.DataEntity;
import kz.bsbnb.reader.AttributeReader;
import kz.bsbnb.reader.base.BaseReader;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.List;

public class RefsReader extends BaseReader {

    List<DataEntity> refs = new LinkedList<>();

    public RefsReader(XMLEventReader xmlEventReader) {
        super(xmlEventReader);
    }

    public RefsReader withExitTag(String localName) {
        this.exitTag = localName;
        return this;
    }

    public void read() throws XMLStreamException {
        while(xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if(xmlEvent.isStartElement()) {
                String localPart = xmlEvent.asStartElement().getName().getLocalPart();
                long id = Long.parseLong(xmlEvent.asStartElement().getAttributeByName(new QName("id")).getValue());
                if(localPart.equals("balance_account")) {
                    readRef("change.remains.debt.current.balance_account", localPart, id);
                } else if(localPart.equals("creditor")) {
                    readRef("creditor", localPart, id);
                } else if(localPart.equals("pledge_type")) {
                    readRef("pledges.pledge_type",localPart, id);
                } else if(localPart.equals("currency")) {
                    readRef("currency", localPart, id);
                }

            } else if(xmlEvent.isEndElement()) {
                String localPart = xmlEvent.asEndElement().getName().getLocalPart();
                if(localPart.equals(exitTag))
                    return;
            }
        }
    }

    void readRef(String path, String localPart, Long id) throws XMLStreamException {
        DataEntity entity = new DataEntity(((MetaClass) metaClass.getEl(path)));
        new AttributeReader(xmlEventReader)
                .withRootEntity(entity)
                .withExitTagName(localPart)
                .read();
        entity.setId(id);
        refs.add(entity);
    }

    public RefsReader withMeta(MetaClass meta) {
        metaClass = meta;
        return this;
    }

    public List<DataEntity> getRefs() {
        return refs;
    }
}
