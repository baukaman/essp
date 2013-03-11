/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kz.bsbnb.usci.eav_persistance.test.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import kz.bsbnb.usci.eav_model.model.Batch;
import kz.bsbnb.usci.eav_model.model.base.impl.BaseEntity;
import kz.bsbnb.usci.eav_model.model.base.impl.BaseSet;
import kz.bsbnb.usci.eav_model.model.base.impl.BaseValue;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaAttribute;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaSet;
import kz.bsbnb.usci.eav_model.model.meta.impl.MetaValue;
import kz.bsbnb.usci.eav_model.model.type.DataTypes;
import kz.bsbnb.usci.eav_persistance.factory.IMetaFactory;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBaseEntityDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IBatchDao;
import kz.bsbnb.usci.eav_persistance.persistance.dao.IMetaClassDao;
import kz.bsbnb.usci.eav_persistance.test.GenericTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author a.tkachenko
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class BaseEntityTest extends GenericTestCase {
    private final Logger logger = LoggerFactory.getLogger(BaseEntityTest.class);

    @Autowired
    IMetaClassDao metaClassDao;

    @Autowired
    IBatchDao batchDao;

    @Autowired
    IMetaFactory metaFactory;

    @Autowired
    IBaseEntityDao baseEntityDao;
    
    public BaseEntityTest() {
    }

    @Test
    public void typesCount()
    {
    	assertEquals(DataTypes.values().length, 5);
    }

    @Test
    public void testSimple() {
        String name = "testClass";

        Batch batch = new Batch();
        batchDao.save(batch);

        MetaClass meta = new MetaClass(name);
        Long metaId = metaClassDao.save(meta);

        BaseEntity instance = metaFactory.getBaseEntity(name);
        Date dateResult = new Date();
        Integer intResult = new Integer(10);
        String strResult = new String();
        Boolean boolResult = new Boolean(true);
        Double doubleResult = new Double(2);

        instance.getMeta().setMetaAttribute("testDate", new MetaAttribute(false,false,new MetaValue(DataTypes.DATE)) );
        instance.getMeta().setMetaAttribute("testInteger", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.getMeta().setMetaAttribute("testDouble", new MetaAttribute(false, false, new MetaValue(DataTypes.DOUBLE)));
        instance.getMeta().setMetaAttribute("testBoolean", new MetaAttribute(false, false, new MetaValue(DataTypes.BOOLEAN)));
        instance.getMeta().setMetaAttribute("testString", new MetaAttribute(false, false, new MetaValue(DataTypes.STRING)));
        instance.put("testDate", new BaseValue(batch, 1, dateResult));
        instance.put("testInteger", new BaseValue(batch, 1, intResult));
        instance.put("testDouble", new BaseValue(batch, 1, doubleResult));
        instance.put("testBoolean", new BaseValue(batch, 1, boolResult));
        instance.put("testString",new BaseValue(batch,1,strResult));


        assertEquals(dateResult, ((BaseValue) instance.getBaseValue("testDate")).getValue());
        assertEquals(intResult, ((BaseValue) instance.getBaseValue("testInteger")).getValue());
        assertEquals(doubleResult, ((BaseValue) instance.getBaseValue("testDouble")).getValue());
        assertEquals(strResult, ((BaseValue) instance.getBaseValue("testString")).getValue());
        assertEquals(boolResult, ((BaseValue) instance.getBaseValue("testBoolean")).getValue());

        boolean pass = false;
        try {
            // instance.getDate("unknownName");
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }

        if(!pass) {
            fail("Gives value with unknown name");
        }

//        pass = false;
//        try {
//            instance.getBaseValue("testNotDate");
//        }
//        catch(IllegalArgumentException e)
//        {
//            pass = true;
//        }
//
//        if(!pass) {
//            fail("Gives Date with illegal type");
//        }
    }

    @Test
    public void testComplex() {
        String name = "testClass";

        Batch batch = new Batch();
        batchDao.save(batch);

        MetaClass meta = new MetaClass(name);
        Long metaId = metaClassDao.save(meta);

        MetaClass meta2 = new MetaClass("testClass2");
        Long meta2Id = metaClassDao.save(meta2);

        BaseEntity instance = metaFactory.getBaseEntity(name);
        BaseEntity expEntity = metaFactory.getBaseEntity("testClass2");

        Date dateResult  = new Date();
        Integer intResult = new Integer(10);

        instance.getMeta().setMetaAttribute("testDate", new MetaAttribute(false,false,new MetaValue(DataTypes.DATE)) );
        instance.getMeta().setMetaAttribute("testInteger", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.getMeta().setMetaAttribute("testComplex", new MetaAttribute(meta2));
        instance.put("testDate", new BaseValue(batch, 1, dateResult));
        instance.put("testInteger", new BaseValue(batch, 1, intResult));
        instance.put("testComplex",new BaseValue(batch,1,expEntity));

        assertEquals(dateResult, ((BaseValue) instance.getBaseValue("testDate")).getValue());
        assertEquals(intResult, ((BaseValue) instance.getBaseValue("testInteger")).getValue());
        assertEquals(expEntity, ((BaseValue) instance.getBaseValue("testComplex")).getValue());


        instance.getMeta().setMetaAttribute("testArrayInteger", new MetaAttribute(false, true, new MetaSet(new MetaValue(DataTypes.INTEGER))));
        BaseSet baseSet = new BaseSet(((MetaSet)instance.getMemberType("testArrayInteger")).getMemberType());
        baseSet.put(new BaseValue(batch,1,11));
        baseSet.put(new BaseValue(batch,1,22));
        baseSet.put(new BaseValue(batch,1,33));
        instance.put("testArrayInteger",new BaseValue(batch,1,baseSet));

        assertEquals(baseSet,((BaseValue)instance.getBaseValue("testArrayInteger")).getValue());



        boolean pass = false;
        try {
            // instance.getDate("unknownName");
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }

        if(!pass) {
            fail("Gives value with unknown name");
        }


    }

    @Test
    public void testSets() throws Exception {
        String name = "testClass";

        Batch batch = new Batch();
        batchDao.save(batch);

        MetaClass meta = new MetaClass(name);
        Long metaId = metaClassDao.save(meta);


        BaseEntity instance = metaFactory.getBaseEntity(name);

        Date dateResult  = new Date();
        Integer intResult = new Integer(10);

        instance.getMeta().setMetaAttribute("testDate", new MetaAttribute(false,false,new MetaValue(DataTypes.DATE)) );
        instance.getMeta().setMetaAttribute("testInteger", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.put("testDate", new BaseValue(batch, 1, dateResult));
        instance.put("testInteger", new BaseValue(batch, 1, intResult));

        Set<String> set = new TreeSet<String>();
        set.add("testDate");
        set.add("testInteger");

        assertEquals(set,instance.getAttributeNames());
        assertEquals(2,instance.getAttributeCount());

    }

    @Test
    public void testEqual() throws Exception {
        String name = "testClass";

        Batch batch = new Batch();
        batchDao.save(batch);

        MetaClass meta = new MetaClass(name);
        Long metaId = metaClassDao.save(meta);


        BaseEntity instance = metaFactory.getBaseEntity(name);
        BaseEntity instance2 = metaFactory.getBaseEntity(name);

        Date dateResult  = new Date();
        Integer intResult = new Integer(10);

        instance.getMeta().setMetaAttribute("testDate", new MetaAttribute(false,false,new MetaValue(DataTypes.DATE)) );
        instance.getMeta().setMetaAttribute("testInteger", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.put("testDate", new BaseValue(batch, 1, dateResult));
        instance.put("testInteger", new BaseValue(batch, 1, intResult));

        instance2.getMeta().setMetaAttribute("testDate", new MetaAttribute(false,false,new MetaValue(DataTypes.DATE)) );
        instance2.getMeta().setMetaAttribute("testInteger", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance2.put("testDate", new BaseValue(batch, 1, dateResult));
        instance2.put("testInteger", new BaseValue(batch, 1, intResult));

        assertTrue(instance.equals(instance));
        assertFalse(instance.equals(null));
        assertTrue(instance.equals(instance2));

        instance.getMeta().setMetaAttribute("testArrayInteger", new MetaAttribute(false, true, new MetaSet(new MetaValue(DataTypes.INTEGER))));
        instance2.getMeta().setMetaAttribute("testArrayInteger", new MetaAttribute(false, true, new MetaSet(new MetaValue(DataTypes.INTEGER))));
        BaseSet baseSet = new BaseSet(((MetaSet)instance.getMemberType("testArrayInteger")).getMemberType());
        baseSet.put(new BaseValue(batch,1,11));
        baseSet.put(new BaseValue(batch,1,22));
        baseSet.put(new BaseValue(batch,1,33));
        instance.put("testArrayInteger",new BaseValue(batch,1,baseSet));
        instance2.put("testArrayInteger",new BaseValue(batch,1,baseSet));
        assertTrue(instance.equals(instance2));

        instance2.getMeta().setMetaAttribute("testInteger2", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance2.put("testInteger2", new BaseValue(batch, 1, intResult));
        assertFalse(instance.equals(instance2));

        instance.getMeta().setMetaAttribute("testInteger2", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.put("testInteger2", new BaseValue(batch, 1, null));
        assertFalse(instance.equals(instance2));

        instance.getMeta().setMetaAttribute("testInteger3", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance.put("testInteger3", new BaseValue(batch, 1, null));
        instance2.getMeta().setMetaAttribute("testInteger3", new MetaAttribute(false, false, new MetaValue(DataTypes.INTEGER)));
        instance2.put("testInteger3", new BaseValue(batch, 1, null));
        assertFalse(instance.equals(instance2));


    }

    /*
    @Test
    public void getComplex() {
    	String name = "testClass";
        BaseEntity instance = new BaseEntity(name);
        BaseEntity expResult;
        
        expResult = new BaseEntity("testClass");
        expResult.getMeta().setMemberType("testField1", new MetaValue(DataTypes.DATE, false, false));
        expResult.getMeta().setMemberType("testField2", new MetaValue(DataTypes.INTEGER, false, false));
        
        instance.getMeta().setMemberType("testComplex", new MetaClass("some_class", false, false));
        instance.getMeta().setMemberType("testNotComplex", new MetaValue(DataTypes.INTEGER, false, false));
        instance.set("testComplex", new Batch(), 1, expResult);
        
        
        BaseEntity result = instance.getBaseValue("testComplex"); //instance.getComplex("testComplex");
        assertEquals(expResult, result);
        
        boolean pass = false;
        try {
            instance.getBaseValue("uknownName");
            //instance.getComplex("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Complex with unknown name");
        }
        
        pass = false;
        try {
            instance.getBaseValue("testNotComplex");
            // instance.getComplex("testNotComplex");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Complex with illegal type");
        }
    }
  */
    /*
    @Test
    public void getInteger() {
        String name = "testClass";
        BaseEntity instance = new BaseEntity(name);
        Integer expResult;
        
        expResult = 7;
        instance.getMeta().setMemberType("testInteger", new MetaValue(DataTypes.INTEGER, false, false));
        instance.getMeta().setMemberType("testNotInteger", new MetaValue(DataTypes.DATE, false, false));
        instance.set("testInteger", new Batch(), 1, expResult);
        
        
        Integer result = instance.getBaseValue("testInteger");
        assertEquals(expResult, result);
        
        boolean pass = false;
        try {
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Integer with unknown name");
        }
        
        pass = false;
        try {
            instance.getBaseValue("testNotInteger");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Integer with illegal type");
        }
    }
    
    @Test
    public void getString() {
        String name = "testClass";
        BaseEntity instance = new BaseEntity(name);
        String expResult;
        
        expResult = "testString";
        instance.getMeta().setMemberType("testString", new MetaValue(DataTypes.STRING, false, false));
        instance.getMeta().setMemberType("testNotString", new MetaValue(DataTypes.INTEGER, false, false));
        instance.set("testString", new Batch(), 1, expResult);
        
        
        String result = instance.getBaseValue("testString");
        assertEquals(expResult, result);
        
        boolean pass = false;
        try {
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives String with unknown name");
        }
        
        pass = false;
        try {
            instance.getBaseValue("testNotDate");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives String with illegal type");
        }
    }
    
    @Test
    public void getDouble() {
        String name = "testClass";
        BaseEntity instance = new BaseEntity(name);
        Double expResult;
        
        expResult = 1.;
        instance.getMeta().setMemberType("testDouble", new MetaValue(DataTypes.DOUBLE, false, false));
        instance.getMeta().setMemberType("testNotDouble", new MetaValue(DataTypes.INTEGER, false, false));
        instance.set("testDouble", new Batch(), 1, expResult);
        
        
        Double result = instance.getBaseValue("testDouble");
        assertEquals(expResult, result);
        
        boolean pass = false;
        try {
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Double with unknown name");
        }
        
        pass = false;
        try {
            instance.getBaseValue("testNotDate");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Double with illegal type");
        }
    }
    
    @Test
    public void getBoolean() {
        String name = "testClass";
        BaseEntity instance = new BaseEntity(name);
        Boolean expResult;
        
        expResult = true;
        instance.getMeta().setMemberType("testBoolean", new MetaValue(DataTypes.BOOLEAN, false, false));
        instance.getMeta().setMemberType("testNotBoolean", new MetaValue(DataTypes.INTEGER, false, false));
        instance.set("testBoolean", new Batch(), 1, expResult);
        
        
        Boolean result = instance.getBaseValue("testBoolean");
        assertEquals(expResult, result);
        
        boolean pass = false;
        try {
            instance.getBaseValue("unknownName");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Boolean with unknown name");
        }
        
        pass = false;
        try {
            instance.getBaseValue("testNotDate");
        }
        catch(IllegalArgumentException e)
        {
            pass = true;
        }
        
        if(!pass) {
            fail("Gives Boolean with illegal type");
        }
    }*/
}