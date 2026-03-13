/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.generators.tree

import org.cangjie.generators.tree.imports.ImportCollecting
import org.cangjie.generators.tree.imports.Importable

/**
 * 树生成器中树节点的抽象类/接口。
 *
 * 例如：`CfirElement`、`CfirRegularClass`。
 */
abstract class AbstractElement<Element, Field, Implementation>(
    val name: String,
) : ElementOrRef<Element>, FieldContainer<Field>, ImplementationKindOwner, ImportCollecting
        where Element : AbstractElement<Element, Field, Implementation>,
              Field : AbstractField<Field>,
              Implementation : AbstractImplementation<Implementation, Element, *> {

    abstract val propertyName: String

    abstract val namePrefix: String

    var kDoc: String? = null

    val fields = mutableSetOf<Field>()

    val params = mutableListOf<TypeVariable>()

    private val _elementParents = mutableListOf<ElementRef<Element>>()
    val elementParents: List<ElementRef<Element>> get() = _elementParents

    fun addParent(parent: ElementRef<Element>) {
        _elementParents.add(parent)
        parent.element._subElements.add(element)
    }

    fun replaceParent(oldParent: Element, newParent: ElementRef<Element>) {
        val parentIndex = _elementParents.indexOfFirst { it.element == oldParent }
        require(parentIndex >= 0) {
            "$oldParent is not parent of $this"
        }
        _elementParents[parentIndex] = newParent
        oldParent._subElements.remove(element)
        newParent.element._subElements.add(element)
    }

    val otherParents = mutableListOf<ClassRef<*>>()

    val parentRefs: List<ClassOrElementRef>
        get() = elementParents + otherParents

    val isRootElement: Boolean
        get() = elementParents.isEmpty()

    @Suppress("PropertyName")
    val _subElements = mutableSetOf<Element>()
    val subElements: Set<Element> get() = _subElements

    var isSealed: Boolean = false

    var nameInVisitorMethod: String = name

    val visitFunctionName: String
        get() = "visit$nameInVisitorMethod"

    abstract val visitorParameterName: String

    var customParentInVisitor: Element? = null

    open val parentInVisitor: Element?
        get() = customParentInVisitor ?: elementParents.singleOrNull()?.element?.takeIf { !it.isRootElement }

    override val allParents: List<Element>
        get() = elementParents.map { it.element }

    override val typeName: String
        get() = namePrefix + name

    final override fun renderTo(appendable: Appendable, importCollector: ImportCollecting) {
        importCollector.addImport(this)
        appendable.append(typeName)
    }

    override lateinit var allFields: List<Field>

    internal fun inheritFields() {
        val result = LinkedHashMap<String, Field>()
        fields.toList().asReversed().associateByTo(result) { it.name }

        val allInheritedFieldsByParent = buildMap<String, MutableList<Pair<ElementRef<Element>, Field>>> {
            elementParents.asReversed().forEach { parentRef ->
                parentRef.element.allFields.asReversed().forEach { field ->
                    val list = remove(field.name) ?: mutableListOf()
                    list.add(parentRef to field)
                    put(field.name, list)
                }
            }
        }

        for ((fieldName, inheritedFieldsByParent) in allInheritedFieldsByParent) {
            var field = result[fieldName]
            if (field == null) {
                val inheritFrom = inheritedFieldsByParent.distinctBy { it.second.typeRef }.singleOrNull() ?: error(
                    "Field $fieldName has ambiguous type, coming from [${inheritedFieldsByParent.joinToString { it.first.element.typeName }}], " +
                            "please specify it explicitly for the ${element.name} element"
                )

                field = inheritFrom.second.copy().apply {
                    substituteType(inheritFrom.first.args)
                }

                result[fieldName] = field
            }

            val inheritedFields = inheritedFieldsByParent.map { it.second }
            field.isOverride = true
            field.updatePropertiesFromOverriddenFields(inheritedFields)
        }

        allFields = result.values.toList().asReversed()
    }

    var transformerReturnType: Element? = null

    internal var baseTransformerType: Element? = null

    val transformerClass: Element
        get() = transformerReturnType ?: baseTransformerType ?: element

    val implementations = mutableListOf<Implementation>()

    var doesNotNeedImplementation: Boolean = false

    val additionalImports = mutableListOf<Importable>()

    override fun addImport(importable: Importable) {
        additionalImports.add(importable)
    }

    override var kind: ImplementationKind? = null

    override var hasAcceptChildrenMethod: Boolean = false
    override var hasTransformChildrenMethod: Boolean = false

    @Suppress("UNCHECKED_CAST")
    final override val element: Element
        get() = this as Element

    final override val args: Map<NamedTypeParameterRef, TypeRef>
        get() = emptyMap()

    final override val nullable: Boolean
        get() = false

    var doPrint = true

    final override fun copy(nullable: Boolean) = ElementRef(element, args, nullable)

    final override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementRef(element, args, nullable)

    @Suppress("UNCHECKED_CAST")
    override fun substitute(map: TypeParameterSubstitutionMap): Element = this as Element

    fun withStarArgs(): ElementRef<Element> = copy(params.associateWith { TypeRef.Star })

    fun withSelfArgs(): ElementRef<Element> = copy(params.associateWith { it })

    operator fun TypeVariable.unaryPlus() = apply {
        params.add(this)
    }

    operator fun Field.unaryPlus() = apply {
        fields.add(this)
    }

    override fun toString(): String = buildString { renderTo(this, ImportCollecting.Empty) }
}
