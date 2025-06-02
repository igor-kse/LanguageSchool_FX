package by.poskorbko.languageschool_fx.dto;

import java.util.List;

public record UserDTO(String id, String firstName, String lastName, String email, String password, List<String> roles, String avatarBase64) {
}
