package org.cangnova.cangjie.utils

import java.io.Closeable


/**
 * Translate exception to unchecked exception.
 *
 * Return type is specified to make it possible to use it like this:
 *     throw ExceptionUtils.rethrow(e);
 * In this case compiler knows that code after this rethrowing won't be executed.
 */
fun rethrow(e: Throwable): RuntimeException {
    throw e
}

fun closeQuietly(closeable: Closeable?) {
    if (closeable != null) {
        try {
            closeable.close()
        }
        catch (ignored: Throwable) {
            // Do nothing
        }
    }
}

fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}
