package kz.bsbnb.usci.bconv.cr.parser.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.Stack;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.TypeErrorException;
import kz.bsbnb.usci.bconv.cr.parser.exceptions.UnknownTagException;
import kz.bsbnb.usci.bconv.cr.parser.BatchParser;
import kz.bsbnb.usci.bconv.cr.parser.util.ParserUtils;
import kz.bsbnb.usci.eav.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav.model.base.impl.BaseValue;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author k.tulbassiyev
 */
@Component
public class CreditParser extends BatchParser {
    private Stack stack = new Stack();

    private Logger logger = Logger.getLogger(CreditParser.class);

    @Autowired
    private CreditContractParser creditContractParser;

    @Autowired
    private CreditorBranchParser creditorBranchParser;
    
    public CreditParser() {
        super();        
    }

    @Override
    public void init()
    {
        currentBaseEntity = new BaseEntity(metaClassRepository.getMetaClass("credit"), new Date());
    }

    public boolean startElement(XMLEvent event, StartElement startElement, String localName) throws SAXException {
        try {
            if(localName.equals("credit")) {
            } else if(localName.equals("contract")) {
                creditContractParser.parse(xmlReader, batch);
                BaseEntity creditContract = creditContractParser.getCurrentBaseEntity();
                currentBaseEntity.put("contract", new BaseValue(batch, 0, creditContract));
            } else if(localName.equals("currency")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("currency", new BaseValue(batch, 0, event.asCharacters().getData()));
            } else if(localName.equals("interest_rate_yearly")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("interest_rate_yearly", new BaseValue(batch, 0,
                        new Double(event.asCharacters().getData())
                    ));
            } else if(localName.equals("contract_maturity_date")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("contract_maturity_date", new BaseValue(batch, 0,
                        dateFormat.parse(event.asCharacters().getData())
                    ));
            } else if(localName.equals("actual_issue_date")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("actual_issue_date", new BaseValue(batch, 0,
                        dateFormat.parse(event.asCharacters().getData())
                    ));
            } else if(localName.equals("credit_purpose")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("credit_purpose", new BaseValue(batch, 0,
                        event.asCharacters().getData()
                    ));
            } else if(localName.equals("credit_object")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("credit_object", new BaseValue(batch, 0,
                        event.asCharacters().getData()
                    ));
            } else if(localName.equals("amount")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("amount", new BaseValue(batch, 0,
                        new Double(event.asCharacters().getData())
                    ));
            } else if(localName.equals("finance_source")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("finance_source", new BaseValue(batch, 0,
                        event.asCharacters().getData()
                    ));
            } else if(localName.equals("has_currency_earn")) {
                event = (XMLEvent) xmlReader.next();
                currentBaseEntity.put("has_currency_earn", new BaseValue(batch, 0,
                        new Boolean(event.asCharacters().getData())
                ));
            } else if(localName.equals("creditor_branch")) {
                creditorBranchParser.parse(xmlReader, batch);
                BaseEntity creditorBranch = creditorBranchParser.getCurrentBaseEntity();
                currentBaseEntity.put("creditor_branch", new BaseValue(batch, 0, creditorBranch));
            } else if(localName.equals("portfolio")) {
                if(!stack.pop().equals("portfolio")) {}
                    //portfolio = new Portfolio();
            } else if(localName.equals("portfolio_msfo")) {
            } else {
                throw new UnknownTagException(localName);
            }
        
            stack.push(localName);
        } catch(ParseException parseException) {
            throw new TypeErrorException(localName);
        }

        return false;
    }

    public boolean endElement(String localName) throws SAXException {
        //try {
            if(localName.equals("credit")) {
                //currentPackage.setCredit(ctCredit);
                //xmlReader.setContentHandler(contentHandler);
                return true;
            } else if(localName.equals("contract")) {
            } else if(localName.equals("currency")) {
                //ctCredit.setCurrency(contents.toString());
            } else if(localName.equals("interest_rate_yearly")) {
                //ctCredit.setInterestRateYearly(new BigDecimal(contents.toString()));
            } else if(localName.equals("contract_maturity_date")) {
                //ctCredit.setContractMaturityDate(convertDateToCalendar(dateFormat.parse(contents.toString())));
            } else if(localName.equals("actual_issue_date")) {
                //ctCredit.setActualIssueDate(convertDateToCalendar(dateFormat.parse(contents.toString())));
            } else if(localName.equals("credit_purpose")) {
                //ctCredit.setCreditPurpose(contents.toString());
            } else if(localName.equals("credit_object")) {
                //ctCredit.setCreditObject(contents.toString());
            } else if(localName.equals("amount")) {
                //ctCredit.setAmount(new BigDecimal(contents.toString()));
            } else if(localName.equals("finance_source")) {
                //ctCredit.setFinanceSource(contents.toString());
            } else if(localName.equals("has_currency_earn")) {
                //ctCredit.setHasCurrencyEarn(ParserUtils.parseBoolean(contents.toString()));
            } else if(localName.equals("creditor_branch")) {
            } else if(localName.equals("portfolio")) {
                if(stack.pop().equals("portfolio")) {
                    //portfolio.setPortfolio(contents.toString());
                } else {
                    //ctCredit.setPortfolio(portfolio);
                }
            } else if(localName.equals("portfolio_msfo")) {
                //portfolio.setPortfolioMsfo(contents.toString());
            } else {
                throw new UnknownTagException(localName);
            }
        /*} catch(ParseException parseException) {
            throw new TypeErrorException(localName);
        }*/
        return false;
    }
}
