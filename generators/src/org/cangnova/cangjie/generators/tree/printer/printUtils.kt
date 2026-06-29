/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.printer

import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.util.printBlock
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly
import org.cangnova.cangjie.utils.IndentingPrinter
import org.cangnova.cangjie.generators.tree.joinToWithBuffer
import org.cangnova.cangjie.utils.withIndent
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * 同时支持导入收集和缩进输出的源码打印器接口。
 */
interface ImportCollectingPrinter : ImportCollecting, IndentingPrinter

/**
 * 打印类型声明后的继承子句。
 *
 * 类类型会附加父类构造参数，接口类型只输出类型引用。
 */
fun ImportCollectingPrinter.printInheritanceClause(
    supertypes: List<ClassOrElementRef>,
    superclassConstructorArgs: List<String> = emptyList(),
) {
    if (supertypes.isEmpty()) return
    print(
        buildString {
            supertypes.sortedBy { it.typeKind }.joinToWithBuffer(this, prefix = " : ") { supertype ->
                append(supertype.render())
                if (supertype.typeKind == TypeKind.Class) {
                    append("(")
                    superclassConstructorArgs.joinTo(this)
                    append(")")
                }
            }
        }
    )
}

/**
 * 按 Kotlin KDoc 格式打印多行文档注释。
 */
fun IndentingPrinter.printKDoc(kDoc: String?) {
    if (kDoc == null) return
    println("/**")
    for (line in kDoc.lineSequence()) {
        print(" *")
        if (line.isBlank()) {
            println()
        } else {
            print(" ")
            println(line)
        }
    }
    println(" */")
}

/**
 * 生成元素默认 KDoc。
 *
 * 会把元素显式 KDoc 与生成来源属性名组合到同一段文档中。
 */
fun AbstractElement<*, *, *>.extendedKDoc(): String = buildString {
    val doc = kDoc
    if (doc != null) {
        appendLine(doc)
        appendLine()
    }
    append("Generated from: [${element.propertyName}]")
}

/**
 * 待打印函数参数的模型。
 */
data class FunctionParameter(
    /**
     * 参数名称。
     */
    val name: String,
    /**
     * 参数类型。
     */
    val type: TypeRef,
    /**
     * 参数默认值表达式。
     */
    val defaultValue: String? = null,
    /**
     * 是否在参数前添加未使用参数抑制注解。
     */
    val markAsUnused: Boolean = false,
) {

    /**
     * 将参数渲染为 Kotlin 源码片段。
     */
    fun render(importCollector: ImportCollecting): String = buildString {
        if (markAsUnused) {
            append("@Suppress(\"UNUSED_PARAMETER\") ")
        }
        append(name, ": ")
        type.renderTo(this, importCollector)
        defaultValue?.let {
            append(" = ", it)
        }
    }
}

/**
 * 打印函数声明，不包含函数体。
 */
fun ImportCollectingPrinter.printFunctionDeclaration(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    extensionReceiver: TypeRef? = null,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isInline: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
    optInAnnotation: ClassRef<*>? = null,
    deprecation: Deprecated? = null,
) {
    optInAnnotation?.let {
        println("@", it.render())
    }

    deprecation?.let {
        printAnnotation(it)
    }

    if (visibility != Visibility.PUBLIC) {
        print(visibility.name.toLowerCaseAsciiOnly(), " ")
    }
    when (modality) {
        null -> {}
        Modality.FINAL -> print("final ")
        Modality.OPEN -> print("open ")
        Modality.ABSTRACT -> print("abstract ")
        Modality.SEALED -> error("Function cannot be sealed")
    }
    if (override) {
        print("override ")
    }
    if (isInline) {
        print("inline ")
    }
    print("fun ")
    print(typeParameters.typeParameters(end = " "))
    if (extensionReceiver != null) {
        print(extensionReceiver.render(), ".")
    }
    print(name, "(")

    if (allParametersOnSeparateLines) {
        if (parameters.isNotEmpty()) {
            println()
            withIndent {
                for (parameter in parameters) {
                    print(parameter.render(this))
                    println(",")
                }
            }
        }
    } else {
        print(parameters.joinToString { it.render(this) })
    }
    print(")")
    if (returnType != StandardTypes.unit) {
        print(": ", returnType.render())
    }
    print(typeParameters.multipleUpperBoundsList())
}

/**
 * 打印带块体的函数声明。
 */
inline fun ImportCollectingPrinter.printFunctionWithBlockBody(
    name: String,
    parameters: List<FunctionParameter>,
    returnType: TypeRef,
    typeParameters: List<TypeVariable> = emptyList(),
    extensionReceiver: TypeRef? = null,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isInline: Boolean = false,
    allParametersOnSeparateLines: Boolean = false,
    deprecation: Deprecated? = null,
    blockBody: () -> Unit,
) {
    printFunctionDeclaration(
        name,
        parameters,
        returnType,
        typeParameters,
        extensionReceiver,
        visibility,
        modality,
        override,
        isInline,
        allParametersOnSeparateLines,
        deprecation = deprecation,
    )
    printBlock(body = blockBody)
}

