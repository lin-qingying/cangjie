package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.utils.Printer

// ─────────────────────────────────────────────────────────────────────────────
// Printer
// ─────────────────────────────────────────────────────────────────────────────

open class CfirPrinter(builder: StringBuilder = StringBuilder()) {
    private val printer = Printer(builder)
    private var lineBeginning = true

    fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    internal fun pushIndent() = printer.pushIndent()
    internal fun popIndent() = printer.popIndent()

    fun newLine() = println()

    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", block: () -> Unit) {
        println(" ", leftBrace)
        pushIndent()
        block()
        popIndent()
        println(rightBrace)
    }

    override fun toString(): String = printer.toString()
}

// ─────────────────────────────────────────────────────────────────────────────
// Error expression renderer
// ─────────────────────────────────────────────────────────────────────────────

abstract class CfirErrorExpressionRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val printer: CfirPrinter get() = components.printer

    fun renderDiagnostic(diagnostic: ConeDiagnostic) {
        printer.println("ERROR_EXPR(${diagnostic.reason})")
    }

    abstract fun renderErrorExpression(errorExpression: CfirErrorExpression)
}

class CfirErrorExpressionOnlyErrorRenderer : CfirErrorExpressionRenderer() {
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

interface CfirRendererComponents {
    val visitor: CfirRenderer.Visitor
    val printer: CfirPrinter
    val annotationRenderer: CfirAnnotationRenderer?
    val callArgumentsRenderer: CfirCallArgumentsRenderer?

