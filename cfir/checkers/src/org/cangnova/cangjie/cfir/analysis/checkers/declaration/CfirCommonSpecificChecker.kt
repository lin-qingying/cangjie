package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.annotations.supportsBuiltinAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.expressions.annotationVersionSupport
import org.cangnova.cangjie.cfir.expressions.isSupportedBuiltinAnnotation
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.expressions.platformAnnotationKind
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.session.CfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.primaryBindingNameOrNull
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * common/specific 跨平台匹配检查器（CommonSpecific 分组）
 *
 * 对齐 C++ CJMP/CheckCJMP.cpp:
 * 当编译 specific 模块时，对 specific 声明与 common 声明进行配对检查，
 * 验证类型、修饰符、注解、参数、超类型的一致性。
 *
 * 注册为 classLikeCheckers
 */
object CfirCommonSpecificChecker : CfirClassLikeChecker() {
    /**
     * 分发 common/specific class 声明的匹配、约束和注解检查。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // 覆盖面（G21）：class/struct/interface/enum 全量参与（官方 MatchNominativeDecl 面）；
        // typealias 不参与 CJMP 配对（修饰符谓词已拒绝其携带 cjmp）。
        if (declaration is CfirTypeAlias) return

        // 版本门（D16 单入口）。
        if (!CjmpGate.isEnabled(context)) return

        // 配对结论（NOT_MATCHED / 种类 / 修饰符 / 超类型 / 穷尽性）统一由 CfirCjmpMatchingChecker
        // 与 CfirCjmpCommonSideChecker 消费配对存储报告；此处保留 specific 额外约束及
        // common CJO 与未标记 direct member 之间的普通 overload 冲突检查。
        if (declaration.status.isSpecific) {
            checkUnmarkedDirectMemberOverloadConflicts(declaration)
            checkSpecificExtraConstraints(declaration)
        }

        // 对 common 声明执行约束检查
        if (declaration.status.isCommon) {
            checkCommonDeclarationConstraints(declaration)
        }

        // common/specific 声明的修饰符和注解限制
        if (declaration.status.isCommon || declaration.status.isSpecific) {
            checkCJMPAbstractClassMembers(declaration)
            // 注意：explicitly abstract 的 sema 诊断无官方触发点（origin/main 全文无引用），
            // 该形态由 parse 变体统一负责（CfirCjmpParseRulesChecker，1.1.3 实测消息对位），
            // 此处不再重复检查（Phase 3 收敛：sema 条目已登记为后续清理项）。
        }
    }

    /**
     * common CJO 与 specific nominal 的直接函数声明来自不同 session；一般 redeclaration
     * collector 看不到跨 session 的同签名声明。未标记为 specific 的 specific 直接成员仍按
     * 官方 `PreCheckFuncRedefinition` 与 common 成员组成普通 overload conflict。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkUnmarkedDirectMemberOverloadConflicts(specificOwner: CfirClassLikeDeclaration) {
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val storage = context.session.cjmpMappingStorageOrNull ?: return
        val commonOwner = storage.commonFor(specificOwner) as? CfirClassLikeDeclaration ?: return
        val classTypeParameterMapping = storage.typeParameterMappingFor(specificOwner)
        val reportedSignatures = linkedSetOf<List<String>>()

        for (specificFunction in specificOwner.declarations.filterIsInstance<CfirNamedFunction>()) {
            if (specificFunction.status.isSpecific) continue

            for (commonFunction in commonOwner.declarations.filterIsInstance<CfirNamedFunction>()) {
                if (!commonFunction.status.isCommon) continue
                if (commonFunction.name != specificFunction.name) continue
                if (commonFunction.status.isStatic != specificFunction.status.isStatic) continue
                if (commonFunction.typeParameters.size != specificFunction.typeParameters.size) continue
                if (commonFunction.valueParameters.size != specificFunction.valueParameters.size) continue

                val typeParameterMapping = buildMap {
                    putAll(classTypeParameterMapping)
                    commonFunction.typeParameters.zip(specificFunction.typeParameters).forEach { (common, specific) ->
                        put(common, specific)
                    }
                }
                val commonToSpecificSubstitutor =
                    CfirTypeSubstitutorByMap.fromTypeParameterMapping(typeParameterMapping)
                val sameParameterSignature = commonFunction.valueParameters.zip(specificFunction.valueParameters)
                    .all { (commonParameter, specificParameter) ->
                        val commonType = commonParameter.returnTypeRef.coneTypeOrNull ?: return@all false
                        val specificType = specificParameter.returnTypeRef.coneTypeOrNull ?: return@all false
                        val mappedCommonType = commonToSpecificSubstitutor.substituteOrSelf(commonType)
                        commonType !is ConeErrorType && specificType !is ConeErrorType &&
                                AbstractTypeChecker.equalTypes(context.session.typeContext, mappedCommonType, specificType)
                    }
                if (!sameParameterSignature) continue

                val signature = specificFunction.valueParameters.map { parameter ->
                    parameter.returnTypeRef.coneTypeOrNull?.toString().orEmpty()
                }
                if (!reportedSignatures.add(signature)) continue
                reporter.reportOn(
                    source = specificFunction.source,
                    factory = CfirErrors.CONFLICTING_OVERLOADS,
                    a = listOf(specificFunction.name.asString()),
                )
                break
            }
        }
    }

    /**
     * common 声明的约束检查。
     *
     * - common open class 必须有构造器
     * - common 成员带有 explicitly abstract 不能有函数体
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonDeclarationConstraints(commonDecl: CfirClassLikeDeclaration) {
        // common open class 必须有显式构造器
        if (commonDecl.status.isOpen) {
            val hasConstructor = commonDecl.declarations.any {
                it is org.cangnova.cangjie.cfir.declarations.CfirConstructor
            }
            if (!hasConstructor) {
                reporter.reportOn(
                    source = commonDecl.source,
                    factory = CfirErrors.COMMON_OPEN_CLASS_NO_INIT,
                    a = commonDecl.name,
                )
            }
        }

        // explicitly abstract 成员不能有函数体
        for (member in commonDecl.declarations) {
            if (member is CfirNamedFunction && member.status.isAbstract && member.body != null) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY,
                    a = "function",
                )
            }
        }
    }

    /**
     * specific 声明的额外约束：
     * - open abstract specific 不能替代 open common
     * - 非 specific 抽象成员不能在 specific 类中
     * - specific 类不能同时有多个相同的 extension
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSpecificExtraConstraints(specificDecl: CfirClassLikeDeclaration) {
        // 配对结果消费存储（与 checkSpecificMatchesCommon 同源）。
        val commonDecl = context.session.cjmpMappingStorageOrNull?.commonFor(specificDecl) as? CfirClassLikeDeclaration

        // open abstract specific 不能替代 open common
        if (specificDecl.status.isOpen && specificDecl.status.isAbstract && commonDecl != null) {
            if (commonDecl.status.isOpen && !commonDecl.status.isAbstract) {
                reporter.reportOn(
                    source = specificDecl.source,
                    factory = CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON,
                    a = "class",
                    b = "class",
                )
            }
        }

        // 官方判据（CheckCJMP.cpp:1310-1338 CheckAbstractClassMembers）：
        // specific **abstract class**（仅 CLASS_DECL）的成员不得为非 specific 的抽象成员；
        // 成员限 function/property，且跳过来自 common 部分的成员。
        if (specificDecl is CfirClass && specificDecl.status.isAbstract && specificDecl.status.isSpecific) {
            for (member in specificDecl.declarations) {
                val memberKind = when (member) {
                    is CfirNamedFunction -> "function"
                    is CfirProperty -> "property"
                    else -> continue
                }
                if (member.status.isAbstract && !member.status.isSpecific && !member.status.isCommon) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS,
                        a = specificDecl.name,
                        b = memberKind,
                    )
                }
            }
        }

        // specific 同名 extension 不能重复
        val extensionNames = mutableMapOf<Name, Int>()
        for (member in specificDecl.declarations) {
            if (member !is org.cangnova.cangjie.cfir.declarations.CfirExtend) continue
            val extendedTypeRef = member.extendedTypeRef
            val extendedType = (extendedTypeRef as? CfirResolvedTypeRef)?.coneType
            val className = when (extendedType) {
                is org.cangnova.cangjie.cfir.types.ConeClassLikeType -> extendedType.classId.shortClassName
                is org.cangnova.cangjie.cfir.types.ConeStructType -> extendedType.classId.shortClassName
                is org.cangnova.cangjie.cfir.types.ConeEnumType -> extendedType.classId.shortClassName
                else -> null
            } ?: continue
            val count = extensionNames.getOrDefault(className, 0) + 1
            extensionNames[className] = count
            if (count == 2) {
                reporter.reportOn(
                    source = member.source,
                    factory = CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS,
                    a = className,
                )
            }
        }

        // SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR / SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL 不在此报告：
        // 官方只在 MatchCJMPFunction 把 specific 构造器与 common 标记的主构造器配对时触发（CheckCJMP.cpp:967-980），
        // 而主构造器不能带 common/specific（WRONG_MODIFIER_TARGET），cjc 1.1.3 无可达触发点。
        // 此前按"common 侧存在任意主构造器 / specific 标记主构造器"的启发式报告，会在合法程序上误报。
    }

    /**
     * common/specific 抽象类成员必须有明确修饰符。
     *
     * 对齐 C++ DiagKind::sema_cjmp_abstract_class_member_has_no_explicit_modifier
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCJMPAbstractClassMembers(decl: CfirClassLikeDeclaration) {
        if (!decl.status.isAbstract) return
        if (!decl.status.isCommon && !decl.status.isSpecific) return

        for (member in decl.declarations) {
            if (member !is CfirNamedFunction) continue
            // open/abstract 修饰符必须显式
            if (!member.status.isOpen && !member.status.isAbstract && !member.status.isStatic) {
                // 成员既不是 open 也不是 abstract，可能缺少修饰符
                if (member.body != null && !member.status.isSpecific) continue // 非抽象成员不需要
                reporter.reportOn(
                    source = member.source ?: decl.source,
                    factory = CfirErrors.CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER,
                    a = decl.name,
                    b = "function",
                    c = "open/abstract",
                )
            }
        }
    }

    /**
     * 取得声明在 common/specific 成员诊断中的名称。
     */
    private fun memberName(decl: CfirDeclaration): Name? = when (decl) {
        is CfirNamedFunction -> decl.name
        is CfirProperty -> decl.name
        is CfirFieldVariable -> decl.name
        else -> null
    }

