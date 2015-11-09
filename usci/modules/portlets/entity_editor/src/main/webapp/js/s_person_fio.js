Ext.onReady(function() {
    var serviceCode = 'kz.bsbnb.usci.core.service.form.searcher.impl.cr.PersonFormImpl';
    forms[serviceCode] = function(panel) {
        panel.removeAll();
        panel.add(Ext.create("Ext.form.field.Text",
            {
                id: 'edFirstName',
                fieldLabel: 'Имя',
                width: '40%',
                margin: 10
            }));

        panel.add(Ext.create("Ext.form.field.Text",
            {
                id: 'edLastName',
                fieldLabel: 'Фамилия',
                width: '40%',
                margin: 10
            }));

        panel.add(Ext.create("Ext.form.field.Text",
            {
                id: 'edMiddleName',
                fieldLabel: 'Отчество',
                width: '40%',
                margin: 10
            }));

        panel.doSearch = function(){
            var params = {
                op : 'LIST_ENTITY',
                metaClass: 'subject',
                searchName: serviceCode,
                timeout: 120000,
                firstName: Ext.getCmp('edFirstName').value,
                lastName: Ext.getCmp('edLastName').value,
                middleName: Ext.getCmp('edMiddleName').value,
                date: Ext.getCmp('edDate').value,
                creditorId: Ext.getCmp('edCreditor').value
            };

            entityStore.load({
                params: params,
                callback: function (records, operation, success) {
                    if (!success) {
                        Ext.MessageBox.alert(label_ERROR, label_ERROR_NO_DATA_FOR.format(operation.request.proxy.reader.rawData.errorMessage));
                    }
                }});
        }
    };
});