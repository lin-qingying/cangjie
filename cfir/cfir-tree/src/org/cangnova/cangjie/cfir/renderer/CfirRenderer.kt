package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
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
abstract class CfirErrorExpressionRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val printer: CfirPrinter get() = components.printer

    fun renderDiagnostic(diagnostic: ConeDiagnostic) {
        printer.print("ERROR_EXPR(${diagnostic.reason})")
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
    val errorExpressionRenderer: CfirErrorExpressionRenderer?

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
        is CfirErrorTypeRef -> "R|ERROR: ${typeRef.diagnostic.reason}|"

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

    }
}

private class CfirDefaultReferenceRenderer : CfirReferenceRenderer {
    override fun render(reference: CfirReference): String = when (reference) {
        is CfirNamedReference -> reference.name.asString()
        is CfirResolvedNamedReference -> "${reference.name.asString()} -> ${reference.resolvedSymbol}"
        is CfirThisReference -> buildString {
            append("this")
            reference.boundSymbol?.let { append(" -> ").append(it) }
        }
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
        is CfirFunctionCall -> "${referenceRenderer.render(expression.calleeReference)}(${expression.argumentList.arguments.joinToString { argument -> render(argument) }})"

        is CfirNamedAccessExpression -> referenceRenderer.render(expression.calleeReference)
        is CfirQualifiedAccessExpression -> referenceRenderer.render(expression.calleeReference)
        is CfirComparisonExpression -> "${render(expression.left)} ${expression.operation.symbol} ${render(expression.right)}"
        is CfirBinaryOp -> "${render(expression.left)} ${expression.kind.symbol} ${render(expression.right)}"
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
    override val errorExpressionRenderer: CfirErrorExpressionRenderer? = CfirErrorExpressionOnlyErrorRenderer(),

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
        element.accept(visitor)
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

    inner class Visitor internal constructor() : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement ) {
            val className = element::class.simpleName.orEmpty()
            val publicName = className.removeSuffix("Impl")
            println("<element: $publicName>")
        }

        override fun visitFile(file: CfirFile ) {
            declarationRenderer?.renderResolveInfo(file)
            resolvePhaseRenderer?.render(file)
            print("FILE: ")
            println(file.name)

            printer.pushIndent()
            file.packageDirective.accept(this )
            file.imports.forEach { it.accept(this ) }
            file.declarations.forEach { it.accept(this ) }
            printer.popIndent()
        }

        override fun visitPackageDirective(packageDirective: CfirPackageDirective ) {
            packageDirectiveRenderer?.render(packageDirective)
            if (!packageDirective.packageFqName.isRoot) {
                println("package ${packageDirective.packageFqName.asString()}")
            }
        }

        override fun visitImport(import_: CfirImport ) {
            val suffix = if (import_.isAllUnder) ".*" else ""
            val alias = import_.aliasName?.let { " as ${it.asString()}" } ?: ""
            val importedFqName = import_.importedFqName?.asString() ?: "<error>"
            println("import ${importedFqName}$suffix$alias")
        }

// ---- 替换原来的 visitClass，新增 visitInterface / visitStruct / visitEnum ----

