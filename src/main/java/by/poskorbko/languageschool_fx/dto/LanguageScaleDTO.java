package by.poskorbko.languageschool_fx.dto;

import java.util.Set;

public record LanguageScaleDTO(String name, String description, Set<LanguageScaleLevelDTO> levels) {}
