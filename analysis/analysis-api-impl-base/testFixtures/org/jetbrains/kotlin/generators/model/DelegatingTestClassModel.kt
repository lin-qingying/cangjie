package org.jetbrains.kotlin.generators.model

abstract class DelegatingTestClassModel(private val delegate: TestClassModel) : TestClassModel() {
    override val name: String
        get() = delegate.name
    override val innerTestClasses: Collection<TestClassModel>
        get() = delegate.innerTestClasses
    override val methods: Collection<MethodModel<*>>
        get() = delegate.methods
    override val isEmpty: Boolean
        get() = delegate.isEmpty
    override val dataPathRoot: String?
        get() = delegate.dataPathRoot
    override val dataString: String?
        get() = delegate.dataString
    override val annotations: Collection<AnnotationModel>
        get() = delegate.annotations
    override val tags: List<String>
        get() = delegate.tags
}