/**
 * 主构造函数参数模型。
 */
data class PrimaryConstructorParameter(
    /**
     * 底层函数参数定义。
     */
    val functionParameter: FunctionParameter,
    /**
     * 参数在主构造函数中的属性形态。
     */
    val kind: VariableKind,
    /**
     * 参数生成属性的可见性。
     */
    val visibility: Visibility = Visibility.PUBLIC,
) {
    /**
     * 参数名称快捷访问。
     */
    val name by functionParameter::name

    /**
     * 参数类型快捷访问。
     */
    val type by functionParameter::type

    /**
     * 参数默认值快捷访问。
     */
    val defaultValue by functionParameter::defaultValue
}

/**
 * 将字符串转义为 Kotlin 字符串字面量。
 */
private fun String.asStringLiteral(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

/**
 * 打印注解参数值。
 */
private fun ImportCollectingPrinter.printAnnotationArgument(argument: Any?) {
    when (argument) {
        is String -> print(argument.asStringLiteral())
        is Enum<*> -> print(argument::class.asRef<PositionTypeParameterRef>().render(), ".", argument.name)
        is Array<*> -> {
            print("[")
            for ((i, element) in argument.withIndex()) {
                printAnnotationArgument(element)
                if (i != argument.lastIndex) {
                    print(", ")
                }
            }
            print("]")
        }
        else -> print(argument)
    }
}

/**
 * 根据运行时注解实例打印对应的 Kotlin 注解调用。
 */
fun <A : Annotation> ImportCollectingPrinter.printAnnotation(annotation: A) {
    @Suppress("UNCHECKED_CAST")
    val annotationInterface = annotation::class.java.interfaces.single().kotlin as KClass<Annotation>
    print("@", annotationInterface.asRef<PositionTypeParameterRef>().render())
    val properties = annotationInterface.memberProperties
    if (properties.isNotEmpty()) {
        println("(")
        withIndent {
            for (property in properties) {
                print(property.name, " = ")
                printAnnotationArgument(property.get(annotation))
                println(",")
            }
        }
        println(")")
    } else {
        println()
    }
}

/**
 * 打印属性声明或主构造函数参数声明。
 */
fun ImportCollectingPrinter.printPropertyDeclaration(
    name: String,
    type: TypeRef,
    kind: VariableKind,
    inConstructor: Boolean = false,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    override: Boolean = false,
    isLateinit: Boolean = false,
    isVolatile: Boolean = false,
    kDoc: String? = null,
    optInAnnotation: ClassRef<*>? = null,
    printOptInWrapped: Boolean = false,
    deprecation: Deprecated? = null,
    initializer: String? = null,
    additionalAnnotations: List<ClassRef<*>> = emptyList(),
) {
    printKDoc(kDoc)

    deprecation?.let {
        printAnnotation(it)
    }

    if (isVolatile) {
        println("@kotlin.concurrent.Volatile")
    }

    // 打印额外的自定义注解
    for (annotation in additionalAnnotations) {
        val rendered = annotation.render()
        if (inConstructor) {
            println("@property:", rendered)
        } else {
            println("@", rendered)
        }
    }

    optInAnnotation?.let {
        val rendered = it.render()
        when {
            printOptInWrapped -> println("@OptIn(", rendered, "::class)")
            inConstructor -> println("@property:", rendered)
            else -> println("@", rendered)
        }
    }

    if (visibility != Visibility.PUBLIC) {
        print(visibility.name.toLowerCaseAsciiOnly(), " ")
    }

    modality?.let {
        print(it.name.toLowerCaseAsciiOnly(), " ")
    }

    if (override) {
        print("override ")
    }
    if (isLateinit) {
        print("lateinit ")
    }
    when (kind) {
        VariableKind.PARAMETER -> {}
        VariableKind.VAL -> print("val ")
        VariableKind.VAR -> print("var ")
    }
    print(name, ": ", type.render())

    if (initializer != null) {
        print(" = $initializer")
    }

    if (inConstructor) {
        print(",")
    }
}

/**
 * 变量声明种类。
 */
enum class VariableKind { VAL, VAR, PARAMETER }

/**
 * Visitor/Transformer data 类型参数。
 */
private val dataTP = TypeVariable("D")

/**
 * Visitor/Transformer data 函数参数。
 */
private val dataParameter = FunctionParameter("data", dataTP)

/**
 * 生成 `accept` 方法的 KDoc 文本。
 */
private fun acceptMethodKDoc(
    visitorParameter: FunctionParameter,
    dataParameter: FunctionParameter?,
    returnType: TypeRef,
    treeName: String,
) = buildString {
    append("Runs the provided [")
    append(visitorParameter.name)
    append("] on the ")
    append(treeName)
    append(" subtree with the root at this node.\n\n")
    append("@param ")
    append(visitorParameter.name)
    append(" The visitor to accept.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(visitorParameter.name)
        append("]'s methods.")
    }
    if (returnType != StandardTypes.unit) {
        append("\n@return The value returned by the topmost `visit*` invocation.")
    }
}

/**
 * 打印元素上的 `accept` 方法。
 */
fun ImportCollectingPrinter.printAcceptMethod(
    element: AbstractElement<*, *, *>,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    hasImplementation: Boolean,
    treeName: String,
) {
    if (!element.hasAcceptMethod) return
    println()
    val resultTP = TypeVariable("R")
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(resultTP, dataTP))
    if (element.isRootElement) {
        printKDoc(acceptMethodKDoc(visitorParameter, dataParameter, resultTP, treeName))
    }
    printFunctionDeclaration(
        name = "accept",
        parameters = listOf(visitorParameter, dataParameter),
        returnType = resultTP,
        typeParameters = listOf(resultTP, dataTP),
        override = !element.isRootElement,
    )
    if (hasImplementation) {
        println(" =")
        withIndent {
            print(visitorParameter.name, ".", element.visitFunctionName, "(this, ", dataParameter.name, ")")
        }
    }
    println()
}