    /**
     * 取得声明在 common/specific 成员诊断中的种类文本。
     */
    private fun memberKind(decl: CfirDeclaration): String? = when (decl) {
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        is CfirFieldVariable -> "field"
        else -> null
    }
}

/** common/specific 声明通用限制；覆盖 class-like、顶层 callable 与 extend。 */
object CfirCommonSpecificDeclarationChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.status.isCommon && !member.status.isSpecific) return
        if (!CjmpGate.isEnabled(context)) return

        checkGenericFrozen(member)
        checkCommonSpecificAnnotations(member)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericFrozen(declaration: CfirMemberDeclaration) {
        if (declaration.typeParameters.isEmpty()) return
        val frozen = declaration.annotations
            .filterIsInstance<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall>()
            .firstOrNull { annotation ->
                annotation.annotationKind == BuiltInAnnotationKind.FROZEN &&
                        annotation.isSupportedBuiltinAnnotation(
                            BuiltInAnnotationKind.FROZEN,
                            context.languageVersionSettings,
                        )
            } ?: return
        reporter.reportOn(
            source = frozen.source ?: declaration.source,
            factory = CfirErrors.COMMON_GENERIC_FROZEN_NOT_SUPPORTED,
            a = cjmpDeclKind(declaration),
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkCommonSpecificAnnotations(declaration: CfirDeclaration) {
        for (annotation in declaration.annotations) {
            val call = annotation as? org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall ?: continue
            val builtinKind = annotation.annotationKind
            val platformKind = call.platformAnnotationKind
            val supportedPlatform = call.annotationVersionSupport(context.languageVersionSettings) ==
                    org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus.SUPPORTED
            val supportedBuiltin = builtinKind?.let { kind ->
                call.isSupportedBuiltinAnnotation(kind, context.languageVersionSettings)
            } ?: true
            if (supportedBuiltin && builtinKind in DISALLOWED_ON_COMMON_SPECIFIC ||
                supportedPlatform && platformKind in DISALLOWED_PLATFORM_ON_COMMON_SPECIFIC
            ) {
                val name = call.builtInDescriptor?.sourceName
                    ?: call.platformAnnotationDescriptor?.sourceName
                    ?: continue
                reporter.reportOn(
                    source = call.source ?: declaration.source,
                    factory = CfirErrors.COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED,
                    a = Name.identifier(name),
                )
            }
        }
    }
}

/** Common 编译中的顶层 common extend 检查，对齐官方 `CheckCommonExtensions`。 */
object CfirCommonSpecificExtendChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        if (!declaration.status.isCommon || declaration.superTypeRefs.isNotEmpty()) return
        if (!CjmpGate.isEnabled(context) || context.session.cjmpSettings.mode != CfirCjmpMode.COMMON) return

        val targetKey = (declaration.extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.toString() ?: return
        val packageName = context.session.extendProvider.getPackageFqName(declaration)
            ?: context.session.extendProvider.getContainingFile(declaration)?.packageDirective?.packageFqName
            ?: return
        data class PrivateMember(val extend: CfirExtend, val declaration: CfirDeclaration, val name: Name, val kind: String)

        val matchingExtends = context.session.extendProvider.getAllExtends().filter { candidate ->
            candidate.status.isCommon && candidate.superTypeRefs.isEmpty() &&
                    candidate.moduleData.name == declaration.moduleData.name &&
                    (context.session.extendProvider.getPackageFqName(candidate)
                        ?: context.session.extendProvider.getContainingFile(candidate)?.packageDirective?.packageFqName) == packageName &&
                    (candidate.extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.toString() == targetKey
        }
        val privateMembers = matchingExtends.flatMap { extend ->
            extend.declarations.mapNotNull { member ->
                val (name, kind, visibility) = when (member) {
                    is CfirNamedFunction -> Triple(member.name, "function", member.status.visibility)
                    is CfirProperty -> Triple(member.name, "property", member.status.visibility)
                    else -> return@mapNotNull null
                }
                if (visibility != org.cangnova.cangjie.descriptors.Visibilities.Private) return@mapNotNull null
                PrivateMember(extend, member, name, kind)
            }
        }

        for ((name, duplicates) in privateMembers.groupBy { it.name }) {
            val ordered = duplicates.sortedWith(
                compareBy<PrivateMember>(
                    { context.session.extendProvider.getContainingFile(it.extend)?.sourceFile?.path.orEmpty() },
                    { it.declaration.source?.startOffset ?: Int.MAX_VALUE },
                ),
            )
            for ((index, duplicate) in ordered.withIndex()) {
                if (duplicate.extend !== declaration || index == 0) continue
                reporter.reportOn(
                    source = duplicate.declaration.source ?: declaration.source,
                    factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS,
                    a = CfirCjmpCommonSideFacts.declName(declaration),
                    b = duplicate.kind,
                    c = name,
                )
            }
        }

        for (privateMember in privateMembers.filter { it.extend === declaration }) {
            val member = privateMember.declaration
            val name = privateMember.name
            val kind = privateMember.kind
            if ((member as? CfirMemberDeclaration)?.status?.isCommon != true) continue
            reporter.reportOn(
                source = member.source ?: declaration.source,
                factory = CfirErrors.COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS,
                a = kind,
                b = name,
            )
        }
    }
}

/**
 * common 包 main 函数检查器
 *
 * 对齐官方 `TypeCheckerImpl::CheckEntryFunc` 的 `sema_common_package_has_main`（cjc 1.1.3）：判据是**文件级**
 * `File::isCommon`——common 编译（CHIR 输出）中，文件内任一声明带 `common` 修饰符即成为 common package part
 * 文件（`MPParserImpl::CheckCJMPModifiers` 置位），该文件中的 main 报错（与 main 在文件中的先后无关）；
 * 同包其它不含 common 声明的文件中的 main 不报。`common main()` 本身由修饰符规则拒绝，不使文件成为 common。
 *
 * 注册为 fileCheckers
 */
object CfirCommonPackageMainChecker : CfirFileChecker() {
    override val requiresImplementation: Boolean get() = true

    /**
     * 检查 common package part 文件中是否声明了 main 函数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        // 版本门 + 模式门（G12 逃逸修复）：非 1.1+ 语义或非 CJMP 编译下整族静默；common part 只存在于 common 编译。
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.COMMON) return
        if (declaration.declarations.none { it.declaresCjmpCommon() }) return
        for (decl in declaration.declarations) {
            if (decl is org.cangnova.cangjie.cfir.declarations.CfirMainFunction) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.COMMON_PACKAGE_HAS_MAIN,
                )
            }
        }
    }

    /**
     * 声明自身或其成员带 `common` 修饰符（官方对每个经 `ParseDecl` 解析的声明调用 `CheckCJMPModifiers`，成员亦然）。
     */
    private fun CfirDeclaration.declaresCjmpCommon(): Boolean {
        if (this is org.cangnova.cangjie.cfir.declarations.CfirMainFunction) return false
        if ((this as? CfirMemberDeclaration)?.status?.isCommon == true) return true
        val members = when (this) {
            is CfirClassLikeDeclaration -> declarations
            is CfirExtend -> declarations
            else -> return false
        }
        return members.any { it.declaresCjmpCommon() }
    }
}

/**
 * Mock 语义检查器（Mock 分组）
 *
 * 注册为 classLikeCheckers
 */
object CfirMockSemanticsChecker : CfirClassLikeChecker() {
    /**
     * 检查 class-like 声明中 mock 相关静态成员限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // Mock 检查需要编译选项 API
        // 当 mock 功能启用时检查 static 声明约束
        for (member in declaration.declarations) {
            if (member !is CfirNamedFunction) continue
            if (!member.status.isStatic) continue

            // static/private/local/constructor 声明不能被 mock
            // `Mock` is a user/system macro identity, not an official built-in
            // annotation kind. Without a resolved macro declaration identity
            // this checker must not infer it from a short name.
        }
    }
}

/**
 * 取得声明上的解析注解身份列表（保留出现次数）。
 *
 * common/specific 匹配必须比较 builtin kind 或 resolved ClassId；短名相同的两个
 * custom annotation 不能被视为同一注解。**保留重复项**：官方按出现次数逐次配对
 *（CheckCJMPAnnotations.cpp:253-296）。
 *
 * special-handled 注解不参与该比较（官方 `IsSpecialHandledAnnotation`）：
 * @Deprecated / @Attribute / 非序列化家族（C、JAVA_HAS_DEFAULT、OBJ_C_MIRROR、
 * OBJ_C_INIT、OBJ_C_OPTIONAL）/ 不支持家族（JAVA、CALLING_CONV、FOREIGN_GETTER_NAME、
 * FOREIGN_SETTER_NAME、CONSTSAFE、ENSURE_PREPARED_TO_MOCK、NON_PRODUCT）——
 * 其中 @Deprecated 由 [CfirCjmpMatchingChecker] 按官方 `PostCheckDeprecatedAnnotation` 单独负责。
 */
private fun CfirDeclaration.annotationMatches(
    settings: org.cangnova.cangjie.LanguageVersionSettings,
): List<CjmpAnnotationMatch> =
    annotations.mapNotNull { annotation ->
        val key = when {
            annotation.annotationKind != null &&
                ((annotation as? org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall)
                    ?.isSupportedBuiltinAnnotation(annotation.annotationKind!!, settings)
                    ?: settings.supportsBuiltinAnnotationKind(annotation.annotationKind!!)) ->
                AnnotationMatchKey.BuiltIn(annotation.annotationKind!!)

            annotation is org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall &&
                annotation.platformAnnotationKind != null &&
                annotation.annotationVersionSupport(settings) ==
                org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus.SUPPORTED ->
                AnnotationMatchKey.Platform(annotation.platformAnnotationKind!!)
            annotation.annotationClassId != null -> AnnotationMatchKey.Resolved(annotation.annotationClassId!!)
            else -> null
        }
        key?.takeUnless { it.isSpecialHandledForCjmpMatch() }?.let { CjmpAnnotationMatch(it, annotation) }
    }

/**
 * 官方 `IsSpecialHandledAnnotation`（CheckCJMPAnnotations.cpp:198-203）对位：
 * 这些注解不参与 common/specific 注解多重集比较。
 */
private fun AnnotationMatchKey.isSpecialHandledForCjmpMatch(): Boolean = when (this) {
    is AnnotationMatchKey.BuiltIn -> kind in SPECIAL_HANDLED_BUILTIN_ANNOTATIONS
    is AnnotationMatchKey.Platform -> kind in SPECIAL_HANDLED_PLATFORM_ANNOTATIONS
    is AnnotationMatchKey.Resolved -> false
}

/** 官方 non-serialized + unsupported + deprecated/attribute 家族（BuiltIn 侧对位）。 */
private val SPECIAL_HANDLED_BUILTIN_ANNOTATIONS: Set<BuiltInAnnotationKind> = setOf(
    BuiltInAnnotationKind.DEPRECATED,
    BuiltInAnnotationKind.ATTRIBUTE,
    BuiltInAnnotationKind.C,
    BuiltInAnnotationKind.JAVA_HAS_DEFAULT,
    BuiltInAnnotationKind.OBJ_C_MIRROR,
    BuiltInAnnotationKind.OBJ_C_INIT,
    BuiltInAnnotationKind.OBJ_C_OPTIONAL,
    BuiltInAnnotationKind.JAVA,
    BuiltInAnnotationKind.CALLING_CONV,
    BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
    BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
    BuiltInAnnotationKind.CONSTSAFE,
    BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
    BuiltInAnnotationKind.NON_PRODUCT,
)

/** 平台注解身份侧的官方 special-handled 家族。 */
private val SPECIAL_HANDLED_PLATFORM_ANNOTATIONS: Set<CangjiePlatformAnnotationKind> = setOf(
    CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT,
    CangjiePlatformAnnotationKind.OBJ_C_MIRROR,
    CangjiePlatformAnnotationKind.OBJ_C_INIT,
    CangjiePlatformAnnotationKind.OBJ_C_OPTIONAL,
    CangjiePlatformAnnotationKind.FOREIGN_GETTER_NAME,
    CangjiePlatformAnnotationKind.FOREIGN_SETTER_NAME,
)

/** common 注解在匹配后传播到 specific；只报告 specific 侧仍无法与 common 一一匹配的注解。 */
private fun firstUnmatchedSpecificAnnotation(
    specific: List<CjmpAnnotationMatch>,
    common: List<CjmpAnnotationMatch>,
): CjmpAnnotationMatch? {
    val remainingCommon = common.groupingBy { it.key }.eachCount().toMutableMap()
    for (annotation in specific) {
        val remaining = remainingCommon[annotation.key] ?: 0
        if (remaining == 0) return annotation
        remainingCommon[annotation.key] = remaining - 1
    }
    return null
}

/**
 * 注解身份匹配键，用于 common/specific 双份声明的注解一致性比较。
 *
 * [BuiltIn] 按官方 kind 比较，[Platform] 按平台注解身份比较，
 * [Resolved] 按解析出的 ClassId 比较；三者互不相等，避免不同身份体系误判等价。
 */
private sealed interface AnnotationMatchKey {
    data class BuiltIn(val kind: BuiltInAnnotationKind) : AnnotationMatchKey
    data class Platform(val kind: CangjiePlatformAnnotationKind) : AnnotationMatchKey
    data class Resolved(val classId: org.cangnova.cangjie.name.ClassId) : AnnotationMatchKey
}

private data class CjmpAnnotationMatch(
    val key: AnnotationMatchKey,
    val annotation: org.cangnova.cangjie.cfir.expressions.CfirAnnotation,
)

/**
 * Deprecated 注解名。
 */
/** Deprecated 诊断参数的稳定显示名；语义判断使用 [BuiltInAnnotationKind.DEPRECATED]。 */
private val DEPRECATED_NAME = Name.identifier("Deprecated")

/**
 * 不允许出现在 common/specific 声明上的注解。
 * 对齐 C++ MPTypeCheckerImpl::CheckNotAllowedAnnotations。
 */
private val DISALLOWED_ON_COMMON_SPECIFIC: Set<BuiltInAnnotationKind> = setOf(
    // C/Java AST 互操作身份不能与 common/specific 共存
    BuiltInAnnotationKind.C,
    BuiltInAnnotationKind.JAVA,
    BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
)

/** common/specific 声明上禁止出现的平台注解身份，对齐官方 CheckNotAllowedAnnotations 的平台侧集合。 */
private val DISALLOWED_PLATFORM_ON_COMMON_SPECIFIC: Set<CangjiePlatformAnnotationKind> = setOf(
    CangjiePlatformAnnotationKind.JAVA_MIRROR,
    CangjiePlatformAnnotationKind.JAVA_IMPL,
)

/**
 * CJMP 配对结论检查器（specific 方向；逐条对位 cjc 1.1.3 `CheckCJMP.cpp`）。
 *
 * 只在 specific 编译（mode=SPECIFIC）且版本门开启时工作，消费 CJMP_MATCHING 阶段写入的
 * [org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage]：
 * - 未配对：`DiagNotMatchedDecl(decl, "specific", …, "common")`；参数级失败先在参数上报告
 *  （`sema_specific_has_different_parameter` / `sema_cjmp_parameter_default_value_both_sides`）；
 *   nominal 种类不同只报 `specific_has_different_kind`、common 带默认实现而 specific 为 abstract 只报
 *   `sema_specific_member_must_have_implementation`（官方同位置后报诊断被诊断引擎去重，1.1.3 实测）；
 * - 已配对：修饰符（`MatchCJMPDeclAttrs`）、变量/属性类型（`MatchCJMPVar`/`MatchCJMPProp`）、
 *   var/let、注解多重集（`MatchCJMPDeclAnnotations`）、第二绑定（`TrySetSpecificImpl`）、
 *   nominal 超类型与枚举穷尽性（`MatchCommonNominalDeclWithSpecific`/`MatchNominativeDecl`）。
 */
object CfirCjmpMatchingChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (declaration is CfirEnumConstructor) {
            checkEnumConstructor(declaration)
            return
        }
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.status.isSpecific) return
        if (declaration is CfirTypeAlias) return
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val storage = context.session.cjmpMappingStorageOrNull ?: return

        if (declaration is CfirExtend && storage.isDuplicateSpecificExtend(declaration)) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS,
                a = CfirCjmpCommonSideFacts.declName(declaration),
            )
            return
        }

        val commonCounterparts = storage.commonCounterpartsFor(declaration)
        reportCandidateDiagnostics(declaration, storage)
        if (commonCounterparts.isNotEmpty()) {
            commonCounterparts.forEach { common ->
                checkMatched(declaration, common as CfirMemberDeclaration, storage)
            }
        } else {
            reportUnmatched(declaration, storage)
        }
    }

    /**
     * specific enum 的构造器（官方构造器随外层携带 SPECIFIC）：无 common 对应构造器即 NOT_MATCHED
     *（common enum 非穷尽时的多出构造器由配对阶段静默）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEnumConstructor(declaration: CfirEnumConstructor) {
        val enum = context.findClosestDeclaration<CfirEnum>() ?: return
        if (!enum.status.isSpecific) return
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val storage = context.session.cjmpMappingStorageOrNull ?: return
        val commonEnum = storage.commonFor(enum) as? CfirEnum
        if (commonEnum?.isNonExhaustive == true) return
        val signatureUnresolved = CjmpMismatchKind.TYPE_NOT_RESOLVED in storage.mismatchKindsFor(declaration)
        if (!storage.isUnmatched(declaration) && !signatureUnresolved) return
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.NOT_MATCHED,
            a = "specific",
            b = CfirCjmpCommonSideFacts.declInfo(declaration, enum),
            c = "common",
        )
    }

    /** 重放 first-fit 失败候选已触发的诊断；后续候选成功只决定映射，不撤销这些诊断。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportCandidateDiagnostics(
        declaration: CfirDeclaration,
        storage: CfirCjmpMappingStorage,
    ) {
        for (diagnostic in storage.candidateDiagnosticsFor(declaration)) {
            when (diagnostic) {
                is CfirCjmpCandidateDiagnostic.MissingBody -> {
                    val callable = declaration as? CfirCallableDeclaration
                    val commonContainerName = diagnostic.commonCandidate?.let(::cjmpCandidateContainerName)
                        ?: callable?.symbol?.callableId?.classId?.shortClassName?.asString().orEmpty()
                    reporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION,
                        a = callable?.symbol?.name?.asString().orEmpty(),
                        b = commonContainerName,
                    )
                }
                is CfirCjmpCandidateDiagnostic.ParameterMismatch -> {
                    val mismatch = diagnostic.mismatch
                    val parameter = (declaration as? CfirFunction)?.valueParameters?.getOrNull(mismatch.parameterIndex)
                    reporter.reportOn(
                        source = parameter?.source ?: declaration.source,
                        factory = if (mismatch.kind == CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES) {
                            CfirErrors.CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES
                        } else {
                            CfirErrors.SPECIFIC_HAS_DIFFERENT_PARAMETER
                        },
                    )
                }
            }
        }
    }

    /** 对齐官方 `commonDecl.outerDecl.identifier`，同时覆盖 class 成员与 extend 成员。 */
    private fun cjmpCandidateContainerName(candidate: CfirDeclaration): String? {
        val callable = candidate as? CfirCallableDeclaration ?: return null
        val containingExtend = callable.moduleData.session.extendProvider.getContainingExtend(callable.symbol)
        return containingExtend?.let(CfirCjmpCommonSideFacts::declName)?.asString()
            ?: callable.symbol.callableId.classId?.shortClassName?.asString()
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnmatched(declaration: CfirDeclaration, storage: CfirCjmpMappingStorage) {
        val kinds = storage.mismatchKindsFor(declaration)
        if (!storage.isUnmatched(declaration) && kinds.isEmpty()) return

        // 官方 NeedToReportMissingBody 只报告成员缺实现诊断，不再追加 NOT_MATCHED。
        if (CjmpMismatchKind.MISSING_BODY in kinds) return

        if (declaration is CfirClassLikeDeclaration && CjmpMismatchKind.CLASS_KIND in kinds) {
            val common = storage.mismatchCandidatesFor(declaration)
                .filterIsInstance<CfirClassLikeDeclaration>()
                .firstOrNull()
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
                a = cjmpDeclKind(declaration),
                b = common?.let(::cjmpDeclKind) ?: cjmpDeclKind(declaration),
            )
            return
        }
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.NOT_MATCHED,
            a = "specific",
            b = cjmpDeclInfo(declaration),
            c = "common",
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMatched(specific: CfirDeclaration, common: CfirMemberDeclaration, storage: CfirCjmpMappingStorage) {
        val specificStatus = (specific as CfirMemberDeclaration).status

        // 官方 `PostCheckDeprecatedAnnotation`：specific 声明隐式继承 common 的弃用，显式 @Deprecated 即报（锚注解）
        specific.annotations.firstOrNull { it.annotationKind == BuiltInAnnotationKind.DEPRECATED }?.let { annotation ->
            reporter.reportOn(
                source = annotation.source ?: specific.source,
                factory = CfirErrors.SPECIFIC_HAS_DEPRECATED_ANNOTATION,
                a = DEPRECATED_NAME,
                b = cjmpDeclKind(specific),
                c = cjmpDeclName(specific),
            )
        }

        if (specific is CfirClassLikeDeclaration && common is CfirClassLikeDeclaration) {
            CfirOverrideChecker.checkGenericConstraintCompatibility(specific, listOf(common))
            if (!checkModifiers(specific, specificStatus, common, isNominal = true)) return
            if (specific.superTypeRefs.size != common.superTypeRefs.size) {
                reporter.reportOn(
                    source = specific.source,
                    factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE,
                    a = cjmpDeclKind(specific),
                )
                return
            }
            if (specific is CfirEnum && common is CfirEnum && !common.isNonExhaustive && specific.isNonExhaustive) {
                reporter.reportOn(
                    source = specific.source,
                    factory = CfirErrors.COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH,
                    a = cjmpDeclKind(common),
                    b = cjmpDeclKind(specific),
                )
            }
            return
        }

        // MatchCJMPVar / MatchCJMPProp：类型与 var/let 在配对后报告
        val specificType = callableType(specific)
        val commonType = callableType(common)
        val mappedCommonType = commonType?.let {
            CfirTypeSubstitutorByMap.fromTypeParameterMapping(storage.typeParameterMappingFor(specific))
                .substituteOrSelf(it)
        }
        if (specificType != null && mappedCommonType != null &&
            !AbstractTypeChecker.equalTypes(context.session.typeContext, specificType, mappedCommonType)
        ) {
            val kind = when (specific) {
                is CfirProperty -> "property"
                is CfirVariable -> if (specific.isVar) "var" else "let"
                else -> null
            }
            if (kind != null) {
                reporter.reportOn(source = specific.source, factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_TYPE, a = kind)
            }
        }
        if (specific is CfirVariable && common is CfirVariable && specific.isVar != common.isVar) {
            reporter.reportOn(
                source = specific.source,
                factory = CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET,
                a = if (specific.isVar) "var" else "let",
                b = if (common.isVar) "var" else "let",
            )
        }

        if (!checkModifiers(specific, specificStatus, common, isNominal = false)) return

        // 官方 `CheckCommonSpecificGenericMatch`：specific 泛型约束须不严于 common（按位置映射，D6）
        if (specific is CfirCallableDeclaration && common is CfirCallableDeclaration) {
            CfirOverrideChecker.checkGenericConstraintCompatibility(specific, listOf(common.symbol))
        }

        val specificAnnotations = specific.annotationMatches(context.languageVersionSettings)
        val commonAnnotations = common.annotationMatches(context.languageVersionSettings)
        val unmatchedSpecificAnnotation = firstUnmatchedSpecificAnnotation(specificAnnotations, commonAnnotations)
        if (unmatchedSpecificAnnotation != null) {
            reporter.reportOn(
                source = unmatchedSpecificAnnotation.annotation.source ?: specific.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION,
                a = cjmpDeclKind(specific),
            )
        }
    }

    /**
     * 官方 `MatchCJMPDeclAttrs`：nominal 比较 ABSTRACT/PUBLIC/OPEN/PROTECTED/C/SEALED，
     * 其余比较 STATIC/MUT/PRIVATE/PUBLIC/PROTECTED/FOREIGN/UNSAFE/OPEN/ABSTRACT；首个差异报告后返回 false。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkModifiers(
        specific: CfirDeclaration,
        specificStatus: CfirDeclarationStatus,
        common: CfirMemberDeclaration,
        isNominal: Boolean,
    ): Boolean {
        val commonStatus = common.status
        val isFuncOrProp = common is CfirNamedFunction || common is CfirProperty
        val abstractOrOpenDiffers = specificStatus.isAbstract != commonStatus.isAbstract ||
                specificStatus.isOpen != commonStatus.isOpen
        val sealedAbstractAllowed = isNominal && common is CfirClass && specificStatus.isSealed && commonStatus.isAbstract
        if (abstractOrOpenDiffers) {
            when {
                isFuncOrProp && specificStatus.isAbstract && commonStatus.isOpen -> {
                    val kind = if (common is CfirNamedFunction) "function" else "property"
                    reporter.reportOn(
                        source = specific.source,
                        factory = CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON,
                        a = kind,
                        b = kind,
                    )
                    return false
                }
                // abstract 成员可由 open 成员实现；两侧 static 的 abstract 同理（官方例外）
                isFuncOrProp && commonStatus.isAbstract && specificStatus.isOpen -> Unit
                commonStatus.isAbstract && commonStatus.isStatic && specificStatus.isStatic -> Unit
                sealedAbstractAllowed -> Unit
                else -> return reportModifier(specific)
            }
        }
        val differs = specificStatus.visibility != commonStatus.visibility ||
                (isNominal && specificStatus.isSealed != commonStatus.isSealed && !sealedAbstractAllowed) ||
                (isNominal && specificStatus.isC != commonStatus.isC) ||
                (!isNominal && (
                        specificStatus.isStatic != commonStatus.isStatic ||
                                specificStatus.isMut != commonStatus.isMut ||
                                specificStatus.isForeign != commonStatus.isForeign ||
                                specificStatus.isUnsafe != commonStatus.isUnsafe))
        if (differs) return reportModifier(specific)
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportModifier(specific: CfirDeclaration): Boolean {
        reporter.reportOn(
            source = specific.source,
            factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER,
            a = cjmpDeclKind(specific),
        )
        return false
    }

    private fun callableType(declaration: CfirDeclaration): ConeCangJieType? {
        val typeRef = when (declaration) {
            is CfirProperty -> declaration.returnTypeRef
            is CfirVariable -> declaration.returnTypeRef
            else -> return null
        }
        val type = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        return type.takeUnless { it is ConeErrorType }
    }
}

/**
 * CJMP 配对结论检查器（common 方向；官方 `MatchCJMPDecls` 的 common 声明循环 + `MustMatchWithSpecific`）。
 *
 * 在 specific 编译的 specific 会话里运行（配对存储在该会话）：对当前文件所在包的 common 声明
 *（来自 refinement 依赖模块，含 nominal 成员）逐个判定——未被任何 specific 绑定、且不在官方豁免面内
 *（`COMMON_WITH_DEFAULT`、interface 成员、enum 构造器）时报 `NOT_MATCHED("common", …, "specific")`，
 * 锚在 common 声明上（计划 G20：源码 common 有真实 source；反序列化 common 无 source 时不报，
 * 不改锚到 specific 侧）。同一包只处理一次。
 */
object CfirCjmpCommonSideChecker : CfirFileChecker() {
    override val requiresImplementation: Boolean get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val storage = context.session.cjmpMappingStorageOrNull ?: return
        val packageFqName = declaration.packageDirective.packageFqName
        // 同一包只处理一次：只在本会话该包的首个源文件上执行（无状态去重，检查器可重复运行）
        val firstFileOfPackage = runCatching {
            context.session.cfirProvider.getCfirFilesByPackage(packageFqName).firstOrNull()
        }.getOrNull()
        if (firstFileOfPackage != null && firstFileOfPackage !== declaration) return

        // common 声明取自 refinement 依赖模块自身的源文件（名称索引可能返回"未知"，不能据此枚举）；
        // 反序列化来源（CLI 加载 common cjo）没有源文件，按 G20 不在此报告
        val commonFiles = context.session.moduleData.allRefinementDependencies
            .flatMap { module ->
                val moduleSession = runCatching { module.session }.getOrNull() ?: return@flatMap emptyList()
                runCatching { moduleSession.cfirProvider.getCfirFilesByPackage(packageFqName) }.getOrDefault(emptyList())
            }
            .distinct()

        for (commonFile in commonFiles) {
            // 诊断归属 common 文件（跨文件诊断：由 specific 会话产出，锚在 common 声明，对位 Kotlin
            // MppCheckerKind.Platform 在 common 源上报告）
            val commonContext = CjmpCommonFileDiagnosticContext(commonFile, context)
            for ((common, outer) in CfirCjmpCommonSideFacts.commonDeclarationsWithOuter(commonFile.declarations)) {
                val source = common.source ?: continue
                when {
                    CfirCjmpCommonSideFacts.hasMultipleImplementations(common, storage) -> reporter.reportOn(
                        source = source,
                        factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
                        a = CfirCjmpCommonSideFacts.implementationKind(common),
                        context = commonContext,
                    )

                    CfirCjmpCommonSideFacts.mustReportNotMatched(common, outer, storage) -> reporter.reportOn(
                        source = source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = "common",
                        b = CfirCjmpCommonSideFacts.declInfo(common, outer),
                        c = "specific",
                        context = commonContext,
                    )
                }
            }
        }
    }

    /** common 文件归属的诊断上下文（抑制判定按 common 文件自身的上下文进行）。 */
    private class CjmpCommonFileDiagnosticContext(
        private val file: CfirFile,
        private val context: CheckerContext,
    ) : DiagnosticContext {
        private val fileContext: CheckerContext by lazy(LazyThreadSafetyMode.NONE) {
            org.cangnova.cangjie.cfir.analysis.checkers.context.PersistentCheckerContext(
                context.sessionHolder,
                context.returnTypeCalculator,
            ).enterFile(file)
        }

        override val languageVersionSettings: org.cangnova.cangjie.LanguageVersionSettings
            get() = context.languageVersionSettings
        override val containingFilePath: String? get() = file.sourceFile?.path
        override val isCrossFileDiagnostic: Boolean get() = true
        override fun isDiagnosticSuppressed(diagnostic: org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic): Boolean =
            fileContext.isDiagnosticSuppressed(diagnostic)
    }
}

private fun cjmpDeclKind(declaration: CfirDeclaration): String = CfirCjmpCommonSideFacts.declKind(declaration)

private fun cjmpDeclName(declaration: CfirDeclaration): Name = CfirCjmpCommonSideFacts.declName(declaration)

private fun cjmpDeclInfo(declaration: CfirDeclaration): String = CfirCjmpCommonSideFacts.declInfo(declaration)
