package org.cangnova.cangjie.utils.exceptions

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import java.util.concurrent.CancellationException

/**
 * Some exceptions that originate from Intellij Platform should never be logged or handled and must always be rethrown.
 *
 * Examples of such exceptions include [ProcessCanceledException] and [IndexNotReadyException].
 */
fun shouldIjPlatformExceptionBeRethrown(exception: Throwable): Boolean = when (exception) {
    is CancellationException -> true
    is ControlFlowException -> true
    is IndexNotReadyException -> true
    else -> false
}