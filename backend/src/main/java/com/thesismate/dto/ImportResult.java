package com.thesismate.dto;

import java.util.List;

public record ImportResult(int created, int updated, List<String> errors) {
}