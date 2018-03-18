package sg.edu.ntu.hospitalbeesqdemo.model;

import java.util.Map;

public enum LateRank {
    ON_TIME,
    LITTLE_LATE,
    VERY_LATE;


    public static LateRank parse(String lateRank) {

        try {
            return LateRank.valueOf(lateRank.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("'%s' is an illegal value for lateRank", lateRank), e);
        }

    }
}