    val declarationRenderer: CfirDeclarationRenderer?
    val packageDirectiveRenderer: CfirPackageDirectiveRenderer?
    val resolvePhaseRenderer: CfirResolvePhaseRenderer?
    val referenceRenderer: CfirReferenceRenderer
    val modifierRenderer: CfirModifierRenderer?
    val patternRenderer: CfirPatternRenderer?
    val errorExpressionRenderer: CfirErrorExpressionRenderer?
    val typeRenderer: ConeTypeRenderer
    val callableSignatureRenderer: CfirCallableSignatureRenderer?
    val inlineExpressionRenderer: CfirInlineExpressionRenderer?
}

open class CfirPackageDirectiveRenderer {
    open fun render(packageDirective: CfirPackageDirective) = Unit
}

open class CfirReferenceRenderer {
    open fun render(reference: CfirReference): String = when (reference) {
        is CfirNamedReference -> reference.name.asString()
        is CfirResolvedNamedReference -> "${reference.name.asString()} -> ${reference.resolvedSymbol}"
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

class CfirRenderer(
    builder: StringBuilder = StringBuilder(),
    override val annotationRenderer: CfirAnnotationRenderer? = CfirAnnotationRenderer(),

    override val declarationRenderer: CfirDeclarationRenderer? = CfirDeclarationRenderer(),
    override val packageDirectiveRenderer: CfirPackageDirectiveRenderer? = CfirPackageDirectiveRenderer(),
    override val resolvePhaseRenderer: CfirResolvePhaseRenderer? = null,
    override val errorExpressionRenderer: CfirErrorExpressionRenderer? = CfirErrorExpressionOnlyErrorRenderer(),
    override val typeRenderer: ConeTypeRenderer = ConeTypeRendererForDebugging(),
    override val callableSignatureRenderer: CfirCallableSignatureRenderer? = CfirCallableSignatureRenderer(),
    override val callArgumentsRenderer: CfirCallArgumentsRenderer? = CfirCallArgumentsRenderer(),
    override val referenceRenderer: CfirReferenceRenderer = CfirReferenceRenderer(),
    override val modifierRenderer: CfirModifierRenderer? = CfirModifierRenderer(),
    override val inlineExpressionRenderer: CfirInlineExpressionRenderer? = CfirInlineExpressionRenderer(
        referenceRenderer = CfirReferenceRenderer(),
        typeRenderer = ConeTypeRendererForDebugging(),
    ),
    override val patternRenderer: CfirPatternRenderer? = CfirPatternRenderer(
        typeRenderer = ConeTypeRendererForDebugging(),
        referenceRenderer = CfirReferenceRenderer(),
        inlineExpressionRenderer = CfirInlineExpressionRenderer(
            referenceRenderer = CfirReferenceRenderer(),
            typeRenderer = ConeTypeRendererForDebugging(),
        ),
    ),
) : CfirRendererComponents {

    override val visitor: Visitor = Visitor()
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

    companion object {
        fun withGoldenCompat(): CfirRenderer = CfirRenderer()
        fun withDebug(): CfirRenderer = CfirRenderer()
        fun withReadability(): CfirRenderer = CfirRenderer()

        fun render(element: CfirElement): String = withGoldenCompat().renderElementAsString(element)
    }

    fun renderElementAsString(element: CfirElement, trim: Boolean = true): String {
        element.accept(visitor)
        val normalized = printer.toString().replace("\r\n", "\n")
        return if (trim) normalized.trimEnd() else normalized
    }

    // ── Core print helpers ────────────────────────────────────────────────────

    private fun print(vararg objects: Any) = printer.print(*objects)
    private fun println(vararg objects: Any) = printer.println(*objects)

    /**
     * Renders a type directly into the shared printer/builder.
     * Output format:  R|<type>|
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

    private fun renderReference(reference: CfirReference): String =
        referenceRenderer.render(reference)

    private fun renderPattern(pattern: CfirPattern): String =
        patternRenderer?.render(pattern) ?: "<pattern>"

    private fun renderPatternVariableName(variable: CfirPatternVariable): String {
        val pattern = variable.pattern
        return if (pattern is CfirBindingPattern) pattern.name.asString() else "<anonymous>"
    }

    // ── Shared structural helpers ─────────────────────────────────────────────

    /**
     * Prints a comma-separated list of super-types using the printer.
     * Nothing is printed when [superTypeRefs] is empty.
     *
     * Example output:  ` <: R|Foo|, R|Bar|`
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
     * Prints `<T1, T2, …>` type-parameter list (names only).
     * Nothing is printed when the list is empty.
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

    inner class Visitor internal constructor() : CfirVisitorVoid() {

        override fun visitElement(element: CfirElement) {
            val className = element::class.simpleName.orEmpty()
            println("<element: ${className.removeSuffix("Impl")}>")
        }

        // ── Top-level ─────────────────────────────────────────────────────────

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

        override fun visitPackageDirective(packageDirective: CfirPackageDirective) {
            packageDirectiveRenderer?.render(packageDirective)
            if (!packageDirective.packageFqName.isRoot) {
                println("package ${packageDirective.packageFqName.asString()}")
            }
        }

        override fun visitImport(import_: CfirImport) {
            val suffix = if (import_.isAllUnder) ".*" else ""
            val alias = import_.aliasName?.let { " as ${it.asString()}" } ?: ""
            val importedFqName = import_.importedFqName?.asString() ?: "<error>"
            println("import $importedFqName$suffix$alias")
        }

        // ── Type declarations ─────────────────────────────────────────────────

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

        override fun visitInterface(iface: CfirInterface) {
            resolvePhaseRenderer?.render(iface)
            annotationRenderer?.render(iface)
            modifierRenderer?.renderModifiers(iface)
            printClassLikeHeader("interface", iface.name.asString(), iface.typeParameters, iface.superTypeRefs)
            printer.pushIndent()
            iface.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

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

        override fun visitEnum(enum_: CfirEnum) {
            resolvePhaseRenderer?.render(enum_)
            annotationRenderer?.render(enum_)
            modifierRenderer?.renderModifiers(enum_)
            val refPrefix = if (enum_.isRefEnum) "ref " else ""
            printClassLikeHeader(
                keyword = "${refPrefix}enum",
                name = enum_.name.asString(),
                typeParameters = enum_.typeParameters,
                superTypeRefs = enum_.superTypeRefs,
            )
            printer.pushIndent()
            enum_.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

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

        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            visitFunction(namedFunction)
        }

        // ── Variables / properties ────────────────────────────────────────────

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

        override fun visitFieldVariable(variable: CfirFieldVariable) {
            resolvePhaseRenderer?.render(variable)
            annotationRenderer?.render(variable)
            modifierRenderer?.renderModifiers(variable)
            print(if (variable.isVar) "var" else "let")
            print(" ")
            print(variable.name.asString())
            print(": ")
            renderType(variable.returnTypeRef)
            if (variable.initializer != null) print(" = ...")
            println()
            variable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
                printer.popIndent()
            }
        }

        override fun visitPatternVariable(variable: CfirPatternVariable) {
            resolvePhaseRenderer?.render(variable)
            annotationRenderer?.render(variable)
            modifierRenderer?.renderModifiers(variable)
            print(if (variable.isVar) "var" else "let")
            print(" ")
            print(renderPattern(variable.pattern))
            print(": ")
            renderType(variable.returnTypeRef)
            if (variable.initializer != null) print(" = ...")
            println()
            variable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this)
                printer.popIndent()
                printer.popIndent()
            }
        }

        // ── Type-system declarations ──────────────────────────────────────────

        override fun visitValueParameter(valueParameter: CfirValueParameter) {
            callableSignatureRenderer?.renderParameter(valueParameter)
        }

        override fun visitTypeParameter(typeParameter: CfirTypeParameter) {
            resolvePhaseRenderer?.render(typeParameter)
            annotationRenderer?.render(typeParameter)
            println("type-param ${typeParameter.name.asString()}")
        }

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

        override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor) {
            resolvePhaseRenderer?.render(enumConstructor)
            annotationRenderer?.render(enumConstructor)
            print(enumConstructor.name.asString())
            if (enumConstructor.valueParameters.isNotEmpty()) {
                callableSignatureRenderer?.renderParameters(enumConstructor.valueParameters)
            }
            println()
        }

        override fun visitAnnotation(annotation: CfirAnnotation) {
            annotationRenderer?.renderAnnotation(annotation)
        }

        override fun visitAnnotationCall(annotationCall: CfirAnnotationCall) {
            annotationRenderer?.renderAnnotation(annotationCall)
        }

        // ── Statements ────────────────────────────────────────────────────────

        override fun visitBlock(block: CfirBlock) {
            block.statements.forEach { it.accept(this) }
        }

        // ── Expressions ───────────────────────────────────────────────────────

        override fun visitLiteralExpression(literal: CfirLiteralExpression) {
            val value = when (literal.kind) {
                CfirLiteralKind.STRING -> "\"${literal.value}\""
                CfirLiteralKind.RUNE -> "'${literal.value}'"
                CfirLiteralKind.UNIT -> "()"
                else -> "${literal.value}"
            }
            println("${literal.kind.name}($value)")
        }

        override fun visitStringInterpolation(interpolation: CfirStringInterpolation) {
            println("STRING_INTERPOLATION {")
            printer.pushIndent()
            interpolation.parts.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitFunctionCall(call: CfirFunctionCall) {
            val ref = renderReference(call.calleeReference)
            print("FUNCTION_CALL($ref")
            if (call.typeArguments.isNotEmpty()) {
                print("<")
                call.typeArguments.forEachIndexed { index, typeArg ->
                    if (index > 0) print(", ")
                    renderType(typeArg)
                }
                print(">")
            }
            println(") {")
            printer.pushIndent()
            call.explicitReceiver?.let { receiver ->
                println("receiver:")
                printer.pushIndent()
                receiver.accept(this)
                printer.popIndent()
            }
            val arguments = call.argumentList.arguments
            if (arguments.isNotEmpty()) {
                if (call.explicitReceiver != null) {
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

        override fun visitNamedAccessExpression(access: CfirNamedAccessExpression) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("NAMED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("NAMED_ACCESS($ref)")
            }
        }

        override fun visitQualifiedAccessExpression(access: CfirQualifiedAccessExpression) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("QUALIFIED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("QUALIFIED_ACCESS($ref)")
            }
        }

        override fun visitOptionalExpression(optionalExpression: CfirOptionalExpression) {
            println("OPTIONAL_EXPRESSION {")
            printer.pushIndent()
            optionalExpression.expression.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitOptionalChainExpression(optionalChainExpression: CfirOptionalChainExpression) {
            println("OPTIONAL_CHAIN_EXPRESSION {")
            printer.pushIndent()
            optionalChainExpression.expression.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitSuperReceiverExpression(superReceiverExpression: CfirSuperReceiverExpression) {
            visitQualifiedAccessExpression(superReceiverExpression)
        }

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

        override fun visitBinaryOp(binaryOp: CfirBinaryOp) {
            println("BINARY_OP(${binaryOp.kind.name}) {")
            printer.pushIndent()
            binaryOp.left.accept(this)
            binaryOp.right.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitComparisonExpression(comparison: CfirComparisonExpression) {
            println("COMPARISON(${comparison.operation.name}) {")
            printer.pushIndent()
            comparison.left.accept(this)
            comparison.right.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitTypeOperator(typeOperator: CfirTypeOperator) {
            print("TYPE_OP(${typeOperator.operation.name}, ")
            renderType(typeOperator.typeRef)
            println(") {")
            printer.pushIndent()
            typeOperator.argument.accept(this)
            printer.popIndent()
            println("}")
        }

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

        override fun visitLoopExpression(loop: CfirLoopExpression) {
            val kind = if (loop.isDoWhile) "DO_WHILE" else "WHILE"
            println("$kind {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            loop.condition.accept(this)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            loop.body.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitForInExpression(forIn: CfirForInExpression) {
            val variableName = renderPatternVariableName(forIn.variable)
            print("FOR_IN($variableName: ")
            renderType(forIn.variable.returnTypeRef)
            println(") {")
            printer.pushIndent()
            println("iterable:")
            printer.pushIndent()
            forIn.iterable.accept(this)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            forIn.body.accept(this)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
            if (returnExpression.result != null) {
                println("RETURN {")
                printer.pushIndent()
                returnExpression.result!!.accept(this)
                printer.popIndent()
                println("}")
            } else {
                println("RETURN")
            }
        }

        override fun visitBreakExpression(breakExpression: CfirBreakExpression) {
            println("BREAK")
        }

        override fun visitContinueExpression(continueExpression: CfirContinueExpression) {
            println("CONTINUE")
        }

        override fun visitThrowExpression(throwExpression: CfirThrowExpression) {
            println("THROW {")
            printer.pushIndent()
            throwExpression.exception.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitTryExpression(tryExpression: CfirTryExpression) {
            println("TRY {")
            printer.pushIndent()
            println("try:")
            printer.pushIndent()
            tryExpression.tryBlock.accept(this)
            printer.popIndent()
            tryExpression.catches.forEach { catchClause ->
                print("catch(${catchClause.parameter.name.asString()}: ")
                renderType(catchClause.parameter.returnTypeRef)
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

        override fun visitRangeExpression(range: CfirRangeExpression) {
            val op = if (range.isInclusive) "..=" else ".."
            println("RANGE($op) {")
            printer.pushIndent()
            range.start.accept(this)
            range.end.accept(this)
            printer.popIndent()
            println("}")
        }

        override fun visitArrayLiteral(array: CfirArrayLiteral) {
            println("ARRAY_LITERAL {")
            printer.pushIndent()
            array.elements.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitTupleLiteral(tuple: CfirTupleLiteral) {
            println("TUPLE_LITERAL {")
            printer.pushIndent()
            tuple.elements.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitSpawnExpression(spawn: CfirSpawnExpression) {
            println("SPAWN {")
            printer.pushIndent()
            spawn.body.accept(this)
            printer.popIndent()
            println("}")
        }

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

        override fun visitUnsafeExpression(unsafeExpression: CfirUnsafeExpression) {
            println("UNSAFE {")
            printer.pushIndent()
            unsafeExpression.body.accept(this)
            printer.popIndent()
            println("}")
        }

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

        override fun visitSubscriptExpression(subscript: CfirSubscriptExpression) {
            println("SUBSCRIPT {")
            printer.pushIndent()
            println("receiver:")
            printer.pushIndent()
            subscript.receiver.accept(this)
            printer.popIndent()
            println("indices:")
            printer.pushIndent()
            subscript.indices.forEach { it.accept(this) }
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitOptionTypeRef(optionTypeRef: CfirOptionTypeRef) {
            renderType(optionTypeRef)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
            renderType(resolvedTypeRef)
        }

        override fun visitBasicTypeRef(basicTypeRef: CfirBasicTypeRef) {
            renderType(basicTypeRef)
        }

        override fun visitUserTypeRef(userTypeRef: CfirUserTypeRef) {
            renderType(userTypeRef)
        }

        override fun visitFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef) {
            renderType(functionTypeRef)
        }

        override fun visitImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef) {
            renderType(implicitTypeRef)
        }

        override fun visitTupleTypeRef(tupleTypeRef: CfirTupleTypeRef) {
            renderType(tupleTypeRef)
        }

        override fun visitVArrayTypeRef(varrayTypeRef: CfirVArrayTypeRef) {
            renderType(varrayTypeRef)
        }

        override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef) {
            renderType(errorTypeRef)
        }

        override fun visitErrorExpression(errorExpression: CfirErrorExpression) {
            errorExpressionRenderer?.renderErrorExpression(errorExpression)
        }
    }
}
