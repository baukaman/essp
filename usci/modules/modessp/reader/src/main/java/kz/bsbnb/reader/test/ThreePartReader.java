package kz.bsbnb.reader.test;

import kz.bsbnb.DataEntity;
import kz.bsbnb.reader.RootReader;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.List;

@Component
public class ThreePartReader {
    XMLEventReader xmlEventReader;
    private MetaClass meta;
    protected List<DataEntity> refs;

    public DataEntity read() throws XMLStreamException {
        DataEntity entity = null;
        InfoReader infoReader = null;

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartDocument()) {

            } else if (xmlEvent.isStartElement()) {
                String localName = xmlEvent.asStartElement().getName().getLocalPart();
                if(localName.equals("info")) {
                    infoReader = new InfoReader(xmlEventReader)
                            .withExitTag(localName);
                    infoReader.read();

                } else if(localName.equals("entities")) {
                    entity = new RootReader(xmlEventReader)
                            .withMeta(meta)
                            .withExitTag(localName)
                            .read();
                    entity.setCreditorId(infoReader.getCreditorId());
                    entity.setReportDate(infoReader.getReportDate());
                } else if(localName.equals("refs")) {
                    RefsReader refsReader = new RefsReader(xmlEventReader);

                    refsReader.withMeta(meta)
                            .withExitTag(localName)
                            .read();

                    refs = refsReader.getRefs();
                }
            }
        }

        return entity;
    }

    public ThreePartReader withSource(InputStream source) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty("javax.xml.stream.isCoalescing", true);
        xmlEventReader = inputFactory.createXMLEventReader(source);
        return this;
    }

    public ThreePartReader withMeta(MetaClass meta) {
        this.meta = meta;
        return this;
    }

    public List<DataEntity> getRefs() {
        return refs;
    }
}
