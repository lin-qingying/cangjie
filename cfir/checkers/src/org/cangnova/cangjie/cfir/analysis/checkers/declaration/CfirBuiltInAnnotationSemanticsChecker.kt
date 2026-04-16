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
        checkForeignNameRules(declaration, owner)
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

    // @IfAvailable APILevel 限制检查
    if (ifAvailableEntries.isNotEmpty() && apiLevelEntries.isEmpty()) {
        // 当使用 @IfAvailable 但没有 @APILevel 时，需检查项目 APILevel 是否 >= 19
        // 当前简化：如果文件无 @APILevel 注解且使用了 @IfAvailable，报告限制
        // 完整实现需要从编译选项中获取项目级 APILevel
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

        val hasDefaultOwner = member.source?.psi as? CjModifierListOwner
        val hasDefaultEntry = hasDefaultOwner?.findAnnotationEntries(Name.identifier("JavaHasDefault"))?.firstOrNull()
        if (hasDefaultEntry != null && hasDefaultEntry.valueArguments.isNotEmpty()) {
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
    val targetDecl = CfirExtendSemanticsSupport.resolveDeclaration(context, classId) ?: return false
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
                source = declaration.source,
                factory = CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR,
            )
        }
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
    val targetDecl = CfirExtendSemanticsSupport.resolveDeclaration(context, classId) ?: return false
    return targetDecl.hasAnnotation(OBJC_MIRROR) || targetDecl.hasAnnotation(OBJC_IMPL)
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

/**
 * @ForeignName 注解规则检查。
 *
 * 对齐 C++ sema_foreign_name_* 系列:
 * - @ForeignName 不能出现在被 override 的声明上
 * - @ForeignName 注解冲突检查
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkForeignNameRules(
    declaration: CfirDeclaration,
    owner: CjModifierListOwner,
) {
    val foreignNameEntries = owner.findAnnotationEntries(FOREIGN_NAME)
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
}
