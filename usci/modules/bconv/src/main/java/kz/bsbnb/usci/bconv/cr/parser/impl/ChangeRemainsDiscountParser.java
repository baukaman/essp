package kz.bsbnb.usci.bconv.cr.parser.impl;

import kz.bsbnb.usci.bconv.cr.parser.BatchParser;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.UnknownTagException;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityComplexValue;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityDoubleValue;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityStringValue;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@Component
@Scope("prototype")
public class ChangeRemainsDiscountParser extends BatchParser {
    public ChangeRemainsDiscountParser() {
        super();
    }

    private MetaClass refBalanceAccountMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("remains_discount"), batch.getRepDate(), creditorId);
        refBalanceAccountMeta = metaClassRepository.getMetaClass("ref_balance_account");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "discount":
                break;
            case "value":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("value",
                        new BaseEntityDoubleValue(0, creditorId, batch.getRepDate(),
                                new Double(trim(event.asCharacters().getData())), false, true));
                break;
            case "value_currency":
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("value_currency",
                        new BaseEntityDoubleValue(0, creditorId, batch.getRepDate(),
                                new Double(trim(event.asCharacters().getData())), false, true));
                break;
            case "balance_account":
                event = (XMLEvent) xmlReader.next();

                BaseEntity balanceAccount = new BaseEntity(refBalanceAccountMeta, batch.getRepDate(), creditorId);

                balanceAccount.put("no_",
                        new BaseEntityStringValue(0, creditorId, batch.getRepDate(), trim(event.asCharacters().getData()), false, true));

                currentBaseEntity.put("balance_account",
                        new BaseEntityComplexValue(0, creditorId, batch.getRepDate(), balanceAccount, false, true));
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        switch (localName) {
            case "discount":
                return true;
            case "value":
                break;
            case "value_currency":
                break;
            case "balance_account":
                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }
}
