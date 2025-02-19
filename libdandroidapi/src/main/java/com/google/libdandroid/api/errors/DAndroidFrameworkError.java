package com.google.libdandroid.api.errors;

/**
 * Thrown to indicate that the DAndroid framework function is broken.
 */
public class DAndroidFrameworkError extends Error {

    public DAndroidFrameworkError(String message) {
        super(message);
    }

    public DAndroidFrameworkError(String message, Throwable cause) {
        super(message, cause);
    }

    public DAndroidFrameworkError(Throwable cause) {
        super(cause);
    }
}
