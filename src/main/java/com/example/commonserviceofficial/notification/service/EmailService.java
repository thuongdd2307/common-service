package com.example.commonserviceofficial.notification.service;

import com.example.commonserviceofficial.notification.dto.EmailRequest;
import com.example.commonserviceofficial.notification.dto.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service để gửi email với đầy đủ tính năng enterprise
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@hddt.com}")
    private String defaultFromEmail;

    @Value("${spring.mail.from-name:HDDT System}")
    private String defaultFromName;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.async:true}")
    private boolean asyncEnabled;

    @Value("${notification.email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * Gửi email đồng bộ
     */
    public EmailResponse sendEmail(EmailRequest request) {
        if (!emailEnabled) {
            log.warn("Email service is disabled");
            return EmailResponse.failed(null, request.getTo(), request.getSubject(), "Email service is disabled");
        }

        long startTime = System.currentTimeMillis();
        String messageId = generateMessageId();

        try {
            if (request.getTemplateVariables() != null && request.getTemplateName() != null) {
                // Sử dụng template
                return sendTemplateEmail(request, messageId, startTime);
            } else {
                // Gửi email thường
                return sendSimpleEmail(request, messageId, startTime);
            }

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", request.getTo(), e.getMessage(), e);
            
            EmailResponse response = EmailResponse.failed(messageId, request.getTo(), request.getSubject(), e.getMessage());
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;
        }
    }

    /**
     * Gửi email bất đồng bộ
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<EmailResponse> sendEmailAsync(EmailRequest request) {
        EmailResponse response = sendEmail(request);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Gửi email với retry logic
     */
    public EmailResponse sendEmailWithRetry(EmailRequest request) {
        EmailResponse lastResponse = null;
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                lastResponse = sendEmail(request);
                
                if ("SUCCESS".equals(lastResponse.getStatus())) {
                    lastResponse.setRetryCount(attempt - 1);
                    return lastResponse;
                }
                
            } catch (Exception e) {
                log.warn("Email send attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt == maxRetryAttempts) {
                    lastResponse = EmailResponse.failed(
                        lastResponse != null ? lastResponse.getMessageId() : generateMessageId(),
                        request.getTo(),
                        request.getSubject(),
                        "Max retry attempts reached: " + e.getMessage()
                    );
                    lastResponse.setRetryCount(attempt);
                }
                
                // Exponential backoff
                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(1000L * attempt * attempt); // 1s, 4s, 9s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return lastResponse;
    }

    /**
     * Gửi email đơn giản (text hoặc HTML)
     */
    private EmailResponse sendSimpleEmail(EmailRequest request, String messageId, long startTime) 
            throws MessagingException {
        
        if (request.getHtmlContent() != null || !request.getAttachments().isEmpty()) {
            return sendMimeEmail(request, messageId, startTime);
        }
        
        // Plain text email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(buildFromAddress(request));
        message.setTo(request.getTo().toArray(new String[0]));
        
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            message.setCc(request.getCc().toArray(new String[0]));
        }
        
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            message.setBcc(request.getBcc().toArray(new String[0]));
        }
        
        message.setSubject(request.getSubject());
        message.setText(request.getTextContent());
        
        if (request.getReplyTo() != null) {
            message.setReplyTo(request.getReplyTo());
        }

        mailSender.send(message);
        
        log.info("Simple email sent successfully. MessageId: {}, Recipients: {}", messageId, request.getTo());
        
        EmailResponse response = EmailResponse.success(messageId, request.getTo(), request.getSubject());
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Gửi MIME email (HTML + attachments)
     */
    private EmailResponse sendMimeEmail(EmailRequest request, String messageId, long startTime) 
            throws MessagingException {
        
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(buildFromAddress(request));
        helper.setTo(request.getTo().toArray(new String[0]));
        
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(new String[0]));
        }
        
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(new String[0]));
        }
        
        helper.setSubject(request.getSubject());
        
        if (request.getReplyTo() != null) {
            helper.setReplyTo(request.getReplyTo());
        }

        // Set content
        if (request.getHtmlContent() != null) {
            helper.setText(request.getTextContent(), request.getHtmlContent());
        } else {
            helper.setText(request.getTextContent());
        }

        // Set priority
        if (request.getPriority() != null) {
            mimeMessage.setHeader("X-Priority", String.valueOf(request.getPriority().getValue()));
        }

        // Custom headers
        if (request.getCustomHeaders() != null) {
            request.getCustomHeaders().forEach((key, value) -> {
                try {
                    mimeMessage.setHeader(key, value);
                } catch (MessagingException e) {
                    log.warn("Failed to set custom header {}: {}", key, e.getMessage());
                }
            });
        }

        // Tracking headers
        if (Boolean.TRUE.equals(request.getTrackOpening())) {
            mimeMessage.setHeader("X-Track-Opening", "true");
        }
        
        if (Boolean.TRUE.equals(request.getTrackClicking())) {
            mimeMessage.setHeader("X-Track-Clicking", "true");
        }

        // Category
        if (request.getCategory() != null) {
            mimeMessage.setHeader("X-Category", request.getCategory());
        }

        // Message ID
        mimeMessage.setHeader("Message-ID", messageId);

        // Attachments
        if (request.getAttachments() != null) {
            for (EmailRequest.EmailAttachment attachment : request.getAttachments()) {
                ByteArrayResource resource = new ByteArrayResource(attachment.getContent());
                
                if (Boolean.TRUE.equals(attachment.getInline()) && attachment.getContentId() != null) {
                    helper.addInline(attachment.getContentId(), resource, attachment.getContentType());
                } else {
                    helper.addAttachment(attachment.getFileName(), resource, attachment.getContentType());
                }
            }
        }

        mailSender.send(mimeMessage);
        
        log.info("MIME email sent successfully. MessageId: {}, Recipients: {}", messageId, request.getTo());
        
        EmailResponse response = EmailResponse.success(messageId, request.getTo(), request.getSubject());
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Gửi email sử dụng template
     */
    private EmailResponse sendTemplateEmail(EmailRequest request, String messageId, long startTime) 
            throws MessagingException {
        
        // Process template
        Context context = new Context();
        if (request.getTemplateVariables() != null) {
            request.getTemplateVariables().forEach(context::setVariable);
        }
        
        String htmlContent = templateEngine.process(request.getTemplateName(), context);
        
        // Create new request with processed template
        EmailRequest processedRequest = EmailRequest.builder()
                .to(request.getTo())
                .cc(request.getCc())
                .bcc(request.getBcc())
                .subject(request.getSubject())
                .textContent(request.getTextContent())
                .htmlContent(htmlContent)
                .attachments(request.getAttachments())
                .priority(request.getPriority())
                .replyTo(request.getReplyTo())
                .fromName(request.getFromName())
                .trackOpening(request.getTrackOpening())
                .trackClicking(request.getTrackClicking())
                .category(request.getCategory())
                .customHeaders(request.getCustomHeaders())
                .build();

        return sendMimeEmail(processedRequest, messageId, startTime);
    }

    /**
     * Build from address với tên
     */
    private String buildFromAddress(EmailRequest request) {
        String fromName = request.getFromName() != null ? request.getFromName() : defaultFromName;
        return String.format("%s <%s>", fromName, defaultFromEmail);
    }

    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return UUID.randomUUID().toString() + "@hddt.com";
    }

    /**
     * Validate email request
     */
    public boolean validateEmailRequest(EmailRequest request) {
        if (request.getTo() == null || request.getTo().isEmpty()) {
            return false;
        }
        
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            return false;
        }
        
        if (request.getTextContent() == null && request.getHtmlContent() == null && request.getTemplateName() == null) {
            return false;
        }
        
        return true;
    }

    /**
     * Get email service status
     */
    public boolean isEmailServiceHealthy() {
        try {
            // Test connection
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.error("Email service health check failed: {}", e.getMessage());
            return false;
        }
    }
}