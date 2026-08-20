/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.utils.Printer

// ─────────────────────────────────────────────────────────────────────────────
// Printer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CFIR 渲染使用的轻量 printer。
 *
 * @property builder 承载输出文本的 StringBuilder。
 */
open class CfirPrinter(private val builder: StringBuilder = StringBuilder()) {
    /**
     * 底层带缩进 printer。
     */
    private var printer = Printer(builder)

    /**
     * 当前输出位置是否在行首。
     */
    private var lineBeginning = true

    /**
     * 输出对象并遵循当前行首缩进规则。
     */
    fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    /**
     * 输出对象并换行。
     */
    fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    /**
     * 增加缩进层级。
     */
    internal fun pushIndent() = printer.pushIndent()

    /**
     * 降低缩进层级。
     */
    internal fun popIndent() = printer.popIndent()

    /**
     * 输出空行。
     */
    fun newLine() = println()

    /**
     * 重置单次渲染的文本和缩进状态。
     *
     * `CfirRenderer.renderElementAsString()` 是“一个元素 -> 一个字符串”的公共入口，
     * 因此复用 renderer 时不能继承上一次渲染的 builder 内容或缩进状态。
     */
    fun reset() {
        builder.setLength(0)
        printer = Printer(builder)
        lineBeginning = true
    }

    /**
     * 在花括号块中渲染内容。
     */
    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", block: () -> Unit) {
        println(" ", leftBrace)
        pushIndent()
        block()
        popIndent()
        println(rightBrace)
    }

    /**
     * 返回当前 printer 输出文本。
     */
    override fun toString(): String = printer.toString()
}

// ─────────────────────────────────────────────────────────────────────────────
// Error expression renderer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 错误表达式渲染器基类。
 */
abstract class CfirErrorExpressionRenderer {
    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * 渲染错误表达式诊断。
     */
    fun renderDiagnostic(diagnostic: ConeDiagnostic) {
        printer.println("ERROR_EXPR(${diagnostic.reason})")
    }

    /**
     * 渲染错误表达式节点。
     */
    abstract fun renderErrorExpression(errorExpression: CfirErrorExpression)
}

/**
 * 只输出错误诊断的错误表达式渲染器。
 */
