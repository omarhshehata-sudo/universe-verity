package com.universeexe.verity.trust;

/**
 * Why trust changed — used for anti-farming cooldowns and debug logs.
 */
public enum TrustReason {
    // +1
    OPENED_BOX(1),
    FIRST_GREETING(1),
    POLITE_GREETING(1),
    THANKED_AFTER_HELP(1),
    COMPLETED_VERITY_QUEST(1),
    GENTLE_PICKUP(1),
    GAVE_LIKED_ITEM(1),
    POSITIVE_RESPONSE(1),
    // -1
    HIT_VERITY(-1),
    DANGEROUS_DROP(-1),
    STORED_IN_CONTAINER(-1),
    COMMAND_SPAM(-1),
    INSULTED_VERITY(-1),
    ABANDONED_VERITY(-1),
    GREEDY_REQUEST_SPAM(-1),
    // debug
    DEBUG_SET(0),
    DEBUG_ADD(0);

    private final int defaultDelta;

    TrustReason(int defaultDelta) {
        this.defaultDelta = defaultDelta;
    }

    public int defaultDelta() {
        return defaultDelta;
    }

    public static TrustReason fromName(String name) {
        return TrustReason.valueOf(name.trim().toUpperCase());
    }
}
