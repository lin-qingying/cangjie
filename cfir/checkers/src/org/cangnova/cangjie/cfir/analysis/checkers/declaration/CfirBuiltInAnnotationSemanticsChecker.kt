package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemanticsSupport
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement

/**
 * 统一承接当前 CFIR 中已经具备稳定建模基础的内建注解/平台注解规则。
 *
 * 设计原则：
 * - 只处理“声明自身即可判断”的规则，不把解析/推断期问题塞进这里；
 * - 优先复用已解析的 typeRef / superTypeRef，而不是重新发明绑定逻辑；
 * - 对尚未结构化建模的 APILevel/Hide/IfAvailable，使用 PSI 注解项做结构化检查，
 *   保持规则集中在 declaration checker 层，而不是散落到解析器或测试侧。
 */
object CfirBuiltInAnnotationDeclarationChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val owner = declaration.source?.psi as? CjModifierListOwner ?: return
        checkAnnotationMetaRules(declaration, owner)
        checkPlatformAnnotationSyntax(declaration, owner)
    }
}

/**
 * 互操作注解语义检查。
 *
 * 这组规则依赖 class-like 声明的父类型关系与成员签名，因此集中放在 classLike checker。
 */
object CfirInteropAnnotationChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        checkJavaInteropSemantics(declaration)
        checkObjCInteropSemantics(declaration)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkAnnotationMetaRules(
    declaration: CfirDeclaration,
    owner: CjModifierListOwner,
) {
    val annotationEntry = owner.findAnnotationEntry(ANNOTATION) ?: return

    if (annotationEntry.valueArguments.isNotEmpty()) {
        val arguments = annotationEntry.valueArguments
        val targetArgument = arguments.singleOrNull()
        val isValidTargetArgument =
            targetArgument != null &&
                targetArgument.isNamed() &&
                targetArgument.getArgumentName()?.asName?.asString() == "target"

        if (!isValidTargetArgument) {
            reporter.reportOn(
                source = annotationEntry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.ANNOTATION_ARG_TARGET,
            )
        } else {
            val expression = targetArgument.getArgumentExpression()
            if (expression !is CjCollectionLiteralExpression) {
                reporter.reportOn(
                    source = annotationEntry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.ANNOTATION_ARG_TARGET_ARRAY_LIT,
                )
            }
        }
    }

    if (declaration is CfirClassLikeDeclaration && !declaration.isPublicLike()) {
        reporter.reportOn(
            source = annotationEntry.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.ANNOTATION_NON_PUBLIC,
        )
    }

    if (owner.hasAnnotationEntry(JAVA)) {
        reporter.reportOn(
            source = annotationEntry.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.DEFINE_JAVA_ANNOTATION,
        )
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkPlatformAnnotationSyntax(
    declaration: CfirDeclaration,
    owner: CjModifierListOwner,
) {
    val apiLevelEntries = owner.findAnnotationEntries(API_LEVEL)
    if (apiLevelEntries.size > 1) {
        reporter.reportOn(
            source = apiLevelEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.APILEVEL_MULTI_ANNO,
        )
    }
    if (apiLevelEntries.isNotEmpty()) {
        val seenSyscaps = linkedSetOf<String>()
        for (entry in apiLevelEntries) {
            val args = entry.valueArguments
            val sinceArgument = args.firstOrNull { argument ->
                argument.isNamed() && argument.getArgumentName()?.asName?.asString() == "since"
            }
            if (sinceArgument == null) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MISSING_ARG,
                    a = Name.identifier("since"),
                )
            }

            for (argument: ValueArgument in args) {
                val expression = argument.getArgumentExpression()
                if (expression != null && !expression.isLiteralLike()) {
                    reporter.reportOn(entry.toSourceOrDeclarationSource(declaration), CfirErrors.ONLY_LITERAL_SUPPORT, "annotation")
                }
            }

            val syscapLiteral = args.firstOrNull { argument ->
                argument.isNamed() && argument.getArgumentName()?.asName?.asString() == "syscap"
            }?.getArgumentExpression()?.literalStringOrNull()
            if (syscapLiteral != null && !seenSyscaps.add(syscapLiteral)) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MULTI_DIFF_SYSCAP,
                )
            }
        }
    }

    val ifAvailableEntries = owner.findAnnotationEntries(IF_AVAILABLE)
    for (entry in ifAvailableEntries) {
        val firstArgument = entry.valueArguments.firstOrNull()
        if (firstArgument != null && !firstArgument.isNamed()) {
            reporter.reportOn(
                source = entry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.IFAVAILABLE_ARG_NO_NAME,
            )
        }
        if (firstArgument?.getArgumentExpression()?.isLiteralLike() == false) {
            reporter.reportOn(
                source = entry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL,
            )
        }

        entry.valueArguments
            .filter(ValueArgument::isNamed)
            .firstOrNull { argument: ValueArgument ->
                argument.getArgumentName()?.asName?.asString() !in allowedIfAvailableArgumentNames
            }
            ?.let { argument ->
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.IFAVAILABLE_UNKNOWN_ARG_NAME,
                    a = argument.getArgumentName()?.asName?.asString().orEmpty(),
                )
            }
    }

    val hideEntries = owner.findAnnotationEntries(HIDE)
    if (hideEntries.size > 1) {
        reporter.reportOn(
            source = hideEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.HIDE_MULTI_ANNOTATION,
        )
    }
    if (declaration.source?.psi is CjParameter && hideEntries.isNotEmpty()) {
        reporter.reportOn(
            source = hideEntries.first().toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.HIDE_AT_FUNC_PARAM,
        )
    }
    hideEntries.firstOrNull()?.let { hideEntry ->
        if (owner.annotationEntries.lastOrNull() != hideEntry) {
            reporter.reportOn(
                source = hideEntry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.HIDE_MUST_AT_END,
                a = HIDE.asString(),
            )
        }

        hideEntry.valueArguments.firstOrNull()?.let { argument ->
            val isCheckedName = argument.getArgumentName()?.asName?.asString() == "isChecked"
            val isBoolLiteral = argument.getArgumentExpression()?.isBooleanLiteral() == true
            if (!argument.isNamed() || !isCheckedName || !isBoolLiteral) {
                reporter.reportOn(
                    source = hideEntry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.HIDE_DIFF_PARAM,
                    a = "unexpected",
                )
            }
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaInteropSemantics(declaration: CfirClassLikeDeclaration) {
    val hasJavaMirror = declaration.hasAnnotation(JAVA_MIRROR)
    val hasJavaImpl = declaration.hasAnnotation(JAVA_IMPL)
    val superDeclarations = declaration.superDeclarations()

    if (!hasJavaMirror && !hasJavaImpl) {
        if (superDeclarations.any { it.hasAnnotation(JAVA_MIRROR) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED,
                a = declaration.name,
            )
        }
        return
    }

    if (hasJavaMirror) {
        if (superDeclarations.any { it is org.cangnova.cangjie.cfir.declarations.CfirInterface }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE,
            )
        }
        if (superDeclarations.any { !it.hasAnyAnnotation(JAVA_MIRROR, JAVA_IMPL) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE,
            )
        }
        checkJavaMirrorMemberTypes(declaration)
    }

    if (hasJavaImpl) {
        if (superDeclarations.any { it is org.cangnova.cangjie.cfir.declarations.CfirInterface }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE,
            )
        }
        if (superDeclarations.none { it.hasAnnotation(JAVA_MIRROR) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR,
            )
        }
        if (superDeclarations.any { !it.hasAnyAnnotation(JAVA_MIRROR, JAVA_IMPL) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE,
            )
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaMirrorMemberTypes(declaration: CfirClassLikeDeclaration) {
    for (member in declaration.declarations) {
        when (member) {
            is CfirConstructor -> {
                if (member.valueParameters.any { !it.returnTypeRef.isInteropMirrorCompatible(JAVA_MIRROR, JAVA_IMPL) }) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            is CfirFunction -> {
                if (member.valueParameters.any { !it.returnTypeRef.isInteropMirrorCompatible(JAVA_MIRROR, JAVA_IMPL) }) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            is CfirProperty -> {
                if (!member.returnTypeRef.isInteropMirrorCompatible(JAVA_MIRROR, JAVA_IMPL)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            else -> Unit
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCInteropSemantics(declaration: CfirClassLikeDeclaration) {
    val hasObjCMirror = declaration.hasAnnotation(OBJC_MIRROR)
    val hasObjCImpl = declaration.hasAnnotation(OBJC_IMPL)
    val superDeclarations = declaration.superDeclarations()

    if (!hasObjCMirror && !hasObjCImpl) {
        if (superDeclarations.any { it.hasAnnotation(OBJC_MIRROR) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED,
            )
        }
        return
    }

    if (hasObjCMirror && superDeclarations.any { !it.hasAnnotation(OBJC_MIRROR) }) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_MIRROR_MUST_INHERIT_MIRROR,
        )
    }

    if (hasObjCImpl && superDeclarations.none { it.hasAnnotation(OBJC_MIRROR) }) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS,
        )
    }

    for (member in declaration.declarations) {
        when (member) {
            is CfirNamedFunction -> {
                if (member.valueParameters.size > 1 && !member.hasAnnotation(FOREIGN_NAME)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_METHOD_MUST_HAVE_FOREIGN_NAME,
                        a = "ObjC",
                        b = member.name,
                    )
                }
            }

            is CfirConstructor -> {
                if (member.valueParameters.size > 1 && !member.hasAnnotation(FOREIGN_NAME)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_CTOR_MUST_HAVE_FOREIGN_NAME,
                        a = "ObjC",
                    )
                }
            }

            else -> Unit
        }
    }
}

private fun CfirDeclaration.hasAnnotation(annotationName: Name): Boolean {
    val owner = source?.psi as? CjModifierListOwner
    return owner?.hasAnnotationEntry(annotationName) == true
}

private fun CfirDeclaration.hasAnyAnnotation(vararg annotationNames: Name): Boolean =
    annotationNames.any(::hasAnnotation)

context(context: CheckerContext)
private fun CfirClassLikeDeclaration.superDeclarations(): List<CfirClassLikeDeclaration> {
    val typeStatement = source?.psi as? CjTypeStatement
    val superEntries = typeStatement?.superTypeListEntries.orEmpty()
    val resolved = mutableListOf<CfirClassLikeDeclaration>()
    for (superTypeRef in superTypeRefs) {
        val classId = CfirExtendSemanticsSupport.run { superTypeRef.toClassIdOrNull() } ?: continue
        val target = CfirExtendSemanticsSupport.resolveDeclaration(context, classId) ?: continue
        resolved += target
    }
    // superTypeRefs 更可信；只有在 CFIR 还未绑定齐时才退回 PSI 数量。
    if (resolved.isNotEmpty() || superEntries.isEmpty()) return resolved
    return emptyList()
}

context(context: CheckerContext)
private fun CfirTypeRef.isInteropMirrorCompatible(
    vararg requiredAnnotations: Name,
): Boolean {
    val resolvedType = this as? CfirResolvedTypeRef ?: return true
    if (resolvedType.coneType is ConePrimitiveType) return true
    val classId = resolvedType.coneType.classIdOrPrimitiveClassId ?: return true
    val declaration = CfirExtendSemanticsSupport.resolveDeclaration(context, classId) ?: return true
    return declaration.hasAnyAnnotation(*requiredAnnotations)
}

private fun CfirClassLikeDeclaration.isPublicLike(): Boolean =
    status.visibility.externalDisplayName == "public"

private fun CjModifierListOwner.findAnnotationEntries(annotationName: Name): List<CjAnnotation> =
    annotationEntries.filter { entry -> entry.shortName == annotationName }

private fun CjModifierListOwner.findAnnotationEntry(annotationName: Name): CjAnnotation? =
    findAnnotationEntries(annotationName).firstOrNull()

private fun CjModifierListOwner.hasAnnotationEntry(annotationName: Name): Boolean =
    findAnnotationEntry(annotationName) != null

private fun CjAnnotation.toSourceOrDeclarationSource(declaration: CfirDeclaration): org.cangnova.cangjie.source.CjSourceElement? =
    this.toCjPsiSourceElement() ?: declaration.source

private fun CjExpression.isLiteralLike(): Boolean = when (this) {
    is CjConstantExpression -> true
    is CjStringTemplateExpression -> !hasInterpolation()
    is CjCollectionLiteralExpression -> innerExpressions.all { it.isLiteralLike() }
    else -> false
}

private fun CjExpression.isBooleanLiteral(): Boolean =
    this is CjConstantExpression && text in setOf("true", "false")

private fun CjExpression.literalStringOrNull(): String? = when (this) {
    is CjConstantExpression -> text
    is CjStringTemplateExpression -> if (!hasInterpolation()) text else null
    else -> null
}

private fun CjStringTemplateExpression.hasInterpolation(): Boolean =
    entries.any { entry -> entry !is org.cangnova.cangjie.psi.CjLiteralStringTemplateEntry }

private val ANNOTATION = Name.identifier("Annotation")
private val JAVA = Name.identifier("Java")
private val JAVA_MIRROR = Name.identifier("JavaMirror")
private val JAVA_IMPL = Name.identifier("JavaImpl")
private val OBJC_MIRROR = Name.identifier("ObjCMirror")
private val OBJC_IMPL = Name.identifier("ObjCImpl")
private val FOREIGN_NAME = Name.identifier("ForeignName")
private val API_LEVEL = Name.identifier("APILevel")
private val IF_AVAILABLE = Name.identifier("IfAvailable")
private val HIDE = Name.identifier("Hide")

private val allowedIfAvailableArgumentNames: Set<String> = setOf(
    "level",
    "since",
    "syscap",
)
