package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAvailabilityProvider
import org.cangnova.cangjie.cfir.declarations.CfirHideAnnotationState
import org.cangnova.cangjie.cfir.declarations.CfirPlatformAnnotationClassIds
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.declarationAvailabilityProvider
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.annotationVersionSupport
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.expressions.stringArgument
import org.cangnova.cangjie.cfir.analysis.diagnostics.literalConversionMismatch
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.CfirObjCTypeSemantics
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.type.AbstractTypeChecker

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
    /**
     * 对所有基础声明执行内建注解和平台注解规则。
     *
     * 入口按互不依赖的规则簇顺序调用：注解元规则、平台注解语法、CallingConv 使用范围、
     * 以及 ForeignName 相关冲突。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        checkAnnotationMetaRules(declaration)
        checkAnnotationTargetConstants(declaration)
        if (reportUnsupportedPlatformAnnotationVersions(declaration)) return
        checkPlatformAnnotationSyntax(declaration)
        checkForeignNameRules(declaration)
    }
}

/**
 * 互操作注解语义检查。
 *
 * 这组规则依赖 class-like 声明的父类型关系与成员签名，因此集中放在 classLike checker。
 */
object CfirInteropAnnotationChecker : CfirClassLikeChecker() {
    /**
     * 对 class-like 声明执行 Java/Objective-C 互操作注解语义检查。
     *
     * 该入口会分别处理基础继承约束、类型级成员签名约束和额外互操作平台规则。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (hasUnsupportedPlatformAnnotationVersion(declaration)) return
        checkJavaInteropSemantics(declaration)
        checkJavaTypeDeclarationSemantics(declaration)
        checkJavaInteropExtraSemantics(declaration)
        checkObjCInteropSemantics(declaration)
    }
}

/**
 * Report a platform annotation which is newer than the current language/API
 * settings before running its semantic owner.  The annotation remains a
 * resolved platform identity; it is never reinterpreted as a custom call.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportUnsupportedPlatformAnnotationVersions(declaration: CfirDeclaration): Boolean {
    var reported = false
    declaration.annotations.filterIsInstance<CfirAnnotationCall>().forEach { annotation ->
        val descriptor = annotation.platformAnnotationDescriptor ?: return@forEach
        if (annotation.annotationVersionSupport(context.session.languageVersionSettings) == AnnotationVersionSupportStatus.SUPPORTED) return@forEach
        reporter.reportOn(
            source = annotation.source ?: declaration.source,
            factory = CfirErrors.UNSUPPORTED_FEATURE,
            a = descriptor.requiredLanguageFeature to context.session.languageVersionSettings,
        )
        reported = true
    }
    return reported
}

context(context: CheckerContext)
private fun hasUnsupportedPlatformAnnotationVersion(declaration: CfirDeclaration): Boolean {
    if (declaration.annotations.filterIsInstance<CfirAnnotationCall>().any { annotation ->
            annotation.annotationVersionSupport(context.session.languageVersionSettings)
                ?.let { it != AnnotationVersionSupportStatus.SUPPORTED } == true
        }
    ) return true

    val children = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.declarations
        is CfirExtend -> declaration.declarations
        else -> emptyList()
    }
    return children.any { child -> hasUnsupportedPlatformAnnotationVersion(child) }
}

/**
 * 检查 `@Annotation` 自身的元注解规则。
 *
 * 该规则覆盖 target 参数形态、注解声明可见性以及禁止定义 Java 注解的基础约束。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkAnnotationMetaRules(
    declaration: CfirDeclaration,
) {
    val annotationEntry = declaration.findBuiltinAnnotations(BuiltInAnnotationKind.ANNOTATION)
        .firstOrNull() as? CfirAnnotationCall ?: return

    if (annotationEntry.hasArguments()) {
        val isValidTargetArgument =
            annotationEntry.argumentCount() == 1 &&
                annotationEntry.hasNamedArgument("target")

        if (!isValidTargetArgument) {
            reporter.reportOn(
                source = annotationEntry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.ANNOTATION_ARG_TARGET,
            )
        } else {
            val expression = annotationEntry.argumentByName("target")
            if (expression !is org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral) {
                reporter.reportOn(
                    source = annotationEntry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.ANNOTATION_ARG_TARGET_ARRAY_LIT,
                )
            }
        }
    }

    if (declaration is CfirClassLikeDeclaration && !declaration.isPublicLike()) {
        reporter.reportOn(
            source = declaration.classLikeNameDiagnosticSource(),
            factory = CfirErrors.ANNOTATION_NON_PUBLIC,
        )
    }

    if (declaration.hasSupportedBuiltinAnnotation(
            context.languageVersionSettings,
            BuiltInAnnotationKind.JAVA,
        )) {
        reporter.reportOn(
            source = annotationEntry.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.DEFINE_JAVA_ANNOTATION,
        )
    }

    // Official ParseDecl::CheckAnnotationAnno rejects abstract/open/sealed classes.
    // The diagnostic is anchored at the builtin annotation entry in the CFIR surface,
    // matching the project JFFI normalization used by the annotation LLT fixtures.
    if (declaration is org.cangnova.cangjie.cfir.declarations.CfirClass) {
        val illegalModifier = when {
            declaration.status.isAbstract -> "abstract"
            declaration.status.isOpen -> "open"
            declaration.status.isSealed -> "sealed"
            else -> null
        }
        if (illegalModifier != null) {
            reporter.reportOn(
                source = annotationEntry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI,
                a = "Annotation",
                b = illegalModifier,
            )
        }
    }
}

/** 官方 ConstEvaluationChecker::ChkAnnotations 对 @Annotation target 数组的检查。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkAnnotationTargetConstants(declaration: CfirDeclaration) {
    val annotation = declaration.annotations
        .filterIsInstance<CfirAnnotationCall>()
        .firstOrNull { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.ANNOTATION }
        ?: return
    val target = annotation.argumentByName("target") as? org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
        ?: return
    val annotationKindType = ConeEnumType(StdlibClassIds.AnnotationKind.toLookupTag())
    for (element in target.elements) {
        val actualType = element.coneTypeOrNull?.fullyExpandedType(context.session) ?: continue
        if (actualType is ConeErrorType || actualType.classIdOrPrimitiveClassId == annotationKindType.classId) continue

        // 官方 `CheckAnnotationDecl` 在构造注解声明后检查
        // Array<AnnotationKind>。标量字面量继续走共享的字面量转换路径
        // (`CANNOT_CONVERT_LITERAL`)，非字面量值才归类为普通解析类型不匹配。
        val source = element.source ?: annotation.source ?: declaration.source ?: continue
        val literalMismatch = literalConversionMismatch(annotationKindType, element, context.session)
        if (literalMismatch != null) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.CANNOT_CONVERT_LITERAL,
                a = literalMismatch.literalDescription,
                b = literalMismatch.expectedType,
            )
        } else {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.TYPE_MISMATCH,
                a = annotationKindType,
                b = actualType,
                c = false,
            )
        }
    }
    checkConstAnnotationExpression(target)
}

/**
 * 检查 APILevel、Hide 等平台注解的声明级语法规则。
 *
 * 这里处理参数命名、字面量限制、多重注解限制以及 Hide override 继承关系等
 * 仅依赖声明与注解文本即可判断的约束。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkPlatformAnnotationSyntax(
    declaration: CfirDeclaration,
) {
    val availability = context.session.declarationAvailabilityProvider
    val apiLevelEntries = availability.findAnnotations(declaration, CfirPlatformAnnotationClassIds.API_LEVEL)
    if (apiLevelEntries.isNotEmpty()) {
        val seenSyscaps = linkedSetOf<String>()
        for (entry in apiLevelEntries) {
            if (!entry.hasApiLevelSinceArgument()) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MISSING_ARG,
                    a = Name.identifier("since"),
                )
            }

            // 官方 ParseAPILevelArgs 将 literal 限制诊断在实际参数表达式上；
            // 注解整体只用于 missing-arg/参数绑定错误，不能吞掉参数 source。
            entry.explicitArgumentExpressions()
                .asSequence()
                .filterNot { it.isAnnotationLiteralLike() }
                .forEach { argument ->
                    val argumentSource = argument.annotationLiteralDiagnosticSource(entry.source)
                        ?: entry.toSourceOrDeclarationSource(declaration)
                    reporter.reportOn(argumentSource, CfirErrors.ONLY_LITERAL_SUPPORT, "annotation")
                }

            val syscapLiteral = entry.argumentByName("syscap")?.literalStringOrNull()
            if (syscapLiteral != null && !seenSyscaps.add(syscapLiteral)) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MULTI_DIFF_SYSCAP,
                )
            }
        }
    }

    val hideEntries = availability.findAnnotations(declaration, CfirPlatformAnnotationClassIds.HIDE)
    val firstHideEntry = hideEntries.firstOrNull()
    val isRegularFunctionParameter =
        declaration is CfirValueParameter && declaration.correspondingProperty == null
    if (firstHideEntry != null && isRegularFunctionParameter) {
        // 官方 Parse 对首个 Hide 先执行普通函数参数检查并 continue；
        // 后续重复项仍各自进入 duplicate 分支。
        val parameterSource = checkNotNull(declaration.source) {
            "Source function parameter with Hide annotation must have a declaration source."
        }
        reporter.reportOn(
            source = CjOffsetsOnlySourceElement(
                parameterSource.startOffset,
                parameterSource.endOffset,
            ),
            factory = CfirErrors.HIDE_AT_FUNC_PARAM,
        )
    } else if (firstHideEntry != null) {
        val snapshot = context.session.annotationMetadataRegistryOrNull?.snapshot(firstHideEntry)
        if (snapshot == null) {
            check(!declaration.origin.fromSource && firstHideEntry.source == null) {
                "Source Hide annotation metadata is missing for ${declaration.symbol}."
            }
        } else if (!snapshot.isCompileTimeVisible) {
            reporter.reportOn(
                source = snapshot.annotationSource,
                factory = CfirErrors.HIDE_COMPILE_TIME_INVISIBLE,
            )
        }

        if (firstHideEntry.hasArguments()) {
            val isCheckedArgument = firstHideEntry.argumentByName("isChecked")
            val hasBooleanIsChecked =
                isCheckedArgument is org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression &&
                    isCheckedArgument.value is Boolean
            if (!firstHideEntry.hasNamedArgument("isChecked") || !hasBooleanIsChecked) {
                reporter.reportOn(
                    source = firstHideEntry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.HIDE_DIFF_PARAM,
                    a = "unexpected",
                )
            }
        }
    }

    if (hideEntries.size > 1) {
        // 官方 Parse 以首项建立 hideExist，此后每个 Hide 都在 declaration 位置
        // 报告一次 multi 并 continue，不再消费该重复项的可见性、位置或参数规则。
        val duplicateHideSource = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.classLikeDeclarationHeaderDiagnosticSource()
            else -> declaration.source
        }
        val source = checkNotNull(duplicateHideSource) {
            "Duplicate Hide annotation requires a declaration source."
        }
        repeat(hideEntries.size - 1) {
            reporter.reportOn(
                source = source,
                factory = CfirErrors.HIDE_MULTI_ANNOTATION,
            )
        }
    }

    checkHideOfExtendDeclaration(declaration, availability)
    checkHideOfOverrideFunction(declaration, availability)
}

/** 按官方矩阵检查 extend 、被扩展类型与 extend 成员的 Hide 关系。 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkHideOfExtendDeclaration(
    declaration: CfirDeclaration,
    availability: CfirDeclarationAvailabilityProvider,
) {
    val extend = declaration as? CfirExtend ?: return
    val target = CfirExtendSemantics.targetDeclaration(context, extend) ?: return
    val targetHide = availability.ownHideState(target)
    val extendHide = availability.ownHideState(extend)

    if (targetHide is CfirHideAnnotationState.Present && extendHide is CfirHideAnnotationState.Absent) {
        reporter.reportOn(extend.source, CfirErrors.HIDE_MISSING_HIDE)
    } else if (
        targetHide is CfirHideAnnotationState.Present &&
        extendHide is CfirHideAnnotationState.Present &&
        targetHide.isChecked &&
        !extendHide.isChecked
    ) {
        reporter.reportOn(
            source = extend.source,
            factory = CfirErrors.HIDE_DIFF_PARAM,
            a = extendHide.isChecked.toString(),
        )
    }

    val effectiveExtendHide = extendHide as? CfirHideAnnotationState.Present ?: return
    for (member in extend.declarations) {
        val memberHide = availability.ownHideState(member) as? CfirHideAnnotationState.Present ?: continue
        if (memberHide.isChecked == effectiveExtendHide.isChecked) continue
        reporter.reportOn(
            source = member.source,
            factory = CfirErrors.HIDE_DIFF_PARAM,
            a = memberHide.isChecked.toString(),
        )
    }

}

/**
 * 使用 use-site scope 的 direct-override 图查找顶层基函数，再比较 effective Hide。
 *
 * effective Hide 严格按“函数自身优先，自身缺失时才继承 outer”计算。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkHideOfOverrideFunction(
    declaration: CfirDeclaration,
    availability: CfirDeclarationAvailabilityProvider,
) {
    val function = declaration as? CfirNamedFunction ?: return
    val currentHide = availability.effectiveHideStateForOverride(function.symbol)
        as? CfirHideAnnotationState.Present ?: return
    val topOverridden = collectTopOverriddenFunctions(function.symbol)
    if (topOverridden.isEmpty()) return

    var hasDifferentParameter = false
    for (topFunction in topOverridden) {
        when (val topHide = availability.effectiveHideStateForOverride(topFunction)) {
            CfirHideAnnotationState.Absent -> reporter.reportOn(
                source = topFunction.cfir.functionNameDiagnosticSource(),
                factory = CfirErrors.HIDE_MISSING_HIDE,
            )

            is CfirHideAnnotationState.Present -> {
                if (topHide.isChecked != currentHide.isChecked) hasDifferentParameter = true
            }
        }
    }

    if (hasDifferentParameter) {
        reporter.reportOn(
            source = function.functionNameDiagnosticSource(),
            factory = CfirErrors.HIDE_DIFF_PARAM,
            a = currentHide.isChecked.toString(),
        )
    }
}

/** 递归展开真实 direct-override 边，保留多父类与 intersection 分支。 */
context(context: CheckerContext)
private fun collectTopOverriddenFunctions(
    functionSymbol: CfirNamedFunctionSymbol,
): List<CfirNamedFunctionSymbol> {
    val direct = functionSymbol.directOverriddenFunctions()
    if (direct.isEmpty()) return emptyList()

    val result = linkedSetOf<CfirNamedFunctionSymbol>()
    val visited = linkedSetOf<CfirNamedFunctionSymbol>()
    fun collect(symbol: CfirNamedFunctionSymbol) {
        if (!visited.add(symbol)) return
        val parents = symbol.directOverriddenFunctions()
        if (parents.isEmpty()) {
            result += symbol.unwrapSubstitutionOverrides()
            return
        }
        parents.forEach(::collect)
    }
    direct.forEach(::collect)
    return result.toList()
}

/** 在符号所属 class-like 的 use-site scope 中收集直接覆写函数。 */
context(context: CheckerContext)
private fun CfirNamedFunctionSymbol.directOverriddenFunctions(): List<CfirNamedFunctionSymbol> {
    val normalized = unwrapSubstitutionOverrides()
    val scope = context.overrideOwnerUseSiteMemberScope(normalized) ?: return emptyList()
    return scope.collectDirectOverriddenFunctions(normalized)
        .mapNotNull { symbol -> symbol as? CfirNamedFunctionSymbol }
        .distinct()
}

/**
 * 检查 `@JavaMirror` 与 `@JavaImpl` 的基础继承语义。
 *
 * 规则覆盖镜像类型继承限制、实现类型必须继承镜像类型、禁止继承纯仓颉类型以及
 * JavaMirror 成员签名类型约束。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaInteropSemantics(declaration: CfirClassLikeDeclaration) {
    val hasJavaMirror = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR)
    val hasJavaImpl = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_IMPL)
    val superDeclarations = declaration.superDeclarations()

    // JavaHasDefault 的位置约束独立于 enclosing declaration 是否已被
    // JavaMirror 规则接受；否则将注解放在普通类或 JavaImpl 上会被早期
    // `return` 静默吞掉。
    checkJavaHasDefaultSemantics(declaration)

    if (!hasJavaMirror && !hasJavaImpl) {
        if (superDeclarations.any { it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR) }) {
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
        if (superDeclarations.any {
                !it.hasAnyPlatformAnnotation(
                    CangjiePlatformAnnotationKind.JAVA_MIRROR,
                    CangjiePlatformAnnotationKind.JAVA_IMPL,
                )
            }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE,
            )
        }
        checkJavaMirrorMemberTypes(declaration)
    }

    if (hasJavaImpl) {
        // @JavaImpl interface:不支持
        if (declaration is org.cangnova.cangjie.cfir.declarations.CfirInterface) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_INTEROP_NOT_SUPPORTED,
                a = "@JavaImpl interface",
            )
        }
        // @JavaImpl abstract class:不支持
        if (declaration is org.cangnova.cangjie.cfir.declarations.CfirClass
            && declaration.status.isAbstract) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_INTEROP_NOT_SUPPORTED,
                a = "@JavaImpl abstract",
            )
        }
        if (superDeclarations.any { it is org.cangnova.cangjie.cfir.declarations.CfirInterface }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE,
            )
        }
        if (superDeclarations.none { it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR,
            )
        }
        if (superDeclarations.any {
                !it.hasAnyPlatformAnnotation(
                    CangjiePlatformAnnotationKind.JAVA_MIRROR,
                    CangjiePlatformAnnotationKind.JAVA_IMPL,
                )
            }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE,
            )
        }
    }
}

/**
 * 检查 `@JavaMirror` 类型的成员参数、返回值和属性类型。
 *
 * Java mirror 成员只能暴露 primitive、JavaMirror 或 JavaImpl 兼容类型；同时检查
 * `@JavaHasDefault` 在接口默认方法上的使用位置和参数限制。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaMirrorMemberTypes(declaration: CfirClassLikeDeclaration) {
    for (member in declaration.declarations) {
        when (member) {
            is CfirConstructor -> {
                if (member.valueParameters.any {
                        !it.returnTypeRef.isInteropMirrorCompatible(
                            CangjiePlatformAnnotationKind.JAVA_MIRROR,
                            CangjiePlatformAnnotationKind.JAVA_IMPL,
                        )
                    }) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            is CfirFunction -> {
                if (member.valueParameters.any {
                        !it.returnTypeRef.isInteropMirrorCompatible(
                            CangjiePlatformAnnotationKind.JAVA_MIRROR,
                            CangjiePlatformAnnotationKind.JAVA_IMPL,
                        )
                    }) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            is CfirProperty -> {
                if (!member.returnTypeRef.isInteropMirrorCompatible(
                        CangjiePlatformAnnotationKind.JAVA_MIRROR,
                        CangjiePlatformAnnotationKind.JAVA_IMPL,
                    )) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR,
                    )
                }
            }

            else -> Unit
        }
    }

    // @JavaMirror 函数返回类型检查
    for (member in declaration.declarations) {
        if (member is CfirNamedFunction) {
            val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            if (returnType != null && !returnType.isUnit && !member.returnTypeRef.isInteropMirrorCompatible(
                    CangjiePlatformAnnotationKind.JAVA_MIRROR,
                    CangjiePlatformAnnotationKind.JAVA_IMPL,
                )) {
                val classKind = if (declaration is org.cangnova.cangjie.cfir.declarations.CfirInterface) "interface" else "class"
                reporter.reportOn(
                    source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                    factory = CfirErrors.JAVA_MIRROR_METHOD_RET_UNSUPPORTED,
                    a = returnType,
                    b = classKind,
                )
            }
        }
    }

}

/**
 * `@JavaHasDefault` 只能标记 JavaMirror interface 的非 static member function。
 * 该规则必须在 class-like owner 上无条件执行，不能依赖 JavaMirror 成员类型检查
 * 的早期分支；参数数量由统一平台 annotation argument owner 检查。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaHasDefaultSemantics(declaration: CfirClassLikeDeclaration) {
    for (member in declaration.declarations) {
        if (member !is CfirNamedFunction) continue
        if (!member.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT)) continue

        val hasDefaultEntry = member.findPlatformAnnotations(CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT)
            .filterIsInstance<CfirAnnotationCall>()
            .firstOrNull()
        if (hasDefaultEntry != null && hasDefaultEntry.hasArguments()) {
            reporter.reportOn(
                source = hasDefaultEntry.toSourceOrDeclarationSource(member),
                factory = CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_ARGS,
            )
        }

        if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirInterface ||
            !declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR)
        ) {
            reporter.reportOn(
                source = member.source ?: declaration.source,
                factory = CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE,
            )
        }

        if (member.status.isStatic) {
            reporter.reportOn(
                source = member.source ?: declaration.source,
                factory = CfirErrors.JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC,
            )
        }
    }
}

/**
 * @Java 类型声明级语义检查。
 *
 * 对齐 C++ FFI/FFICheck.cpp 中的 @Java 类型级检查：
 * - STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY: @Java 接口的 static 函数必须有体
 * - JAVA_UNSUPPORTED_DECL: @Java 类型中不支持某些声明类型
 * - JAVA_NON_JTYPE: @Java 声明中的类型必须满足 JType 约束
 * - JAVA_INVALID_UNIT: @Java 声明中的类型不能是 Unit
 * - MISSING_JAVA_INTEROP_ANNOTATION: 缺少 @Java 注解
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaTypeDeclarationSemantics(declaration: CfirClassLikeDeclaration) {
    val hasJava = declaration.hasSupportedBuiltinAnnotation(
        context.languageVersionSettings,
        BuiltInAnnotationKind.JAVA,
    )
    if (!hasJava) return

    // @Java 接口的 static 函数必须有函数体
    if (declaration is org.cangnova.cangjie.cfir.declarations.CfirInterface) {
        for (member in declaration.declarations) {
            if (member is CfirNamedFunction && member.status.isStatic && member.body == null) {
                reporter.reportOn(
                    source = member.source ?: declaration.source,
                    factory = CfirErrors.STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY,
                )
            }
        }
    }

    // 检查 @Java 类型成员的类型是否满足 JType 约束
    for (member in declaration.declarations) {
        when (member) {
            is CfirNamedFunction -> {
                // 检查返回类型
                val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (returnType != null && returnType.isUnit) {
                    // Unit 作为返回类型在 @Java 声明中是允许的（映射为 void）
                } else if (returnType != null && !isJTypeCompatible(returnType)) {
                    reporter.reportOn(
                        source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_NON_JTYPE,
                        a = "return type",
                        b = "function",
                        c = member.name,
                    )
                }
                // 检查参数类型
                for (param in member.valueParameters) {
                    val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                    if (paramType.isUnit) {
                        reporter.reportOn(
                            source = param.source ?: member.source ?: declaration.source,
                            factory = CfirErrors.JAVA_INVALID_UNIT,
                            a = "parameter type",
                            b = "function",
                            c = member.name,
                        )
                    } else if (!isJTypeCompatible(paramType)) {
                        reporter.reportOn(
                            source = param.source ?: member.source ?: declaration.source,
                            factory = CfirErrors.JAVA_NON_JTYPE,
                            a = "parameter type",
                            b = "function",
                            c = member.name,
                        )
                    }
                }
            }
            is CfirProperty -> {
                val propType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (propType != null && propType.isUnit) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_INVALID_UNIT,
                        a = "property type",
                        b = "property",
                        c = member.name,
                    )
                } else if (propType != null && !isJTypeCompatible(propType)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_NON_JTYPE,
                        a = "property type",
                        b = "property",
                        c = member.name,
                    )
                }
            }
            is CfirFieldVariable -> {
                // @Java 类型中不能存储 Java 互操作类型的变量
                val fieldType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (fieldType != null && fieldType.isUnit) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.JAVA_INVALID_UNIT,
                        a = "field type",
                        b = "field",
                        c = member.name,
                    )
                }
            }
            else -> Unit
        }
    }
}

/**
 * 判断类型是否与 Java 类型系统兼容（JType）。
 *
 * JType 兼容类型包括：
 * - 基本类型（Int8..Int64, UInt8..UInt64, Float32, Float64, Bool, Unit）
 * - @Java 注解的类/接口
 * - String（std.core.String 映射为 java.lang.String）
 *
 * 对齐 C++ Sema/FFI/FFICheck.cpp 中的 IsJType 判断。
 */
context(context: CheckerContext)
private fun isJTypeCompatible(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): Boolean {
    if (type is org.cangnova.cangjie.cfir.types.ConePrimitiveType) return true
    if (type is org.cangnova.cangjie.cfir.types.ConeErrorType) return true
    val classId = type.classIdOrPrimitiveClassId ?: return false
    // String 类型兼容
    if (classId.shortClassName.asString() == "String") return true
    // 检查目标类型声明是否有 @Java 注解
    val targetDecl = CfirExtendSemantics.resolveDeclaration(context, classId) ?: return false
    return targetDecl.hasSupportedBuiltinAnnotation(
        context.languageVersionSettings,
        BuiltInAnnotationKind.JAVA,
    ) ||
        targetDecl.hasAnyPlatformAnnotation(
            CangjiePlatformAnnotationKind.JAVA_MIRROR,
            CangjiePlatformAnnotationKind.JAVA_IMPL,
        )
}

/**
 * 检查 `@ObjCMirror` 与 `@ObjCImpl` 的基础继承和成员类型语义。
 *
 * 规则覆盖 ObjC 抽象类限制、镜像/实现继承链约束、多重继承限制、ForeignName 要求、
 * 成员参数/返回值/属性/字段的 ObjC 类型兼容性，以及 setter 名称在不可变属性上的限制。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCInteropSemantics(declaration: CfirClassLikeDeclaration) {
    val hasObjCMirror = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
    val hasObjCImpl = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_IMPL)
    val superDeclarations = declaration.superDeclarations()

    if (!hasObjCMirror && !hasObjCImpl) {
        if (superDeclarations.any { it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR) }) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED,
            )
        }
        return
    }

    // ObjC abstract class 不支持(对齐 C++ CheckAbstractClass.cpp:22)
    if (declaration is org.cangnova.cangjie.cfir.declarations.CfirClass
        && declaration.status.isAbstract) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_INTEROP_NOT_SUPPORTED,
            a = "abstract",
        )
    }

    if (hasObjCMirror && superDeclarations.any {
            !it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
        }) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_MIRROR_MUST_INHERIT_MIRROR,
        )
    }

    // ObjC mirror 不能继承其他超类型（只能继承 @ObjCMirror 或 @ObjCImpl）
    if (hasObjCMirror && superDeclarations.size > 1) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_MIRROR_DECL_CANNOT_INHERIT,
        )
    }

    // ObjC mirror 子类型不能多重继承
    if ((hasObjCMirror || hasObjCImpl) && superDeclarations.count { it is org.cangnova.cangjie.cfir.declarations.CfirClass } > 1) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT,
        )
    }

    // @ObjCImpl 必须继承 @ObjCMirror
    if (hasObjCImpl) {
        if (superDeclarations.none { it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR) }) {
            reporter.reportOn(
                source = declaration.classLikeNameDiagnosticSource(),
                factory = CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR,
            )
        }
    }

    if (hasObjCImpl && superDeclarations.none {
            it.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
        }) {
        reporter.reportOn(
            source = declaration.classLikeNameDiagnosticSource(),
            factory = CfirErrors.OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS,
        )
    }

    for (member in declaration.declarations) {
        when (member) {
            is CfirNamedFunction -> {
                checkObjCInitMethodReturnType(declaration, member)
                if (member.valueParameters.size > 1 &&
                    !member.hasPlatformAnnotation(CangjiePlatformAnnotationKind.FOREIGN_NAME)
                ) {
                    reporter.reportOn(
                        source = member.functionNameDiagnosticSource() ?: declaration.source,
                        factory = CfirErrors.OBJC_METHOD_MUST_HAVE_FOREIGN_NAME,
                        a = "ObjC",
                        b = member.name,
                    )
                }
                // 检查方法参数类型 ObjC 兼容性
                for (param in member.valueParameters) {
                    val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                    if (!isObjCTypeCompatible(paramType)) {
                        reporter.reportOn(
                            source = param.source ?: member.source ?: declaration.source,
                            factory = CfirErrors.OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE,
                            a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                        )
                    }
                }
                // 检查方法返回类型 ObjC 兼容性
                val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (returnType != null && !returnType.isUnit &&
                    (!isObjCTypeCompatible(returnType) ||
                        CfirObjCTypeSemantics.isObjCPointerToClass(context.session, returnType))
                ) {
                    reporter.reportOn(
                        source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE,
                        a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                    )
                }
            }

            is CfirConstructor -> {
                if (member.valueParameters.size > 1 &&
                    !member.hasPlatformAnnotation(CangjiePlatformAnnotationKind.FOREIGN_NAME)
                ) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_CTOR_MUST_HAVE_FOREIGN_NAME,
                        a = "ObjC",
                    )
                }
                // 检查构造器参数类型 ObjC 兼容性
                for (param in member.valueParameters) {
                    val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                    if (!isObjCTypeCompatible(paramType)) {
                        reporter.reportOn(
                            source = param.source ?: member.source ?: declaration.source,
                            factory = CfirErrors.OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE,
                            a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                        )
                    }
                }
            }

            is CfirProperty -> {
                val propType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (propType != null &&
                    (!isObjCTypeCompatible(propType) ||
                        CfirObjCTypeSemantics.isObjCPointerToClass(context.session, propType))
                ) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE,
                        a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                    )
                }
                // @ForeignSetterName 不能用在不可变属性上（没有 setter 的属性）
                if (member.setter == null &&
                    member.hasPlatformAnnotation(CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME)
                ) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_SETTER_NAME_ON_IMMUTABLE_PROP,
                    )
                }
            }

            is CfirFieldVariable -> {
                val fieldType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                if (fieldType != null && !isObjCTypeCompatible(fieldType)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE,
                        a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                    )
                }
            }

            else -> Unit
        }
    }
}

/**
 * 判断类型是否与 Objective-C 类型系统兼容。
 *
 * 对齐 C++ Sema 中的 ObjC 类型兼容判断。
 */
context(context: CheckerContext)
private fun isObjCTypeCompatible(type: org.cangnova.cangjie.cfir.types.ConeCangJieType): Boolean {
    return CfirObjCTypeSemantics.isObjCCompatible(context.session, type)
}

/**
 * 解析 class-like 声明的直接父声明列表。
 *
 * 优先使用已经绑定好的 superTypeRefs；只有在 CFIR 父类型缺失且 PSI 中也没有父类型项时
 * 返回空列表，避免用 PSI 文本重新实现父类型解析。
 */
context(context: CheckerContext)
private fun CfirClassLikeDeclaration.superDeclarations(): List<CfirClassLikeDeclaration> {
    val typeStatement = source?.psi as? CjTypeStatement
    val superEntries = typeStatement?.superTypeListEntries.orEmpty()
    val resolved = mutableListOf<CfirClassLikeDeclaration>()
    for (superTypeRef in superTypeRefs) {
        val classId = CfirExtendSemantics.run { superTypeRef.toClassIdOrNull() } ?: continue
        val target = CfirExtendSemantics.resolveDeclaration(context, classId) ?: continue
        resolved += target
    }
    // superTypeRefs 更可信；只有在 CFIR 还未绑定齐时才退回 PSI 数量。
    if (resolved.isNotEmpty() || superEntries.isEmpty()) return resolved
    return emptyList()
}

/**
 * 判断类型引用是否满足指定互操作镜像注解集合。
 *
 * primitive、未解析类型和无法定位声明的类型保持宽松通过；可定位 class-like 声明时，
 * 必须携带调用方要求的 Java/ObjC mirror 或 impl 注解之一。
 */
context(context: CheckerContext)
private fun CfirTypeRef.isInteropMirrorCompatible(
    vararg requiredAnnotations: CangjiePlatformAnnotationKind,
): Boolean {
    val resolvedType = this as? CfirResolvedTypeRef ?: return true
    if (resolvedType.coneType is ConePrimitiveType) return true
    val classId = resolvedType.coneType.classIdOrPrimitiveClassId ?: return true
    val declaration = CfirExtendSemantics.resolveDeclaration(context, classId) ?: return true
    return declaration.hasAnySupportedPlatformAnnotation(context.languageVersionSettings, *requiredAnnotations)
}

/**
 * 判断 class-like 声明是否具有平台注解要求的 public-like 可见性。
 */
private fun CfirClassLikeDeclaration.isPublicLike(): Boolean =
    status.visibility.externalDisplayName == "public"

/**
 * 取得注解本身的 source，缺失时回退到所属声明 source。
 */
private fun CfirAnnotation.toSourceOrDeclarationSource(declaration: CfirDeclaration): org.cangnova.cangjie.source.CjSourceElement? =
    this.source ?: declaration.source

/** 外部符号名称映射注解名称。 */
private val FOREIGN_NAME = Name.identifier("ForeignName")

/**
 * @ForeignName 注解规则检查。
 *
 * 对齐 C++ sema_foreign_name_* 系列:
 * - @ForeignName 不能出现在被 override 的声明上
 * - @ForeignName 注解冲突检查
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCInitMethodReturnType(
    declaration: CfirClassLikeDeclaration,
    member: CfirNamedFunction,
) {
    if (!member.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_INIT)) return
    if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirClass ||
        !declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
    ) {
        reporter.reportOn(
            source = member.source ?: declaration.source,
            factory = CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI,
            a = "ObjCInit",
            b = "an ObjCMirror class static member function",
        )
        return
    }

    if (!member.status.isStatic) {
        reporter.reportOn(
            source = member.source ?: declaration.source,
            factory = CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI,
            a = "ObjCInit",
            b = "non-static member function",
        )
    }

    val returnTypeRef = member.returnTypeRef as? CfirResolvedTypeRef ?: return
    val expectedType = declaration.defaultType()
    val actualType = returnTypeRef.coneType
    if (AbstractTypeChecker.equalTypes(context.session.typeContext, expectedType, actualType)) return

    reporter.reportOn(
        source = returnTypeRef.source ?: member.source ?: declaration.source ?: return,
        factory = CfirErrors.TYPE_MISMATCH,
        a = expectedType,
        b = actualType,
        c = false,
    )
}

/**
 * 检查 `@ForeignName` 及其 getter/setter 派生注解的冲突规则。
 *
 * 覆写声明不能重新声明 foreign name；同一声明上多个 ForeignName 或 ForeignName 与
 * ForeignGetterName/ForeignSetterName 组合使用都会产生冲突诊断。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkForeignNameRules(
    declaration: CfirDeclaration,
) {
    val foreignNameEntries = declaration.findPlatformAnnotations(CangjiePlatformAnnotationKind.FOREIGN_NAME)
        .filterIsInstance<CfirAnnotationCall>()
    if (foreignNameEntries.isEmpty()) return
    val javaForeignNameEntries = foreignNameEntries.filter { it.isJavaInteropForeignName() }
    val objcForeignNameEntries = foreignNameEntries.filter { it.isObjCInteropForeignName() }

    // @ForeignName 不能出现在被 override 的声明上
    val isOverride = when (declaration) {
        is CfirNamedFunction -> declaration.status.isOverride
        is CfirProperty -> declaration.status.isOverride
        else -> false
    }
    // JavaMirror/JavaImpl names are inherited from the original method; an
    // override must not redeclare them. ObjC selectors are per-declaration
    // metadata and the official ObjC examples explicitly redeclare them on
    // overrides, so this rule must not be shared across the two domains.
    if (isOverride && javaForeignNameEntries.isNotEmpty()) {
        reporter.reportOn(
            source = javaForeignNameEntries.first().toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.FOREIGN_NAME_APPEARED_IN_CHILD,
            a = FOREIGN_NAME,
        )
    }

    // 多个 @ForeignName 注解冲突
    val conflictingEntries = when {
        javaForeignNameEntries.size > 1 -> javaForeignNameEntries
        objcForeignNameEntries.size > 1 -> objcForeignNameEntries
        else -> emptyList()
    }
    if (conflictingEntries.size > 1) {
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> Name.identifier("<unknown>")
        }
        reporter.reportOn(
            source = conflictingEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.FOREIGN_NAME_CONFLICTING_ANNOTATION,
            a = declName,
            b = FOREIGN_NAME,
        )
    }

    // 派生注解冲突：@ForeignName 与其衍生出的 @ForeignSetterName/@ForeignGetterName 不能同时出现
    val foreignGetterEntries = declaration.findPlatformAnnotations(CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME)
    val foreignSetterEntries = declaration.findPlatformAnnotations(CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME)
    if (objcForeignNameEntries.isNotEmpty() && (foreignGetterEntries.isNotEmpty() || foreignSetterEntries.isNotEmpty())) {
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> Name.identifier("<unknown>")
        }
        val derivedName = if (foreignGetterEntries.isNotEmpty())
            Name.identifier("ForeignGetterName") else Name.identifier("ForeignSetterName")
        reporter.reportOn(
            source = objcForeignNameEntries.first().source ?: declaration.source,
            factory = CfirErrors.FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION,
            a = declName,
            b = FOREIGN_NAME,
            c = derivedName,
        )
    }
}

/**
 * @Java 互操作的额外声明级约束。
 *
 * 对齐 C++ FFI/FFICheck.cpp 中剩余 JavaInterop 诊断：
 * - JAVA_INCORRECT_USE_BETWEEN_TYPES: @Java 注解存在多个不匹配值
 * - JAVA_APP_INHERIT_EXT: 仅 @Java["ext"] 能被 ext 继承
 * - JAVA_UNSUPPORTED_DECL: @Java 类型中不支持某些声明（enum/typealias/extend）
 * - MISSING_JAVA_INTEROP_ANNOTATION: 需要 @Java 互操作注解但缺失
 * - SHADOW_CANNOT_IN_TYPE_ARGS: @Java 泛型参数不能用 shadow
 * - UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP: 泛型参数类型不支持
 * - INVALID_USE_OF_JAVA_ANNOTATION: 导入的 Java 注解使用位置不对
 * - INVALID_USE_OF_ANNOTATION_JFFI: 仅 @Java 类型可使用 Java 注解
 * - VARIABLE_OF_JAVA_TYPE: 不能存储 Java 互操作类型的变量
 * - GENERIC_PARAMETER_OF_JAVA_TYPE: 不能用 Java 互操作类型实例化泛型
 * - JAVA_INTEROP_NOT_SUPPORTED: 不支持的 Java 互操作特性
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkJavaInteropExtraSemantics(declaration: CfirClassLikeDeclaration) {
    val hasJava = declaration.hasSupportedBuiltinAnnotation(
        context.languageVersionSettings,
        BuiltInAnnotationKind.JAVA,
    )
    val hasJavaMirror = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_MIRROR)
    val hasJavaImpl = declaration.hasPlatformAnnotation(CangjiePlatformAnnotationKind.JAVA_IMPL)
    val isJavaRelated = hasJava || hasJavaMirror || hasJavaImpl

    // MISSING_JAVA_INTEROP_ANNOTATION: 继承 Java 类型必须标注对应注解
    val superDeclarations = declaration.superDeclarations()
    if (!isJavaRelated && superDeclarations.any {
            it.hasSupportedBuiltinAnnotation(
                context.languageVersionSettings,
                BuiltInAnnotationKind.JAVA,
            ) ||
                it.hasAnyPlatformAnnotation(
                    CangjiePlatformAnnotationKind.JAVA_MIRROR,
                    CangjiePlatformAnnotationKind.JAVA_IMPL,
                )
        }) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.MISSING_JAVA_INTEROP_ANNOTATION,
            a = "class",
            b = declaration.name,
        )
    }

    if (!isJavaRelated) return

    // JAVA_INCORRECT_USE_BETWEEN_TYPES: @Java 注解的不同值域不能混用
    val javaEntries = declaration.findSupportedBuiltinAnnotations(
        context.languageVersionSettings,
        BuiltInAnnotationKind.JAVA,
    )
        .filterIsInstance<CfirAnnotationCall>()
    if (javaEntries.isNotEmpty()) {
        val javaValues = javaEntries.mapNotNull { entry ->
            entry.stringArgument("name")
        }.toSet()
        if (javaValues.size > 1) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_INCORRECT_USE_BETWEEN_TYPES,
            )
        }

        // JAVA_APP_INHERIT_EXT: 仅 @Java["ext"] 能被 ext 继承
        val isExt = javaValues.any { it == "ext" }
        if (!isExt) {
            for (superDecl in superDeclarations) {
                val superJavaEntry = superDecl.findSupportedBuiltinAnnotations(
                    context.languageVersionSettings,
                    BuiltInAnnotationKind.JAVA,
                )
                    .filterIsInstance<CfirAnnotationCall>()
                    .firstOrNull() ?: continue
                val superIsExt = superJavaEntry.stringArgument("name") == "ext"
                if (superIsExt) {
                    reporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.JAVA_APP_INHERIT_EXT,
                        a = "inherit",
                    )
                }
            }
        }
    }

    // JAVA_UNSUPPORTED_DECL: @Java 类型中不支持某些嵌套声明
    for (member in declaration.declarations) {
        val unsupportedKind = when (member) {
            is org.cangnova.cangjie.cfir.declarations.CfirEnum -> "enum"
            is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> "type alias"
            is org.cangnova.cangjie.cfir.declarations.CfirExtend -> "extend"
            else -> null
        }
        if (unsupportedKind != null) {
            val memberName = when (member) {
                is org.cangnova.cangjie.cfir.declarations.CfirEnum -> member.name
                is org.cangnova.cangjie.cfir.declarations.CfirTypeAlias -> member.name
                else -> Name.identifier("<extend>")
            }
            val containerKind = if (declaration is org.cangnova.cangjie.cfir.declarations.CfirInterface) "interface" else "class"
            reporter.reportOn(
                source = member.source ?: declaration.source,
                factory = CfirErrors.JAVA_UNSUPPORTED_DECL,
                a = unsupportedKind,
                b = containerKind,
                c = memberName,
            )
        }
    }

    // VARIABLE_OF_JAVA_TYPE & GENERIC_PARAMETER_OF_JAVA_TYPE: 字段类型和泛型参数类型
    // 只有非 @Java 声明中引用 @Java 类型才报告（@Java 类型内部互相引用是允许的）
    // 已在 checkJavaTypeDeclarationSemantics 中处理 JAVA_NON_JTYPE，此处不再重复

    // SHADOW_CANNOT_IN_TYPE_ARGS 需要 Java class-file/CJO 产生的结构化 type-parameter
    // attribute。源码 CFIR 没有该事实时不根据名称猜测，避免把普通 custom annotation
    // 误报为 Java annotation。
    val typeParams = when (declaration) {
        is org.cangnova.cangjie.cfir.declarations.CfirClass -> declaration.typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirInterface -> declaration.typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirStruct -> declaration.typeParameters
        else -> emptyList()
    }
    // UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP: 类型参数类型不支持
    for (typeParam in typeParams) {
        val boundType = typeParam.symbol.resolvedBounds.firstOrNull()?.coneType
        if (boundType != null && boundType !is org.cangnova.cangjie.cfir.types.ConeErrorType &&
            boundType !is org.cangnova.cangjie.cfir.types.ConePrimitiveType &&
            boundType !is org.cangnova.cangjie.cfir.types.ConeClassLikeType) {
            reporter.reportOn(
                source = typeParam.source ?: declaration.source,
                factory = CfirErrors.UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP,
            )
        }
    }

    // INVALID_USE_OF_JAVA_ANNOTATION / INVALID_USE_OF_ANNOTATION_JFFI
    // 这些诊断只能由 Java class-file/CJO loader 发布的结构化 annotation origin 触发。
    // 未携带该事实的 custom annotation 即使短名相似，也不能被猜测为 Java annotation。
    if (!isJavaRelated) {
        reportInvalidImportedJavaAnnotations(declaration)
    }
}

/**
 * 消费 loader 发布的 Java annotation 身份。
 *
 * 源码 custom annotation 不提供“来自 Java class file”的证据；在该证据缺失时必须
 * 保持未知，而不是根据短名或 `Jffi` 前缀制造诊断。当前 source CFIR 没有此类 origin
 * producer，因此该方法只保留结构化入口，待 Java metadata loader 发布事实后消费。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportInvalidImportedJavaAnnotations(declaration: CfirClassLikeDeclaration) {
    val importedJavaAnnotations = declaration.annotations.filter {
        it.annotationOrigin == org.cangnova.cangjie.annotations.CangjieAnnotationOrigin.PLATFORM_DERIVED
    }
    importedJavaAnnotations.forEach { annotation ->
        reporter.reportOn(
            source = annotation.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.INVALID_USE_OF_JAVA_ANNOTATION,
        )
    }
}

/**
 * 顶层函数（非 class 成员）的 ObjC 参数/返回类型检查。
 * 注册为 file-level checker 的一部分。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCTopLevelFunction(function: CfirNamedFunction) {
    if (function.dispatchReceiverType != null) return
    if (hasUnsupportedPlatformAnnotationVersion(function)) return
    // 官方 ObjC 互操作只把 @ObjCMirror 用于映射 ObjC 全局函数；
    // ObjCImpl/ObjCName 不是该声明形态的语义入口。
    val hasObjCAnnotation = function.hasPlatformAnnotation(CangjiePlatformAnnotationKind.OBJ_C_MIRROR)
    if (!hasObjCAnnotation) return

    for (param in function.valueParameters) {
        val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
        if (!isObjCTypeCompatible(paramType)) {
            reporter.reportOn(
                source = param.source ?: function.source,
                factory = CfirErrors.OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE,
                a = function.name.asString(),
            )
        }
    }
    val returnType = (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType
    if (returnType != null && !returnType.isUnit &&
        (!isObjCTypeCompatible(returnType) ||
            CfirObjCTypeSemantics.isObjCPointerToClass(context.session, returnType))
    ) {
        reporter.reportOn(
            source = function.returnTypeRef.source ?: function.source,
            factory = CfirErrors.OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE,
            a = function.name.asString(),
        )
    }
}

/**
 * 顶层 ObjCMirror 函数的函数级 checker。
 *
 * 该规则不能挂在 class-like checker 上，否则顶层函数永远不会经过相同的签名约束；
 * 也不能放回表达式 checker，因为全局函数的函数体/函数类型在声明阶段已经确定。
 */
object CfirObjCTopLevelFunctionChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        val function = declaration as? CfirNamedFunction ?: return
        checkObjCTopLevelFunction(function)
    }
}
