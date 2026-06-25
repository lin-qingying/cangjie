package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangnova.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangnova.cangjie.cfir.tree.generator.cfirVisitorType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.cfir.tree.generator.model.SimpleField
import org.cangnova.cangjie.cfir.tree.generator.pureAbstractElementType
import org.cangnova.cangjie.cfir.tree.generator.toMutableOrEmptyImport
import org.cangnova.cangjie.cfir.tree.generator.transformInPlaceImport
import org.cangnova.cangjie.cfir.tree.generator.util.getMutableType
import org.cangnova.cangjie.cfir.tree.generator.util.replaceFunctionDeclaration
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.AbstractFieldPrinter
import org.cangnova.cangjie.generators.tree.AbstractImplementationPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.TypeRefWithNullability
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.call
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.withIndent

/**
 * CFIR 实现类字段源码打印器。
 */
private class ImplementationFieldPrinter(printer: ImportCollectingPrinter) : AbstractFieldPrinter<Field>(printer) {
    /**
     * 判断字段在实现类中是否必须生成为可变属性。
     */
    override fun forceMutable(field: Field): Boolean = field.isMutable && (field !is ListField || field.isMutableOrEmptyList)

    /**
     * 返回实现类字段使用的实际类型。
     */
    override fun actualTypeOfField(field: Field) = field.getMutableType()

    /**
     * 是否为字段访问包裹 opt-in 注解。
     */
    override val wrapOptInAnnotations
        get() = true
}

/**
 * CFIR 具体实现类源码打印器。
 */
