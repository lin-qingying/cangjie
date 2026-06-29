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

/**
 * 安静关闭资源，忽略关闭过程中抛出的任何异常。
 */
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

/**
 * 判断当前异常类型链是否为 IntelliJ 平台的 ProcessCanceledException。
 */
fun Throwable.isProcessCanceledException(): Boolean {
    var klass: Class<out Any?> = this.javaClass
    while (true) {
        if (klass.canonicalName == "com.intellij.openapi.progress.ProcessCanceledException") return true
        klass = klass.superclass ?: return false
    }
}
