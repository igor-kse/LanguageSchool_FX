package by.poskorbko.languageschool_fx.dto;

public record ScheduleToPost(
        String id,
        String groupId,
        String dayOfWeek,
        String startTime,
        String endTime
) {}
