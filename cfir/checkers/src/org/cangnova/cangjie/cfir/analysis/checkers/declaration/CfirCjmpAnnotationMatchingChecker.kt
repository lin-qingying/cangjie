package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirDiagnosticRenderers
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForSpecific
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationMetadataOwner
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpAnnotationPresentation
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticArgument
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpRelatedInformationMessage
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedInformation
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.diagnostics.withRelatedInformation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchingContext
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.cjmpCommonCounterpartsOf
import org.cangnova.cangjie.cfir.session.cjmpFamilySnapshotFor
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpAnnotationMatchChecker
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMatchingIssue
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMismatchSide

/**
 * 已配对声明的注解兼容性检查。
 *
 * 注解是 specific 编译期检查，独立注册为 Platform checker；共享匹配逻辑只接收注解事实，
 * CFIR 层负责枚举已发布配对并将结构化问题投影为诊断和 related information。
 */
object CfirCjmpAnnotationMatchingChecker : CfirBasicDeclarationChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return

        val specificDeclarations = when (declaration) {
            is CfirEnumConstructor -> {
                val enum = context.findClosestDeclaration<CfirEnum>() ?: return
                if (!enum.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return
                listOf(declaration)
            }

            is CfirPatternVariable -> {
                if (!declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return
                declaration.pattern.bindingVariables().filter { it.symbol.cjmpMatchResultForSpecific() != null }
            }

            is CfirMemberDeclaration -> {
                if (declaration is CfirTypeAlias ||
                    !declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
                ) return
                listOf(declaration)
            }

            else -> return
        }

        if (declaration is CfirExtend &&
            context.session.cjmpFamilySnapshotFor(declaration)?.isDuplicateSpecificExtend(declaration) == true
        ) return

        for (specific in specificDeclarations) {
            if (specific !is CfirEnumConstructor &&
                !specific.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
            ) continue
            if (specific.symbol.cjmpMatchResultForSpecific() == null) continue
            for (common in context.session.cjmpCommonCounterpartsOf(specific)) {
                if (common !is CfirMemberDeclaration) continue
                checkMatched(specific, common)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMatched(specific: CfirDeclaration, common: CfirMemberDeclaration) {
        val specificSource = specific.cjmpVariableDiagnosticOwner().source
        val compatibilityContext = CfirCjmpMatchingContext(context.session)
        val issues = AbstractCjmpAnnotationMatchChecker.checkAnnotations(
            specific.symbol,
            common.symbol,
            compatibilityContext,
        )

        for (issue in issues) {
            when (issue) {
                is CjmpAnnotationMatchingIssue.DeprecatedAnnotation -> {
                    reporter.reportOn(
                        source = issue.annotation.source ?: specificSource,
                        factory = CfirErrors.SPECIFIC_HAS_DEPRECATED_ANNOTATION,
                        a = Name.identifier(requireNotNull(
                            BuiltInAnnotationRegistry.findLanguageBuiltIn(BuiltInAnnotationKind.DEPRECATED)?.sourceName,
                        )),
                        b = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
                        c = cjmpDeclName(specific),
                        context = context.withRelatedInformation(
                            listOf(
                                CjDiagnosticRelatedInformation(
                                    element = null,
                                    message = CfirDiagnosticRenderers.CJMP_RELATED_INFORMATION.render(
                                        CfirCjmpRelatedInformationMessage.DeprecatedAnnotationInheritedFromCommon,
                                    ),
                                ),
                            ),
                        ),
                    )
                }

                is CjmpAnnotationMatchingIssue.AnnotationMismatch -> {
                    val relatedInformation = when (issue.side) {
                        CjmpAnnotationMismatchSide.COMMON -> listOf(
                            CjDiagnosticRelatedInformation(
                                element = null,
                                message = CfirDiagnosticRenderers.CJMP_RELATED_INFORMATION.render(
                                    CfirCjmpRelatedInformationMessage.MismatchedAnnotation(
                                        issue.annotation.toDiagnosticPresentation(),
                                    ),
                                ),
                            ),
                        )

                        CjmpAnnotationMismatchSide.SPECIFIC -> listOf(
                            common.relatedInformation(
                                CfirDiagnosticRenderers.CJMP_RELATED_INFORMATION.render(
                                    CfirCjmpRelatedInformationMessage.CorrespondingCommonDeclaration,
                                ),
                            ),
                            CjDiagnosticRelatedInformation(
                                element = null,
                                message = CfirDiagnosticRenderers.CJMP_RELATED_INFORMATION.render(
                                    CfirCjmpRelatedInformationMessage.AnnotationCorrectionAdvice,
                                ),
                            ),
                        )
                    }
                    val source = when (issue.side) {
                        CjmpAnnotationMismatchSide.COMMON -> specificSource
                        CjmpAnnotationMismatchSide.SPECIFIC -> issue.annotation.source ?: specificSource
                    }
                    reporter.reportOn(
                        source = source,
                        factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION,
                        a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
                        context = context.withRelatedInformation(relatedInformation),
                    )
                }

                is CjmpAnnotationMatchingIssue.AnnotationAttributeMismatch -> {
                    reporter.reportOn(
                        source = specificSource,
                        factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION,
                        a = CfirCjmpDiagnosticArgument.DeclarationKind(cjmpDeclarationKind(specific)),
                        context = context.withRelatedInformation(
                            listOf(
                                CjDiagnosticRelatedInformation(
                                    element = null,
                                    message = CfirDiagnosticRenderers.CJMP_RELATED_INFORMATION.render(
                                        CfirCjmpRelatedInformationMessage.MismatchedAnnotationAttribute(
                                            kind = issue.kind,
                                            annotation = issue.annotation?.toDiagnosticPresentation(),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
                }

            }
        }
    }
}
private fun CfirAnnotation.toDiagnosticPresentation(): CfirCjmpAnnotationPresentation {
    val call = this as? CfirAnnotationCall
    val sourceName = call?.annotationSourceName
        ?: call?.builtInDescriptor?.sourceName
        ?: call?.platformAnnotationDescriptor?.sourceName
        ?: annotationClassId?.shortClassName?.asString()
        ?: error("A CJMP annotation mismatch must retain its annotation identity")
    return CfirCjmpAnnotationPresentation(this, sourceName)
}

internal fun CfirDeclaration.cjmpVariableDiagnosticOwner(): CfirDeclaration {
    if (this !is CfirPatternBindingVariable || isLocal) return this
    val owner = symbol.getDeclarationMetadataOwner() as CfirPatternVariable
    val pattern = owner.pattern
    return if (pattern is org.cangnova.cangjie.cfir.patterns.CfirBindingPattern &&
        pattern.nestedPattern == null && pattern.bindingVariable === this
    ) {
        owner
    } else {
        this
    }
}