internal class ImplementationPrinter(
    printer: ImportCollectingPrinter,
) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {

    /**
     * 实现类使用的内部实现 opt-in 注解。
     */
    override val implementationOptInAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    /**
     * 返回纯抽象 CFIR 元素基类类型。
     */
    override fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        pureAbstractElementType

    /**
     * 创建实现类字段打印器。
     */
    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> = ImplementationFieldPrinter(printer)

    /**
     * 判断当前实现的任一父元素是否满足条件。
     */
    private inline fun Implementation.anyParent(condition: (Element) -> Boolean): Boolean {
        val visited = mutableSetOf<Element>()
        val stack = this.allParents.toMutableList()

        while (stack.isNotEmpty()) {
            val next = stack.removeLast()

            when {
                !visited.add(next) -> continue
                condition(next) -> return true
                else -> stack += next.allParents
            }
        }

        return false
    }

    /**
     * 打印实现类的初始化、acceptChildren、transformChildren、replace 等附加方法。
     */
    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        fun Field.transform() {
            when (this) {
                is SimpleField ->
                    println("$name = ${name}${call()}transform(transformer, data)")

                is ListField -> {
                    addImport(transformInPlaceImport)
                    println("${name}.transformInplace(transformer, data)")
                }
            }
        }

        with(implementation) {
            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

            val bindingCalls = element.allFields.filter {
                it.withBindThis && it.hasSymbolType && it !is ListField && it.name != "companionObjectSymbol"
            }.takeIf {
                it.isNotEmpty() && !isInterface && !isAbstract &&
                    !element.typeName.contains("Reference") &&
                    !element.typeName.contains("ResolvedQualifier") &&
                    !element.typeName.endsWith("Ref")
            }.orEmpty()

            val customCalls = fieldsInConstructor.filter { it.customInitializationCall != null }
            if (bindingCalls.isNotEmpty() || customCalls.isNotEmpty()) {
                println()
                println("init {")
                withIndent {
                    for (symbolField in bindingCalls) {
                        println("${symbolField.name}${symbolField.call()}bind(this)")
                    }

                    for (customCall in customCalls) {
                        addAllImports(customCall.arbitraryImportables)
                        println("${customCall.name} = ${customCall.customInitializationCall}")
                    }

                    val sourceField = implementation.allFields.find { it.name == "source" }
                    val originField = implementation.allFields.find { it.name == "origin" }
                    if (originField != null && sourceField != null && sourceField.typeRef.nullable) {
                        println("@Suppress(\"SENSELESS_COMPARISON\")")
                        println("require(source != null || origin != CfirDeclarationOrigin.Source) { \"\${this::class.simpleName} with Source origin was instantiated without a source element.\" }")
                    }
                }
                println("}")
            }

            fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"

            if (hasAcceptChildrenMethod) {
                printAcceptChildrenMethod(this, cfirVisitorType, TypeVariable("R"), override = true)
                print(" {")

                val walkableFields = walkableChildren
                if (walkableFields.isNotEmpty()) {
                    println()
                    withIndent {
                        for (field in walkableFields) {
                            when (field.name) {
                                "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]
                                    val dispatchReceiver = implementation.getOrNull("dispatchReceiver")
                                    val extensionReceiver = implementation.getOrNull("extensionReceiver")
                                    when {
                                        dispatchReceiver != null && extensionReceiver != null -> {
                                            println(
                                                """
                                    |${explicitReceiver.acceptString()}
                                    |        if (dispatchReceiver !== explicitReceiver) {
                                    |            ${dispatchReceiver.acceptString()}
                                    |        }
                                    |        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            ${extensionReceiver.acceptString()}
                                    |        }
                                                """.trimMargin(),
                                            )
                                        }

                                        dispatchReceiver != null -> {
                                            println(
                                                """
                                    |${explicitReceiver.acceptString()}
                                    |        if (dispatchReceiver !== explicitReceiver) {
                                    |            ${dispatchReceiver.acceptString()}
                                    |        }
                                                """.trimMargin(),
                                            )
                                        }

                                        else -> {
                                            println(explicitReceiver.acceptString())
                                        }
                                    }
                                }

                                in setOf("dispatchReceiver", "extensionReceiver") if (walkableFields.any { it.name == "explicitReceiver" }) -> {}
                                "companionObject" -> {}

                                else -> {
                                    when (field) {
                                        is SimpleField -> println(field.acceptString())
                                        is ListField -> println(field.name, field.call(), "forEach { it.accept(visitor, data) }")
                                    }
                                }
                            }
                        }
                    }
                }
                println("}")
            }

            if (hasTransformChildrenMethod) {
                printTransformChildrenMethod(
                    implementation,
                    cfirTransformerType,
                    implementation,
                    modality = Modality.ABSTRACT.takeIf { isAbstract },
                    override = true,
                )
                if (!isInterface && !isAbstract) {
                    printBlock {
                        for (field in transformableChildren) {
                            when {
                                field.name == "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]
                                    val dispatchReceiver = implementation.getOrNull("dispatchReceiver")
                                    val extensionReceiver = implementation.getOrNull("extensionReceiver")
                                    if (explicitReceiver.isMutable) {
                                        println("explicitReceiver = explicitReceiver${explicitReceiver.call()}transform(transformer, data)")
                                    }
                                    if (dispatchReceiver?.isMutable == true) {
                                        println(
                                            """
                                    |if (dispatchReceiver !== explicitReceiver) {
                                    |            dispatchReceiver = dispatchReceiver?.transform(transformer, data)
                                    |        }
                                """.trimMargin(),
                                        )
                                    }
                                    if (extensionReceiver?.isMutable == true) {
                                        println(
                                            """
                                    |if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            extensionReceiver = extensionReceiver?.transform(transformer, data)
                                    |        }
                                """.trimMargin(),
                                        )
                                    }
                                }

                                field.name == "dispatchReceiver" && this.typeName != "CfirSuperReceiverExpressionImpl" -> {}
                                field.name == "extensionReceiver" -> {}

                                field.withTransform -> {
                                    if (!(element.needTransformOtherChildren && field.needTransformInOtherChildren)) {
                                        println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                                    }
                                }

                                !element.needTransformOtherChildren -> field.transform()
                                else -> {}
                            }
                        }

                        if (element.needTransformOtherChildren) {
                            println("transformOtherChildren(transformer, data)")
                        }
                        println("return this")
                    }
                }
            }

            for (field in allFields) {
                if (!field.withTransform) continue
                println()
                transformFunctionDeclaration(field, implementation, override = true, kind!!)
                if (isInterface || isAbstract) {
                    println()
                    continue
                }
                printBlock {
                    if (field.isMutable && field.containsElement) {
                        if (typeName == "CfirWhenExpressionImpl" && field.name == "subject") {
                            println(
                                """
                                |if (subjectVariable != null) {
                                |            subjectVariable = subjectVariable?.transform(transformer, data)
                                |            subject = subjectVariable?.initializer
                                |        } else {
                                |            subject = subject?.transform(transformer, data)
                                |        }
                                    """.trimMargin(),
                            )
                        } else {
                            field.transform()
                        }
                    }
                    println("return this")
                }
            }

            if (element.needTransformOtherChildren) {
                println()
                transformOtherChildrenFunctionDeclaration(implementation, override = true, kind!!)
                if (isInterface || isAbstract) {
                    println()
                } else {
                    printBlock {
                        for (field in allFields) {
                            if (!field.isMutable || !field.containsElement || field.name == "subjectVariable") continue
                            if (!field.withTransform) {
                                field.transform()
                            }
                            if (field.needTransformInOtherChildren) {
                                println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                            }
                        }
                        println("return this")
                    }
                }
            }

            fun generateReplace(
                field: Field,
                overridenType: TypeRefWithNullability? = null,
                forceNullable: Boolean = false,
                body: () -> Unit,
            ) {
                println()
                if (field.name == "source") {
                    println("@${cfirImplementationDetailType.render()}")
                }
                replaceFunctionDeclaration(field, override = true, kind!!, overridenType, forceNullable)
                if (isInterface || isAbstract) {
                    println()
                    return
                }
                print(" {")
                if (!field.isMutable) {
                    if (field.name == "coneTypeOrNull") {
                        println()
                        withIndent {
                            println("require(newConeTypeOrNull == coneTypeOrNull) { \"\${javaClass.simpleName}.replaceConeTypeOrNull() called with invalid type '\${newConeTypeOrNull}'. Current type is '\$coneTypeOrNull'\" }")
                        }
                    }
                    println("}")
                    return
                }
                println()
                withIndent {
                    body()
                }
                println("}")
            }

            for (field in allFields.filter { it.withReplace }) {
                val capitalizedFieldName = field.name.replaceFirstChar(Char::uppercaseChar)
                val newValue = "new$capitalizedFieldName"
                generateReplace(field, forceNullable = field.receiveNullableTypeInReplace) {
                    when {
                        field.implementationDefaultStrategy!!.withGetter -> {}
                        field is ListField && !field.isMutableOrEmptyList -> {
                            println("if (${field.name} === $newValue) return")
                            println("${field.name}.clear()")
                            println("${field.name}.addAll($newValue)")
                        }

                        else -> {
                            if (field.receiveNullableTypeInReplace && !field.typeRef.nullable) {
                                println("require($newValue != null)")
                            }
                            print("${field.name} = $newValue")
                            if (field is ListField && field.isMutableOrEmptyList) {
                                addImport(toMutableOrEmptyImport)
                                print(".toMutableOrEmpty()")
                            }
                            println()
                        }
                    }
                }

                val ownTypeKey = field.typeRef.copy(nullable = false).render()
                val additionalOverriddenTypes = field.overriddenFields
                    .map { it.typeRef.copy(nullable = false) }
                    .distinctBy { it.render() }
                    .filter { it.render() != ownTypeKey }

                for (overriddenType in additionalOverriddenTypes) {
                    generateReplace(field, overriddenType) {
                        println("require($newValue is ${field.typeRef.render()})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
    }
}

/**
 * 判断字段类型是否为 symbol 类型。
 */
private val Field.hasSymbolType: Boolean
    get() = (typeRef as? ClassRef<*>)?.simpleName?.contains("Symbol") ?: false
