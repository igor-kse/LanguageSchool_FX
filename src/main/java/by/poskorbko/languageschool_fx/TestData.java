package by.poskorbko.languageschool_fx;

import by.poskorbko.languageschool_fx.dto.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class TestData {
    public static List<ScheduleDTO> getTestSchedule() {
        return List.of(
                new ScheduleDTO("1",
                        new GroupDTO("g1", "A1-Morning",
                                new GradeDTO("gr1", new LanguageDTO("Английский"), CEFRLevel.A1),
                                new LanguageDTO("Английский")
                        ),
                        DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30)),
                new ScheduleDTO("2",
                        new GroupDTO("g2", "B1-Evening",
                                new GradeDTO("gr2", new LanguageDTO("Испанский"), CEFRLevel.B1),
                                new LanguageDTO("Испанский")
                        ),
                        DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(19, 30)),
                new ScheduleDTO("3",
                        new GroupDTO("g3", "C1-Weekend",
                                new GradeDTO("gr3", new LanguageDTO("Французский"), CEFRLevel.C1),
                                new LanguageDTO("Французский")
                        ),
                        DayOfWeek.SATURDAY, LocalTime.of(12, 0), LocalTime.of(13, 30))
        );
    }

    public static String dayOfWeekRus(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Понедельник";
            case TUESDAY -> "Вторник";
            case WEDNESDAY -> "Среда";
            case THURSDAY -> "Четверг";
            case FRIDAY -> "Пятница";
            case SATURDAY -> "Суббота";
            case SUNDAY -> "Воскресенье";
        };
    }

    public static List<LevelScaleDTO> getTestLevelScale() {
        return List.of(new LevelScaleDTO("CEFR", List.of("A1", "A2", "B1", "B2", "C1", "C2")),
                new LevelScaleDTO("ACTFL", List.of("Novice", "Intermediate", "Advanced", "Superior")),
                new LevelScaleDTO("Авторская", List.of("Начальный", "Средний", "Продвинутый", "Эксперт")));
    }

    public static List<LanguageEntryDTO> getTestLanguageEntries() {
        return List.of(
                new LanguageEntryDTO("Английский", "CEFR", ""),
                new LanguageEntryDTO("Французский", "CEFR", "для детей"),
                new LanguageEntryDTO("Испанский", "ACTFL", "")
        );
    }

}
