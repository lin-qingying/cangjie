package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.CfirPackageDirective
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter

import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJumpExpression
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.expressions.CfirSpawnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSynchronizedExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirErrorReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.utils.Printer

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

    internal fun pushIndent() {
        printer.pushIndent()
    }

    internal fun popIndent() {
        printer.popIndent()
    }

    fun newLine() {
        println()
    }

    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", block: () -> Unit) {
        println(" ", leftBrace)
        pushIndent()
        block()
        popIndent()
        println(rightBrace)
    }

    override fun toString(): String = printer.toString()
}

interface CfirRendererComponents {
    val visitor: CfirRenderer.Visitor
    val printer: CfirPrinter
    val declarationRenderer: CfirDeclarationRenderer?
    val packageDirectiveRenderer: CfirPackageDirectiveRenderer?
    val resolvePhaseRenderer: CfirResolvePhaseRenderer?
    val typeRenderer: CfirTypeRenderer
    val referenceRenderer: CfirReferenceRenderer
    val statusRenderer: CfirStatusRenderer?
    val patternRenderer: CfirPatternRenderer?
    val inlineExpressionRenderer: CfirInlineExpressionRenderer?
}

fun interface CfirDeclarationRenderer {
    fun renderResolveInfo(declaration: CfirDeclaration)
}

fun interface CfirPackageDirectiveRenderer {
    fun render(packageDirective: CfirPackageDirective)
}

fun interface CfirResolvePhaseRenderer {
    fun render(declaration: CfirDeclaration)
}

fun interface CfirTypeRenderer {
    fun render(typeRef: CfirTypeRef): String
}

fun interface CfirReferenceRenderer {
    fun render(reference: CfirReference): String
}

fun interface CfirStatusRenderer {
    fun render(status: CfirDeclarationStatus): String
}

fun interface CfirPatternRenderer {
    fun render(pattern: CfirPattern): String
}

fun interface CfirInlineExpressionRenderer {
    fun render(expression: CfirExpression): String
}

private class CfirDefaultTypeRenderer : CfirTypeRenderer {
    override fun render(typeRef: CfirTypeRef): String = when (typeRef) {
        is CfirUserTypeRef -> buildString {
            append("R|")
            typeRef.qualifier.joinTo(this, ".")
            if (typeRef.typeArguments.isNotEmpty()) {
                append("<")
                typeRef.typeArguments.joinTo(this) { render(it) }
                append(">")
            }
            append("|")
        }

        is CfirBasicTypeRef -> "R|${typeRef.name.asString()}|"
        is CfirImplicitTypeRef -> "<implicit>"
        is CfirResolvedTypeRef -> "R|${typeRef.coneType}|"
        is CfirFunctionTypeRef -> buildString {
            append("R|(")
            typeRef.parameterTypeRefs.joinTo(this) { render(it) }
            append(") -> ")
            append(render(typeRef.returnTypeRef))
            append("|")
        }

        is CfirTupleTypeRef -> buildString {
            append("R|(")
            typeRef.elementTypeRefs.joinTo(this) { render(it) }
            append(")|")
        }

        is CfirVArrayTypeRef -> buildString {
            append("R|VArray<")
            append(render(typeRef.elementTypeRef))
            append(", $")
            append(typeRef.sizeLiteral)
            append(">|")
        }

        is CfirErrorTypeRef -> "R|ERROR: ${typeRef.reason}|"
    }
}

private class CfirDefaultReferenceRenderer : CfirReferenceRenderer {
    override fun render(reference: CfirReference): String = when (reference) {
        is CfirNamedReference -> reference.name.asString()
        is CfirResolvedNamedReference -> "${reference.name.asString()} -> ${reference.resolvedSymbol}"
        is CfirErrorReference -> "ERROR_REF(${reference.reason})"
        is CfirControlFlowGraphReference -> "<cfg-ref>"
    }
}

private class CfirDefaultStatusRenderer : CfirStatusRenderer {
    override fun render(status: CfirDeclarationStatus): String = buildString {
        append("${status.visibility.name} ")
        if (status.isAbstract) append(if (status.isModalityExplicit) "abstract " else "abstract? ")
        if (status.isOpen) append(if (status.isModalityExplicit) "open " else "open? ")
        if (status.isSealed) append(if (status.isModalityExplicit) "sealed " else "sealed? ")
        if (status.isStatic) append("static ")

        if (status.isMut) append("mut ")
        if (status.isOverride) append("override ")
        if (status.isOperator) append("operator ")
        if (status.isUnsafe) append("unsafe ")
        if (status.isForeign) append("foreign ")
    }.trimEnd()
}

