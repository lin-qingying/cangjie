

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirDanglingFileSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibraryOrLibrarySourceResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.*
import org.cangnova.cangjie.analysis.utils.errors.unexpectedElementError
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.utils.SmartList
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * This class describes where locates [target] element and its essential [path].
 *
 * Usually a resolver uses [path] to resolve [target] in the proper context.
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
 */
class CfirDesignation(
    /**
     * The path to [target] element.
     *
     * ### Contracts:
     * * Can contain [CfirFile] only in the first position
     * @see file
     * @see fileOrNull
     */
    val path: List<CfirDeclaration>,
    /**
     * designation 最终要解析或处理的 CFIR 目标元素。
     */
    val target: CfirElementWithResolveState,
) {
    constructor(target: CfirElementWithResolveState) : this(emptyList(), target)

    init {
        for ((index, declaration) in path.withIndex()) {
            when (declaration) {
                is CfirFile -> requireWithAttachment(
                    index == 0,
                    { "${CfirFile::class.simpleName} can be only in the first position of the path, but actual is '$index'" },
                ) {
                    withCfirDesignationEntry("designation", this@CfirDesignation)
                }

                is CfirClassLikeDeclaration,
                is CfirExtend,
                    -> {}
                else -> errorWithAttachment("Unexpected declaration type: ${declaration::class.simpleName}") {
                    withCfirDesignationEntry("designation", this@CfirDesignation)
                }
            }
        }
    }

    /**
     * designation 所属的 CFIR 文件；当路径中无法确定文件时抛出带附件的错误。
     */
    val file: CfirFile
        get() = fileOrNull ?: errorWithAttachment("File is not found") {
            withCfirDesignationEntry("designation", this@CfirDesignation)
        }

    /**
     * designation 所属的 CFIR 文件；适用于允许无文件上下文的低层恢复路径。
     */
    val fileOrNull: CfirFile? get() = path.firstOrNull() as? CfirFile ?: target as? CfirFile

    /**
     * 以路径到目标的顺序输出 designation，便于异常附件和调试日志定位解析路径。
     */
    override fun toString(): String = path.plus(target).joinToString(separator = " -> ") {
        it::class.simpleName ?: it.toString()
    }
}

/**
 * 将 designation 的路径和目标元素写入异常附件。
 */
fun ExceptionAttachmentBuilder.withCfirDesignationEntry(name: String, designation: CfirDesignation) {
    withEntryGroup(name) {
        for ((index, declaration) in designation.path.withIndex()) {
            withCfirEntry("path$index", declaration)
        }

        withCfirEntry("target", designation.target)
    }
}

/**
 * 按路径顺序遍历 designation，按需包含最终目标元素。
 */
fun CfirDesignation.toSequence(includeTarget: Boolean): Sequence<CfirElementWithResolveState> = sequence {
    yieldAll(path)
    if (includeTarget) yield(target)
}

/**
 * 尝试为非局部 CFIR 声明收集可 lazy resolve 的 designation。
 */
private fun tryCollectDesignation(providedFile: CfirFile?, target: CfirElementWithResolveState): CfirDesignation? {
    if (target !is CfirDeclaration) {
        unexpectedElementError<CfirDeclaration>(target)
    }

    return when (target) {
        is CfirAnonymousFunction,
        is CfirErrorFunction,
        is CfirTypeParameter,
        is CfirValueParameter,
            -> null

        is CfirNamedFunction,
        is CfirMainFunction,
        is CfirMacroDeclaration,
        is CfirFinalizer,
        is CfirProperty,
        is CfirFieldVariable,
        is CfirPatternBindingVariable,
        is CfirPatternVariable,
        is CfirConstructor,
        is CfirEnumConstructor,
            -> {
            if (target.symbol.isLocalForLazyResolutionPurposes) {
                return null
            }

            // 仓颉 extend 成员归属于 extend 声明本身；ClassId 只标识 class-like，无法表达这种归属，
            // 因此 extend 容器作为声明前缀进入路径，而不是退化成 ClassId（对标 Kotlin 的 script 前缀）。
            val containingExtend = target.containingExtendOrNull()
            if (containingExtend != null) {
                return collectDesignationPathWithContainingClass(
                    providedFile = providedFile,
                    target = target,
                    containingClassId = null,
                    additionalPathPrefix = listOf(containingExtend),
                )
            }

            // class-like 容器：优先用 CFIR lookup tag（Kotlin containingClassLookupTag()?.classId 的形状），
            // 缺失时回退到 PSI class-like 容器；extend 不是 class-like，已在上面按前缀处理。
            val containingClassId = target.containingClassLookupTag()?.toClassSymbol(target.moduleData.session)?.classId
                ?: (target.psi as? CjDeclaration)
                    ?.containingTypeStatement
                    ?.takeUnless { it is CjExtend }
                    ?.getClassId()
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId)
        }

        is CfirExtend -> {
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }

        is CfirClassLikeDeclaration -> {
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }

        is CfirFile -> CfirDesignation(target)
        is CfirCodeFragment -> {
            collectDesignationPathWithContainingClass(providedFile, target, containingClassId = null)
        }
        else -> null
    }
}

