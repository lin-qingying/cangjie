/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.model

import org.cangnova.cangjie.cfir.tree.generator.BASE_PACKAGE
import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.ElementOrRef as GenericElementOrRef
import org.cangnova.cangjie.generators.tree.ElementRef as GenericElementRef

class Element(name: String, override val propertyName: String, kind: Kind) : AbstractElement<Element, Field, Implementation>(name) {
    companion object {
        private val allowedKinds = setOf(
            ImplementationKind.Interface,
            ImplementationKind.SealedInterface,
            ImplementationKind.AbstractClass,
            ImplementationKind.SealedClass,
        )
    }

    override val namePrefix: String
        get() = "Cfir"

    override val packageName: String = BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else ".$it" }

    override var kind: ImplementationKind?
        get() = super.kind
        set(value) {
            if (value !in allowedKinds) {
                throw IllegalArgumentException(value.toString())
            }
            super.kind = value
        }

    var _needTransformOtherChildren: Boolean = false

    override val hasAcceptMethod: Boolean
        get() = true

    override val hasTransformMethod: Boolean
        get() = true

    override val walkableChildren: List<Field>
        get() = emptyList()

    override val transformableChildren: List<Field>
        get() = emptyList()

    override val visitorParameterName: String
        get() = safeDecapitalizedName

    val needTransformOtherChildren: Boolean
        get() = _needTransformOtherChildren || elementParents.any { it.element.needTransformOtherChildren }

    operator fun FieldSet.unaryPlus() {
        val copiedFields = fieldDefinitions.map { it.copy() }
        this@Element.fields.addAll(copiedFields)
    }

    enum class Kind(val packageName: String) {
        Expression("expressions"),
        Declaration("declarations"),
        Pattern("patterns"),
        Reference("references"),
        TypeRef("types"),
        Contracts("contracts"),
        Diagnostics("diagnostics"),
        Other(""),
    }
}

typealias ElementRef = GenericElementRef<Element>

typealias ElementOrRef = GenericElementOrRef<Element>
