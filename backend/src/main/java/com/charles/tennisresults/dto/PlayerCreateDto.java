package com.charles.tennisresults.dto;

import jakarta.validation.constraints.NotBlank;

public record PlayerCreateDto(@NotBlank String lastName, String firstName, String nationality) {}
