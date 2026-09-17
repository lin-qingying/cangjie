package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentViewEntry
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression

/**
 * `@Deprecated` 的 parser 级参数与目标检查。
 *
 * 官方 parser 不把该注解当成可任意调用的普通构造器：所有显式实参必须是
 * 字面量常量，message/since/strict 各自只有一个槽位，且 extend、main、静态
 * 构造器以及不满足默认参数约束的参数不是合法目标。此 checker 只消费已经
 * 发布的 annotation argument view，不从源码文本重解析参数绑定。
 */
object CfirDeprecatedAnnotationChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val entries = declaration.findBuiltinAnnotations(BuiltInAnnotationKind.DEPRECATED)
            .filterIsInstance<CfirAnnotationCall>()
        if (entries.isEmpty()) return

        entries.forEach { annotation ->
            checkArguments(annotation)
            invalidTarget(declaration)?.let { target ->
                reporter.reportOn(
                    source = annotation.source ?: declaration.source,
                    factory = CfirErrors.DEPRECATED_INVALID_TARGET,
                    a = target,
                )
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkArguments(annotation: CfirAnnotationCall) {
        val explicitEntries = annotation.argumentView?.entries
            ?.filter { !it.isDefaultOrigin && it.argument != null }
            ?: annotation.argumentList.arguments.mapIndexed { index, argument ->
                CfirAnnotationArgumentViewEntry(
                    sourceOrder = index,
                    explicitName = (argument as? CfirNamedArgumentExpression)?.argumentName,
                    argument = argument,
                    resolvedParameter = null,
                    status = CfirAnnotationArgumentStatus.UNRESOLVED,
                    isDefaultOrigin = false,
                    constantExpression = null,
                    source = argument.source,
                )
            }

        val seen = linkedSetOf<String>()
        for (entry in explicitEntries) {
            val argument = entry.argument ?: continue
            val value = (argument as? CfirNamedArgumentExpression)?.expression ?: argument
            val valueSource = value.source ?: entry.source ?: annotation.source
            if (value !is CfirLiteralExpression) {
                reporter.reportOn(
                    source = entry.source ?: annotation.source,
                    factory = CfirErrors.DEPRECATED_ARGUMENTS_MUST_BE_LITERAL_CONST,
                )
                // Official CheckDeprecatedAnnotation returns immediately after this
                // diagnostic, so later arguments must not produce secondary errors.
                return
            }

            val explicitName = entry.explicitName?.asString()
            val argumentName = explicitName ?: MESSAGE_ARGUMENT
            if (!seen.add(argumentName)) {
                reporter.reportOn(
                    source = valueSource,
                    factory = CfirErrors.DEPRECATED_ARGUMENT_DUPLICATION,
                    a = argumentName,
                )
                continue
            }

            val expectedType = when (explicitName) {
                null, MESSAGE_ARGUMENT, SINCE_ARGUMENT -> STRING_TYPE
                STRICT_ARGUMENT -> BOOLEAN_TYPE
                else -> null
            }
            if (expectedType == null) {
                reporter.reportOn(
                    source = entry.source ?: annotation.source,
                    factory = CfirErrors.DEPRECATED_UNKNOWN_ARGUMENT,
                    a = argumentName,
                )
                continue
            }

            if (value.kind == CfirLiteralKind.STRING && value.value == "") {
                reporter.reportOn(
                    source = valueSource,
                    factory = CfirErrors.DEPRECATED_EMPTY_STRING_ARGUMENT,
                    a = argumentName,
                )
            } else if ((expectedType == STRING_TYPE && value.kind != CfirLiteralKind.STRING) ||
                (expectedType == BOOLEAN_TYPE && value.kind != CfirLiteralKind.BOOLEAN)
            ) {
                reporter.reportOn(
                    source = valueSource,
                    factory = CfirErrors.DEPRECATED_WRONG_ARGUMENT,
                    a = argumentName,
                    b = expectedType,
                )
            }
        }
    }

    private fun invalidTarget(declaration: CfirDeclaration): String? = when (declaration) {
        is CfirExtend -> EXTEND_TARGET
        is CfirMainFunction -> MAIN_TARGET
        is CfirFinalizer -> FINALIZER_TARGET
        is CfirConstructor -> STATIC_CONSTRUCTOR_TARGET.takeIf { declaration.status.isStatic }
        is CfirValueParameter -> when {
            !declaration.isNamed -> NOT_NAMED_PARAMETER_TARGET
            declaration.defaultValue == null -> PARAMETER_WITHOUT_DEFAULT_TARGET
            else -> null
        }
        else -> null
    }

    private const val MESSAGE_ARGUMENT = "message"
    private const val SINCE_ARGUMENT = "since"
    private const val STRICT_ARGUMENT = "strict"
    private const val STRING_TYPE = "String"
    private const val BOOLEAN_TYPE = "Bool"
    private const val EXTEND_TARGET = "extend"
    private const val MAIN_TARGET = "main"
    private const val FINALIZER_TARGET = "~init"
    private const val STATIC_CONSTRUCTOR_TARGET = "static constructor"
    private const val NOT_NAMED_PARAMETER_TARGET = "Not named parameter"
    private const val PARAMETER_WITHOUT_DEFAULT_TARGET = "Parameter without default value"
}