/**
 * 生成 `transform` 方法的 KDoc 文本。
 */
private fun transformMethodKDoc(
    transformerParameter: FunctionParameter,
    dataParameter: FunctionParameter?,
    treeName: String,
) = buildString {
    append("Runs the provided [")
    append(transformerParameter.name)
    append("] on the $treeName subtree with the root at this node.\n\n")
    append("@param ")
    append(transformerParameter.name)
    append(" The transformer to use.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(transformerParameter.name)
        append("]'s methods.")
    }
    append("\n@return The transformed node.")
}

/**
 * 打印元素上的 `transform` 方法。
 */
fun ImportCollectingPrinter.printTransformMethod(
    element: AbstractElement<*, *, *>,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    implementation: String?,
    returnType: TypeRefWithNullability,
    treeName: String,
) {
    if (!element.hasTransformMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (element.isRootElement) {
        printKDoc(transformMethodKDoc(transformerParameter, dataParameter, treeName))
    }
    if (returnType is TypeParameterRef && implementation != null) {
        println("@Suppress(\"UNCHECKED_CAST\")")
    }
    printFunctionDeclaration(
        name = "transform",
        parameters = listOf(transformerParameter, dataParameter),
        returnType = returnType,
        typeParameters = listOfNotNull(returnType as? TypeVariable, dataTP),
        override = !element.isRootElement,
    )
    if (implementation != null) {
        println(" =")
        withIndent {
            print(implementation, " as ", returnType.render())
        }
    }
    println()
}

/**
 * 生成 `acceptChildren` 方法的 KDoc 文本。
 */
private fun acceptChildrenKDoc(visitorParameter: FunctionParameter, dataParameter: FunctionParameter?) = buildString {
    append("Runs the provided [")
    append(visitorParameter.name)
    append("] on subtrees with roots in this node's children.\n\n")
    append("Basically, calls `accept(")
    append(visitorParameter.name)
    if (dataParameter != null) {
        append(", ")
        append(dataParameter.name)
    }
    append(")` on each child of this node.\n\n")
    append("Does **not** run [")
    append(visitorParameter.name)
    append("] on this node itself.\n\n")
    append("@param ")
    append(visitorParameter.name)
    append(" The visitor for children to accept.")
    if (dataParameter != null) {
        append("\n@param ")
        append(dataParameter.name)
        append(" An arbitrary context to pass to each invocation of [")
        append(visitorParameter.name)
        append("]'s methods.")
    }
}

/**
 * 打印递归访问子节点的 `acceptChildren` 方法声明。
 */
fun ImportCollectingPrinter.printAcceptChildrenMethod(
    element: FieldContainer<*>,
    visitorClass: ClassRef<PositionTypeParameterRef>,
    visitorResultType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasAcceptChildrenMethod) return
    println()
    val visitorParameter = FunctionParameter("visitor", visitorClass.withArgs(visitorResultType, dataTP))
    if (!override) {
        printKDoc(acceptChildrenKDoc(visitorParameter, dataParameter))
    }
    printFunctionDeclaration(
        name = "acceptChildren",
        parameters = listOf(visitorParameter, dataParameter),
        returnType = StandardTypes.unit,
        typeParameters = listOfNotNull(visitorResultType as? TypeVariable, dataTP),
        modality = modality,
        override = override,
    )
}

