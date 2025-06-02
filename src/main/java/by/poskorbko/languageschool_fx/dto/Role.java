package by.poskorbko.languageschool_fx.dto;

import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.Set;

public enum Role {
    USER("role.user", 1),
    STUDENT("role.student", 2),
    TEACHER("role.teacher",3),
    ADMIN("role.admin", 4);

    private static final ResourceBundle bundle = ResourceBundle.getBundle("messages");

    private final int weight;
    private final String name;

    Role(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public String getName() {
        return bundle.getString(name);
    }

    public static Role getStrongest(Set<Role> roles) {
        return roles.stream().max(Comparator.comparing(Role::getWeight)).orElseThrow();
    }
}
