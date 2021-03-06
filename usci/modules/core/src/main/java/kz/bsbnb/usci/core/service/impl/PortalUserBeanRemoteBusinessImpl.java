package kz.bsbnb.usci.core.service.impl;

import kz.bsbnb.usci.core.service.PortalUserBeanRemoteBusiness;
import kz.bsbnb.usci.cr.model.Creditor;
import kz.bsbnb.usci.cr.model.PortalUser;
import kz.bsbnb.usci.eav.persistance.dao.IMailDao;
import kz.bsbnb.usci.eav.persistance.dao.IUserDao;
import kz.bsbnb.usci.eav.persistance.dao.IBaseLogsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortalUserBeanRemoteBusinessImpl implements PortalUserBeanRemoteBusiness {
    @Autowired
    IUserDao userDao;

    @Autowired
    IMailDao mailDao;

    @Autowired
    IBaseLogsDao logsDao;

    /**
     * Проверяет наличие связи между пользователем портала и БВУ/НО.
     *
     * @param userId     Id пользователя портала.
     * @param creditorId Id БВУ/НО.
     * @return true, если связь есть, false в противном случае.
     */
    @Override
    public boolean hasPortalUserCreditor(long userId, long creditorId) {
        return userDao.hasPortalUserCreditor(userId, creditorId);
    }

    /**
     * Устанавливает связь между пользователем портала и БВУ/НО.
     *
     * @param userId     Id пользователя Liferay-я.
     * @param creditorId Id БВУ/НО.
     */
    @Override
    public void setPortalUserCreditors(long userId, long creditorId) {
        userDao.setPortalUserCreditors(userId, creditorId);
    }

    /**
     * Удаляет связь между пользователем портала и БВУ/НО.
     *
     * @param userId     Id пользователя Liferay-я.
     * @param creditorId Id БВУ/НО.
     */
    @Override
    public void unsetPortalUserCreditors(long userId, long creditorId) {
        userDao.unsetPortalUserCreditors(userId, creditorId);
    }

    /**
     * Возвращает список БВУ/НО пользователя портала.
     *
     * @param userId Id пользователя Liferay-я.
     * @return Список БВУ/НО.
     */
    @Override
    public List<Creditor> getPortalUserCreditorList(long userId) {
        return userDao.getPortalUserCreditorList(userId);
    }

    /**
     * Проводит синхранизацию между пользователями Liferay-я и пользователями портала.
     * Если пользователю Liferay-я нет соответствующего пользователя портала, то он будет добавлен.
     * Если пользователю Liferay-я соответствует более одного пользователя портала,
     * то сначала они будут удалены, после этого будет добавлен соответствующий пользователь.
     * В случае присутствия соответствующего пользователя портала,
     * то он будет проверен по полю modifiedDate и при необходимости изменен.
     *
     * @param users Список пользователей Liferay-я.
     * @throws Exception Неожиданная ошибка.
     */
    @Override
    public void synchronize(List<PortalUser> users) {
        mailDao.insertNewUsers(users);
        userDao.synchronize(users);
    }

    @Override
    public List<Creditor> getMainCreditorsInAlphabeticalOrder(long userId) {
        return userDao.getPortalUserCreditorList(userId);
    }

    @Override
    public PortalUser getUser(long userId) {
        return userDao.getUser(userId);
    }

    @Override
    public List<PortalUser> getPortalUsersHavingAccessToCreditor(Creditor creditor) {
        return userDao.getPortalUsersHavingAccessToCreditor(creditor);
    }

    @Override
    public void insertLogs(String portletname, String username, String comment) {
        logsDao.insertLogs(portletname,username,comment);
    }

}
