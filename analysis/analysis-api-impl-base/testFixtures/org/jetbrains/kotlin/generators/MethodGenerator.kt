package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer

abstract class MethodGenerator<in T : MethodModel<in T>> {
    companion object {
        fun generateDefaultSignature(method: MethodModel<*>, p: Printer) {
            p.print("public void ${method.name}()")
        }

        const val DEFAULT_RUN_TEST_METHOD_NAME = "runTest"
    }

    abstract fun generateSignature(method: T, p: Printer)
    abstract fun generateBody(method: T, p: Printer)
}
