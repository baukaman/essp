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
public class ChangeCreditFlowParser extends BatchParser {
    public ChangeCreditFlowParser() {
        super();
    }

    private BaseEntity currentProvisionGroup;

    private BaseEntity currentProvisionKfn;
    private BaseEntity currentProvisionMsfo;
    private BaseEntity currentProvisionMsfoOverB;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("credit_flow"), batch.getRepDate());
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName)
            throws SAXException {
        if (localName.equals("credit_flow")) {
        } else if (localName.equals("classification")) {
            BaseEntity classification = new BaseEntity(
                    metaClassRepository.getMetaClass("ref_classification"),
                    batch.getRepDate()
            );
            event = (XMLEvent) xmlReader.next();
            classification.put("code", new BaseEntityStringValue(-1, batch, index, event.asCharacters().getData()));
            currentBaseEntity.put("classification", new BaseEntityComplexValue(-1, batch, index, classification));
        } else if (localName.equals("provision")) {
            currentProvisionGroup = new BaseEntity(metaClassRepository.getMetaClass("provision_group"),
                    batch.getRepDate());

            currentProvisionKfn = null;
            currentProvisionMsfo = null;
            currentProvisionMsfoOverB = null;
        } else if (localName.equals("value")) {
            event = (XMLEvent) xmlReader.next();
            getCurrentProvisionKfn().put("value",
                    new BaseEntityDoubleValue(-1, batch, index, new Double(event.asCharacters().getData())));
        } else if (localName.equals("balance_account")) {
            event = (XMLEvent) xmlReader.next();
            BaseEntity balanceAccount = new BaseEntity(metaClassRepository.getMetaClass("ref_balance_account"),
                    batch.getRepDate());

            balanceAccount.put("no_", new BaseEntityStringValue(-1, batch, index, event.asCharacters().getData()));
            getCurrentProvisionKfn().put("balance_account", new BaseEntityComplexValue(-1,
                    batch, index, balanceAccount));
        } else if (localName.equals("value_msfo")) {
            event = (XMLEvent) xmlReader.next();
            getCurrentProvisionMsfo().put("value",
                    new BaseEntityDoubleValue(-1, batch, index, new Double(event.asCharacters().getData())));
        } else if (localName.equals("balance_account_msfo")) {
            event = (XMLEvent) xmlReader.next();
            BaseEntity balanceAccount = new BaseEntity(metaClassRepository.getMetaClass("ref_balance_account"),
                    batch.getRepDate());
            balanceAccount.put("no_", new BaseEntityStringValue(-1, batch, index, event.asCharacters().getData()));
            getCurrentProvisionMsfo().put("balance_account", new BaseEntityComplexValue(-1,
                    batch, index, balanceAccount));
        } else if (localName.equals("value_msfo_over_balance")) {
            event = (XMLEvent) xmlReader.next();
            getCurrentProvisionMsfoOverB().put("value",
                    new BaseEntityDoubleValue(-1, batch, index, new Double(event.asCharacters().getData())));
        } else if (localName.equals("balance_account_msfo_over_balance")) {
            event = (XMLEvent) xmlReader.next();
            BaseEntity balanceAccount = new BaseEntity(metaClassRepository.getMetaClass("ref_balance_account"),
                    batch.getRepDate());
            balanceAccount.put("no_", new BaseEntityStringValue(-1, batch, index, event.asCharacters().getData()));
            getCurrentProvisionMsfoOverB().put("balance_account", new BaseEntityComplexValue(-1, batch, index,
                    balanceAccount));
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        if (localName.equals("credit_flow")) {
            return true;
        } else if (localName.equals("classification")) {
        } else if (localName.equals("provision")) {
            if (currentProvisionKfn != null)
                currentProvisionGroup.put("provision_kfn", new BaseEntityComplexValue(-1, batch, index,
                        currentProvisionKfn));

            if (currentProvisionMsfo != null)
                currentProvisionGroup.put("provision_msfo", new BaseEntityComplexValue(-1, batch, index,
                        currentProvisionMsfo));

            if (currentProvisionMsfoOverB != null)
                currentProvisionGroup.put("provision_msfo_over_balance", new BaseEntityComplexValue(-1, batch, index,
                        currentProvisionMsfoOverB));

            currentBaseEntity.put("provision", new BaseEntityComplexValue(-1, batch, index, currentProvisionGroup));

        } else if (localName.equals("balance_account")) {
        } else if (localName.equals("balance_account_msfo")) {
        } else if (localName.equals("balance_account_msfo_over_balance")) {
        } else if (localName.equals("value")) {
        } else if (localName.equals("value_msfo")) {
        } else if (localName.equals("value_msfo_over_balance")) {
        } else {
            throw new UnknownTagException(localName);
        }

        return false;
    }

    private BaseEntity getCurrentProvisionKfn() {
        if (currentProvisionKfn == null)
            currentProvisionKfn = new BaseEntity(metaClassRepository.getMetaClass("provision"), batch.getRepDate());
        return currentProvisionKfn;
    }

    private BaseEntity getCurrentProvisionMsfo() {
        if (currentProvisionMsfo == null)
            currentProvisionMsfo = new BaseEntity(metaClassRepository.getMetaClass("provision"), batch.getRepDate());
        return currentProvisionMsfo;
    }

    private BaseEntity getCurrentProvisionMsfoOverB() {
        if (currentProvisionMsfoOverB == null)
            currentProvisionMsfoOverB = new BaseEntity(metaClassRepository.getMetaClass("provision"),
                    batch.getRepDate());
        return currentProvisionMsfoOverB;
    }
}