/**
 * 取得 [this] 作为 extend 成员时所属的 extend 声明；不是 extend 成员时返回 `null`。
 *
 * 归属判断以 raw builder 显式记录的 [CfirCallableDeclaration.containingExtend] 为准，
 * 它与文件结构映射名使用同一个接收者文本，与 PSI 身份无关，因此对 AST PSI、stub PSI 和文件副本都成立。
 */
private fun CfirDeclaration.containingExtendOrNull(): CfirExtend? =
    (this as? CfirCallableDeclaration)?.containingExtend

/**
 * 在 [cfirFile] 中按 PSI 身份定位 [this] 作为 extend 容器时对应的 CFIR 声明；不是 extend 容器时返回 `null`。
 *
 * PSI 入口（[CfirElementFinder.collectDesignationPath]、上下文收集等）需要以 PSI 身份推导路径前缀，
 * 这里复用同一份文件结构按接收者文本 + PSI 身份查找，保证路径解析只有一套实现。
 */
internal fun CjDeclaration.findExtendDeclarationIn(cfirFile: CfirFile): CfirExtend? {
    val extendPsi = containingTypeStatement as? CjExtend ?: return null
    return CfirElementFinder.findDeclaration(cfirFile, extendPsi) as? CfirExtend
}

/**
 * 在已知可选文件、外围 class-like ID 和非 class-like 容器前缀的情况下构造目标声明的 designation 路径。
 *
 * [additionalPathPrefix] 用于表达路径中位于文件与 class-like 之间的声明容器（仓颉 extend，对标 Kotlin script）。
 */
private fun collectDesignationPathWithContainingClass(
    providedFile: CfirFile?,
    target: CfirDeclaration,
    containingClassId: ClassId?,
    additionalPathPrefix: List<CfirDeclaration> = emptyList(),
): CfirDesignation? {
    val file = providedFile ?: target.getContainingFile()
    if (file != null && (containingClassId == null || file.packageDirective.packageFqName == containingClassId.packageFqName)) {
        val designationPath = CfirElementFinder.collectDesignationPath(
            cfirFile = file,
            declarationContainerClassId = containingClassId,
            additionalPathPrefix = additionalPathPrefix,
            targetMemberDeclaration = target,
        )

        if (designationPath != null) {
            return designationPath
        }
    }

    val fallbackClassPath = containingClassId?.let { collectDesignationPathWithContainingClassFallback(target, it) }.orEmpty()
    val fallbackFile = providedFile ?: additionalPathPrefix.lastOrNull()?.getContainingFile() ?: fallbackClassPath.lastOrNull()?.getContainingFile() ?: file
    val fallbackPath = listOfNotNull(fallbackFile) + additionalPathPrefix + fallbackClassPath
    val patchedPath = patchDesignationPathIfNeeded(target, fallbackPath)
    return CfirDesignation(patchedPath, target)
}

/**
 * Whether the search via [CfirSymbolProvider] is required to find a declaration in the context of [this] session.
 *
 * Not all sessions have required providers in the session itself (not its dependencies).
 * In such cases, the search might not be able to find even the containing declaration
 */
