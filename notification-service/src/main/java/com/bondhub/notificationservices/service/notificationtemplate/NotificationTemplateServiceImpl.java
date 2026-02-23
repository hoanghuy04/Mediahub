package com.bondhub.notificationservices.service.notificationtemplate;

import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.notificationservices.dto.request.notificationtemplate.CreateTemplateRequest;
import com.bondhub.notificationservices.dto.request.notificationtemplate.UpdateTemplateRequest;
import com.bondhub.notificationservices.dto.response.notificationtemplate.NotificationTemplateResponse;
import com.bondhub.notificationservices.enums.NotificationChannel;
import com.bondhub.notificationservices.enums.NotificationType;
import com.bondhub.notificationservices.mapper.NotificationTemplateMapper;
import com.bondhub.notificationservices.model.NotificationTemplate;
import com.bondhub.notificationservices.repository.NotificationTemplateRepository;
import com.bondhub.notificationservices.utils.TemplateEngine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    NotificationTemplateRepository notificationTemplateRepository;
    TemplateEngine templateEngine;
    NotificationTemplateMapper notificationTemplateMapper;

    @Override
    public NotificationTemplateResponse create(
            CreateTemplateRequest request) {

        log.info("Creating notification template for type={} locale={}",
                request.type(), request.locale());

        NotificationTemplate template = notificationTemplateMapper.toEntity(request);
        template.setActive(true);

        notificationTemplateRepository.save(template);

        log.info("Template created with id={}", template.getId());

        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public NotificationTemplateResponse update(String id, UpdateTemplateRequest request) {

        log.info("Updating notification template id={}", id);

        NotificationTemplate template = notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new AppException((ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND)));

        if (request.titleTemplate() != null) {
            template.setTitleTemplate(request.titleTemplate());
        }

        if (request.bodyTemplate() != null) {
            template.setBodyTemplate(request.bodyTemplate());
        }

        if (request.active() != null) {
            template.setActive(request.active());
        }

        notificationTemplateRepository.save(template);

        log.info("Template updated id={}", id);

        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public NotificationTemplateResponse getTemplate(
            NotificationType type, NotificationChannel channel, String locale) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));

        return notificationTemplateMapper.toResponse(template);
    }

    @Override
    public String renderTitle(NotificationType type, NotificationChannel channel,
                              String locale, Map<String, Object> data) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow();

        return templateEngine.render(template.getTitleTemplate(), data);
    }

    @Override
    public String renderBody(NotificationType type, NotificationChannel channel,
                             String locale, Map<String, Object> data) {
        NotificationTemplate template = notificationTemplateRepository
                .findByTypeAndChannelAndLocaleAndActiveTrue(type, channel, locale)
                .orElseThrow();

        return templateEngine.render(template.getBodyTemplate(), data);
    }
}
