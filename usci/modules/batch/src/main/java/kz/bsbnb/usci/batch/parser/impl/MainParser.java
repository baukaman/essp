package kz.bsbnb.usci.batch.parser.impl;

import kz.bsbnb.usci.batch.exception.UnknownTagException;
import kz.bsbnb.usci.batch.parser.AbstractParser;
import kz.bsbnb.usci.eav.model.BaseEntity;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * @author k.tulbassiyev
 */
public class MainParser extends AbstractParser
{
    private InputSource inputSource;

    private Logger logger = Logger.getLogger(MainParser.class);

    private Stack<BaseEntity> stack = new Stack<BaseEntity>();

    private List<BaseEntity> entities = new ArrayList<BaseEntity>();

    private BaseEntity currentEntity;

    private int level = 0;

    public MainParser(byte[] xmlBytes)
    {
        ByteArrayInputStream bStream = new ByteArrayInputStream(xmlBytes);
        inputSource = new InputSource(bStream);
    }

    public void parse() throws SAXException, IOException
    {
        xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(inputSource);
    }

    @Override
    public void startDocument() throws SAXException
    {
        logger.info("started");

    }

    @Override
    public void endDocument() throws SAXException
    {
        logger.info("finished");
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException
    {
        contents.reset();

        if(localName.equalsIgnoreCase("batch"))
        {
            logger.info("batch");
        }
        else if(localName.equalsIgnoreCase("entities"))
        {
            logger.info("entities");
        }
        else if(localName.equalsIgnoreCase("entity"))
        {
            if(currentEntity != null)
                stack.push(currentEntity);

            currentEntity = new BaseEntity(attributes.getValue("class"));

            level++;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException
    {
        if(localName.equalsIgnoreCase("batch"))
        {
            logger.info("batch");
        }
        else if(localName.equalsIgnoreCase("entities"))
        {
            logger.info("entities");
        }
        else if(localName.equalsIgnoreCase("entity"))
        {
            BaseEntity parentEntity;

            try
            {
                if(stack.size() == level)
                {
                    parentEntity = stack.pop();
                }
                else
                {
                    parentEntity = stack.peek();
                }

            }
            catch(EmptyStackException ex)
            {
                parentEntity = null;
            }

            if(parentEntity != null)
            {
                parentEntity.set(currentEntity.getMeta().getClassName(), null, 0L, currentEntity);
            }
            else
            {
               entities.add(currentEntity);
               currentEntity = null;
            }

            level--;
        }
        else
        {
            currentEntity.set(localName, null, 0L, contents.toString());
        }
    }
}