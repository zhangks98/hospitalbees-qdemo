package sg.edu.ntu.hospitalbeesqdemo.model;

import java.util.Map;

public enum LateRank {
    ON_TIME,
    LITTLE_LATE,
    VERY_LATE;

    private static final String LATE_KEY = "laterank";

    public static LateRank parse(Map<String, String> payload) {
        String value = payload.get(LATE_KEY);

        if (value == null) {
            throw new IllegalArgumentException("Payload is missing laterank'");
        }

        try {
            return LateRank.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("'%s' is an illegal value for key '%s'", value, LATE_KEY), e);
        }

    }
}
