package kz.bsbnb.usci.bconv.cr.parser.impl;

import kz.bsbnb.usci.bconv.cr.parser.BatchParser;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.UnknownTagException;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityComplexValue;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityDoubleValue;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityStringValue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@Component
@Scope("prototype")
public class PledgesParser extends BatchParser {
    public PledgesParser() {
        super();
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        if (localName.equals("pledges")) {
        } else if (localName.equals("pledge")) {
            currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("pledge"), batch.getRepDate(), creditorId);
        } else if (localName.equals("pledge_type")) {
            event = (XMLEvent) xmlReader.next();
            BaseEntity pledgeType = new BaseEntity(metaClassRepository.getMetaClass("ref_pledge_type"),
                    batch.getRepDate(), creditorId);
            pledgeType.put("code", new BaseEntityStringValue(0, creditorId, batch.getRepDate(),
                    event.asCharacters().getData(), false, true));
            currentBaseEntity.put("pledge_type", new BaseEntityComplexValue(0, creditorId, batch.getRepDate(),pledgeType,
                    false, true));
        } else if (localName.equals("contract")) {
        } else if (localName.equals("no")) {
            event = (XMLEvent) xmlReader.next();
            currentBaseEntity.put("contract", new BaseEntityStringValue(0, creditorId, batch.getRepDate(),
                    event.asCharacters().getData(), false, true));
        } else if (localName.equals("value")) {
            event = (XMLEvent) xmlReader.next();
            currentBaseEntity.put("value", new BaseEntityDoubleValue(0, creditorId, batch.getRepDate(),
                    new Double(event.asCharacters().getData()), false, true));
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        if (localName.equals("pledges")) {
            hasMore = false;
            return true;
        } else if (localName.equals("pledge")) {
            hasMore = true;
            return true;
        } else if (localName.equals("pledge_type")) {
        } else if (localName.equals("contract")) {
        } else if (localName.equals("no")) {
        } else if (localName.equals("value")) {
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }
}
