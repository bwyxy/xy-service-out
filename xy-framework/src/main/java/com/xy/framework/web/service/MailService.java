package com.xy.framework.web.service;

import com.xy.common.config.MailConfig;
import com.xy.common.email.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class MailService {

    @Autowired
    private MailConfig mailConfig;

    @Autowired
    private EmailProperties emailProperties;

    /**
     * 发送邮箱验证码
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * */
    public void sendSimpleMail(String toEmail, String code) throws MessagingException, UnsupportedEncodingException {

        MimeMessage mimeMessage = mailConfig.javaMailSender().createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String fromDisplayName = "飞鸽即时通讯";
        helper.setFrom(emailProperties.getUsername(), fromDisplayName);
        helper.setTo(toEmail);

        String subCodeTitle = "验证码【 " + code + " 】";
        helper.setSubject(subCodeTitle);

        String htmlContent = "<html><body>" +
                "<h2 style='text-align: center;font-size: 24px;letter-spacing: 2px;color: #1f1f1f'>飞鸽即时通讯-验证码</h2>" +
                "<p style='font-size: 16px;color: #8c8c8c;letter-spacing: 1px'>尊敬的用户您好:</p>" +
                "<p style='font-size: 16px;color: #8c8c8c;letter-spacing: 1px'>感谢您使用飞鸽即时通讯, 您当前正在进行登录/注册等操作，您的验证码是:</p>" +
                "<p style='font-size: 32px;color: #fa541c;margin: 0;letter-spacing: 2px'>" + code + "</p>" +
                "<p style='font-size: 16px;color: #8c8c8c;letter-spacing: 1px'>请勿向任何人提供此验证码。</p>" +
                "<p style='font-size: 16px;color: #8c8c8c;letter-spacing: 1px'>注意: 本次验证码只能用于登录/注册, 验证码有效期为2分钟。如果不是您本人操作，请忽略此邮件。</p>" +
                "</body></html>";

        helper.setText(htmlContent, true);

        mailConfig.javaMailSender().send(mimeMessage);
    }
}
