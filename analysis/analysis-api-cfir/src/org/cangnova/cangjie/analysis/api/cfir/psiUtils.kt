package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CJ_DIAGNOSTIC_CONVERTER
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.extensionSignatureKey
import org.cangnova.cangjie.cfir.declarations.normalizeCfirExtendTypeText
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes2
import org.cangnova.cangjie.source.CjFakePsiSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.source.SuspiciousFakeSourceCheck
import org.cangnova.cangjie.source.psiOrNull
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 将 CFIR PSI 诊断转换成 Analysis API 公开诊断对象。
 */
internal fun CjPsiDiagnostic.asCaDiagnostic(analysisSession: CaCfirSession): CaDiagnosticWithPsi<*> {
    return CJ_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as CjDiagnostic)
}

/**
 * 允许从 fake source element 恢复 PSI 的 CFIR fake element 种类。
 *
 * 这些 fake element 对公开 API 来说有稳定的源码锚点，其他 fake source 仍保持不可见。
 */
private val allowedFakeElementKinds = setOf(
    CjFakeSourceElementKind.FromUseSiteTarget,
    CjFakeSourceElementKind.PropertyFromParameter,
    CjFakeSourceElementKind.ItLambdaParameter,
    CjFakeSourceElementKind.EnumGeneratedDeclaration,
    CjFakeSourceElementKind.DataClassGeneratedMembers,
    CjFakeSourceElementKind.ImplicitConstructor,
    CjFakeSourceElementKind.ImplicitJavaAnnotationConstructor,
    CjFakeSourceElementKind.SamConstructor,
    CjFakeSourceElementKind.JavaRecordComponentFunction,
    CjFakeSourceElementKind.PatternBindingVariable,
)

/**
 * 从 CFIR 元素恢复允许暴露给 Analysis API 的 PSI。
 *
 * 真实 PSI 会直接恢复到当前 compiled view；fake PSI 仅在白名单种类中暴露。
 */
@OptIn(SuspiciousFakeSourceCheck::class)
internal fun CfirElement.getAllowedPsi(preferredProject: Project? = null): PsiElement? = when (val source = source) {
    null -> null
    is CjRealPsiSourceElement -> source.psi.restoreCurrentCompiledPsi(preferredProject)
    is CjFakePsiSourceElement -> if (source.kind in allowedFakeElementKinds) source.psi.restoreCurrentCompiledPsi(preferredProject) else null
    else -> null
}

/**
 * 查找 CFIR 元素对应的公开 PSI。
 */
internal fun CfirElement.findPsi(preferredProject: Project? = null): PsiElement? = getAllowedPsi(preferredProject)

/**
 * compiled `.cjo` 的 source psi 可能在 IDE 生命周期里被新的 decompiled view provider 重建。
 *
 * low-level / analysis 缓存里的 CFIR 仍然会持有旧 provider 上的 PSI，
 * 因而所有从 CFIR 往 IDE 暴露 PSI 的出口，都必须先把 compiled 元素恢复到当前 live PSI。
 */
internal fun PsiElement.restoreCurrentCompiledPsi(preferredProject: Project? = null): PsiElement? {
    val cjElement = this as? CjElement ?: return this
    return cjElement.restoreCurrentCjElement(preferredProject)
}

/**
 * 直接从 PSI 修饰符读取声明可见性。
 *
 * 该 helper 对齐 Kotlin `visibilityByModifiers`，只表达显式修饰符；
 * 仓颉默认可见性仍由调用方按 raw CFIR builder 的容器规则补齐。
 */
internal val CjDeclaration.visibilityByModifiers: Visibility?
    get() = when {
        hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
        hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
        hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
        hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
        else -> null
    }

/**
 * 直接从 PSI 修饰符读取声明模态。
 *
 * 只返回源码中显式声明的模态，避免 source renderer 为判断隐式修饰符强制恢复 CFIR。
 */
internal val CjDeclaration.caSymbolModalityByModifiers: CaSymbolModality?
    get() = when {
        hasModifier(CjTokens.ABSTRACT_KEYWORD) -> CaSymbolModality.ABSTRACT
        hasModifier(CjTokens.OPEN_KEYWORD) -> CaSymbolModality.OPEN
        this is CjTypeStatement && hasModifier(CjTokens.SEALED_KEYWORD) -> CaSymbolModality.SEALED
        else -> null
    }

