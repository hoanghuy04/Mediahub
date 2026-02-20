package com.bondhub.notificationservices.utils;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateEngine {
    public String render(String template, Map<String, Object> data) {

        String result = template;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    entry.getValue().toString()
            );
        }

        return result;
    }
}