private class CfirDefaultInlineExpressionRenderer(
    private val referenceRenderer: CfirReferenceRenderer,
) : CfirInlineExpressionRenderer {
    override fun render(expression: CfirExpression): String = when (expression) {
        is CfirLiteralExpression -> when (expression.kind) {
            CfirLiteralKind.STRING -> "\"${expression.value}\""
            else -> "${expression.value}"
        }

        is CfirQualifiedAccess -> referenceRenderer.render(expression.calleeReference)
        is CfirPropertyAccess -> referenceRenderer.render(expression.calleeReference)
        is CfirComparisonExpression -> "${render(expression.left)} ${expression.operation.symbol} ${render(expression.right)}"
        is CfirBinaryOp -> "${render(expression.left)} ${expression.kind.symbol} ${render(expression.right)}"
        is CfirFunctionCall -> "${referenceRenderer.render(expression.calleeReference)}(${expression.arguments.joinToString { render(it) }})"
        else -> "<expr>"
    }
}

private class CfirDefaultPatternRenderer(
    private val typeRenderer: CfirTypeRenderer,
    private val referenceRenderer: CfirReferenceRenderer,
    private val inlineExpressionRenderer: CfirInlineExpressionRenderer,
) : CfirPatternRenderer {
    override fun render(pattern: CfirPattern): String = when (pattern) {
        is CfirExpressionPattern -> "expr(${inlineExpressionRenderer.render(pattern.expression)})"
        is CfirOrPattern -> pattern.alternatives.joinToString(" | ") { render(it) }
        is CfirWildcardPattern -> "_"
        is CfirConstPattern -> "const(${inlineExpressionRenderer.render(pattern.expression)})"
        is CfirBindingPattern -> buildString {
            append(pattern.name.asString())
            pattern.typeRef?.let { append(": ${typeRenderer.render(it)}") }
            pattern.nestedPattern?.let { append(" @ ${render(it)}") }
        }

        is CfirTuplePattern -> "(${pattern.elements.joinToString { render(it) }})"
        is CfirEnumPattern -> "${referenceRenderer.render(pattern.constructorReference)}(${
            pattern.arguments.joinToString {
                render(
                    it
                )
            }
        })"

        is CfirTypePattern -> buildString {
            append("is ${typeRenderer.render(pattern.typeRef)}")
            pattern.bindingName?.let { append(" ${it.asString()}") }
        }

        else -> "<unknown-pattern>"
    }
}

private object CfirNoResolvePhaseRenderer : CfirResolvePhaseRenderer {
    override fun render(declaration: CfirDeclaration) = Unit
}

private object CfirNoDeclarationRenderer : CfirDeclarationRenderer {
    override fun renderResolveInfo(declaration: CfirDeclaration) = Unit
}

private object CfirDefaultPackageDirectiveRenderer : CfirPackageDirectiveRenderer {
    override fun render(packageDirective: CfirPackageDirective) = Unit
}

