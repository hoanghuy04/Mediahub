package com.bondhub.notificationservices.dto.request.notificationtemplate;

public record UpdateTemplateRequest(

        String titleTemplate,

        String bodyTemplate,

        Boolean active
) {}
