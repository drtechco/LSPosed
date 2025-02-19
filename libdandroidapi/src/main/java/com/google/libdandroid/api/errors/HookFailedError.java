package com.google.libdandroid.api.errors;

/**
 * Thrown to indicate that a hook failed due to framework internal error.
 */
@SuppressWarnings("unused")
public class HookFailedError extends DAndroidFrameworkError {

    public HookFailedError(String message) {
        super(message);
    }

    public HookFailedError(String message, Throwable cause) {
        super(message, cause);
    }

    public HookFailedError(Throwable cause) {
        super(cause);
    }
}
