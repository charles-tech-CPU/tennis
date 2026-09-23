package com.charles.tennisresults.dto;

/** Une nationalite associee a un compteur (titres, ...) pour une stat donnee. */
public record NationCountDto(String nationality, long count) {}
