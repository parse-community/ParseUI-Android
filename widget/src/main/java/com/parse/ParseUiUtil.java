package com.parse;

import bolts.Task;

/**
 * Plz ignore
 */
public class ParseUiUtil {

    public static <T> Task<T> callbackOnMainThreadAsync(Task<T> task,
                                                 final ParseCallback2<T, ParseException> callback, final boolean reportCancellation) {
        return ParseTaskUtils.callbackOnMainThreadAsync(task, callback, reportCancellation);
    }
}
