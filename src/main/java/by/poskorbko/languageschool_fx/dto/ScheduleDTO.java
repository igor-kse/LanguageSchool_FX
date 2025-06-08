package by.poskorbko.languageschool_fx.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleDTO(
        String id,
        String groupName,
        String languageName,
        String teacherName,
        String levelName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) { }
