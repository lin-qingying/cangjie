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
import org.cangnova.cangjie.generators.tree.AbstractFieldPrinter
import org.cangnova.cangjie.generators.tree.AbstractImplementationPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.printer.call
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printAnnotation
import org.cangnova.cangjie.generators.tree.printer.printAcceptChildrenMethod
import org.cangnova.cangjie.generators.tree.printer.printTransformChildrenMethod
import org.cangnova.cangjie.generators.util.printBlock

internal class ImplementationPrinter(printer: ImportCollectingPrinter) :
    AbstractImplementationPrinter<Implementation, Element, Field>(printer) {
    override val implementationOptInAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        pureAbstractElementType

    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> =
        object : AbstractFieldPrinter<Field>(printer) {
            override fun forceMutable(field: Field): Boolean = field.isMutable
        }

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        val kind = implementation.kind ?: error("Expected non-null implementation kind")
        val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
        val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

        if (implementation.hasAcceptChildrenMethod) {
            printAcceptChildrenMethod(implementation, cfirVisitorType, TypeVariable("R"), override = true)
            if (isInterface || isAbstract) {
                println()
            } else {
                printBlock {
                    for (field in implementation.walkableChildren) {
                        when (field) {
                            is SimpleField -> println("${field.name}${field.call()}accept(visitor, data)")
                            is ListField -> println("${field.name}${field.call()}forEach { it.accept(visitor, data) }")
                            else -> {}
                        }
                    }
                }
            }
        }

        val replaceableFields = implementation.allFields.filter { it.withReplace }
        for (field in replaceableFields) {
            println()
            field.replaceOptInAnnotation?.let { println("@OptIn(${it.render()}::class)") }
            field.deprecation?.let { printAnnotation(it) }
            val replaceSignature =
                "override fun replace${field.name.replaceFirstChar(Char::uppercaseChar)}(new${field.name.replaceFirstChar(Char::uppercaseChar)}: ${field.typeRef.render()})"
            if (isInterface || isAbstract) {
                if (isAbstract) {
                    println("abstract $replaceSignature")
                } else {
                    println(replaceSignature)
                }
                println()
            } else {
                println(replaceSignature)
                printBlock {
                    println("this.${field.name} = new${field.name.replaceFirstChar(Char::uppercaseChar)}")
                }
            }
        }

        val transformableFields = implementation.allFields.filter { it.withTransform }
        for (field in transformableFields) {
            println()
            field.deprecation?.let { printAnnotation(it) }
            val transformSignature =
                "override fun <D> transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer: ${cfirTransformerType.render()}<D>, data: D): ${implementation.element.typeName}"
            if (isInterface || isAbstract) {
                if (isAbstract) {
                    println("abstract $transformSignature")
                } else {
                    println(transformSignature)
                }
                println()
            } else {
                println(transformSignature)
                printBlock {
                    when (field) {
                        is SimpleField -> {
                            val transformed = if (field.nullable) {
                                "${field.name}?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)"
                            } else {
                                "${field.name}.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)"
                            }
                            println("this.${field.name} = $transformed as ${field.typeRef.render()}")
                        }

                        is ListField -> {
                            val baseType = field.baseType.render()
                            println("this.${field.name} = ${field.name}.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as $baseType }")
                        }
                    }
                    println("return this")
                }
            }
        }

        if (implementation.hasTransformChildrenMethod) {
            printTransformChildrenMethod(implementation, cfirTransformerType, implementation, override = true)
            if (isInterface || isAbstract) {
                println()
            } else {
                printBlock {
                    for (field in implementation.transformableChildren) {
                        if (field.withTransform) {
                            println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                        } else {
                            when (field) {
                                is SimpleField -> println("${field.name}${field.call()}transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)")
                                is ListField -> println("${field.name}${field.call()}forEach { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) }")
                                else -> {}
                            }
                        }
                    }
                    println("return this")
                }
            }
        }
    }
}
