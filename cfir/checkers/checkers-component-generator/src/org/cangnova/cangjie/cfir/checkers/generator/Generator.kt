/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangnova.cangjie.generators.util.getGenerationPath
import org.cangnova.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.generators.util.printGeneratedMessage
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import java.io.File
import kotlin.reflect.KClass

/**
 * checker 生成器内部使用的别名文本。
 */
internal typealias Alias = String
/**
 * 全限定名文本。
 */
private typealias Fqn = String
/**
 * 已注册 checker 配置项。
 */
private typealias Checker = Map.Entry<KClass<*>, Pair<String, Boolean>>

/**
 * 生成代码中用于隐藏内部 checker 组件 API 的注解短名。
 */
private const val CHECKERS_COMPONENT_INTERNAL = "CheckersComponentInternal"
/**
 * 生成代码中可直接打印的内部注解表达式。
 */
private const val CHECKERS_COMPONENT_INTERNAL_ANNOTATION = "@$CHECKERS_COMPONENT_INTERNAL"
/**
 * 生成代码需要导入的内部注解全限定名。
 */
private const val CHECKERS_COMPONENT_INTERNAL_FQN = "org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal"

// DiagnosticComponent
/**
 * 生成诊断组件时使用的 CFIR session 全限定名。
 */
private const val FIR_SESSION_FQN = "org.cangnova.cangjie.cfir.session.CfirSession"
/**
 * 生成诊断组件时使用的 pending reporter 全限定名。
 */
private const val DIAGNOSTIC_REPORTER_FQN = "org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter"
/**
 * 生成诊断组件基类的全限定名。
 */
private const val ABSTRACT_DIAGNOSTIC_REPORTER_FQN =
    "org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent"
/**
 * 生成 checker 调用上下文使用的全限定名。
 */
private const val CHECKER_CONTEXT_FQN = "org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext"
/**
 * CFIR 根包全限定名。
 */
private const val FIR_FQN = "org.cangnova.cangjie.cfir"
/**
 * session checker 组件访问器全限定名。
 */
private const val CHECKERS_COMPONENT_FQN = "org.cangnova.cangjie.cfir.analysis.checkersComponent"
/**
 * CFIR 元素基类全限定名。
 */
private const val FIR_ELEMENT_FQN = "org.cangnova.cangjie.cfir.CfirElement"
/**
 * 异常附件工具的全限定名。
 */
private const val WITH_ENTRY_FQN = "org.cangnova.cangjie.utils.exceptions.withCfirEntry"
/**
 * 带上下文重抛异常工具的全限定名。
 */
private const val RETHROW_FQN = "org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails"
/**
 * IntelliJ 平台异常过滤工具的全限定名。
 */
private const val SHOULD_RETHROW_FQN = "org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown"

/**
 * checker 组件代码生成器。
 */