/**
 * 生成 `transformChildren` 方法的 KDoc 文本。
 */
private fun transformChildrenMethodKDoc(transformerParameter: FunctionParameter, dataParameter: FunctionParameter?, returnType: TypeRef) =
    buildString {
        append("Recursively transforms this node's children *in place* using [")
        append(transformerParameter.name)
        append("].\n\n")
        append("Basically, executes `this.child = this.child.transform(")
        append(transformerParameter.name)
        if (dataParameter != null) {
            append(", ")
            append(dataParameter.name)
        }
        append(")` for each child of this node.\n\n")
        append("Does **not** run [")
        append(transformerParameter.name)
        append("] on this node itself.\n\n")
        append("@param ")
        append(transformerParameter.name)
        append(" The transformer to use for transforming the children.")
        if (dataParameter != null) {
            append("\n@param ")
            append(dataParameter.name)
            append(" An arbitrary context to pass to each invocation of [")
            append(transformerParameter.name)
            append("]'s methods.")
        }
        if (returnType != StandardTypes.unit) {
            append("\n@return `this`")
        }
    }

/**
 * 打印递归转换子节点的 `transformChildren` 方法声明。
 */
fun ImportCollectingPrinter.printTransformChildrenMethod(
    element: FieldContainer<*>,
    transformerClass: ClassRef<PositionTypeParameterRef>,
    returnType: TypeRef,
    modality: Modality? = null,
    override: Boolean = false,
) {
    if (!element.hasTransformChildrenMethod) return
    println()
    val transformerParameter = FunctionParameter("transformer", transformerClass.withArgs(dataTP))
    if (!override) {
        printKDoc(transformChildrenMethodKDoc(transformerParameter, dataParameter, returnType))
    }
    printFunctionDeclaration(
        name = "transformChildren",
        parameters = listOf(transformerParameter, dataParameter),
        returnType = returnType,
        typeParameters = listOf(dataTP),
        modality = modality,
        override = override,
    )
}

/**
 * 打印无 data 参数的 `accept` 便捷方法。
 */
fun ImportCollectingPrinter.printAcceptVoidMethod(visitorType: ClassRef<*>, treeName: String) {
    val visitorParameter = FunctionParameter("visitor", visitorType)
    val returnType = StandardTypes.unit
    printKDoc(acceptMethodKDoc(visitorParameter, null, returnType, treeName))
    printFunctionDeclaration("accept", listOf(visitorParameter), returnType)
    printBlock {
        println("accept(", visitorParameter.name, ", null)")
    }
}

/**
 * 打印无 data 参数的 `acceptChildren` 便捷方法。
 */
fun ImportCollectingPrinter.printAcceptChildrenVoidMethod(visitorType: ClassRef<*>) {
    val visitorParameter = FunctionParameter("visitor", visitorType)
    printKDoc(acceptChildrenKDoc(visitorParameter, null))
    printFunctionDeclaration("acceptChildren", listOf(visitorParameter), StandardTypes.unit)
    printBlock {
        println("acceptChildren(", visitorParameter.name, ", null)")
    }
}

/**
 * 打印无 data 参数的根元素 `transform` 便捷方法。
 */
fun ImportCollectingPrinter.printTransformVoidMethod(element: AbstractElement<*, *, *>, transformerType: ClassRef<*>, treeName: String) {
    assert(element.isRootElement) { "Expected root element" }
    val transformerParameter = FunctionParameter("transformer", transformerType)
    val elementTP = TypeVariable("E", listOf(element))
    printKDoc(transformMethodKDoc(transformerParameter, null, treeName))
    printFunctionDeclaration(
        name = "transform",
        parameters = listOf(transformerParameter),
        returnType = elementTP,
        typeParameters = listOf(elementTP)
    )
    println(" =")
    withIndent {
        println("transform(", transformerParameter.name, ", null)")
    }
}

/**
 * 打印无 data 参数的根元素 `transformChildren` 便捷方法。
 */
fun ImportCollectingPrinter.printTransformChildrenVoidMethod(element: AbstractElement<*, *, *>, visitorType: ClassRef<*>, returnType: TypeRef) {
    assert(element.isRootElement) { "Expected root element" }
    val transformerParameter = FunctionParameter("transformer", visitorType)
    printKDoc(transformChildrenMethodKDoc(transformerParameter, null, returnType))
    printFunctionDeclaration("transformChildren", listOf(transformerParameter), returnType)
    println(" =")
    withIndent {
        println("transformChildren(", transformerParameter.name, ", null)")
    }
}

/**
 * 根据字段可空性返回安全调用或普通调用操作符。
 */
fun AbstractField<*>.call(): String = if (nullable) "?." else "."
