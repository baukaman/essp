function newRuleForm(){
    var cars = [{id: 10, name: 'mazda'}];
    var packageStore = Ext.create('Ext.data.ArrayStore', {
        fields: ['id','name'],
        data: cars
    });

    /*var packageStore = Ext.create('Ext.data.Store',{
     id: 'packageStore',
     model: 'packageListModel',
     //data: myData,
     proxy: {
     type: 'ajax',
     url: dataUrl,
     extraParams: {
     op: 'PACKAGE_ALL'
     },
     reader: {
     type: 'json',
     root: 'data'
     }
     },
     autoLoad: true
     });

     var packageVersionStore = Ext.create('Ext.data.Store',{
     id: 'packageVersionStore',
     model: 'packageListModel',
     proxy: {
     type: 'ajax',
     url: dataUrl,
     extraParams: {
     op: 'PACKAGE_VERSIONS'
     },
     reader: {
     type: 'json',
     root: 'data'
     }
     }
     });*/

    /*var packageVersionGrid = Ext.create('Ext.grid.Panel',{
     store: packageVersionStore,
     columns: [{
     text     : 'Дата',
     dataIndex: 'name'
     }],
     forceFit: true,
     height: 600
     });*/

    return new Ext.Window({
        id: 'newRuleForm',
        layout: 'fit',
        modal: 'true',
        title: 'Новое правило',
        items: [
            Ext.create('Ext.form.Panel',{
                region: 'center',
                width: 1200,
                height: 700,
                items: [
                    Ext.create('Ext.form.TextField', {
                        fieldLabel: 'пакет',
                        labelWidth: 35,
                        value: Ext.getCmp('elemComboPackage').getRawValue(),
                        disabled: true,
                        padding: 3
                    }),
                    Ext.create('Ext.form.DateField', {
                        fieldLabel: 'дата',
                        labelWidth: 35,
                        format: 'd.m.Y',
                        padding: 3
                    }),
                    Ext.create('Ext.form.Panel',{
                        tbar: [
                            {
                                text: 'Добавить',
                                id: 'btnPackageControlAdd',
                                //disabled: true,
                                handler: function(){
                                    Ext.Ajax.request({
                                        disableCaching: false,
                                        url: dataUrl,
                                        params: {
                                            op: 'NEW_RULE',
                                            title: Ext.getCmp('txtTitle').value
                                        },
                                        success: function(response){
                                            var ruleId = Ext.decode(response.responseText).data;
                                            ruleListGrid.store.add({id: ruleId, name : Ext.getCmp('txtTitle').value });
                                            ruleListGrid.getSelectionModel().select(ruleListGrid.store.indexOfId(ruleId));
                                            ruleListGrid.fireEvent('cellclick', ruleListGrid, null, 1, ruleListGrid.getSelectionModel().getLastSelected());
                                            Ext.getCmp('btnAddGreen').hide();
                                            Ext.getCmp('txtTitle').hide();
                                            editor.focus();
                                        },
                                        failure: function(response){
                                            Ext.Msg.alert('ошибка',Ext.decode(response.responseText).errorMessage);
                                        }
                                    });
                                }
                            }
                        ]
                    }),
                    {
                        html: "<div id='bknew-rule' style='height: 600px;'>function(){}</div>",
                    }
                ]
            })
        ]
    });
}