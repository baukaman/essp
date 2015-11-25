package com.bsbnb.usci.portlets.protocol.data;

import com.bsbnb.usci.portlets.protocol.PortletEnvironmentFacade;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Link;
import kz.bsbnb.usci.core.service.InputFileBeanRemoteBusiness;
import kz.bsbnb.usci.core.service.InputInfoBeanRemoteBusiness;
import kz.bsbnb.usci.core.service.PortalUserBeanRemoteBusiness;
import kz.bsbnb.usci.core.service.ProtocolBeanRemoteBusiness;
import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.cr.model.InputInfo;
import kz.bsbnb.usci.cr.model.Protocol;
import kz.bsbnb.usci.eav.StaticRouter;
import kz.bsbnb.usci.eav.model.json.BatchFullJModel;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Aidar.Myrzahanov
 */
public class BeanDataProvider implements DataProvider {
    private ProtocolBeanRemoteBusiness protocolBusiness;
    private InputInfoBeanRemoteBusiness inputInfoBusiness;
    private PortalUserBeanRemoteBusiness portalUserBusiness;

    private static final String ENTITY_EDITOR_PAGE = "http://" + StaticRouter.getPortalUrl() + ":" +
            StaticRouter.getPortalPort() + "/entity_editor";

    public BeanDataProvider() {
        initializeBeans();
    }

    private void initializeBeans() {
        RmiProxyFactoryBean protocolBeanRemoteBusinessFactoryBean = new RmiProxyFactoryBean();
        protocolBeanRemoteBusinessFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP() +
                ":1099/protocolBeanRemoteBusiness");
        protocolBeanRemoteBusinessFactoryBean.setServiceInterface(ProtocolBeanRemoteBusiness.class);

        protocolBeanRemoteBusinessFactoryBean.afterPropertiesSet();
        protocolBusiness = (ProtocolBeanRemoteBusiness) protocolBeanRemoteBusinessFactoryBean.getObject();

        RmiProxyFactoryBean inputInfoBeanRemoteBusinessFactoryBean = new RmiProxyFactoryBean();
        inputInfoBeanRemoteBusinessFactoryBean.setServiceUrl("rmi://" + StaticRouter.getAsIP() +
                ":1099/inputInfoBeanRemoteBusiness");
        inputInfoBeanRemoteBusinessFactoryBean.setServiceInterface(InputInfoBeanRemoteBusiness.class);

        inputInfoBeanRemoteBusinessFactoryBean.afterPropertiesSet();
        inputInfoBusiness = (InputInfoBeanRemoteBusiness) inputInfoBeanRemoteBusinessFactoryBean.getObject();

        RmiProxyFactoryBean portalUserBeanRemoteBusinessFactoryBean = new RmiProxyFactoryBean();
        portalUserBeanRemoteBusinessFactoryBean.setServiceUrl("rmi://" +StaticRouter.getAsIP() +
                ":1099/portalUserBeanRemoteBusiness");
        portalUserBeanRemoteBusinessFactoryBean.setServiceInterface(PortalUserBeanRemoteBusiness.class);

        portalUserBeanRemoteBusinessFactoryBean.afterPropertiesSet();
        portalUserBusiness = (PortalUserBeanRemoteBusiness) portalUserBeanRemoteBusinessFactoryBean.getObject();
    }

    public List<Creditor> getCreditorsList() {
        return portalUserBusiness.getMainCreditorsInAlphabeticalOrder(PortletEnvironmentFacade.get().getUserID());
    }

    public List<ProtocolDisplayBean> getProtocolsByInputInfo(InputInfoDisplayBean inputInfo) {
        List<Protocol> protocols = protocolBusiness.getProtocolsBy_InputInfo(inputInfo.getInputInfo());
        List<ProtocolDisplayBean> result = new ArrayList<>();

        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        String sRepDate = df.format(inputInfo.getInputInfo().getReportDate());

        for (Protocol protocol : protocols) {
            ProtocolDisplayBean pr = new ProtocolDisplayBean(protocol);
            if (protocol.getMessageType().getCode().equals("COMPLETED"))
                pr.setLink(new Link("Просмотр",
                        new ExternalResource(ENTITY_EDITOR_PAGE + "?entityId=" +
                        protocol.getNote() + "&repDate=" + sRepDate)));

            result.add(pr);
        }
        return result;
    }

    public List<InputInfoDisplayBean> getInputInfosByCreditors(List<Creditor> creditors, Date reportDate) {
        List<InputInfo> inputInfoList = inputInfoBusiness.getAllInputInfos(creditors,reportDate);
        List<InputInfoDisplayBean> result = new ArrayList<>(inputInfoList.size());
        for (InputInfo inputInfo : inputInfoList) {
            if(inputInfo != null && inputInfo.getCreditor() != null)
                result.add(new InputInfoDisplayBean(inputInfo,this));
        }
        return result;
    }

    public Map<SharedDisplayBean, Map<String, List<ProtocolDisplayBean>>> getProtocolsByInputInfoGrouped(
            InputInfoDisplayBean inputInfo) {
        List<ProtocolDisplayBean> list = getProtocolsByInputInfo(inputInfo);
        Map<SharedDisplayBean, Map<String, List<ProtocolDisplayBean>>> result = new LinkedHashMap<>(list.size());
        for (ProtocolDisplayBean protocol : list) {
            Map<String, List<ProtocolDisplayBean>> innerMap;
            SharedDisplayBean shared = new SharedDisplayBean(protocol.getType());
            if (result.containsKey(shared)) {
                innerMap = result.get(shared);
            } else {
                innerMap = new LinkedHashMap<>();
                result.put(shared, innerMap);
            }
            List<ProtocolDisplayBean> innerList;
            if (innerMap.containsKey(protocol.getDescription())) {
                innerList = innerMap.get(protocol.getDescription());
            } else {
                innerList = new ArrayList<>();
                innerMap.put(protocol.getDescription(), innerList);
            }
            innerList.add(protocol);
        }
        return result;
    }

    public BatchFullJModel getBatchFullModel(BigInteger batchId) {
        return inputInfoBusiness.getBatchFullModel(batchId);
    }
}
