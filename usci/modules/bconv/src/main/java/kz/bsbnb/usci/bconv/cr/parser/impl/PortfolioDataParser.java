package kz.bsbnb.usci.bconv.cr.parser.impl;

import kz.bsbnb.usci.bconv.cr.parser.BatchParser;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.UnknownTagException;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseEntityComplexSet;
import kz.bsbnb.usci.eav.model.base.impl.value.BaseSetComplexValue;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@Component
@Scope("prototype")
public class PortfolioDataParser extends BatchParser {
    @Autowired
    private PortfolioFlowParser portfolioFlowParser = new PortfolioFlowParser();

    @Autowired
    private PortfolioFlowMsfoParser portfolioFlowMsfoParser = new PortfolioFlowMsfoParser();

    public PortfolioDataParser() {
        super();
    }

    private BaseSet portfolioFlow;
    private BaseSet portfolioFlowMsfo;

    private MetaClass portfolioFlowKfnMeta, portfolioFlowMsfoMeta;

    @Override
    public void init() {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("portfolio_data"),
                batch.getRepDate(), creditorId);

        portfolioFlowKfnMeta = metaClassRepository.getMetaClass("portfolio_flow_kfn");
        portfolioFlowMsfoMeta = metaClassRepository.getMetaClass("portfolio_flow_msfo");
    }

    @Override
    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        switch (localName) {
            case "portfolio_data":
                // do nothing
                break;
            case "portfolio_flow":
                portfolioFlowParser.parse(xmlReader, batch, index, creditorId);
                getPortfolioFlow().put(new BaseSetComplexValue(0, creditorId, batch.getRepDate(),
                        portfolioFlowParser.getCurrentBaseEntity(), false, true));

                break;
            case "portfolio_flow_msfo":
                portfolioFlowMsfoParser.parse(xmlReader, batch, index, creditorId);
                getPortfolioFlowMsfo().put(new BaseSetComplexValue(0, creditorId, batch.getRepDate(),
                        portfolioFlowMsfoParser.getCurrentBaseEntity(), false, true));

                break;
            default:
                throw new UnknownTagException(localName);
        }

        return false;
    }

    @Override
    public boolean endElement(String localName) throws SAXException {
        if (localName.equals("portfolio_data")) {
            if (portfolioFlow != null)
                currentBaseEntity.put("portfolio_flows_kfn",
                        new BaseEntityComplexSet(0, creditorId, batch.getRepDate(), portfolioFlow, false, true));

            if (portfolioFlowMsfo != null)
                currentBaseEntity.put("portfolio_flows_msfo",
                        new BaseEntityComplexSet(0, creditorId, batch.getRepDate(), portfolioFlowMsfo, false, true));

            return true;
        } else {
            throw new UnknownTagException(localName);
        }
    }

    private BaseSet getPortfolioFlow() {
        if (portfolioFlow == null)
            portfolioFlow = new BaseSet(portfolioFlowKfnMeta, creditorId);
        return portfolioFlow;
    }

    private BaseSet getPortfolioFlowMsfo() {
        if (portfolioFlowMsfo == null)
            portfolioFlowMsfo = new BaseSet(portfolioFlowMsfoMeta, creditorId);
        return portfolioFlowMsfo;
    }
}
