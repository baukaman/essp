package kz.bsbnb.usci.bconv.cr.parser.impl;

import kz.bsbnb.usci.bconv.cr.parser.BatchParser;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.UnknownTagException;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseValue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Date;

/**
 *
 * @author k.tulbassiyev
 */
@Component
@Scope("prototype")
public class ChangeRemainsCorrectionParser extends BatchParser {
    public ChangeRemainsCorrectionParser() {
        super();
    }

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains_correction"),new Date());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {
        if(localName.equals("correction")) {
        } else if(localName.equals("value")) {
            event = (XMLEvent) xmlReader.next();
            currentBaseEntity.put("value",new BaseValue(batch,index,new Double(event.asCharacters().getData())));
        } else if(localName.equals("value_currency")) {
            event = (XMLEvent) xmlReader.next();
            currentBaseEntity.put("value_currency",new BaseValue(batch,index,
                    new Double(event.asCharacters().getData())));
        } else if(localName.equals("balance_account")) {
            event = (XMLEvent) xmlReader.next();
            BaseEntity baseEntity = new BaseEntity(metaClassRepository.getMetaClass("ref_balance_account"),new Date());
            baseEntity.put("no_",new BaseValue(batch,index,event.asCharacters().getData()));
            currentBaseEntity.put("balance_account", new BaseValue(batch,index,baseEntity) );
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }
    
    @Override
    public boolean endElement(String localName) throws SAXException {
        if(localName.equals("correction")) {
            //ctRemains.setCorrection(ctRemainsTypeCorrection);
            //xmlReader.setContentHandler(contentHandler);
            return true;
        } else if(localName.equals("value")) {
            //ctRemainsTypeCorrection.setValue(new BigDecimal(contents.toString()));
        } else if(localName.equals("value_currency")) {
            //ctRemainsTypeCorrection.setValueCurrency(new BigDecimal(contents.toString()));
        } else if(localName.equals("balance_account")) {
            //ctRemainsTypeCorrection.setBalanceAccount(contents.toString());
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }
}