private val LLCfirSession.requiresDependenciesSearch: Boolean
    get() = when (this) {
        is LLCfirLibraryOrLibrarySourceResolvableModuleSession -> true
        is LLCfirDanglingFileSession -> {
            val module = caModule as CaDanglingFileModule
            // Dangling files in the ignore self mode have the empty declaration provider,
            // so they cannot find any declarations inside themselves. Search in the context is required
            module.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF
        }

        else -> false
    }

/**
 * 当文件内直接查找失败时，按包含类 ID 通过当前 session 的 provider 恢复 class-like 路径。
 *
 * extend 成员走 [CfirCallableDeclaration.containingExtend] 前缀，不进入本函数：
 * extend 不是 class-like，没有 ClassId，也不可能通过 provider 查到。
 */
private fun collectDesignationPathWithContainingClassFallback(
    target: CfirDeclaration,
    containingClassId: ClassId,
): List<CfirDeclaration> {
    val useSiteSession by lazy(LazyThreadSafetyMode.NONE) { getTargetSession(target) }

    fun resolveChunk(classId: ClassId): CfirDeclaration {
        val declaration = if (useSiteSession.requiresDependenciesSearch) {
            useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        } else {
            useSiteSession.cfirProvider.getCfirClassifierByFqName(classId)
                ?: findInContainingFileIfApplicable(classId, target)
        }

        checkWithAttachment(
            declaration is CfirClassLikeDeclaration || declaration is CfirExtend,
            {
                "'${CfirClassLikeDeclaration::class.simpleName}' or '${CfirExtend::class.simpleName}' expected as a containing declaration, " +
                        "got '${declaration?.javaClass?.simpleName}'. " +
                        "Module: ${useSiteSession.caModule::class.simpleName}"
            },
        ) {
                withEntry("chunk", "$classId in $containingClassId")
                withCfirEntry("target", target)
                if (declaration != null) {
                    withCfirEntry("foundDeclaration", declaration)
                }
        }

        return declaration
    }

    val containingClassIds = sequenceOf(containingClassId)
    val (_, containingClasses) = containingClassIds.fold(target to SmartList<CfirDeclaration>()) { (declaration, result), classId ->
        // Psi-based calculator is called explicitly to avoid `LLCfirProvider#getContainingClassSymbol`
        // since we have a fallback logic with strict checking (no dependencies in the search scope)
        val psiBasedContainingClassLike = LLContainingClassCalculator.getContainingClassSymbol(declaration.symbol)?.cfir
        checkWithAttachment(
            psiBasedContainingClassLike == null || psiBasedContainingClassLike is CfirClassLikeDeclaration,
            {
                "${LLContainingClassCalculator::class.simpleName} is supposed to return '${CfirClassLikeDeclaration::class.simpleName}' " +
                        "as a containing declaration since the class-like declaration is not local (classId exists), got '${psiBasedContainingClassLike?.let { it::class.java.simpleName }}'. " +
                        "Module: ${useSiteSession.caModule::class.simpleName}"
            },
        ) {
            withEntry("classId", classId.toString())
            withEntry("containingClassId", containingClassId.toString())
            withCfirEntry("declaration", declaration)
        }

        if (psiBasedContainingClassLike == null && classId.shortClassName.isSpecial) {
            errorWithAttachment(
                "Special classes are supposed to be covered via ${LLContainingClassCalculator::class.simpleName}. " +
                        "Module: ${useSiteSession.caModule::class.simpleName}"
            ) {
                withEntry("classId", classId.toString())
                withEntry("containingClassId", containingClassId.toString())
                withCfirEntry("declaration", declaration)
            }
        }

        val containingDeclaration = psiBasedContainingClassLike ?: resolveChunk(classId)
        result += containingDeclaration
        containingDeclaration to result
    }

    return containingClasses.asReversed()
}

/**
 * 取得目标声明应使用的 low-level session，合成 callable 优先使用其包含类的 session。
 */
private fun getTargetSession(target: CfirDeclaration): LLCfirSession {
    if (target is CfirCallableDeclaration) {
        val containingSymbol = target.getContainingClassSymbol()
        if (containingSymbol != null) {
            // Synthetic declarations might have a call site session
            return containingSymbol.llCfirSession
        }
    }

    return target.llCfirSession
}

