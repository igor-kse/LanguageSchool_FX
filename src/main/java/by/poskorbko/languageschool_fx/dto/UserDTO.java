package by.poskorbko.languageschool_fx.dto;

import java.util.Set;

public record UserDTO(String id, String firstName, String lastName, String email, Set<Role> roles) {
}