        override fun visitClass(klass: CfirClass) {
            declarationRenderer?.renderResolveInfo(klass)
            resolvePhaseRenderer?.render(klass)
            val prefix = renderStatus(klass.status, klass.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (klass.typeParameters.isNotEmpty()) {
                "<${klass.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (klass.superTypeRefs.isNotEmpty()) {
                " <: ${klass.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("${prefix}class ${klass.name.asString()}$typeParams$supers {")
            printer.pushIndent()
            klass.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitInterface(iface: CfirInterface) {
            declarationRenderer?.renderResolveInfo(iface)
            resolvePhaseRenderer?.render(iface)
            val prefix = renderStatus(iface.status, iface.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (iface.typeParameters.isNotEmpty()) {
                "<${iface.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (iface.superTypeRefs.isNotEmpty()) {
                " <: ${iface.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("${prefix}interface ${iface.name.asString()}$typeParams$supers {")
            printer.pushIndent()
            iface.properties.forEach { it.accept(this) }
            iface.functions.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitStruct(struct: CfirStruct) {
            declarationRenderer?.renderResolveInfo(struct)
            resolvePhaseRenderer?.render(struct)
            val prefix = renderStatus(struct.status, struct.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (struct.typeParameters.isNotEmpty()) {
                "<${struct.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (struct.superTypeRefs.isNotEmpty()) {
                " <: ${struct.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("${prefix}struct ${struct.name.asString()}$typeParams$supers {")
            printer.pushIndent()
            struct.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitEnum(enum_: CfirEnum) {
            declarationRenderer?.renderResolveInfo(enum_)
            resolvePhaseRenderer?.render(enum_)
            val prefix = renderStatus(enum_.status, enum_.source).let { if (it.isNotEmpty()) "$it " else "" }
            val refPrefix = if (enum_.isRefEnum) "ref " else ""
            val typeParams = if (enum_.typeParameters.isNotEmpty()) {
                "<${enum_.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val supers = if (enum_.superTypeRefs.isNotEmpty()) {
                " <: ${enum_.superTypeRefs.joinToString { renderType(it) }}"
            } else ""
            println("${prefix}${refPrefix}enum ${enum_.name.asString()}$typeParams$supers {")
            printer.pushIndent()
            enum_.declarations.forEach { it.accept(this) }
            printer.popIndent()
            println("}")
        }

        override fun visitExtend(extend: CfirExtend ) {
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
            extend.declarations.forEach { it.accept(this ) }
            printer.popIndent()
            println("}")
        }

        override fun visitFunction(function: CfirFunction ) {
            declarationRenderer?.renderResolveInfo(function)
            resolvePhaseRenderer?.render(function)
            val prefix = renderStatus(function.status, function.source).let { if (it.isNotEmpty()) "$it " else "" }
            val mutPrefix = if ((function as? CfirNamedFunction)?.isMut == true) "mut " else ""
            val typeParams = if (function.typeParameters.isNotEmpty()) {
                "<${function.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            val params = function.valueParameters.joinToString {
                "${it.name.asString()}${if(it.isNamed) "!" else ""}: ${renderType(it.returnTypeRef)}"
            }
            val functionName = when (function) {
                is CfirNamedFunction -> function.name.asString()
                is CfirMacroDeclaration -> function.name.asString()
                is CfirMainFunction -> "main"
                is CfirConstructor -> "init"
                is CfirFinalizer -> "finalizer"
                is CfirAnonymousFunction -> "<anonymous>"
                else -> "<anonymous>"
            }
            val signature =
                "${prefix}${mutPrefix}func $functionName$typeParams($params): ${renderType(function.returnTypeRef)}"
            if (function.body != null) {
                println("$signature {")
                printer.pushIndent()
                function.body?.accept(this )
                printer.popIndent()
                println("}")
            } else {
                println(signature)
            }
        }

        override fun visitConstructor(constructor: CfirConstructor ) {
            declarationRenderer?.renderResolveInfo(constructor)
            resolvePhaseRenderer?.render(constructor)
            val params = constructor.valueParameters.joinToString {
                "${it.name.asString()}: ${renderType(it.returnTypeRef)}"
            }
            if (constructor.body != null) {
                println("init($params) {")
                printer.pushIndent()
                constructor.body?.accept(this )
                printer.popIndent()
                println("}")
            } else {
                println("init($params)")
            }
        }

        override fun visitProperty(property: CfirProperty ) {
            declarationRenderer?.renderResolveInfo(property)
            resolvePhaseRenderer?.render(property)
            val prefix = renderStatus(property.status, property.source).let { if (it.isNotEmpty()) "$it " else "" }
            val keyword = "prop"
            println("${prefix}$keyword ${property.name.asString()}: ${renderType(property.returnTypeRef)}")
        }

        override fun visitFieldVariable(variable: CfirFieldVariable ) {
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
                it.accept(this )
                printer.popIndent()
                printer.popIndent()
            }
        }

        override fun visitPatternVariable(variable: CfirPatternVariable ) {
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
                it.accept(this )
                printer.popIndent()
                printer.popIndent()
            }
        }

        override fun visitValueParameter(valueParameter: CfirValueParameter ) {
            declarationRenderer?.renderResolveInfo(valueParameter)
            resolvePhaseRenderer?.render(valueParameter)
            println("param ${valueParameter.name.asString()}: ${renderType(valueParameter.returnTypeRef)}")
        }

        override fun visitTypeParameter(typeParameter: CfirTypeParameter ) {
            declarationRenderer?.renderResolveInfo(typeParameter)
            resolvePhaseRenderer?.render(typeParameter)
            println("type-param ${typeParameter.name.asString()}")
        }

        override fun visitTypeAlias(typeAlias: CfirTypeAlias ) {
            declarationRenderer?.renderResolveInfo(typeAlias)
            resolvePhaseRenderer?.render(typeAlias)
            val prefix = renderStatus(typeAlias.status, typeAlias.source).let { if (it.isNotEmpty()) "$it " else "" }
            val typeParams = if (typeAlias.typeParameters.isNotEmpty()) {
                "<${typeAlias.typeParameters.joinToString { it.name.asString() }}>"
            } else ""
            println("${prefix}typealias ${typeAlias.name.asString()}$typeParams = ${renderType(typeAlias.expandedTypeRef)}")
        }

        override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor ) {
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

        override fun visitBlock(block: CfirBlock ) {
            block.statements.forEach { it.accept(this ) }
        }

        override fun visitLiteralExpression(literal: CfirLiteralExpression ) {
            val value = when (literal.kind) {
                CfirLiteralKind.STRING -> "\"${literal.value}\""
                CfirLiteralKind.RUNE -> "'${literal.value}'"
                CfirLiteralKind.UNIT -> "()"
                else -> "${literal.value}"
            }
            println("${literal.kind.name}($value)")
        }

        override fun visitStringInterpolation(interpolation: CfirStringInterpolation ) {
            println("STRING_INTERPOLATION {")
            printer.pushIndent()
            interpolation.parts.forEach { it.accept(this ) }
            printer.popIndent()
            println("}")
        }

        override fun visitFunctionCall(call: CfirFunctionCall ) {
            val ref = renderReference(call.calleeReference)
            val typeArgs = if (call.typeArguments.isNotEmpty()) {
                "<${call.typeArguments.joinToString { renderType(it) }}>"
            } else ""
            println("FUNCTION_CALL($ref$typeArgs) {")
            printer.pushIndent()
            call.explicitReceiver?.let {
                println("receiver:")
                printer.pushIndent()
                it.accept(this )
                printer.popIndent()
            }
            val arguments = call.argumentList.arguments
            if (arguments.isNotEmpty()) {
                if (call.explicitReceiver != null) {
                    println("arguments:")
                    printer.pushIndent()
                    arguments.forEach { argument -> argument.accept(this ) }
                    printer.popIndent()
                } else {
                    arguments.forEach { argument -> argument.accept(this ) }
                }
            }
            printer.popIndent()
            println("}")
        }
        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            visitFunction(namedFunction)
        }
        override fun visitNamedAccessExpression(access: CfirNamedAccessExpression ) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("NAMED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this )
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("NAMED_ACCESS($ref)")
            }
        }

        override fun visitQualifiedAccessExpression(access: CfirQualifiedAccessExpression ) {
            val ref = renderReference(access.calleeReference)
            if (access.explicitReceiver != null) {
                println("QUALIFIED_ACCESS($ref) {")
                printer.pushIndent()
                println("receiver:")
                printer.pushIndent()
                access.explicitReceiver!!.accept(this )
                printer.popIndent()
                printer.popIndent()
                println("}")
            } else {
                println("QUALIFIED_ACCESS($ref)")
            }
        }

        override fun visitAssignment(assignment: CfirAssignment ) {
            println("ASSIGNMENT {")
            printer.pushIndent()
            println("lValue:")
            printer.pushIndent()
            assignment.lValue.accept(this )
            printer.popIndent()
            println("rValue:")
            printer.pushIndent()
            assignment.rValue.accept(this )
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitBinaryOp(binaryOp: CfirBinaryOp ) {
            println("BINARY_OP(${binaryOp.kind.name}) {")
            printer.pushIndent()
            binaryOp.left.accept(this )
            binaryOp.right.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitComparisonExpression(comparison: CfirComparisonExpression ) {
            println("COMPARISON(${comparison.operation.name}) {")
            printer.pushIndent()
            comparison.left.accept(this )
            comparison.right.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitTypeOperator(typeOperator: CfirTypeOperator ) {
            println("TYPE_OP(${typeOperator.operation.name}, ${renderType(typeOperator.typeRef)}) {")
            printer.pushIndent()
            typeOperator.argument.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitIfExpression(ifExpression: CfirIfExpression ) {
            println("IF {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            ifExpression.condition.accept(this )
            printer.popIndent()
            println("then:")
            printer.pushIndent()
            ifExpression.thenBranch.accept(this )
            printer.popIndent()
            ifExpression.elseBranch?.let {
                println("else:")
                printer.pushIndent()
                it.accept(this )
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitMatchExpression(matchExpression: CfirMatchExpression ) {
            println("MATCH {")
            printer.pushIndent()
            matchExpression.subject?.let {
                println("subject:")
                printer.pushIndent()
                it.accept(this )

                printer.popIndent()
            }

            matchExpression.branches.forEach { it.accept(this ) }
            printer.popIndent()
            println("}")
        }

        override fun visitMatchBranch(matchBranch: CfirMatchBranch ) {
            val guardSuffix = matchBranch.guard
                ?.let { " where ${inlineExpressionRenderer?.render(it) ?: "<guard>"}" }
                ?: ""
            println("BRANCH(${renderPattern(matchBranch.pattern)}$guardSuffix) {")
            printer.pushIndent()
            matchBranch.body.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitLoopExpression(loop: CfirLoopExpression ) {
            val kind = if (loop.isDoWhile) "DO_WHILE" else "WHILE"
            println("$kind {")
            printer.pushIndent()
            println("condition:")
            printer.pushIndent()
            loop.condition.accept(this )
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            loop.body.accept(this )
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitForInExpression(forIn: CfirForInExpression ) {
            val variableName = renderPatternVariableName(forIn.variable)
            println("FOR_IN($variableName: ${renderType(forIn.variable.returnTypeRef)}) {")
            printer.pushIndent()
            println("iterable:")
            printer.pushIndent()
            forIn.iterable.accept(this )
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            forIn.body.accept(this )
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitReturnExpression(returnExpression: CfirReturnExpression ) {
            if (returnExpression.result != null) {
                println("RETURN {")
                printer.pushIndent()
                returnExpression.result!!.accept(this )
                printer.popIndent()
                println("}")
            } else {
                println("RETURN")
            }
        }

        override fun visitJumpExpression(jump: CfirJumpExpression ) {
            println(jump.kind.name)
        }

        override fun visitThrowExpression(throwExpression: CfirThrowExpression ) {
            println("THROW {")
            printer.pushIndent()
            throwExpression.exception.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitTryExpression(tryExpression: CfirTryExpression ) {
            println("TRY {")
            printer.pushIndent()
            println("try:")
            printer.pushIndent()
            tryExpression.tryBlock.accept(this )
            printer.popIndent()
            tryExpression.catches.forEach { catchClause ->
                println("catch(${catchClause.parameter.name.asString()}: ${renderType(catchClause.parameter.returnTypeRef)}):")
                printer.pushIndent()
                catchClause.body.accept(this )
                printer.popIndent()
            }
            tryExpression.finallyBlock?.let {
                println("finally:")
                printer.pushIndent()
                it.accept(this )
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression ) {
            val lambda = anonymousFunctionExpression
            val params = lambda.anonymousFunction.valueParameters.joinToString {
                "${it.name.asString()}: ${renderType(it.returnTypeRef)}"
            }
            println("LAMBDA($params) {")
            printer.pushIndent()
            lambda.anonymousFunction.body?.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitRangeExpression(range: CfirRangeExpression ) {
            val op = if (range.isInclusive) "..=" else ".."
            println("RANGE($op) {")
            printer.pushIndent()
            range.start.accept(this )
            range.end.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitArrayLiteral(array: CfirArrayLiteral ) {
            println("ARRAY_LITERAL {")
            printer.pushIndent()
            array.elements.forEach { it.accept(this ) }
            printer.popIndent()
            println("}")
        }

        override fun visitTupleLiteral(tuple: CfirTupleLiteral ) {
            println("TUPLE_LITERAL {")
            printer.pushIndent()
            tuple.elements.forEach { it.accept(this ) }
            printer.popIndent()
            println("}")
        }

        override fun visitSpawnExpression(spawn: CfirSpawnExpression ) {
            println("SPAWN {")
            printer.pushIndent()
            spawn.body.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression ) {
            println("SYNCHRONIZED {")
            printer.pushIndent()
            println("monitor:")
            printer.pushIndent()
            synchronizedExpression.monitor.accept(this )
            printer.popIndent()
            println("body:")
            printer.pushIndent()
            synchronizedExpression.body.accept(this )
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitUnsafeExpression(unsafeExpression: CfirUnsafeExpression ) {
            println("UNSAFE {")
            printer.pushIndent()
            unsafeExpression.body.accept(this )
            printer.popIndent()
            println("}")
        }

        override fun visitQuoteExpression(quoteExpression: CfirQuoteExpression ) {
            println("QUOTE(\"${quoteExpression.rawText}\") {")
            printer.pushIndent()
            if (quoteExpression.interpolations.isNotEmpty()) {
                println("interpolations:")
                printer.pushIndent()
                quoteExpression.interpolations.forEach { it.accept(this ) }
                printer.popIndent()
            }
            printer.popIndent()
            println("}")
        }

        override fun visitMacroExpression(macroExpression: CfirMacroExpression ) {
            val name = macroExpression.name?.asString() ?: "<macro>"
            println("MACRO_EXPR($name) {")
            printer.pushIndent()
            macroExpression.attrText?.let { println("attr: \"$it\"") }
            macroExpression.inputText?.let { println("input: \"$it\"") }
            printer.popIndent()
            println("}")
        }

        override fun visitSubscriptExpression(subscript: CfirSubscriptExpression ) {
            println("SUBSCRIPT {")
            printer.pushIndent()
            println("receiver:")
            printer.pushIndent()
            subscript.receiver.accept(this   )
            printer.popIndent()
            println("indices:")
            printer.pushIndent()
            subscript.indices.forEach { it.accept(this ) }
            printer.popIndent()
            printer.popIndent()
            println("}")
        }

        override fun visitErrorExpression(errorExpression: CfirErrorExpression ) {
            errorExpressionRenderer?.renderErrorExpression(errorExpression)

        }
    }
}
