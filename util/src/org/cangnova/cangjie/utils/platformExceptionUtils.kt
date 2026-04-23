package org.cangnova.cangjie.utils

import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

/**
 * Some exceptions that originate from Intellij Platform should never be logged or handled and must always be rethrown.
 *
 * Examples of such exceptions include [ProcessCanceledException] and [IndexNotReadyException].
 */
fun rethrowIntellijPlatformExceptionIfNeeded(exception: Throwable) {
    if (shouldIjPlatformExceptionBeRethrown(exception)) {
        throw exception
    }
}
