package com.bsbnb.creditregistry.portlets.crosscheck.ui;

import com.bsbnb.creditregistry.portlets.crosscheck.dm.Creditor;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.ComboBox;

public class ExtendedPageComboBox extends ComboBox{
    public ExtendedPageComboBox(String value, BeanItemContainer<Creditor> container) {
        super(value, container);
        pageLength = 20;
        setMultiSelect(true);
    }
}