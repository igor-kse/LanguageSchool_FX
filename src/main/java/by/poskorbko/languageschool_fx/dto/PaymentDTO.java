package by.poskorbko.languageschool_fx.dto;

import java.time.LocalDate;

public record PaymentDTO(String id, UserDTO userDTO, long amount, LocalDate date, String description) {
}