/**
 * 在目标所在文件内按 class ID 查找 class-like 声明，跳过 primitive class ID。
 */
internal fun findInContainingFileIfApplicable(classId: ClassId, target: CfirDeclaration?): CfirClassLikeDeclaration? {
    if (classId.toPrimitiveTypeKindOrNull() != null) {
        return null
    }

    val cfirFile = target?.getContainingFile() ?: return null
    return CfirElementFinder.findClassifierWithClassId(cfirFile, classId)
}

/**
 * Consider using this function only if [collectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [CfirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] can be null or throws an exception.
 *
 * @see collectDesignation
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.collectDesignationWithOptionalFile(providedFile: CfirFile? = null): CfirDesignation =
    tryCollectDesignationWithOptionalFile(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        providedFile?.let { withCfirEntry("cfirFile", it) }
    }

/**
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] not null or throws an exception.
 *
 * @see collectDesignationWithOptionalFile
 * @see tryCollectDesignation
 * @see tryCollectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.collectDesignation(providedFile: CfirFile? = null): CfirDesignation =
    tryCollectDesignation(providedFile) ?: errorWithAttachment("No designation of local declaration") {
        withCfirEntry("CfirDeclaration", this@collectDesignation)
    }

/**
 * Consider using this function only if [tryCollectDesignation] is not applicable.
 *
 * This extension function can be used in the case there your [CfirElementWithResolveState] probably
 * doesn't have [getContainingFile] and it doesn't matter for your purposes.
 * Potentially, this function can become obsolete if we support all possible cases in [getContainingFile]
 *
 * @return [CfirDesignation] where [CfirDesignation.fileOrNull] can be null or null.
 *
 * @see collectDesignationWithOptionalFile
 * @see collectDesignation
 * @see tryCollectDesignation
 */
fun CfirElementWithResolveState.tryCollectDesignationWithOptionalFile(providedFile: CfirFile? = null): CfirDesignation? =
    tryCollectDesignation(providedFile = providedFile, target = this)

/**
 * @return [CfirDesignation] with not-null [CfirDesignation.file] or null.
 *
 * @see collectDesignation
 * @see tryCollectDesignationWithOptionalFile
 * @see collectDesignationWithOptionalFile
 */
fun CfirElementWithResolveState.tryCollectDesignation(providedFile: CfirFile? = null): CfirDesignation? {
    val designation = tryCollectDesignation(providedFile = providedFile, target = this)
    return designation?.takeIf { it.fileOrNull != null }
}

/**
 * 对 dangling copy 等特殊场景修正 designation 路径，普通声明路径保持原样。
 */
internal fun patchDesignationPathIfNeeded(target: CfirElementWithResolveState, targetPath: List<CfirDeclaration>): List<CfirDeclaration> {
    return patchDesignationPathForCopy(target, targetPath) ?: targetPath
}

/**
 * 将 ignore-self dangling 文件中的复制 PSI 路径映射回上下文模块的原始 CFIR 路径。
 */
private fun patchDesignationPathForCopy(target: CfirElementWithResolveState, targetPath: List<CfirDeclaration>): List<CfirDeclaration>? {
    val targetModule = target.llCfirModuleData.caModule

    if (targetModule is CaDanglingFileModule && targetModule.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF) {
        val targetPsiFile = targetModule.files.singleOrNull() ?: return targetPath
        val contextModule = targetModule.contextModule
        val contextResolutionFacade = contextModule.getResolutionFacade(contextModule.project)

        return buildList {
            for (targetPathDeclaration in targetPath) {
                val targetPathPsi = targetPathDeclaration.psi ?: return null
                if (targetPathPsi !is CjTypeStatement && targetPathPsi !is CjFile) return null

                val originalPathPsi = targetPathPsi.unwrapCopy(targetPsiFile) ?: return null
                val originalPathDeclaration = when (originalPathPsi) {
                    is CjTypeStatement -> originalPathPsi.resolveToCfirSymbolOfTypeSafe<CfirClassSymbol>(contextResolutionFacade)?.cfir
                    is CjFile -> originalPathPsi.getOrBuildCfirFile(contextResolutionFacade)
                    else -> null
                } ?: return null

                add(originalPathDeclaration)
            }
        }
    }

    return targetPath
}
