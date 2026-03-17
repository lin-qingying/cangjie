package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable

class TestDisposable(private val debugName: String) : Disposable {
    override fun dispose() = Unit

    override fun toString(): String = "TestDisposable($debugName)"
}
