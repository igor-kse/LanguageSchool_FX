package by.poskorbko.languageschool_fx.dto;

import java.util.Set;

public record ScaleDTO(
        String name,
        String description,
        Set<ScaleLevelDTO> levels
) {}
