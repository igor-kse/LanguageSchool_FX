package by.poskorbko.languageschool_fx.dto;

import java.util.List;

public record TeacherDTO(
        String id,
        String firstName,
        String lastName,
        String education,
        List<String> languages,
        String email) {
}