/**
 * 按 PSI 结构推导公开符号的声明位置。
 */
internal val CjDeclaration.location: CaSymbolLocation
    get() {
        // Note: a declaration can be nested inside a modifier list (for example, in the case of dangling annotations or context parameters)
        val parent = getParentOfTypes2<CjDeclaration, CjModifierList>()

        if (this is CjTypeParameter) {
            return if (parent is CjTypeStatement) CaSymbolLocation.CLASS else CaSymbolLocation.LOCAL
        }

        return when (parent) {
            null -> CaSymbolLocation.TOP_LEVEL

            is CjExtend -> CaSymbolLocation.EXTEND

            is CjTypeStatement -> CaSymbolLocation.CLASS

            is CjDeclarationWithBody,
            is CjDeclarationWithInitializer,
            is CjModifierList,
            is CjParameter,
                -> CaSymbolLocation.LOCAL

            else -> errorWithAttachment("Unexpected parent declaration: ${parent::class.simpleName}") {
                withPsiEntry("parentDeclaration", parent)
                withPsiEntry("psi", this@location)
            }
        }
    }

/**
 * 在缺少源码 PSI 时，统一按 CFIR 语义恢复 callable 的声明位置。
 *
 * Kotlin 这里只有 top-level / class / local；仓颉额外补上 extend 成员归属，
 * 并且仍然放在同一层 location 推导入口，而不是散落到各个 symbol 叶子类中。
 */
internal inline fun CaCfirSession.getCallableSymbolLocation(
    backingPsi: CjDeclaration?,
    cfirSymbolProvider: () -> CfirCallableSymbol<*>,
): CaSymbolLocation {
    backingPsi?.let { return it.location }

    /*
     * 对齐 Kotlin `KaFirNamedFunctionSymbol.location` 等 source fast-path：
     * 有源码 PSI 时先直接按 PSI 结构判定 location，不能为了取 location
     * 反向强制恢复 CFIR symbol。
     */
    val cfirSymbol = cfirSymbolProvider()

    if (cfirSymbol.rawStatus.visibility == Visibilities.Local) {
        return CaSymbolLocation.LOCAL
    }
    if (cfirSession.extendIndexStore.containingExtendOf(cfirSymbol.unwrapSubstitutionOverrides()) != null) {
        return CaSymbolLocation.EXTEND
    }
    return if (cfirSymbol.containingClassLookupTag()?.classId == null) {
        CaSymbolLocation.TOP_LEVEL
    } else {
        CaSymbolLocation.CLASS
    }
}

/**
 * 公开 `receiverType` 时只暴露仓颉源码显式存在的 receiver 语义。
 *
 * 普通类成员的 dispatch receiver 是隐式宿主，不应被渲染成 `Owner.member`；
 * 当前仅 extend 成员需要把该接收者公开为 receiverType。
 */
