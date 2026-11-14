package com.thesismate.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCapacityRequest(
        @NotBlank @Size(max = 30) String semester,
        @Min(0) int capacity,
        Long version) {   // prothom save e null thakte pare, update e lagbei lagbe
}
