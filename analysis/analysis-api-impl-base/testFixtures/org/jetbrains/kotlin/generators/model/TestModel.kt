package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.utils.Printer

sealed class TestEntityModel {
    abstract val name: String
    abstract val dataString: String?
    abstract val tags: List<String>
}

abstract class TestClassModel : TestEntityModel() {
    abstract val innerTestClasses: Collection<TestClassModel>
    abstract val methods: Collection<MethodModel<*>>
    abstract val isEmpty: Boolean
    abstract val dataPathRoot: String?
    abstract val annotations: Collection<AnnotationModel>

    val imports: Set<Class<*>>
        get() {
            return mutableSetOf<Class<*>>().also { allImports ->
                annotations.flatMapTo(allImports) { it.imports() }
                methods.flatMapTo(allImports) { it.imports() }
                innerTestClasses.flatMapTo(allImports) { it.imports }
            }
        }
}

abstract class MethodModel<M : MethodModel<M>> : TestEntityModel() {
    abstract val generator: MethodGenerator<M>
    open val isTestMethod: Boolean get() = true
    open val shouldBeGeneratedForInnerTestClass: Boolean get() = true
    open fun imports(): Collection<Class<*>> = emptyList()

    fun generateBody(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateBody(this as M, p)
    }

    fun generateSignature(p: Printer) {
        @Suppress("UNCHECKED_CAST")
        generator.generateSignature(this as M, p)
    }
}
