package kz.bsbnb.usci.eav.persistance.dao;

import kz.bsbnb.usci.eav.model.mail.MailMessage;
import kz.bsbnb.usci.eav.model.mail.MailMessageParameter;
import kz.bsbnb.usci.eav.model.mail.MailTemplate;
import kz.bsbnb.usci.eav.model.mail.UserMailTemplate;

import java.util.List;
import java.util.Properties;

public interface IMailDao {
    List<UserMailTemplate> getUserMailTemplates(long userId);

    void saveUserMailTemplates(List<UserMailTemplate> userTemplates);

    List<MailMessage> getMailMessagesByUser(Long userId);

    List<MailMessageParameter> getParametersByMessage(MailMessage message);

    MailTemplate getMailTemplateByCode(String templateCode);

    void sendMailMessage(String templateCode, Long recipientUserId, Properties parametersByCode);

    void updateMailMessage(MailMessage message);

    boolean isTemplateEnabledForUser(Long templateId, long userId);

    List<MailMessage> getPendingMessages();

    boolean isMailHandlingOn();
}