package org.example.userservice.common.util;

import com.github.f4b6a3.uuid.UuidCreator;

public final class UuidV7Generator {

    private UuidV7Generator() {
    }

    public static String generate() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }
}
