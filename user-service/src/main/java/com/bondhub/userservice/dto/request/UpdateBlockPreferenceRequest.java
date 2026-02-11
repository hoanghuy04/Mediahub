package com.bondhub.userservice.dto.request;

import lombok.Builder;

/**
 * Request to update block preferences for an existing block
 */
@Builder
public record UpdateBlockPreferenceRequest(
    Boolean blockMessage,
    Boolean blockCall,
    Boolean blockStory
) {}
