package com.skydevs.tgdrive.Constants;

/**
 * A class to hold constants for setting keys.
 * This prevents the use of "magic strings" throughout the application,
 * making the code safer and easier to maintain.
 */
public final class SettingConstant {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SettingConstant() {
    }

    /**
     * The key for the setting that determines whether user registration is allowed.
     * The value should be "true" or "false".
     */
    public static final String ALLOW_REGISTRATION = "allow_registration";
}