class CfirErrorExpressionOnlyErrorRenderer : CfirErrorExpressionRenderer() {
    /**
     * 渲染错误表达式及特定 fake source 下保留的原始表达式。
     */
    override fun renderErrorExpression(errorExpression: CfirErrorExpression) {
        renderDiagnostic(errorExpression.diagnostic)
        errorExpression.expression?.let {
            if (errorExpression.source?.kind == CjFakeSourceElementKind.ErrorExpressionForTopLevelLambda) {
                it.accept(components.visitor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Component interfaces
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CFIR renderer 组件集合。
 */
interface CfirRendererComponents {
    /**
     * 当前 visitor。
     */
    val visitor: CfirRenderer.Visitor

    /**
     * 当前 printer。
     */
    val printer: CfirPrinter

    /**
     * 注解渲染器。
     */
    val annotationRenderer: CfirAnnotationRenderer?

    /**
     * 调用实参渲染器。
     */
    val callArgumentsRenderer: CfirCallArgumentsRenderer?

    /**
     * 声明渲染器。
     */
    val declarationRenderer: CfirDeclarationRenderer?

    /**
     * 包指令渲染器。
     */
    val packageDirectiveRenderer: CfirPackageDirectiveRenderer?

    /**
     * resolve phase 渲染器。
     */
    val resolvePhaseRenderer: CfirResolvePhaseRenderer?

    /**
     * 引用渲染器。
     */
    val referenceRenderer: CfirReferenceRenderer

    /**
     * 修饰符渲染器。
     */
    val modifierRenderer: CfirModifierRenderer?

    /**
     * 模式渲染器。
     */
    val patternRenderer: CfirPatternRenderer?

    /**
     * 错误表达式渲染器。
     */
    val errorExpressionRenderer: CfirErrorExpressionRenderer?

    /**
     * cone 类型渲染器。
     */
    val typeRenderer: ConeTypeRenderer

    /**
     * callable 签名渲染器。
     */
    val callableSignatureRenderer: CfirCallableSignatureRenderer?

    /**
     * 表达式单行渲染器。
     */
    val inlineExpressionRenderer: CfirInlineExpressionRenderer?
}

/**
 * 包指令渲染器。
 */
open class CfirPackageDirectiveRenderer {
    /**
     * 渲染包指令。
     */
    open fun render(packageDirective: CfirPackageDirective) = Unit
}

/**
 * CFIR 引用渲染器。
 */
open class CfirReferenceRenderer {
    /**
     * 将引用渲染为单行文本。
     */
    open fun render(reference: CfirReference): String = when (reference) {
        is CfirResolvedNamedReference -> "${reference.name.asString()} -> ${reference.resolvedSymbol}"
        is CfirNamedReference -> reference.name.asString()

        is CfirThisReference -> buildString {
            append("this")
            reference.boundSymbol?.let { append(" -> ").append(it) }
        }

        is CfirSuperReference -> "super"
        is CfirErrorReference -> "ERROR_REF(${reference.reason})"
        is CfirControlFlowGraphReference -> "<cfg-ref>"
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Main renderer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CFIR 主渲染器。
 *
 * 该类组合各类子 renderer，并通过 [Visitor] 遍历 CFIR 树输出调试文本。
 */
class CfirRenderer(
    /**
     * 输出文本 builder。
     */
    builder: StringBuilder = StringBuilder(),
    /**
     * 注解渲染器。
     */
    override val annotationRenderer: CfirAnnotationRenderer? = CfirAnnotationRenderer(),

    /**
     * 声明渲染器。
     */
    override val declarationRenderer: CfirDeclarationRenderer? = CfirDeclarationRenderer(),
    /**
     * 包指令渲染器。
     */
    override val packageDirectiveRenderer: CfirPackageDirectiveRenderer? = CfirPackageDirectiveRenderer(),
    /**
     * resolve phase 渲染器。
     */
    override val resolvePhaseRenderer: CfirResolvePhaseRenderer? = null,
    /**
     * 错误表达式渲染器。
     */
    override val errorExpressionRenderer: CfirErrorExpressionRenderer? = CfirErrorExpressionOnlyErrorRenderer(),
    /**
     * cone 类型渲染器。
     */
    override val typeRenderer: ConeTypeRenderer = ConeTypeRendererForDebugging(),
    /**
     * callable 签名渲染器。
     */
    override val callableSignatureRenderer: CfirCallableSignatureRenderer? = CfirCallableSignatureRenderer(),
    /**
     * 调用实参渲染器。
     */
    override val callArgumentsRenderer: CfirCallArgumentsRenderer? = CfirCallArgumentsRenderer(),
    /**
     * 引用渲染器。
     */
    override val referenceRenderer: CfirReferenceRenderer = CfirReferenceRenderer(),
    /**
     * 修饰符渲染器。
     */
    override val modifierRenderer: CfirModifierRenderer? = CfirModifierRenderer(),
    /**
     * 表达式单行渲染器。
     */
    override val inlineExpressionRenderer: CfirInlineExpressionRenderer? = CfirInlineExpressionRenderer(
        referenceRenderer = CfirReferenceRenderer(),
        typeRenderer = ConeTypeRendererForDebugging(),
    ),
    /**
     * 模式渲染器。
     */
    override val patternRenderer: CfirPatternRenderer? = CfirPatternRenderer(
        typeRenderer = ConeTypeRendererForDebugging(),
        referenceRenderer = CfirReferenceRenderer(),
        inlineExpressionRenderer = CfirInlineExpressionRenderer(
            referenceRenderer = CfirReferenceRenderer(),
            typeRenderer = ConeTypeRendererForDebugging(),
        ),
    ),
) : CfirRendererComponents {

    /**
     * 主 CFIR visitor。
     */
    override val visitor: Visitor = Visitor()

    /**
     * 共享 printer。
     */
    override val printer: CfirPrinter = CfirPrinter(builder)

    init {
        // 所有子 renderer 都共享同一组 components。
        // 这里必须一次性完成装配，否则异常附加信息在调用独立 renderer 时会触发未初始化访问。
        annotationRenderer?.components = this
        declarationRenderer?.components = this
        resolvePhaseRenderer?.components = this
        errorExpressionRenderer?.components = this
        callableSignatureRenderer?.components = this
        callArgumentsRenderer?.components = this
        modifierRenderer?.components = this
    }

    /**
     * 便捷工厂与静态渲染入口。
     */
    companion object {
        /**
         * 创建 golden 兼容渲染器。
         */
        fun withGoldenCompat(): CfirRenderer = CfirRenderer()

        /**
         * 创建 debug 渲染器。
         */
        fun withDebug(): CfirRenderer = CfirRenderer()

        /**
         * 创建可读性优先渲染器。
         */
        fun withReadability(): CfirRenderer = CfirRenderer()

        /**
         * 使用 golden 兼容渲染器渲染单个元素。
         */
        fun render(element: CfirElement): String = withGoldenCompat().renderElementAsString(element)
    }

    /**
     * 渲染单个 CFIR 元素为字符串。
     */
    fun renderElementAsString(element: CfirElement, trim: Boolean = true): String {
        printer.reset()
        element.accept(visitor)
        val normalized = printer.toString().replace("\r\n", "\n")
        return if (trim) normalized.trimEnd() else normalized
    }

    // ── Core print helpers ────────────────────────────────────────────────────

    /**
     * 向共享 printer 输出对象。
     */
    private fun print(vararg objects: Any) = printer.print(*objects)

    /**
     * 向共享 printer 输出对象并换行。
     */
    private fun println(vararg objects: Any) = printer.println(*objects)

    /**
     * 直接把 cone 类型渲染到共享 printer。
     *
     * 输出格式：`R|<type>|`。
     */
    private fun renderType(type: ConeCangJieType?) {
        if (type == null) return
        print("R|")
        typeRenderer.render(type)
        print("|")
    }

    /**
     * Raw CFIR 阶段的类型引用大多还没有 coneType，
     * 因此这里必须优先按 CfirTypeRef 结构直接渲染，不能只看 coneTypeOrNull。
     */
    private fun renderType(typeRef: CfirTypeRef?) {
        annotationRenderer?.render(typeRef ?: return)
        val rendered = renderTypeRefForDebug(typeRef, typeRenderer)
        if (rendered.isNotEmpty()) {
            print(rendered)
        }
    }

    /**
     * 渲染引用为单行文本。
     */
    private fun renderReference(reference: CfirReference): String =
        referenceRenderer.render(reference)

    /**
     * 渲染模式为单行文本。
     */
    private fun renderPattern(pattern: CfirPattern): String =
        patternRenderer?.render(pattern) ?: "<pattern>"

    /**
     * 渲染模式变量的可读名称。
     */
    private fun renderPatternVariableName(variable: CfirPatternVariable): String {
        val pattern = variable.pattern
        return if (pattern is CfirBindingPattern) pattern.name.asString() else "<anonymous>"
    }

    // ── Shared structural helpers ─────────────────────────────────────────────

    /**
     * 使用共享 printer 输出逗号分隔的父类型列表。
     *
     * [superTypeRefs] 为空时不输出任何内容。
     */
    private fun printSuperTypes(superTypeRefs: List<CfirTypeRef>) {
        if (superTypeRefs.isEmpty()) return
        print(" <: ")
        superTypeRefs.forEachIndexed { index, typeRef ->
            if (index > 0) print(", ")
            renderType(typeRef)
        }
    }

    /**
     * 输出类型参数名列表。
     *
     * 列表为空时不输出任何内容。
     */
    private fun printTypeParams(typeParameters: List<CfirTypeParameter>) {
        if (typeParameters.isEmpty()) return
        print("<")
        typeParameters.forEachIndexed { index, tp ->
            if (index > 0) print(", ")
            print(tp.name.asString())
        }
        print(">")
    }

    // ── Class-like declaration header helper ──────────────────────────────────

    /**
     * 输出 class-like 声明头。
     */
    private fun printClassLikeHeader(
        keyword: String,
        name: String,
        typeParameters: List<CfirTypeParameter>,
        superTypeRefs: List<CfirTypeRef>,
        extraPrefix: String = "",
    ) {
        print(keyword)
        print(" ")
        print(extraPrefix)
        print(name)
        printTypeParams(typeParameters)
        printSuperTypes(superTypeRefs)
        println(" {")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Visitor
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 主 CFIR 树 visitor。
     */
    inner class Visitor internal constructor() : CfirVisitorVoid() {

        /**
         * 渲染未知元素兜底文本。
         */
        override fun visitElement(element: CfirElement) {
            val className = element::class.simpleName.orEmpty()
            println("<element: ${className.removeSuffix("Impl")}>")
        }

        // ── Top-level ─────────────────────────────────────────────────────────

        /**
         * 渲染 CFIR 文件。
         */
        override fun visitFile(file: CfirFile) {
            resolvePhaseRenderer?.render(file)
            print("FILE: ")
            println(file.name)
            printer.pushIndent()
            annotationRenderer?.render(file)
            if (file.annotations.isNotEmpty()) {
                printer.newLine()
            }
            file.packageDirective.accept(this)
            file.imports.forEach { it.accept(this) }
            file.declarations.forEach { it.accept(this) }
            printer.popIndent()
        }

        /**
         * 渲染包指令。
         */
        override fun visitPackageDirective(packageDirective: CfirPackageDirective) {
            packageDirectiveRenderer?.render(packageDirective)
            if (!packageDirective.packageFqName.isRoot) {
                println("package ${packageDirective.packageFqName.asString()}")
            }
        }

        /**
         * 渲染 import 指令。
         */
        override fun visitImport(import: CfirImport) {
            val suffix = if (import.isAllUnder) ".*" else ""
            val alias = import.aliasName?.let { " as ${it.asString()}" } ?: ""
            val importedFqName = import.importedFqName?.asString() ?: "<error>"
            println("import $importedFqName$suffix$alias")
        }

        // ── Type declarations ─────────────────────────────────────────────────

        /**
         * 渲染 class 声明。
         */
        override fun visitClass(klass: CfirClass) {
            resolvePhaseRenderer?.render(klass)
            annotationRenderer?.render(klass)
            modifierRenderer?.renderModifiers(klass)
            printClassLikeHeader("class", klass.name.asString(), klass.typeParameters, klass.superTypeRefs)
            printer.pushIndent()
            klass.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 interface 声明。
         */
        override fun visitInterface(`interface`: CfirInterface) {
            resolvePhaseRenderer?.render(`interface`)
            annotationRenderer?.render(`interface`)
            modifierRenderer?.renderModifiers(`interface`)
            printClassLikeHeader(
                "interface",
                `interface`.name.asString(),
                `interface`.typeParameters,
                `interface`.superTypeRefs
            )
            printer.pushIndent()
            `interface`.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 struct 声明。
         */
        override fun visitStruct(struct: CfirStruct) {
            resolvePhaseRenderer?.render(struct)
            annotationRenderer?.render(struct)
            modifierRenderer?.renderModifiers(struct)
            printClassLikeHeader("struct", struct.name.asString(), struct.typeParameters, struct.superTypeRefs)
            printer.pushIndent()
            struct.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 enum 声明。
         */
        override fun visitEnum(enum: CfirEnum) {
            resolvePhaseRenderer?.render(enum)
            annotationRenderer?.render(enum)
            modifierRenderer?.renderModifiers(enum)
            val refPrefix = if (enum.isRefEnum) "ref " else ""
            printClassLikeHeader(
                keyword = "${refPrefix}enum",
                name = enum.name.asString(),
                typeParameters = enum.typeParameters,
                superTypeRefs = enum.superTypeRefs,
            )
            printer.pushIndent()
            var ellipsisRendered = false
            enum.declarations.forEach { declaration ->
                if (!ellipsisRendered && enum.isNonExhaustive && declaration !is CfirEnumConstructor) {
                    println("...")
                    ellipsisRendered = true
                }
                declaration.accept(this)
            }
            if (!ellipsisRendered && enum.isNonExhaustive) {
                println("...")
            }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 extend 声明。
         */
        override fun visitExtend(extend: CfirExtend) {
            resolvePhaseRenderer?.render(extend)
            annotationRenderer?.render(extend)
            modifierRenderer?.renderModifiers(extend)
            print("extend")
            printTypeParams(extend.typeParameters)
            print(" ")
            renderType(extend.extendedTypeRef)
            printSuperTypes(extend.superTypeRefs)
            println(" {")
            printer.pushIndent()
            extend.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        // ── Callables ─────────────────────────────────────────────────────────

        /**
         * 渲染函数类声明。
         */
        override fun visitFunction(function: CfirFunction) {
            resolvePhaseRenderer?.render(function)
            annotationRenderer?.render(function)
            modifierRenderer?.renderModifiers(function)

            val mutPrefix = if ((function as? CfirNamedFunction)?.isMut == true) "mut " else ""
            if (mutPrefix.isNotEmpty()) print(mutPrefix)

            print("func ")
            printTypeParams(function.typeParameters)

            // function name
            val functionName = when (function) {
                is CfirNamedFunction -> function.name.asString()
                is CfirMacroDeclaration -> function.name.asString()
                is CfirMainFunction -> "main"
                is CfirConstructor -> "init"
                is CfirFinalizer -> "finalizer"
                is CfirAnonymousFunction -> "<anonymous>"
                else -> "<anonymous>"
            }
            print(functionName)

            // parameters
            callableSignatureRenderer?.renderParameters(function.valueParameters)
            print(": ")
            renderType(function.returnTypeRef)

            if (function.body != null) {
                println(" {")
                printer.pushIndent()
                function.body?.accept(this)
                printer.popIndent()
                println("}")
            } else {
                println()
            }
        }

        /**
         * 渲染构造器声明。
         */
        override fun visitConstructor(constructor: CfirConstructor) {
            resolvePhaseRenderer?.render(constructor)
            annotationRenderer?.render(constructor)
            modifierRenderer?.renderModifiers(constructor)
            print("init")
            callableSignatureRenderer?.renderParameters(constructor.valueParameters)
            if (constructor.body != null) {
                println(" {")
                printer.pushIndent()
                constructor.body?.accept(this)
                printer.popIndent()
                println("}")
            } else {
                println()
            }
        }

        /**
         * 渲染具名函数声明。
         */
        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            visitFunction(namedFunction)
        }

        // ── Variables / properties ────────────────────────────────────────────

        /**
         * 渲染属性声明。
         */
        override fun visitProperty(property: CfirProperty) {
            resolvePhaseRenderer?.render(property)
            annotationRenderer?.render(property)
            modifierRenderer?.renderModifiers(property)
            print("prop ")
            print(property.name.asString())
            print(": ")
            renderType(property.returnTypeRef)
            println()
        }

        /**
         * 渲染字段变量声明。
         */
        override fun visitFieldVariable(fieldVariable: CfirFieldVariable) {
            resolvePhaseRenderer?.render(fieldVariable)
            annotationRenderer?.render(fieldVariable)
            modifierRenderer?.renderModifiers(fieldVariable)
            print(if (fieldVariable.isVar) "var" else "let")
            print(" ")
            print(fieldVariable.name.asString())
            print(": ")
            renderType(fieldVariable.returnTypeRef)
            if (fieldVariable.initializer != null) print(" = ...")
            println()
            fieldVariable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
                printer.popIndent()
            }
        }

        /**
         * 渲染模式变量声明。
         */
        override fun visitPatternVariable(patternVariable: CfirPatternVariable) {
            resolvePhaseRenderer?.render(patternVariable)
            annotationRenderer?.render(patternVariable)
            modifierRenderer?.renderModifiers(patternVariable)
            print(if (patternVariable.isVar) "var" else "let")
            print(" ")
            print(renderPattern(patternVariable.pattern))
            print(": ")
            renderType(patternVariable.returnTypeRef)
            if (patternVariable.initializer != null) print(" = ...")
            println()
            patternVariable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
                printer.popIndent()
            }
        }

        // ── Type-system declarations ──────────────────────────────────────────

        /**
         * 渲染值参数声明。
         */
        override fun visitValueParameter(valueParameter: CfirValueParameter) {
            callableSignatureRenderer?.renderParameter(valueParameter)
        }

        /**
         * 渲染类型参数声明。
         */
        override fun visitTypeParameter(typeParameter: CfirTypeParameter) {
            resolvePhaseRenderer?.render(typeParameter)
            annotationRenderer?.render(typeParameter)
            println("type-param ${typeParameter.name.asString()}")
        }

        /**
         * 渲染 typealias 声明。
         */
        override fun visitTypeAlias(typeAlias: CfirTypeAlias) {
            resolvePhaseRenderer?.render(typeAlias)
            annotationRenderer?.render(typeAlias)
            modifierRenderer?.renderModifiers(typeAlias)
            print("typealias ")
            print(typeAlias.name.asString())
            printTypeParams(typeAlias.typeParameters)
            print(" = ")
            renderType(typeAlias.expandedTypeRef)
            println()
        }

        /**
         * 渲染 enum constructor 声明。
         */
        override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor) {
            resolvePhaseRenderer?.render(enumConstructor)
            annotationRenderer?.render(enumConstructor)
            print(enumConstructor.name.asString())
            if (enumConstructor.valueParameters.isNotEmpty()) {
                callableSignatureRenderer?.renderParameters(enumConstructor.valueParameters)
            }
            println()
        }

        /**
         * 渲染注解。
         */
        override fun visitAnnotation(annotation: CfirAnnotation) {
            annotationRenderer?.renderAnnotation(annotation)
        }

        /**
         * 渲染注解调用。
         */
        override fun visitAnnotationCall(annotationCall: CfirAnnotationCall) {
            annotationRenderer?.renderAnnotation(annotationCall)
        }

        // ── Statements ────────────────────────────────────────────────────────

        /**
         * 渲染代码块。
         */
        override fun visitBlock(block: CfirBlock) {
            block.statements.forEach { it.accept(this) }
        }

        // ── Expressions ───────────────────────────────────────────────────────

        /**
         * 渲染字面量表达式。
         */
        override fun visitLiteralExpression(literalExpression: CfirLiteralExpression) {
            val value = when (literalExpression.kind) {
                CfirLiteralKind.STRING -> "\"${literalExpression.value}\""
                CfirLiteralKind.RUNE -> "'${literalExpression.value}'"
                CfirLiteralKind.UNIT -> "()"
                else -> "${literalExpression.value}"
            }
            println("${literalExpression.kind.name}($value)")
        }

        /**
         * 渲染字符串插值表达式。
         */
        override fun visitStringInterpolation(stringInterpolation: CfirStringInterpolation) {
            println("STRING_INTERPOLATION {")
            printer.pushIndent()
            stringInterpolation.parts.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染函数调用表达式。
         */
        override fun visitFunctionCall(functionCall: CfirFunctionCall) {
            val ref = renderReference(functionCall.calleeReference)
            print("FUNCTION_CALL($ref")
            if (functionCall.typeArguments.isNotEmpty()) {
                print("<")
                functionCall.typeArguments.forEachIndexed { index, typeArg ->
                    if (index > 0) print(", ")
                    renderType(typeArg)
                }
                print(">")
            }
            println(") {")
            printer.pushIndent()
            functionCall.explicitReceiver?.let { receiver ->
                println("receiver:")
                printer.pushIndent()
                receiver.accept(this)
                printer.popIndent()
            }
            val arguments = functionCall.argumentList.arguments
            if (arguments.isNotEmpty()) {
                if (functionCall.explicitReceiver != null) {
                    println("arguments:")
                    printer.pushIndent()
                    arguments.forEach { it.accept(this) }
                    printer.popIndent()
                } else {
                    arguments.forEach { it.accept(this) }
                }
            }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染具名访问表达式。
         */
        override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression) {
            val ref = renderReference(namedAccessExpression.calleeReference)
            if (namedAccessExpression.explicitReceiver != null) {
                println("NAMED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                namedAccessExpression.explicitReceiver!!.accept(this)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("NAMED_ACCESS($ref)")
            }
        }

        /**
         * 渲染限定访问表达式。
         */
        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
            val ref = renderReference(qualifiedAccessExpression.calleeReference)
            if (qualifiedAccessExpression.explicitReceiver != null) {
                println("QUALIFIED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                qualifiedAccessExpression.explicitReceiver!!.accept(this)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("QUALIFIED_ACCESS($ref)")
            }
        }

        /**
         * 渲染可选表达式。
         */
        override fun visitOptionalExpression(optionalExpression: CfirOptionalExpression) {
            println("OPTIONAL_EXPRESSION {")
            printer.pushIndent()
            optionalExpression.expression.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染可选链表达式。
         */
        override fun visitOptionalChainExpression(optionalChainExpression: CfirOptionalChainExpression) {
            println("OPTIONAL_CHAIN_EXPRESSION {")
            printer.pushIndent()
            optionalChainExpression.expression.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 super receiver 表达式。
         */
        override fun visitSuperReceiverExpression(superReceiverExpression: CfirSuperReceiverExpression) {
            visitQualifiedAccessExpression(superReceiverExpression)
        }

        /**
         * 渲染赋值表达式。
         */
        override fun visitAssignment(assignment: CfirAssignment) {
            println("ASSIGNMENT {")
            printer.pushIndent()
            println("lValue:")
            printer.pushIndent()
            assignment.lValue.accept(this)
            printer.popIndent()
            println("rValue:")
            printer.pushIndent()
            assignment.rValue.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染二元操作表达式。
         */
        override fun visitBinaryOp(binaryOp: CfirBinaryOp) {
            println("BINARY_OP(${binaryOp.kind.name}) {")
            printer.pushIndent()
            binaryOp.left.accept(this)
            binaryOp.right.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染比较表达式。
         */
        override fun visitComparisonExpression(comparisonExpression: CfirComparisonExpression) {
            println("COMPARISON(${comparisonExpression.operation.name}) {")
            printer.pushIndent()
            comparisonExpression.left.accept(this)
            comparisonExpression.right.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染类型操作表达式。
         */
        override fun visitTypeOperator(typeOperator: CfirTypeOperator) {
            print("TYPE_OP(${typeOperator.operation.name}, ")
            renderType(typeOperator.typeRef)
            println(") {")
            printer.pushIndent()
            typeOperator.argument.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染类型转换表达式。
         */
        override fun visitTypeConversion(typeConversion: CfirTypeConversion) {
            print("TYPE_CONVERSION(")
            renderType(typeConversion.targetTypeRef)
            println(") {")
            printer.pushIndent()
            typeConversion.argument.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 if 表达式。
         */
        override fun visitIfExpression(ifExpression: CfirIfExpression) {
            println("IF {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            ifExpression.condition.accept(this)
            printer.popIndent()
            println("then:")
            printer.pushIndent()
            ifExpression.thenBranch.accept(this)
            printer.popIndent()
            ifExpression.elseBranch?.let {
                println("else:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 match 表达式。
         */
        override fun visitMatchExpression(matchExpression: CfirMatchExpression) {
            println("MATCH {")
            printer.pushIndent()
            matchExpression.subject?.let {
                println("subject:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
            }
            matchExpression.branches.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 match 分支。
         */
        override fun visitMatchBranch(matchBranch: CfirMatchBranch) {
            val guardSuffix = matchBranch.guard
                ?.let { " where ${inlineExpressionRenderer?.render(it) ?: "<guard>"}" }
                ?: ""
            println("BRANCH(${renderPattern(matchBranch.pattern)}$guardSuffix) {")
            printer.pushIndent()
            matchBranch.body.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 while / do-while 循环表达式。
         */
        override fun visitLoopExpression(loopExpression: CfirLoopExpression) {
            val kind = if (loopExpression.isDoWhile) "DO_WHILE" else "WHILE"
            println("$kind {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            loopExpression.condition.accept(this)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            loopExpression.body.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 for-in 表达式。
         */
        override fun visitForInExpression(forInExpression: CfirForInExpression) {
            val variableName = renderPatternVariableName(forInExpression.variable)
            print("FOR_IN($variableName: ")
            renderType(forInExpression.variable.returnTypeRef)
            println(") {")
            printer.pushIndent()
            println("iterable:")
            printer.pushIndent()
            forInExpression.iterable.accept(this)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            forInExpression.body.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 return 表达式。
         */
        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {

            println("RETURN {")
            printer.pushIndent()
            returnExpression.result.accept(this)
            printer.popIndent()
            println("}")

        }

        /**
         * 渲染 break 表达式。
         */
        override fun visitBreakExpression(breakExpression: CfirBreakExpression) {
            println("BREAK")
        }

        /**
         * 渲染 continue 表达式。
         */
        override fun visitContinueExpression(continueExpression: CfirContinueExpression) {
            println("CONTINUE")
        }

        /**
         * 渲染 throw 表达式。
         */
        override fun visitThrowExpression(throwExpression: CfirThrowExpression) {
            println("THROW {")
            printer.pushIndent()
            throwExpression.exception.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 try 表达式。
         */
        override fun visitTryExpression(tryExpression: CfirTryExpression) {
            println("TRY {")
            printer.pushIndent()
            println("try:")
            printer.pushIndent()
            tryExpression.tryBlock.accept(this)
            printer.popIndent()
            tryExpression.catches.forEach { catchClause ->
                val pattern = catchClause.pattern
                print("catch(")
                if (pattern.isWildcard) {
                    print("_")
                } else {
                    print(pattern.bindingName?.asString() ?: "<error>")
                }
                if (pattern.typeRefs.isNotEmpty()) {
                    print(": ")
                    pattern.typeRefs.forEachIndexed { index, typeRef ->
                        if (index > 0) print(" | ")
                        renderType(typeRef)
                    }
                }
                println("):")
                printer.pushIndent()
                catchClause.body.accept(this)
                printer.popIndent()
            }
            tryExpression.finallyBlock?.let {
                println("finally:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染匿名函数表达式。
         */
        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
            val lambda = anonymousFunctionExpression
            print("LAMBDA(")
            lambda.anonymousFunction.valueParameters.forEachIndexed { index, param ->
                if (index > 0) print(", ")
                print(param.name.asString())
                print(": ")
                renderType(param.returnTypeRef)
            }
            println(") {")
            printer.pushIndent()
            lambda.anonymousFunction.body?.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染区间表达式。
         */
        override fun visitRangeExpression(rangeExpression: CfirRangeExpression) {
            val op = if (rangeExpression.isInclusive) "..=" else ".."
            println("RANGE($op) {")
            printer.pushIndent()
            rangeExpression.start.accept(this)
            rangeExpression.end.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染数组字面量。
         */
        override fun visitArrayLiteral(arrayLiteral: CfirArrayLiteral) {
            println("ARRAY_LITERAL {")
            printer.pushIndent()
            arrayLiteral.elements.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染元组字面量。
         */
        override fun visitTupleLiteral(tupleLiteral: CfirTupleLiteral) {
            println("TUPLE_LITERAL {")
            printer.pushIndent()
            tupleLiteral.elements.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 spawn 表达式。
         */
        override fun visitSpawnExpression(spawnExpression: CfirSpawnExpression) {
            println("SPAWN {")
            printer.pushIndent()
            spawnExpression.body.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 synchronized 表达式。
         */
        override fun visitSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression) {
            println("SYNCHRONIZED {")
            printer.pushIndent()
            println("monitor:")
            printer.pushIndent()
            synchronizedExpression.monitor.accept(this)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            synchronizedExpression.body.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 unsafe 表达式。
         */
        override fun visitUnsafeExpression(unsafeExpression: CfirUnsafeExpression) {
            println("UNSAFE {")
            printer.pushIndent()
            unsafeExpression.body.accept(this)
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 quote 表达式。
         */
        override fun visitQuoteExpression(quoteExpression: CfirQuoteExpression) {
            println("QUOTE(\"${quoteExpression.rawText}\") {")
            printer.pushIndent()
            if (quoteExpression.interpolations.isNotEmpty()) {
                println("interpolations:")
                printer.pushIndent()
                quoteExpression.interpolations.forEach { it.accept(this) }
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染下标表达式。
         */
        override fun visitSubscriptExpression(subscriptExpression: CfirSubscriptExpression) {
            println("SUBSCRIPT {")
            printer.pushIndent()
            println("receiver:")
            printer.pushIndent()
            subscriptExpression.receiver.accept(this)
            printer.popIndent()
            println("indices:")
            printer.pushIndent()
            subscriptExpression.indices.forEach { it.accept(this) }
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        /**
         * 渲染 Option 类型引用。
         */
        override fun visitOptionTypeRef(optionTypeRef: CfirOptionTypeRef) {
            renderType(optionTypeRef)
        }

        /**
         * 渲染已解析类型引用。
         */
        override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
            renderType(resolvedTypeRef)
        }

        /**
         * 渲染基础类型引用。
         */
        override fun visitBasicTypeRef(basicTypeRef: CfirBasicTypeRef) {
            renderType(basicTypeRef)
        }

        /**
         * 渲染用户类型引用。
         */
        override fun visitUserTypeRef(userTypeRef: CfirUserTypeRef) {
            renderType(userTypeRef)
        }

        /**
         * 渲染函数类型引用。
         */
        override fun visitFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef) {
            renderType(functionTypeRef)
        }

        /**
         * 渲染隐式类型引用。
         */
        override fun visitImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef) {
            renderType(implicitTypeRef)
        }

        /**
         * 渲染元组类型引用。
         */
        override fun visitTupleTypeRef(tupleTypeRef: CfirTupleTypeRef) {
            renderType(tupleTypeRef)
        }

        /**
         * 渲染 VArray 类型引用。
         */
        override fun visitVArrayTypeRef(vArrayTypeRef: CfirVArrayTypeRef) {
            renderType(vArrayTypeRef)
        }

        /**
         * 渲染错误类型引用。
         */
        override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef) {
            renderType(errorTypeRef)
        }

        /**
         * 渲染错误表达式。
         */
        override fun visitErrorExpression(errorExpression: CfirErrorExpression) {
            errorExpressionRenderer?.renderErrorExpression(errorExpression)
        }
    }
}
