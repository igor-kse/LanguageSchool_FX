package by.poskorbko.languageschool_fx.dto;

public enum CEFRLevel {
    A0("Beginner"),
    A1("Elementary"),
    A2("Pre-Intermediate"),
    B1("Intermediate"),
    B2("Upper Intermediate"),
    C1("Advanced"),
    C2("Proficiency");

    private final String title;

    CEFRLevel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