class Generator(
    /**
     * checker 组件生成配置。
     */
    private val configuration: CheckersConfiguration,
    generationPath: File,
    /**
     * 生成文件所在包名。
     */
    private val packageName: String,
    /**
     * 抽象 checker 基类短名。
     */
    private val abstractCheckerName: String,
    /**
     * 生成 check 扩展方法的类型参数上界。
     */
    private val checkMethodTypeParameterConstraint: KClass<out CfirElement>,
    /**
     * 顶层 visitElement 中用于防止漏调父 checker 的 CFIR 类型。
     */
    private val checkType: KClass<out CfirElement>,
) {
    /**
     * 解析包名后的实际生成目录。
     */
    private val generationPath: File = getGenerationPath(generationPath, packageName)

    /**
     * 生成 checker 别名文件。
     */
    private fun generateAliases() {
        val filename = "${abstractCheckerName}Aliases.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printGeneratedMessage()
            configuration.aliases.keys
                .mapNotNull { it.qualifiedName }
                .sorted()
                .forEach { println("import $it") }
            println()
            for ((kClass, alias) in configuration.aliases) {
                val typeParameters =
                    if (kClass.typeParameters.isEmpty()) ""
                    else kClass.typeParameters.joinToString(separator = ",", prefix = "<", postfix = ">") { "*" }
                println("typealias ${alias.component1()} = $abstractCheckerName<${kClass.simpleName}$typeParameters>")
            }
        }
    }

    /**
     * 生成抽象 checker 集合组件。
     */
    private fun generateAbstractCheckersComponent() {
        val filename = "${checkersComponentName}.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports()
            printGeneratedMessage()

            println("@Suppress(\"UNCHECKED_CAST\")")
            println("abstract class $checkersComponentName {")
            withIndent {
                println("companion object {")
                withIndent {
                    println("val EMPTY: $checkersComponentName = object : $checkersComponentName() {}")
                }
                println("}")
                println()

                for ((alias, _) in configuration.aliases.values) {
                    println("open ${alias.valDeclaration} = emptySet()")
                }
                println()

                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    val fieldClassName = classFqn.simpleName
                    println("open val $fieldName: ${fieldClassName.setType} = emptySet()")
                }
                if (configuration.additionalCheckers.isNotEmpty()) {
                    println()
                }

                for ((kClass, alias) in configuration.aliases) {
                    print("$CHECKERS_COMPONENT_INTERNAL_ANNOTATION internal val ${alias.component1().allFieldName}: ${alias.component1().arrayType} by lazy { ")
                    val parents = configuration.parentsMap.getValue(kClass)
                    if (parents.isNotEmpty()) {
                        print('(')
                    }
                    print(alias.component1().fieldName)
                    for (parent in parents) {
                        val parentAlias = configuration.aliases.getValue(parent)
                        print(" + ${parentAlias.component1().fieldName}")
                    }
                    if (parents.isNotEmpty()) {
                        print(')')
                    }
                    print(".toTypedArray()")
                    if (parents.isNotEmpty()) {
                        // Checker base classes are declared as invariant.
                        // However, they only accept their generic type as input, so it is safe
                        // to cast `CfirChecker<Base>` to `CfirChecker<Specific>`
                        // because calling `fun check(Base)` with an argument of type `Specific` is safe.
                        // The cast wouldn't be necessary if the checker base classes were declared as `class CfirChecker<in T>'.
                        // However, we specifically don't want to do that to prevent us from accidentally adding generic checkers to
                        // sets of more specific checkers in the `*Checkers` implementations.
                        print(" as ${alias.component1().arrayType}")
                    }
                    println(" }")
                }
            }
            println("}")
        }
    }

    /**
     * 生成可组合的 checker 集合组件。
     */
    private fun generateComposedComponent() {
        val composedComponentName = "Composed$checkersComponentName"
        val filename = "${composedComponentName}.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports()
            printGeneratedMessage()
            println("class $composedComponentName : $checkersComponentName() {")
            withIndent {
                // public overrides
                for ((alias, _) in configuration.aliases.values) {
                    println("override ${alias.valDeclaration}")
                    withIndent {
                        println("get() = _${alias.fieldName}")
                    }
                }
                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    println("override val $fieldName: ${classFqn.simpleName.setType}")
                    withIndent {
                        println("get() = _$fieldName")
                    }
                }
                println()

                // private mutable delegates
                for ((alias, _) in configuration.aliases.values) {
                    println("private val _${alias.fieldName}: ${alias.mutableSetType} = mutableSetOf()")
                }
                for ((fieldName, classFqn) in configuration.additionalCheckers) {
                    println("private val _$fieldName: ${classFqn.simpleName.mutableSetType} = mutableSetOf()")
                }
                println()

                // register function
                println(CHECKERS_COMPONENT_INTERNAL_ANNOTATION)
                println("fun register(checkers: $checkersComponentName) {")
                withIndent {
                    for ((alias, _) in configuration.aliases.values) {
                        println("_${alias.fieldName}.addAll(checkers.${alias.fieldName})")
                    }
                    for (fieldName in configuration.additionalCheckers.keys) {
                        println("_$fieldName.addAll(checkers.$fieldName)")
                    }
                }
                println("}")
            }
            println("}")
        }
    }

    /**
     * 生成将 visitor 分派转发到 checker 集合的诊断组件。
     */
    private fun generateDiagnosticComponent() {
        val diagnosticComponentName = "${checkersComponentName}DiagnosticComponent"
        val filename = "$diagnosticComponentName.kt"
        generationPath.resolve(filename).writeToFileUsingSmartPrinterIfFileContentChanged {
            printPackageAndCopyright()
            printImports(
                false,
                FIR_SESSION_FQN,
                DIAGNOSTIC_REPORTER_FQN,
                ABSTRACT_DIAGNOSTIC_REPORTER_FQN,
                CHECKER_CONTEXT_FQN,
                "$FIR_FQN.$checkersPackageName.*",
                CHECKERS_COMPONENT_FQN,
                FIR_ELEMENT_FQN,
                SHOULD_RETHROW_FQN,
                WITH_ENTRY_FQN,
                RETHROW_FQN
            )
            printGeneratedMessage()
            println("@OptIn($CHECKERS_COMPONENT_INTERNAL::class)")
            println("class $diagnosticComponentName(")
            withIndent {
                println("session: CfirSession,")
                println("reporter: PendingDiagnosticReporter,")
                println("private val checkers: $checkersComponentName,")
            }
            println(") : AbstractDiagnosticCollectorComponent(session, reporter) {")

            withIndent {
                printDiagnosticComponentConstructor()
                println()
                printDiagnosticComponentVisitElementMethod()
                println()
                for ((checker, value) in configuration.aliases) {
                    if (value.component2()) {
                        printDiagnosticComponentVisitMethod(checker, value.component1())
                        println()
                    }
                }

                for ((checker, value) in configuration.visitAlso) {
                    printDiagnosticComponentVisitMethod(checker, value)
                    println()
                }

                printDiagnosticComponentCheckMethod()
            }

            println("}")
        }
    }

    /**
     * 打印生成文件的版权头与包声明。
     */
    private fun SmartPrinter.printPackageAndCopyright() {
        printCopyright()
        println("package $packageName")
        println()
    }

    /**
     * 打印生成文件需要的 import 列表。
     */
    private fun SmartPrinter.printImports(includeAdditionalCheckers: Boolean = true, vararg additionalImports: String) {
        val imports = buildList {
            if (includeAdditionalCheckers) {
                addAll(configuration.additionalCheckers.values)
            }
            add(CHECKERS_COMPONENT_INTERNAL_FQN)
            addAll(additionalImports)
        }.sorted()

        for (fqn in imports) {
            println("import $fqn")
        }
        println()
    }

    /**
     * 打印单个 CFIR 元素 visit 方法。
     */
    private fun SmartPrinter.printDiagnosticComponentVisitMethod(checker: KClass<*>, alias: Alias) {
        val elementParamName = when{
            checker.elementParamName  == "class" -> "klass"
            checker.elementParamName  == "interface" -> "`interface`"

            else -> checker.elementParamName
        }



        println("override fun visit${checker.elementName}($elementParamName: ${checker.elementTypeName}, data: CheckerContext) {")
        withIndent {
            println("checkers.${alias.allFieldName}.check($elementParamName, data)")
        }
        println("}")
    }

    /**
     * 打印诊断组件从 session 读取 checker 集合的辅助构造器。
     */
    private fun SmartPrinter.printDiagnosticComponentConstructor() {
        println("constructor(session: CfirSession, reporter: PendingDiagnosticReporter) : this(")
        withIndent {
            println("session,")
            println("reporter,")
            println("session.checkersComponent.${checkersComponentName.replaceFirstChar(Char::lowercaseChar)}")
        }
        println(")")
    }

    /**
     * 打印兜底 visitElement，用于发现具体节点漏调父 checker 的错误。
     */
    private fun SmartPrinter.printDiagnosticComponentVisitElementMethod() {
        println("override fun visitElement(element: CfirElement, data: CheckerContext) {")
        withIndent {
            println("if (element is ${checkType.simpleName}) {")
            withIndent {
                println("error(\"\${element::class.simpleName} should call parent checkers inside \${this::class.simpleName}\")")
            }
            println("}")
        }
        println("}")
    }

    /**
     * 打印 checker 数组执行入口。
     */
    private fun SmartPrinter.printDiagnosticComponentCheckMethod() {
        println("private inline fun <reified E : ${checkMethodTypeParameterConstraint.simpleName}> Array<$abstractCheckerName<E>>.check(")
        withIndent {
            println("element: E,")
            println("context: CheckerContext")
        }
        println(") {")
        withIndent {
            println("for (checker in this) {")
            withIndent {
                println("try {")
                withIndent {
                    println("context(context, reporter) {")
                    withIndent {
                        println("checker.check(element)")
                    }
                    println("}")
                }
                println("} catch (e: Exception) {")
                withIndent {
                    println("if (shouldIjPlatformExceptionBeRethrown(e)) throw e")
                    println("rethrowExceptionWithDetails(\"Exception in $checkersTypeInErrorMsg checkers\", e) {")
                    withIndent {
                        println("withCfirEntry(\"element\", element)")
                        println("context.containingFilePath?.let { withEntry(\"file\", it) }")
                    }
                    println("}")
                }
                println("}")
            }
            println("}")
        }
        println("}")
    }

    /**
     * CFIR 元素类型短名。
     */
    private val KClass<*>.elementTypeName: String
        get() = simpleName!!

    /**
     * 由元素类型名推导出的 visitor 参数名。
     */
    private val KClass<*>.elementParamName: String
        get() = elementName.replaceFirstChar(Char::lowercaseChar)

    /**
     * 去掉 Cfir 前缀后的元素名称。
     */
    private val KClass<*>.elementName: String
        get() = elementTypeName.removePrefix("Cfir")

    /**
     * checker 集合属性声明文本。
     */
    private val Alias.valDeclaration: String
        get() = "val $fieldName: $setType"

    /**
     * checker 集合字段名。
     */
    private val Alias.fieldName: String
        get() = removePrefix("Cfir").replaceFirstChar(Char::lowercaseChar) + "s"

    /**
     * 包含当前 checker 及父类型 checker 的聚合字段名。
     */
    private val Alias.allFieldName: String
        get() = "all${fieldName.replaceFirstChar(Char::uppercaseChar)}"

    /**
     * checker 不可变集合类型文本。
     */
    private val Alias.setType: String
        get() = "Set<$this>"

    /**
     * checker 可变集合类型文本。
     */
    private val Alias.mutableSetType: String
        get() = "MutableSet<$this>"

    /**
     * checker 数组类型文本。
     */
    private val Alias.arrayType: String
        get() = "Array<$this>"

    /**
     * 从全限定名中提取短名。
     */
    private val Fqn.simpleName: String
        get() = this.split(".").last()

    /**
     * 生成的 checker 组件短名。
     */
    private val checkersComponentName = abstractCheckerName.removePrefix("Cfir") + "s"

    /**
     * 生成代码中 checker 所在包的子包名。
     */
    private val checkersPackageName = abstractCheckerName
        .removePrefix("Cfir")
        .removeSuffix("Checker")
        .lowercase() + "s"

    /**
     * 异常消息中使用的 checker 类型描述。
     */
    private val checkersTypeInErrorMsg = abstractCheckerName.removePrefix("Cfir").removeSuffix("Checker").lowercase()

    /**
     * 执行全部 checker 组件文件生成。
     */
    fun generate() {
        generateAliases()
        generateAbstractCheckersComponent()
        generateComposedComponent()
        generateDiagnosticComponent()
    }
}