class CfirRenderer(
    builder: StringBuilder = StringBuilder(),
    override val declarationRenderer: CfirDeclarationRenderer? = CfirNoDeclarationRenderer,
    override val packageDirectiveRenderer: CfirPackageDirectiveRenderer? = CfirDefaultPackageDirectiveRenderer,
    override val resolvePhaseRenderer: CfirResolvePhaseRenderer? = null,
    override val typeRenderer: CfirTypeRenderer = CfirDefaultTypeRenderer(),
    override val referenceRenderer: CfirReferenceRenderer = CfirDefaultReferenceRenderer(),
    override val statusRenderer: CfirStatusRenderer? = CfirDefaultStatusRenderer(),
    override val inlineExpressionRenderer: CfirInlineExpressionRenderer? = CfirDefaultInlineExpressionRenderer(
        referenceRenderer
    ),
    override val patternRenderer: CfirPatternRenderer? = CfirDefaultPatternRenderer(
        typeRenderer,
        referenceRenderer,
        inlineExpressionRenderer!!
    ),
) : CfirRendererComponents {

    override val visitor: Visitor = Visitor()
    override val printer: CfirPrinter = CfirPrinter(builder)

    companion object {
        fun withGoldenCompat(): CfirRenderer = CfirRenderer()

        fun withDebug(): CfirRenderer = CfirRenderer()

        fun withReadability(): CfirRenderer = CfirRenderer()

        fun render(element: CfirElement): String = withGoldenCompat().renderElementAsString(element)
    }

    fun renderElementAsString(element: CfirElement, trim: Boolean = true): String {
        element.accept(visitor, Unit)
        val normalized = printer.toString().replace("\r\n", "\n")
        return if (trim) normalized.trimEnd() else normalized
    }

    private fun print(vararg objects: Any) {
        printer.print(*objects)
    }

    private fun println(vararg objects: Any) {
        printer.println(*objects)
    }

    private fun renderType(typeRef: CfirTypeRef): String = typeRenderer.render(typeRef)

    private fun renderReference(reference: CfirReference): String = referenceRenderer.render(reference)

    private fun renderStatus(status: CfirDeclarationStatus): String = statusRenderer?.render(status).orEmpty()

    private fun renderStatus(status: CfirDeclarationStatus, _source: AbstractCjSourceElement?): String {
        val rendered = renderStatus(status)
        if (status.isVisibilityExplicit) return rendered

        val visibilityName = status.visibility.name
        return when {
            rendered == visibilityName -> "$visibilityName?"
            rendered.startsWith("$visibilityName ") -> rendered.replaceFirst("$visibilityName ", "$visibilityName? ")
            rendered.isEmpty() -> "$visibilityName?"
            else -> "$visibilityName? $rendered"
        }
    }

    private fun renderPattern(pattern: CfirPattern): String = patternRenderer?.render(pattern) ?: "<pattern>"

    private fun renderPatternVariableName(variable: CfirPatternVariable): String {
        val pattern = variable.pattern
        return if (pattern is CfirBindingPattern) pattern.name.asString() else "<anonymous>"
    }

    inner class Visitor internal constructor() : CfirVisitor<Unit, Unit>() {
        override fun visitElement(element: CfirElement, data: Unit) {
            val className = element::class.simpleName.orEmpty()
            val publicName = className.removeSuffix("Impl")
            println("<element: $publicName>")
        }

        override fun visitFile(file: CfirFile, data: Unit) {
            declarationRenderer?.renderResolveInfo(file)
            resolvePhaseRenderer?.render(file)
            print("FILE: ")
            println(file.name)

            printer.pushIndent()
            file.packageDirective.accept(this, data)
            file.imports.forEach { it.accept(this, data) }
            file.declarations.forEach { it.accept(this, data) }
            printer.popIndent()
        }

        override fun visitPackageDirective(packageDirective: CfirPackageDirective, data: Unit) {
            packageDirectiveRenderer?.render(packageDirective)
            if (!packageDirective.packageFqName.isRoot) {
                println("package ${packageDirective.packageFqName.asString()}")
            }
        }

        override fun visitImport(import_: CfirImport, data: Unit) {
            val suffix = if (import_.isAllUnder) ".*" else ""
            val alias = import_.aliasName?.let { " as ${it.asString()}" } ?: ""
            val importedFqName = import_.importedFqName?.asString() ?: "<error>"
            println("import ${importedFqName}$suffix$alias")
        }

        override fun visitClass(klass: CfirClass, data: Unit) {
            declarationRenderer?.renderResolveInfo(klass)
            resolvePhaseRenderer?.render(klass)
            val prefix = renderStatus(klass.status, klass.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (klass.typeParameters.isNotEmpty()) {
                "<${klass.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (klass.superTypeRefs.isNotEmpty()) {
                " : ${klass.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("${prefix}${klass.classKind.name.lowercase()} ${klass.name.asString()}$typeParams$supers {")
            printer.pushIndent()
            klass.declarations.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitExtend(extend: CfirExtend, data: Unit) {
            declarationRenderer?.renderResolveInfo(extend)
            resolvePhaseRenderer?.render(extend)
            val typeParams = if (extend.typeParameters.isNotEmpty()) {
                "<${extend.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (extend.superTypeRefs.isNotEmpty()) {
                " <: ${extend.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("extend$typeParams ${renderType(extend.extendedTypeRef)}$supers {")
            printer.pushIndent()
            extend.declarations.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitFunction(function: CfirFunction, data: Unit) {
            declarationRenderer?.renderResolveInfo(function)
            resolvePhaseRenderer?.render(function)
            val prefix = renderStatus(function.status, function.source).let { if (it.isNotEmpty()) "$it " else "" }
            val mutPrefix = if (function.isMut) "mut " else ""
            val typeParams = if (function.typeParameters.isNotEmpty()) {
                "<${function.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val params = function.valueParameters.joinToString {
                "${it.name.asString()}: ${renderType(it.returnTypeRef)}"
            }
            val signature =
                "${prefix}${mutPrefix}func ${function.name.asString()}$typeParams($params): ${renderType(function.returnTypeRef)}"
            if (function.body != null) {
                println("$signature {")
                printer.pushIndent()
                function.body?.accept(this, data)
                printer.popIndent()
                println("}")
            } else {
                println(signature)
            }
        }

        override fun visitConstructor(constructor: CfirConstructor, data: Unit) {
            declarationRenderer?.renderResolveInfo(constructor)
            resolvePhaseRenderer?.render(constructor)
            val params = constructor.valueParameters.joinToString {
                "${it.name.asString()}: ${renderType(it.returnTypeRef)}"
            }
            if (constructor.body != null) {
                println("init($params) {")
                printer.pushIndent()
                constructor.body?.accept(this, data)
                printer.popIndent()
                println("}")
            } else {
                println("init($params)")
            }
        }

        override fun visitProperty(property: CfirProperty, data: Unit) {
            declarationRenderer?.renderResolveInfo(property)
            resolvePhaseRenderer?.render(property)
            val prefix = renderStatus(property.status, property.source).let { if (it.isNotEmpty()) "$it " else "" }
            val keyword = "prop"
            println("${prefix}$keyword ${property.name.asString()}: ${renderType(property.returnTypeRef)}")
        }

        override fun visitFieldVariable(variable: CfirFieldVariable, data: Unit) {
            declarationRenderer?.renderResolveInfo(variable)
            resolvePhaseRenderer?.render(variable)
            val prefix = renderStatus(variable.status, variable.source).let { if (it.isNotEmpty()) "$it " else "" }
            val keyword = if (variable.isVar) "var" else "let"
            val init = if (variable.initializer != null) " = ..." else ""
            println("${prefix}$keyword ${variable.name.asString()}: ${renderType(variable.returnTypeRef)}$init")
            variable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this, data)
                printer.popIndent()
                printer.popIndent()
            }
        }

        override fun visitPatternVariable(variable: CfirPatternVariable, data: Unit) {
            declarationRenderer?.renderResolveInfo(variable)
            resolvePhaseRenderer?.render(variable)
            val prefix = renderStatus(variable.status, variable.source).let { if (it.isNotEmpty()) "$it " else "" }
            val keyword = if (variable.isVar) "var" else "let"
            val init = if (variable.initializer != null) " = ..." else ""
            println("${prefix}$keyword ${renderPattern(variable.pattern)}: ${renderType(variable.returnTypeRef)}$init")
            variable.initializer?.let {
                printer.pushIndent()
                println("initializer:")
                printer.pushIndent()
                it.accept(this, data)
                printer.popIndent()
                printer.popIndent()
            }
        }

        override fun visitValueParameter(valueParameter: CfirValueParameter, data: Unit) {
            declarationRenderer?.renderResolveInfo(valueParameter)
            resolvePhaseRenderer?.render(valueParameter)
            println("param ${valueParameter.name.asString()}: ${renderType(valueParameter.returnTypeRef)}")
        }

        override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: Unit) {
            declarationRenderer?.renderResolveInfo(typeParameter)
            resolvePhaseRenderer?.render(typeParameter)
            println("type-param ${typeParameter.name.asString()}")
        }

        override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: Unit) {
            declarationRenderer?.renderResolveInfo(typeAlias)
            resolvePhaseRenderer?.render(typeAlias)
            val prefix = renderStatus(typeAlias.status, typeAlias.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (typeAlias.typeParameters.isNotEmpty()) {
                "<${typeAlias.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            println("${prefix}typealias ${typeAlias.name.asString()}$typeParams = ${renderType(typeAlias.expandedTypeRef)}")
        }

        override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor, data: Unit) {
            declarationRenderer?.renderResolveInfo(enumConstructor)
            resolvePhaseRenderer?.render(enumConstructor)
            val rendered = when (val typeRef = enumConstructor.returnTypeRef) {
                is CfirImplicitTypeRef -> enumConstructor.name.asString()
                is CfirTupleTypeRef -> {
                    val args = typeRef.elementTypeRefs.joinToString { renderType(it) }
                    "${enumConstructor.name.asString()}($args)"
                }

                else -> "${enumConstructor.name.asString()}(${renderType(typeRef)})"
            }
            println(rendered)
        }

        override fun visitBlock(block: CfirBlock, data: Unit) {
            block.statements.forEach { it.accept(this, data) }
        }

        override fun visitLiteralExpression(literal: CfirLiteralExpression, data: Unit) {
            val value = when (literal.kind) {
                CfirLiteralKind.STRING -> "\"${literal.value}\""
                CfirLiteralKind.RUNE -> "'${literal.value}'"
                CfirLiteralKind.UNIT -> "()"
                else -> "${literal.value}"
            }
            println("${literal.kind.name}($value)")
        }

        override fun visitStringInterpolation(interpolation: CfirStringInterpolation, data: Unit) {
            println("STRING_INTERPOLATION {")
            printer.pushIndent()
            interpolation.parts.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitFunctionCall(call: CfirFunctionCall, data: Unit) {
            val ref = renderReference(call.calleeReference)
            val typeArgs = if (call.typeArguments.isNotEmpty()) {
                "<${call.typeArguments.joinToString { renderType(it) }}>"
            } else ""
            println("FUNCTION_CALL($ref$typeArgs) {")
            printer.pushIndent()
            call.explicitReceiver?.let {
                println("receiver:")
                printer.pushIndent()
                it.accept(this, data)
                printer.popIndent()
            }
            if (call.arguments.isNotEmpty()) {
                if (call.explicitReceiver != null) {
                    println("arguments:")
                    printer.pushIndent()
                    call.arguments.forEach { argument -> argument.accept(this, data) }
                    printer.popIndent()
                } else {
                    call.arguments.forEach { it.accept(this, data) }
                }
            }
            printer.popIndent()
            println("}")
        }

        override fun visitPropertyAccess(access: CfirPropertyAccess, data: Unit) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("PROPERTY_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this, data)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("PROPERTY_ACCESS($ref)")
            }
        }

        override fun visitQualifiedAccess(access: CfirQualifiedAccess, data: Unit) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("QUALIFIED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this, data)
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("QUALIFIED_ACCESS($ref)")
            }
        }

        override fun visitAssignment(assignment: CfirAssignment, data: Unit) {
            println("ASSIGNMENT {")
            printer.pushIndent()
            println("lValue:")
            printer.pushIndent()
            assignment.lValue.accept(this, data)
            printer.popIndent()
            println("rValue:")
            printer.pushIndent()
            assignment.rValue.accept(this, data)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitBinaryOp(binaryOp: CfirBinaryOp, data: Unit) {
            println("BINARY_OP(${binaryOp.kind.name}) {")
            printer.pushIndent()
            binaryOp.left.accept(this, data)
            binaryOp.right.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitComparisonExpression(comparison: CfirComparisonExpression, data: Unit) {
            println("COMPARISON(${comparison.operation.name}) {")
            printer.pushIndent()
            comparison.left.accept(this, data)
            comparison.right.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitTypeOperator(typeOperator: CfirTypeOperator, data: Unit) {
            println("TYPE_OP(${typeOperator.operation.name}, ${renderType(typeOperator.typeRef)}) {")
            printer.pushIndent()
            typeOperator.argument.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitIfExpression(ifExpression: CfirIfExpression, data: Unit) {
            println("IF {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            ifExpression.condition.accept(this, data)
            printer.popIndent()
            println("then:")
            printer.pushIndent()
            ifExpression.thenBranch.accept(this, data)
            printer.popIndent()
            ifExpression.elseBranch?.let {
                println("else:")
                printer.pushIndent()
                it.accept(this, data)
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: Unit) {
            println("MATCH {")
            printer.pushIndent()
            matchExpression.subject?.let {
                println("subject:")
                printer.pushIndent()
                it.accept(this, data)

                printer.popIndent()
            }

            matchExpression.branches.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitMatchBranch(matchBranch: CfirMatchBranch, data: Unit) {
            val guardSuffix = matchBranch.guard
                ?.let { " where ${inlineExpressionRenderer?.render(it) ?: "<guard>"}" }
                ?: ""
            println("BRANCH(${renderPattern(matchBranch.pattern)}$guardSuffix) {")
            printer.pushIndent()
            matchBranch.body.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitLoopExpression(loop: CfirLoopExpression, data: Unit) {
            val kind = if (loop.isDoWhile) "DO_WHILE" else "WHILE"
            println("$kind {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            loop.condition.accept(this, data)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            loop.body.accept(this, data)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitForInExpression(forIn: CfirForInExpression, data: Unit) {
            val variableName = renderPatternVariableName(forIn.variable)
            println("FOR_IN($variableName: ${renderType(forIn.variable.returnTypeRef)}) {")
            printer.pushIndent()
            println("iterable:")
            printer.pushIndent()
            forIn.iterable.accept(this, data)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            forIn.body.accept(this, data)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression, data: Unit) {
            if (returnExpression.result != null) {
                println("RETURN {")
                printer.pushIndent()
                returnExpression.result!!.accept(this, data)
                printer.popIndent()
                println("}")
            } else {
                println("RETURN")
            }
        }

        override fun visitJumpExpression(jump: CfirJumpExpression, data: Unit) {
            println(jump.kind.name)
        }

        override fun visitThrowExpression(throwExpression: CfirThrowExpression, data: Unit) {
            println("THROW {")
            printer.pushIndent()
            throwExpression.exception.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitTryExpression(tryExpression: CfirTryExpression, data: Unit) {
            println("TRY {")
            printer.pushIndent()
            println("try:")
            printer.pushIndent()
            tryExpression.tryBlock.accept(this, data)
            printer.popIndent()
            tryExpression.catches.forEach { catchClause ->
                println("catch(${catchClause.parameter.name.asString()}: ${renderType(catchClause.parameter.returnTypeRef)}):")
                printer.pushIndent()
                catchClause.body.accept(this, data)
                printer.popIndent()
            }
            tryExpression.finallyBlock?.let {
                println("finally:")
                printer.pushIndent()
                it.accept(this, data)
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitLambdaExpression(lambda: CfirLambdaExpression, data: Unit) {
            val params = lambda.anonymousFunction.valueParameters.joinToString {
                "${it.name.asString()}: ${renderType(it.returnTypeRef)}"
            }
            println("LAMBDA($params) {")
            printer.pushIndent()
            lambda.anonymousFunction.body?.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitRangeExpression(range: CfirRangeExpression, data: Unit) {
            val op = if (range.isInclusive) "..=" else ".."
            println("RANGE($op) {")
            printer.pushIndent()
            range.start.accept(this, data)
            range.end.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitArrayLiteral(array: CfirArrayLiteral, data: Unit) {
            println("ARRAY_LITERAL {")
            printer.pushIndent()
            array.elements.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitTupleLiteral(tuple: CfirTupleLiteral, data: Unit) {
            println("TUPLE_LITERAL {")
            printer.pushIndent()
            tuple.elements.forEach { it.accept(this, data) }
            printer.popIndent()
            println("}")
        }

        override fun visitSpawnExpression(spawn: CfirSpawnExpression, data: Unit) {
            println("SPAWN {")
            printer.pushIndent()
            spawn.body.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression, data: Unit) {
            println("SYNCHRONIZED {")
            printer.pushIndent()
            println("monitor:")
            printer.pushIndent()
            synchronizedExpression.monitor.accept(this, data)
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            synchronizedExpression.body.accept(this, data)
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitUnsafeExpression(unsafeExpression: CfirUnsafeExpression, data: Unit) {
            println("UNSAFE {")
            printer.pushIndent()
            unsafeExpression.body.accept(this, data)
            printer.popIndent()
            println("}")
        }

        override fun visitQuoteExpression(quoteExpression: CfirQuoteExpression, data: Unit) {
            println("QUOTE(\"${quoteExpression.rawText}\") {")
            printer.pushIndent()
            if (quoteExpression.interpolations.isNotEmpty()) {
                println("interpolations:")
                printer.pushIndent()
                quoteExpression.interpolations.forEach { it.accept(this, data) }
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitMacroExpression(macroExpression: CfirMacroExpression, data: Unit) {
            val name = macroExpression.name?.asString() ?: "<macro>"
            println("MACRO_EXPR($name) {")
            printer.pushIndent()
            macroExpression.attrText?.let { println("attr: \"$it\"") }
            macroExpression.inputText?.let { println("input: \"$it\"") }
            printer.popIndent()
            println("}")
        }

        override fun visitSubscriptExpression(subscript: CfirSubscriptExpression, data: Unit) {
            println("SUBSCRIPT {")
            printer.pushIndent()
            println("receiver:")
            printer.pushIndent()
            subscript.receiver.accept(this, data)
            printer.popIndent()
            println("indices:")
            printer.pushIndent()
            subscript.indices.forEach { it.accept(this, data) }
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitErrorExpression(error: CfirErrorExpression, data: Unit) {
            println("ERROR_EXPR(${error.reason})")
        }
    }
}
