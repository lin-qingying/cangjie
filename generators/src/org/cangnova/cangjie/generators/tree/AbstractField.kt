/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.Importable

abstract class AbstractField<Field : AbstractField<Field>> {

    abstract val name: String

    abstract val typeRef: TypeRefWithNullability

    val nullable: Boolean
        get() = typeRef.nullable

    var kDoc: String? = null

    open val isVolatile: Boolean
        get() = false

    abstract val isFinal: Boolean

    open val isParameter: Boolean
        get() = false

    open val arbitraryImportables: MutableList<Importable> = mutableListOf()

    /**
     * 字段上的额外注解（逐个打印为 `@AnnotationName`）。
     *
     * 与 [optInAnnotation] 不同，这些注解不会被 `@OptIn(...)::class` 包裹。
     */
    open val additionalAnnotations: MutableList<ClassRef<*>> = mutableListOf()

    open var optInAnnotation: ClassRef<*>? = null
    open var replaceOptInAnnotation: ClassRef<*>? = null

    abstract var isMutable: Boolean

    open var customInitializationCall: String? = null

    val invisibleField: Boolean
        get() = customInitializationCall != null

    var deprecation: Deprecated? = null

    var visibility: Visibility = Visibility.PUBLIC

    var isOverride: Boolean = false

    open var skippedInCopy: Boolean = false

    open val containsElement: Boolean
        get() = typeRef is ElementOrRef<*> || this is ListField && baseType is ElementOrRef<*>

    open var implementationDefaultStrategy: ImplementationDefaultStrategy? = null

    abstract var defaultValueInBuilder: String?

    abstract var customSetter: String?

    var useInBaseTransformerDetection = true

    abstract val isChild: Boolean

    open val overriddenFields: MutableSet<Field> = mutableSetOf()

    open fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        overriddenFields += parentFields
        isMutable = isMutable || parentFields.any { it.isMutable }
    }

    override fun toString(): String {
        return name
    }

    abstract fun substituteType(map: TypeParameterSubstitutionMap)

    fun copy() = internalCopy().also(::updateFieldsInCopy)

    protected abstract fun internalCopy(): Field

    protected open fun updateFieldsInCopy(copy: Field) {
        copy.kDoc = kDoc
        copy.arbitraryImportables += arbitraryImportables
        copy.additionalAnnotations += additionalAnnotations
        copy.optInAnnotation = optInAnnotation
        copy.replaceOptInAnnotation = replaceOptInAnnotation
        copy.isMutable = isMutable
        copy.deprecation = deprecation
        copy.visibility = visibility
        copy.isOverride = isOverride
        copy.useInBaseTransformerDetection = useInBaseTransformerDetection
        copy.overriddenFields += overriddenFields
        copy.implementationDefaultStrategy = implementationDefaultStrategy
    }

    sealed interface ImplementationDefaultStrategy {
        open val defaultValue: String?
            get() = null
        open val withGetter: Boolean
            get() = false

        data object Required : ImplementationDefaultStrategy
        data object Lateinit : ImplementationDefaultStrategy

        data class DefaultValue(
            override val defaultValue: String,
            override val withGetter: Boolean,
        ) : ImplementationDefaultStrategy
    }

    var symbolFieldRole: SymbolFieldRole? = null

    enum class SymbolFieldRole {
        DECLARED, REFERENCED
    }
}
