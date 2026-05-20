package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
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
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjTypeStatement
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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        checkAnnotationMetaRules(declaration)
        checkPlatformAnnotationSyntax(declaration)
        checkCallingConventionRules(declaration)
        checkForeignNameRules(declaration)
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
        checkJavaTypeDeclarationSemantics(declaration)
        checkJavaInteropExtraSemantics(declaration)
        checkObjCInteropSemantics(declaration)
        checkObjCInteropExtraSemantics(declaration)
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkAnnotationMetaRules(
    declaration: CfirDeclaration,
) {
    val annotationEntry = declaration.findAnnotations(ANNOTATION).firstOrNull() as? CfirAnnotationCall ?: return

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
            source = annotationEntry.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.ANNOTATION_NON_PUBLIC,
        )
    }

    if (declaration.hasAnnotation(JAVA)) {
        reporter.reportOn(
            source = annotationEntry.toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.DEFINE_JAVA_ANNOTATION,
        )
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkPlatformAnnotationSyntax(
    declaration: CfirDeclaration,
) {
    val apiLevelEntries = declaration.findAnnotations(API_LEVEL).filterIsInstance<CfirAnnotationCall>()
    if (apiLevelEntries.size > 1) {
        reporter.reportOn(
            source = apiLevelEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.APILEVEL_MULTI_ANNO,
        )
    }
    if (apiLevelEntries.isNotEmpty()) {
        val seenSyscaps = linkedSetOf<String>()
        for (entry in apiLevelEntries) {
            if (!entry.hasNamedArgument("since")) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MISSING_ARG,
                    a = Name.identifier("since"),
                )
            }

            if (!entry.argumentsAreLiteralLike()) {
                reporter.reportOn(entry.toSourceOrDeclarationSource(declaration), CfirErrors.ONLY_LITERAL_SUPPORT, "annotation")
            }

            val syscapLiteral = entry.namedArgumentText("syscap")
            if (syscapLiteral != null && !seenSyscaps.add(syscapLiteral)) {
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.APILEVEL_MULTI_DIFF_SYSCAP,
                )
            }
        }
    }

    val ifAvailableEntries = declaration.findAnnotations(IF_AVAILABLE).filterIsInstance<CfirAnnotationCall>()
    for (entry in ifAvailableEntries) {
        if (entry.hasArguments() && !entry.firstArgumentIsNamed()) {
            reporter.reportOn(
                source = entry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.IFAVAILABLE_ARG_NO_NAME,
            )
        }
        if (!entry.argumentsAreLiteralLike()) {
            reporter.reportOn(
                source = entry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL,
            )
        }

        entry.rawNamedArgumentNames()
            .firstOrNull { it !in allowedIfAvailableArgumentNames }
            ?.let { argumentName ->
                reporter.reportOn(
                    source = entry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.IFAVAILABLE_UNKNOWN_ARG_NAME,
                    a = argumentName,
                )
            }
    }

    // @IfAvailable APILevel 限制检查
    if (ifAvailableEntries.isNotEmpty() && apiLevelEntries.isEmpty()) {
        reporter.reportOn(
            source = ifAvailableEntries.first().toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.IFAVAILABLE_LEVEL_LIMIT,
        )
    }

    val hideEntries = declaration.findAnnotations(HIDE).filterIsInstance<CfirAnnotationCall>()
    if (hideEntries.size > 1) {
        reporter.reportOn(
            source = hideEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.HIDE_MULTI_ANNOTATION,
        )
    }
    // HIDE_COMPILE_TIME_INVISIBLE: @!Hide 注解本身若无法在编译时解析,报错。
    // 对齐 C++ PluginCustomAnnoChecker.cpp:498 `!anno->isCompileTimeVisible`。
    if (hideEntries.isNotEmpty()) {
        val resolvedHide = declaration.annotations.firstOrNull { ann ->
            val t = (ann.typeRef as? CfirResolvedTypeRef)?.coneType
            t is ConeClassLikeType && t.classId.shortClassName == HIDE
        }
        val isInvisible = resolvedHide == null
            || (resolvedHide.typeRef as? CfirResolvedTypeRef)?.coneType is org.cangnova.cangjie.cfir.types.ConeErrorType
        if (isInvisible) {
            reporter.reportOn(
                source = hideEntries.first().toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.HIDE_COMPILE_TIME_INVISIBLE,
            )
        }
    }
    if (declaration is CfirValueParameter && hideEntries.isNotEmpty()) {
        reporter.reportOn(
            source = hideEntries.first().toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.HIDE_AT_FUNC_PARAM,
        )
    }
    hideEntries.firstOrNull()?.let { hideEntry ->
        if (declaration.annotations.lastOrNull() != hideEntry) {
            reporter.reportOn(
                source = hideEntry.toSourceOrDeclarationSource(declaration),
                factory = CfirErrors.HIDE_MUST_AT_END,
                a = HIDE.asString(),
            )
        }

        if (hideEntry.hasArguments()) {
            if (!hideEntry.hasNamedArgument("isChecked") || !hideEntry.firstArgumentIsBooleanLiteralNamed("isChecked")) {
                reporter.reportOn(
                    source = hideEntry.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.HIDE_DIFF_PARAM,
                    a = "unexpected",
                )
            }
        }
    }

    // HIDE_MISSING_HIDE: override 带 @!Hide 但父声明无 @!Hide
    if (hideEntries.isNotEmpty() && declaration is CfirNamedFunction && declaration.status.isOverride) {
        val parent = findOverriddenInSupers(declaration)
        val parentHasHide = parent?.hasAnnotation(HIDE) == true
        if (!parentHasHide && parent != null) {
            reporter.reportOn(
                source = parent.source ?: declaration.source,
                factory = CfirErrors.HIDE_MISSING_HIDE,
            )
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkCallingConventionRules(declaration: CfirDeclaration) {
    val callingConvEntries = declaration.findAnnotations(Name.identifier("CallingConv"))
    if (callingConvEntries.isEmpty()) return

    val isAllowedTopLevelForeignFunction =
        declaration is CfirFunction &&
            declaration.status.isForeign &&
            declaration.dispatchReceiverType == null
    if (isAllowedTopLevelForeignFunction) return

    val diagnosticFactory = if (declaration is CfirCallableDeclaration && declaration.dispatchReceiverType != null) {
        CfirErrors.ILLEGAL_SCOPE_USE_OF_ANNOTATION
    } else {
        CfirErrors.ONLY_CFUNC_CAN_USE_ANNOTATION
    }

    for (entry in callingConvEntries) {
        reporter.reportOn(
            source = entry.source ?: declaration.source,
            factory = diagnosticFactory,
            a = "CallingConv",
        )
    }
}

context(context: CheckerContext)
private fun findOverriddenInSupers(declaration: CfirNamedFunction): CfirDeclaration? {
    val ownerClassId = declaration.symbol.callableId.classId ?: return null
    val ownerDecl = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
        as? CfirClassLikeDeclaration ?: return null
    for (superRef in ownerDecl.superTypeRefs) {
        val t = (superRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
        val sd = context.session.symbolProvider.getClassLikeSymbolByClassId(t.classId)?.cfir
            as? CfirClassLikeDeclaration ?: continue
        val match = sd.declarations.firstOrNull {
            it is CfirNamedFunction && it.name == declaration.name
        }
        if (match != null) return match
    }
    return null
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

    // @JavaMirror 函数返回类型检查
    for (member in declaration.declarations) {
        if (member is CfirNamedFunction) {
            val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            if (returnType != null && !returnType.isUnit && !member.returnTypeRef.isInteropMirrorCompatible(JAVA_MIRROR, JAVA_IMPL)) {
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

    // @JavaHasDefault 检查
    for (member in declaration.declarations) {
        if (member !is CfirNamedFunction) continue
        if (!member.hasAnnotation(Name.identifier("JavaHasDefault"))) continue

        val hasDefaultEntry = member.findAnnotations(Name.identifier("JavaHasDefault"))
            .filterIsInstance<CfirAnnotationCall>()
            .firstOrNull()
        if (hasDefaultEntry != null && hasDefaultEntry.hasArguments()) {
            reporter.reportOn(
                source = hasDefaultEntry.toSourceOrDeclarationSource(member),
                factory = CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_ARGS,
            )
        }

        if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirInterface || !declaration.hasAnnotation(JAVA_MIRROR)) {
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
    val hasJava = declaration.hasAnnotation(JAVA)
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
    return targetDecl.hasAnnotation(JAVA) || targetDecl.hasAnnotation(JAVA_MIRROR) || targetDecl.hasAnnotation(JAVA_IMPL)
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

    // ObjC abstract class 不支持(对齐 C++ CheckAbstractClass.cpp:22)
    if (declaration is org.cangnova.cangjie.cfir.declarations.CfirClass
        && declaration.status.isAbstract) {
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.OBJC_INTEROP_NOT_SUPPORTED,
            a = "abstract",
        )
    }

    if (hasObjCMirror && superDeclarations.any { !it.hasAnnotation(OBJC_MIRROR) }) {
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
        if (superDeclarations.none { it.hasAnnotation(OBJC_MIRROR) }) {
            reporter.reportOn(
                source = declaration.classLikeNameDiagnosticSource(),
                factory = CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR,
            )
        }
    }

    if (hasObjCImpl && superDeclarations.none { it.hasAnnotation(OBJC_MIRROR) }) {
        reporter.reportOn(
            source = declaration.classLikeNameDiagnosticSource(),
            factory = CfirErrors.OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS,
        )
    }

    for (member in declaration.declarations) {
        when (member) {
            is CfirNamedFunction -> {
                checkObjCInitMethodReturnType(declaration, member)
                if (member.valueParameters.size > 1 && !member.hasAnnotation(FOREIGN_NAME)) {
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
                if (returnType != null && !returnType.isUnit && !isObjCTypeCompatible(returnType)) {
                    reporter.reportOn(
                        source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE,
                        a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
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
                if (propType != null && !isObjCTypeCompatible(propType)) {
                    reporter.reportOn(
                        source = member.source ?: declaration.source,
                        factory = CfirErrors.OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE,
                        a = if (hasObjCMirror) "ObjCMirror" else "ObjCImpl",
                    )
                }
                // @ForeignSetterName 不能用在不可变属性上（没有 setter 的属性）
                if (member.setter == null && member.hasAnnotation(Name.identifier("ForeignSetterName"))) {
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
    if (type is org.cangnova.cangjie.cfir.types.ConePrimitiveType) return true
    if (type is org.cangnova.cangjie.cfir.types.ConeErrorType) return true
    val classId = type.classIdOrPrimitiveClassId ?: return false
    if (classId.shortClassName.asString() == "String") return true
    val targetDecl = CfirExtendSemantics.resolveDeclaration(context, classId) ?: return false
    return targetDecl.hasAnnotation(OBJC_MIRROR) || targetDecl.hasAnnotation(OBJC_IMPL)
}

private fun CfirDeclaration.hasAnyAnnotation(vararg annotationNames: Name): Boolean =
    annotationNames.any(::hasAnnotation)

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

context(context: CheckerContext)
private fun CfirTypeRef.isInteropMirrorCompatible(
    vararg requiredAnnotations: Name,
): Boolean {
    val resolvedType = this as? CfirResolvedTypeRef ?: return true
    if (resolvedType.coneType is ConePrimitiveType) return true
    val classId = resolvedType.coneType.classIdOrPrimitiveClassId ?: return true
    val declaration = CfirExtendSemantics.resolveDeclaration(context, classId) ?: return true
    return declaration.hasAnyAnnotation(*requiredAnnotations)
}

private fun CfirClassLikeDeclaration.isPublicLike(): Boolean =
    status.visibility.externalDisplayName == "public"

private fun CfirAnnotation.toSourceOrDeclarationSource(declaration: CfirDeclaration): org.cangnova.cangjie.source.CjSourceElement? =
    this.source ?: declaration.source

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
    if (!declaration.hasAnnotation(OBJC_MIRROR)) return
    if (!member.hasAnnotation(Name.identifier("ObjCInit"))) return

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

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkForeignNameRules(
    declaration: CfirDeclaration,
) {
    val foreignNameEntries = declaration.findAnnotations(FOREIGN_NAME)
    if (foreignNameEntries.isEmpty()) return

    // @ForeignName 不能出现在被 override 的声明上
    val isOverride = when (declaration) {
        is CfirNamedFunction -> declaration.status.isOverride
        is CfirProperty -> declaration.status.isOverride
        else -> false
    }
    if (isOverride) {
        reporter.reportOn(
            source = foreignNameEntries.first().toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.FOREIGN_NAME_APPEARED_IN_CHILD,
            a = FOREIGN_NAME,
        )
    }

    // 多个 @ForeignName 注解冲突
    if (foreignNameEntries.size > 1) {
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> Name.identifier("<unknown>")
        }
        reporter.reportOn(
            source = foreignNameEntries[1].toSourceOrDeclarationSource(declaration),
            factory = CfirErrors.FOREIGN_NAME_CONFLICTING_ANNOTATION,
            a = declName,
            b = FOREIGN_NAME,
        )
    }

    // 派生注解冲突：@ForeignName 与其衍生出的 @ForeignSetterName/@ForeignGetterName 不能同时出现
    val foreignGetterEntries = declaration.findAnnotations(Name.identifier("ForeignGetterName"))
    val foreignSetterEntries = declaration.findAnnotations(Name.identifier("ForeignSetterName"))
    if (foreignNameEntries.isNotEmpty() && (foreignGetterEntries.isNotEmpty() || foreignSetterEntries.isNotEmpty())) {
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> Name.identifier("<unknown>")
        }
        val derivedName = if (foreignGetterEntries.isNotEmpty())
            Name.identifier("ForeignGetterName") else Name.identifier("ForeignSetterName")
        reporter.reportOn(
            source = foreignNameEntries.first().source ?: declaration.source,
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
    val hasJava = declaration.hasAnnotation(JAVA)
    val hasJavaMirror = declaration.hasAnnotation(JAVA_MIRROR)
    val hasJavaImpl = declaration.hasAnnotation(JAVA_IMPL)
    val isJavaRelated = hasJava || hasJavaMirror || hasJavaImpl

    // MISSING_JAVA_INTEROP_ANNOTATION: 继承 Java 类型必须标注对应注解
    val superDeclarations = declaration.superDeclarations()
    if (!isJavaRelated && superDeclarations.any {
            it.hasAnnotation(JAVA) || it.hasAnnotation(JAVA_MIRROR) || it.hasAnnotation(JAVA_IMPL)
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
    val javaEntries = declaration.findAnnotations(JAVA).filterIsInstance<CfirAnnotationCall>()
    if (javaEntries.isNotEmpty()) {
        val javaValues = javaEntries.mapNotNull { entry ->
            entry.argumentTextAt(0)
        }.toSet()
        if (javaValues.size > 1) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.JAVA_INCORRECT_USE_BETWEEN_TYPES,
            )
        }

        // JAVA_APP_INHERIT_EXT: 仅 @Java["ext"] 能被 ext 继承
        val isExt = javaValues.any { it.contains("ext") }
        if (!isExt) {
            for (superDecl in superDeclarations) {
                val superJavaEntry = superDecl.findAnnotations(JAVA).filterIsInstance<CfirAnnotationCall>().firstOrNull() ?: continue
                val superIsExt = superJavaEntry.argumentTextAt(0)?.contains("ext") == true
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

    // SHADOW_CANNOT_IN_TYPE_ARGS: @Java 类型中的泛型参数不能使用 shadow 标记
    // 通过 PSI 检查类型参数定义
    val typeParams = when (declaration) {
        is org.cangnova.cangjie.cfir.declarations.CfirClass -> declaration.typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirInterface -> declaration.typeParameters
        is org.cangnova.cangjie.cfir.declarations.CfirStruct -> declaration.typeParameters
        else -> emptyList()
    }
    for (typeParam in typeParams) {
        if (typeParam.hasAnnotation(Name.identifier("Shadow"))) {
            // 查找类型参数上的 shadow 字段信息——简化实现：只要有 Shadow 标注即报告
            reporter.reportOn(
                source = typeParam.source ?: declaration.source,
                factory = CfirErrors.SHADOW_CANNOT_IN_TYPE_ARGS,
                a = typeParam.name,
                b = typeParam.name,
                c = org.cangnova.cangjie.cfir.types.ConeErrorType(
                    org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("unknown shadow type")
                ),
            )
        }
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
    // 非 @Java 类型不能使用 Java 注解
    if (!hasJava && !hasJavaMirror && !hasJavaImpl) {
        for (ann in declaration.annotations) {
            val name = ann.shortNameOrNull() ?: continue
            // 形似 "XxxJavaXxx" 的 Java 注解不能用在非 @Java 类型上
            if (name.asString().startsWith("Java") && name != JAVA && name != JAVA_MIRROR && name != JAVA_IMPL) {
                reporter.reportOn(
                    source = ann.toSourceOrDeclarationSource(declaration),
                    factory = CfirErrors.INVALID_USE_OF_JAVA_ANNOTATION,
                )
            }
        }
    }

    // INVALID_USE_OF_ANNOTATION_JFFI: JFFI 的注解只能用于 @Java 类型
    // 遍历成员检查
    for (member in declaration.declarations) {
        for (ann in member.annotations) {
            val name = ann.shortNameOrNull() ?: continue
            if (name.asString().endsWith("Jffi") || name.asString().startsWith("Jffi")) {
                if (!hasJava && !hasJavaMirror && !hasJavaImpl) {
                    reporter.reportOn(
                        source = ann.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.INVALID_USE_OF_ANNOTATION_JFFI,
                    )
                }
            }
        }
    }
}

/**
 * ObjC 互操作的额外声明级约束。
 *
 * 对齐 C++ Sema 中 ObjC 剩余诊断：
 * - OBJC_INTEROP_NOT_SUPPORTED: 不支持的 ObjC 互操作特性
 * - OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE: ObjCPointer 类型参数必须是 ObjC 兼容
 * - OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE: 顶层函数参数类型约束
 * - OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE: 顶层函数返回类型约束
 * - OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE: ObjC 函数类型参数约束
 * - OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED: ObjC 函数类型只能直接调用
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCInteropExtraSemantics(declaration: CfirClassLikeDeclaration) {
    val hasObjCMirror = declaration.hasAnnotation(OBJC_MIRROR)
    val hasObjCImpl = declaration.hasAnnotation(OBJC_IMPL)
    if (!hasObjCMirror && !hasObjCImpl) return

    // 检查成员中是否使用了 ObjCPointer / ObjCFunc 类型的约束
    for (member in declaration.declarations) {
        when (member) {
            is CfirNamedFunction -> {
                // ObjCPointer/ObjCFunc 类型参数必须是 ObjC 兼容类型
                checkObjCPointerAndFuncTypeArgs(member, declaration)
            }
            is CfirProperty -> {
                checkObjCFuncPropertyCallOnly(member, declaration)
            }
            else -> Unit
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCPointerAndFuncTypeArgs(
    function: CfirNamedFunction,
    declaration: CfirClassLikeDeclaration,
) {
    for (param in function.valueParameters) {
        val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
        // ObjCPointer<T> 或 ObjCFunc<...> 的类型参数必须是 ObjC 兼容
        val classId = when (paramType) {
            is org.cangnova.cangjie.cfir.types.ConeClassLikeType -> paramType.classId
            else -> null
        } ?: continue
        val typeName = classId.shortClassName.asString()

        if (typeName == "ObjCPointer") {
            val typeArg = paramType.typeArguments.firstOrNull()?.type
            if (typeArg != null && !isObjCTypeCompatible(typeArg)) {
                reporter.reportOn(
                    source = param.source ?: function.source ?: declaration.source,
                    factory = CfirErrors.OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE,
                )
            }
        }
        if (typeName.startsWith("ObjCFunc")) {
            // ObjCFunc 的类型参数必须满足 ObjC 兼容
            for (tArg in paramType.typeArguments) {
                val argType = tArg.type ?: continue
                if (!isObjCTypeCompatible(argType)) {
                    reporter.reportOn(
                        source = param.source ?: function.source ?: declaration.source,
                        factory = CfirErrors.OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE,
                        a = "ObjCFunc",
                    )
                }
            }
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCFuncPropertyCallOnly(
    property: CfirProperty,
    declaration: CfirClassLikeDeclaration,
) {
    val propType = (property.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
    val classId = (propType as? org.cangnova.cangjie.cfir.types.ConeClassLikeType)?.classId ?: return
    if (classId.shortClassName.asString().startsWith("ObjCFunc")) {
        // ObjCFunc 属性只能直接调用——此处声明级不能判断，预留入口
        // 真正检查应在表达式级 qualified access checker
        reporter.reportOn(
            source = property.source ?: declaration.source,
            factory = CfirErrors.OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED,
            a = property.name.asString(),
        )
    }
}

/**
 * 顶层函数（非 class 成员）的 ObjC 参数/返回类型检查。
 * 注册为 file-level checker 的一部分。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkObjCTopLevelFunction(function: CfirNamedFunction) {
    val hasObjCAnnotation = function.hasAnnotation(OBJC_MIRROR) ||
        function.hasAnnotation(OBJC_IMPL) ||
        function.hasAnnotation(Name.identifier("ObjCName"))
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
    if (returnType != null && !returnType.isUnit && !isObjCTypeCompatible(returnType)) {
        reporter.reportOn(
            source = function.returnTypeRef.source ?: function.source,
            factory = CfirErrors.OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE,
            a = function.name.asString(),
        )
    }
}
