package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/** 对齐官方 CFFICheck：目标/作用域使用同一 resolved annotation 与 ABI 状态。 */
object CfirCAnnotationChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val enclosing = context.containingDeclarations.map { it.cfir }.filter { it !== declaration }
        // `extend` is a declaration container even though it is not a
        // CfirClassLikeDeclaration. Its members are not top-level for the
        // official CFFI scope checks and must not inherit top-level legality.
        val topLevel = enclosing.none {
            it is CfirClassLikeDeclaration || it is CfirExtend || it is CfirFunction || it is CfirProperty
        }
        for (annotation in declaration.annotations.filterIsInstance<CfirAnnotationCall>()) {
            val kind = annotation.annotationKind ?: continue
            val descriptor = annotation.builtInDescriptor ?: continue
            val label = "@"+descriptor.sourceName
            val source = annotation.source
            when (kind) {
                BuiltInAnnotationKind.C -> {
                    if (declaration is CfirFunction && declaration.typeParameters.isNotEmpty()) {
                        reporter.reportOn(declaration.typeParameterListDiagnosticSource(), CfirErrors.CFFI_CANNOT_HAVE_TYPE_PARAM, "CFunc")
                    }
                    if (declaration !is CfirFunction && declaration !is CfirStruct) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_USE_OF_ANNOTATION, declaration.kindDescription(), label)
                    } else if (!topLevel) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_SCOPE_USE_OF_ANNOTATION, label)
                    }
                }
                BuiltInAnnotationKind.CALLING_CONV -> {
                    if (!topLevel) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_SCOPE_USE_OF_ANNOTATION, label)
                        continue
                    }
                    if (declaration !is CfirFunction || (!declaration.status.isForeign && !declaration.status.isC)) {
                        reporter.reportOn(source, CfirErrors.ONLY_CFUNC_CAN_USE_ANNOTATION, label)
                        continue
                    }
                    if (annotation.argumentCount() != 1) {
                        reporter.reportOn(source, CfirErrors.ANNOTATION_ERROR_ARG_NUM, label, "one")
                        continue
                    }
                    val convention = annotation.stringArgument("convention")
                    when {
                        convention == null -> reporter.reportOn(source, CfirErrors.ANNOTATION_INVALID_ARGS_TYPE, label)
                        CangjieCallingConvention.entries.none { it.name == convention } ->
                            reporter.reportOn(source, CfirErrors.ANNOTATION_CALLING_CONV_NOT_SUPPORT, convention)
                    }
                }
                BuiltInAnnotationKind.FASTNATIVE -> {
                    if (declaration !is CfirFunction || !declaration.status.isForeign) {
                        val prefix = if (declaration is CfirFunction) "non-foreign " else ""
                        reporter.reportOn(source, CfirErrors.ILLEGAL_USE_OF_ANNOTATION, prefix + declaration.kindDescription(), label)
                    } else if (!topLevel) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_SCOPE_USE_OF_ANNOTATION, label)
                    }
                }
                BuiltInAnnotationKind.FROZEN -> {
                    if (declaration !is CfirFunction && declaration !is CfirProperty) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_USE_OF_ANNOTATION, declaration.kindDescription(), label)
                    } else if (enclosing.any { it is CfirFunction }) {
                        reporter.reportOn(source, CfirErrors.ILLEGAL_USE_OF_ANNOTATION, "local function", label)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun CfirDeclaration.kindDescription(): String = when (this) {
        is CfirConstructor -> "constructor"
        is CfirFunction -> "function"
        is CfirProperty -> "property"
        is CfirStruct -> "struct"
        is CfirClass -> "class"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        is CfirVariable -> "variable"
        else -> "declaration"
    }
}

/** C struct 的所有直接字段使用统一 CType 判断，Unit 单独诊断。 */
object CfirCStructFieldChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirStruct || !declaration.status.isC) return
        for (field in declaration.declarations.filterIsInstance<CfirFieldVariable>()) {
            val type = field.returnTypeRef.coneTypeOrNull?.fullyExpandedType(context.session) ?: continue
            if (type is ConeErrorType) continue
            val nameSource = field.fieldVariableNameDiagnosticSource() ?: continue
            val typeSource = field.returnTypeRef.source
            val source = CjOffsetsOnlySourceElement(nameSource.startOffset, typeSource?.endOffset ?: nameSource.endOffset)
            when {
                type.isUnit -> reporter.reportOn(source, CfirErrors.CSTRUCT_CANNOT_HAVE_UNIT_FIELDS)
                !CfirCTypeSemantics.isMetCType(context.session, type) ->
                    reporter.reportOn(source, CfirErrors.ILLEGAL_MEMBER_OF_CSTRUCT, field.name, declaration.name)
            }
        }
    }
}