internal fun CaCfirSession.getExplicitCallableReceiverType(
    backingPsi: CjDeclaration?,
    builder: CaSymbolByCfirBuilder,
    cfirSymbolProvider: () -> CfirCallableSymbol<*>?,
): CaType? {
    val psiLocation = backingPsi?.location
    if (psiLocation != null && psiLocation != CaSymbolLocation.EXTEND) {
        return null
    }

    val cfirSymbol = cfirSymbolProvider() ?: return null
    if (psiLocation == null && getCallableSymbolLocation(backingPsi = null) { cfirSymbol } != CaSymbolLocation.EXTEND) {
        return null
    }

    return (cfirSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.let(builder.typeBuilder::buildType)
}

/**
 * 按 raw CFIR builder 的规则从 PSI 推导 callable 可见性。
 *
 * Kotlin FIR 中该职责位于 `KaFirPsiSymbol.psiBasedVisibility`；仓颉默认可见性
 * 与 Kotlin 不同：局部声明为 local，接口成员默认为 public，其余默认为 internal。
 */
internal fun CjCallableDeclaration.psiBasedVisibility(isOverride: () -> Boolean): Visibility? = when (this) {
    is CjNamedFunction if isLocal -> Visibilities.Local
    is CjProperty if isLocal -> Visibilities.Local
    else -> visibilityByModifiers ?: when {
        containingTypeStatement?.isInterface() == true -> Visibilities.Public
        else -> Visibilities.Internal
    }
}

/**
 * 按 PSI 结构推导 callable 的默认模态。
 *
 * 这里保持 Kotlin `psiBasedDefaultKaModality` 的层级：顶层/局部为 final，
 * 接口成员按是否有实现区分 abstract/open，override 成员保留 open 语义。
 */
internal fun CjCallableDeclaration.psiBasedDefaultCaModality(
    isOverride: () -> Boolean,
): CaSymbolModality {
    val containingTypeStatement = containingTypeStatement
    return when {
        containingTypeStatement == null -> CaSymbolModality.FINAL
        containingTypeStatement.isInterface() -> when {
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> CaSymbolModality.FINAL
            this is CjNamedFunction && !hasBody() -> CaSymbolModality.ABSTRACT
            this is CjProperty && !hasBody() -> CaSymbolModality.ABSTRACT
            else -> CaSymbolModality.OPEN
        }

        isOverride() -> CaSymbolModality.OPEN
        else -> CaSymbolModality.FINAL
    }
    }

/**
 * 判断接口上下文中的 open 模态是否来自接口成员默认规则。
 */
context(callable: CjCallableDeclaration)
internal val CaSymbolModality.isOpenFromInterface: Boolean
    get() = this == CaSymbolModality.OPEN && callable.containingTypeStatement?.isInterface() == true

/**
 * 从公开 CFIR 符号恢复当前分析范围内的 PSI。
 *
 * 对齐 Kotlin `KaSymbol.psi` 的契约：源码符号返回源 PSI；`LIBRARY` 符号必须回链到
 * `.cjo` 的反编译 PSI，而不是 null。
 */
internal fun CaCfirSymbol<*>.findPsi(): PsiElement? {
    return cfirSymbol.findPsi(analysisSession.analysisScope, analysisSession.project)
        ?: findLibraryPsi()
}

/**
 * 为 `LIBRARY` origin 的 CFIR 符号恢复反编译 PSI。
 *
 * 库声明在 CFIR 侧由 `.cjo` 反序列化而来，没有 `source`，因此不能只依赖源 PSI 回链。
 * 这里按 `module + package + 声明身份` 通过 [CaDecompiledBinaryIndex] 定位 binary 文件，
 * 再用 `PsiManager` 取得 `CjDecompiledFile` 中的声明 PSI：
 * - class-like：包 + 短类名；
 * - 普通 callable：包（顶层）或 classId（成员）+ 名字；
 * - xtend 声明：按接收者 + 父类型结构签名定位；xtend 成员：按接收者类型定位所属 extend，再按名字取成员。
 */
internal fun CaCfirSymbol<*>.findLibraryPsi(receiverHint: String? = null): PsiElement? {
    // substitution override（泛型实参替换后的调用点视图）必须剥回原始库声明再回链。
    val baseSymbol = (cfirSymbol as? CfirCallableSymbol<*>)?.unwrapSubstitutionOverrides() ?: cfirSymbol
    if (origin != CaSymbolOrigin.LIBRARY &&
        origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE &&
        origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE &&
        baseSymbol === cfirSymbol
    ) {
        return null
    }
    val module = cfirSymbol.llCfirModuleData.caModule
    val project = module.project
    val binaryIndex = project.getService(CaDecompiledBinaryIndex::class.java) ?: return null

    fun decompiledFile(packageFqName: FqName): CjFile? {
        val binaryFile = when (module) {
            is CaBuiltinsModule -> binaryIndex.findBinaryFile(module, packageFqName)
            is CaLibraryModule -> binaryIndex.findBinaryFile(module, packageFqName)
            else -> null
        } ?: return null
        return PsiManager.getInstance(project).findFile(binaryFile) as? CjFile
    }

    val element = when (val symbol = baseSymbol) {
        is CfirClassLikeSymbol<*> -> {
            val classId = symbol.classId
            val file = decompiledFile(classId.packageFqName) ?: return null
            file.declarations
                .filterIsInstance<CjTypeStatement>()
                .firstOrNull { declaration ->
                    declaration.name == classId.relativeClassName.shortName().asString()
                }
        }

        is CfirCallableSymbol<*> -> {
            val callableId = symbol.callableId
            val file = decompiledFile(callableId.packageName) ?: return null
            findLibraryCallablePsi(file, callableId, receiverHint, symbol.dispatchReceiverType)
        }

        is CfirExtendSymbol -> {
            /*
             * 库 extend 的符号实例与 extend provider 索引里的实例可能不是同一个对象
             * （库会话/provider 重建），因此不能直接用 `getPackageFqName(cfirSymbol.cfir)`。
             * 这里按「接收者 + 父类型」结构签名在 provider 索引里找对应项，再用同一个
             * provider 取包名，最后在反编译文件里按同样的签名定位 CjExtend。
             */
            val extendSymbol = this as? CaCfirExtendSymbol ?: return null
            val cfirSession = extendSymbol.analysisSession.resolutionFacade.useSiteCfirSession
            val extendProvider = cfirSession.extendProviderOrNull ?: return null
            val signature = extendSymbol.cfirSymbolOrNull?.cfir?.extensionSignatureKey() ?: return null
            val providerExtend = extendProvider.getExtendByStructuralKey(signature) ?: return null
            val packageFqName = extendProvider.getPackageFqName(providerExtend) ?: return null
            val file = decompiledFile(packageFqName) ?: return null
            file.declarations
                .filterIsInstance<CjExtend>()
                .firstOrNull { psi -> psi.psiExtensionSignatureKey() == signature }
        }

        else -> null
    }
    /*
     * 反编译文件（`.cjo`）的 view provider 会在 SDK 重载/索引刷新时被重建；旧元素即使
     * `isValid == true` 也挂在旧 provider 上，直接交给平台会抛
     * `PsiInvalidElementAccessException: ... different providers`。
     * 所有库 PSI 出口都必须先恢复到当前 live file 上的对应声明。
     */
    return element?.restoreCurrentCompiledPsi(project)
}

/**
 * 反编译 `CjExtend` 的结构签名，与 CFIR 侧 `extensionSignatureKey()` 保持同一规范化规则。
 */
private fun CjExtend.psiExtensionSignatureKey(): String {
    val receiver = normalizeCfirExtendTypeText(receiverTypeReceiver?.text.orEmpty())
    val superTypes = superTypeListEntries
        .mapNotNull { entry -> entry.typeReference?.getTypeText() }
        .map(::normalizeCfirExtendTypeText)
        .filter(String::isNotEmpty)
        .sorted()
        .joinToString(",")
    return "$receiver<$superTypes>"
}

/**
 * 在反编译 PSI 中定位库 callable 声明。
 *
 * [receiverHint] 优先于符号自身的 dispatch receiver：库 `extend` 成员在符号层
 * 不带接收者（primitive 接收者不属于 `ConeSimpleCangJieType`），调用点的上下文
 * 才是可靠来源。
 */
private fun findLibraryCallablePsi(
    file: CjFile,
    callableId: CallableId,
    receiverHint: String?,
    receiverType: ConeSimpleCangJieType?,
): PsiElement? {
    val name = callableId.callableName.asString()

    val classId = callableId.classId
    if (classId != null) {
        val classShortName = classId.relativeClassName.shortName().asString()
        val owner = file.declarations
            .filterIsInstance<CjTypeStatement>()
            .firstOrNull { declaration -> declaration.name == classShortName }
        val declaredInClass = owner?.let { declaration -> findDeclaredCallable(declaration, name) }
        if (declaredInClass != null) return declaredInClass

        // 默认成员可能是库 `extend` 提供的（如 `Option.isNone`），继续在该类型的 extend 里找。
        val receiverKey = normalizeCfirExtendTypeText(classShortName)
        return file.declarations
            .filterIsInstance<CjExtend>()
            .filter { extend -> normalizeCfirExtendTypeText(extend.receiverTypeReceiver?.text.orEmpty()) == receiverKey }
            .mapNotNull { extend -> findDeclaredCallable(extend, name) }
            .singleOrNull()
    }

    file.declarations.firstOrNull { declaration -> isCallableNamed(declaration, name) }?.let { return it }

    // `extend` 成员：用接收者类型收敛到唯一 extend，避免同名成员（如 std.core 的多个 `toString`）误导航。
    val receiverName = receiverHint ?: receiverType?.let(::libraryReceiverTypeName) ?: return null
    val receiverKey = normalizeCfirExtendTypeText(receiverName)
    return file.declarations
        .filterIsInstance<CjExtend>()
        .filter { extend -> normalizeCfirExtendTypeText(extend.receiverTypeReceiver?.text.orEmpty()) == receiverKey }
        .mapNotNull { extend -> findDeclaredCallable(extend, name) }
        .singleOrNull()
}

/** 在声明容器内按名字取 callable 声明。 */
private fun findDeclaredCallable(container: CjDeclarationContainer, name: String): PsiElement? =
    container.declarations.firstOrNull { declaration -> isCallableNamed(declaration, name) }

/** 判断声明是否为目标名字的 callable。 */
private fun isCallableNamed(declaration: CjDeclaration, name: String): Boolean = when (declaration) {
    is CjNamedFunction -> declaration.name == name
    is CjProperty -> declaration.name == name
    else -> false
}

/** 把接收者类型渲染成与 `CjExtend` 接收者文本一致的最短名字。 */
private fun libraryReceiverTypeName(type: ConeSimpleCangJieType): String? = when (type) {
    is ConePrimitiveType -> type.kind.typeName
    is ConeClassLikeType -> type.lookupTag.classId.relativeClassName.shortName().asString()
    else -> null
}

/**
 * 从 CFIR 符号恢复当前作用域可见的 PSI。
 *
 * callable 会先剥离 fake override 或 delegated 包装，再限制结果必须落在给定搜索范围内。
 */
fun CfirBasedSymbol<*>.findPsi(scope: GlobalSearchScope, preferredProject: Project? = null): PsiElement? {
    val psi = (if (this is CfirCallableSymbol<*>) {
        cfir.unwrapFakeOverridesOrDelegated().findPsi(preferredProject)
    } else {
        cfir.findPsi(preferredProject)
    }) ?: return null

    // 宏生成或 fake 声明的 PSI 锚点可能没有 containing file；这种 PSI 不属于项目搜索范围，不能传给 contains。
    val virtualFile = psi.containingFile?.virtualFile ?: return null
    return psi.takeIf { scope.contains(virtualFile) }
}


/**
 * 为引用导航恢复 CFIR 声明的 PSI。当前 macro snapshot 识别出的宏生成声明或表面映射，仅在能唯一定位到宏表面源码，且恢复出的 PSI 有效、物理并位于搜索范围内时才返回映射结果。
 * 其他声明中，callable 会先解包 fake override / delegated 包装，再恢复其声明 PSI。
 */
internal fun CfirDeclaration.findReferencePsi(scope: GlobalSearchScope, preferredProject: Project? = null): PsiElement? {
    val declarationSource = source
    val cfirSession = moduleData.session
    val macroSnapshot = (cfirSession as? LLCfirResolvableModuleSession)?.macroSemanticSnapshot
    val macroSurfaceIds = macroSnapshot?.macroSurfaceIdsForDeclaration(this)
    val isGeneratedSource = declarationSource != null && macroSnapshot?.isGeneratedSource(declarationSource) == true
    if (macroSurfaceIds != null || isGeneratedSource) {
        val surfaceId = macroSurfaceIds?.singleOrNull() ?: return null
        val macroOrigin = macroSnapshot?.originSurface(surfaceId)?.sourceRange?.source
            ?: return null
        return macroOrigin.psiOrNull
            ?.restoreCurrentCompiledPsi(preferredProject)
            ?.takeIf { psi ->
                psi.isValid && psi.isPhysical && psi.containingFile?.virtualFile?.let(scope::contains) == true
            }
    }

    return (if (this is CfirCallableDeclaration) {
        unwrapFakeOverridesOrDelegated().psi
    } else {
        psi
    })?.restoreCurrentCompiledPsi(preferredProject)
}