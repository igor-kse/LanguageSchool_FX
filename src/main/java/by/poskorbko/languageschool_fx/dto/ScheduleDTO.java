package by.poskorbko.languageschool_fx.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleDTO(String id, GroupDTO group, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
