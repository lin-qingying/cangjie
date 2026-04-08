package org.cangnova.cangjie.analysis.light.declarations.symbol

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.CaSourceModule
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.decompiled.light.declarations.CaDecompiledLightSupport
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.stubs.CaStubFileProvider
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.light.declarations.CaLightCallableDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightClassLikeDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCache
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCacheKey
import org.cangnova.cangjie.analysis.light.declarations.CaLightExtendDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightPackageDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.sourceOrigin
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.utils.isCangJieFileType
import java.util.concurrent.atomic.AtomicLong

/**
 * 基于公开 Analysis API 语义的声明视图提供器。
 *
 * 它不直接接触 CFIR 私有实现，只在需要时进入 `analyze`，
 * 并把当前 session 中的 symbol / scope / signature 信息投影成只读声明视图。
 */
class CaSymbolLightDeclarationProvider(
    private val project: Project,
) : CaLightDeclarationProvider {
    private val cachedModificationCount = AtomicLong(Long.MIN_VALUE)
    private var declarationCache: CaLightDeclarationCache = CaLightDeclarationCache()
    private val decompiledLightSupport = CaDecompiledLightSupport(project)

    override fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration? {
        refreshCacheIfNeeded()
        val module = inferModuleForSymbol(symbol) ?: return null

        return analyze(module) {
            when (symbol) {
                is CaPackageSymbol -> buildPackageLightDeclaration(symbol, module)
                is CaClassLikeSymbol -> restoreClassLikeSymbol(symbol)?.let { buildClassLikeLightDeclaration(it, module, null) }
                is CaExtendSymbol -> restoreExtendSymbol(symbol)?.let { buildExtendLightDeclaration(it, module, null) }
                is CaCallableSymbol -> restoreCallableSymbol(symbol)?.let { buildCallableLightDeclaration(it, module, null) }
                else -> null
            }
        }
    }

    override fun getLightDeclarations(file: CjFile, useSiteModule: CaModule?): List<CaLightDeclaration> {
        refreshCacheIfNeeded()
        val module = useSiteModule ?: CaProjectStructureProvider.getInstance(project).getModule(file, null)
        val stubFileProvider = project.getService(CaStubFileProvider::class.java)

        return analyze(module) {
            val packageFqName = file.resolvePackageFqName()
            val names = buildList {
                addAll(stubFileProvider.getTopLevelClassifierNames(file))
                addAll(stubFileProvider.getTopLevelCallableNames(file))
            }.distinct()

            buildList {
                names.flatMapTo(this) { name ->
                    buildList {
                        getTopLevelClassLikeSymbols(packageFqName, name)
                            .filter { sameContainingFile(it, file, module) }
                            .forEach { symbol -> buildClassLikeLightDeclaration(symbol, module, file).let(::add) }
                        getTopLevelCallableSymbols(packageFqName, name)
                            .filter { sameContainingFile(it, file, module) }
                            .forEach { symbol -> buildCallableLightDeclaration(symbol, module, file).let(::add) }
                    }
                }

                getTopLevelExtendSymbols(packageFqName)
                    .filter { sameContainingFile(it, file, module) }
                    .forEach { symbol -> buildExtendLightDeclaration(symbol, module, file).let(::add) }
            }.distinctBy(::stableKey)
        }
    }

    override fun getLightDeclarations(module: CaModule): List<CaLightDeclaration> {
        refreshCacheIfNeeded()
        val files = when (module) {
            is CaLibraryModule,
            is CaBuiltinsModule -> decompiledLightSupport.getDecompiledFiles(module)

            else -> collectModuleFiles(module)
        }

        return files.flatMap { file -> getLightDeclarations(file, module) }.distinctBy(::stableKey)
    }

    override fun getPackageLightDeclaration(packageFqName: FqName, useSiteModule: CaModule): CaLightDeclaration? {
        refreshCacheIfNeeded()
        return analyze(useSiteModule) {
            val packageSymbol = getPackageSymbol(packageFqName) ?: return@analyze null
            buildPackageLightDeclaration(packageSymbol, useSiteModule)
        }
    }

    override fun findLightDeclarations(packageFqName: FqName, name: Name, useSiteModule: CaModule): List<CaLightDeclaration> {
        refreshCacheIfNeeded()
        return analyze(useSiteModule) {
            buildList {
                getTopLevelClassLikeSymbols(packageFqName, name).forEach { symbol ->
                    buildClassLikeLightDeclaration(symbol, useSiteModule, null).let(::add)
                }
                getTopLevelExtendSymbols(packageFqName)
                    .filter { symbol -> resolveExtendLightName(symbol) == name.asString() }
                    .forEach { symbol ->
                        buildExtendLightDeclaration(symbol, useSiteModule, null).let(::add)
                    }
                getTopLevelCallableSymbols(packageFqName, name).forEach { symbol ->
                    buildCallableLightDeclaration(symbol, useSiteModule, null).let(::add)
                }
            }.distinctBy(::stableKey)
        }
    }

    private fun refreshCacheIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (cachedModificationCount.get() == modificationCount) return

        declarationCache = CaLightDeclarationCache()
        cachedModificationCount.set(modificationCount)
    }

    private fun inferModuleForSymbol(symbol: CaSymbol): CaModule? {
        val containingFile = when (symbol) {
            is CaDeclarationSymbol -> symbol.psi?.containingFile as? CjFile
            else -> null
        }
        if (containingFile != null) {
            return CaProjectStructureProvider.getInstance(project).getModule(containingFile, null)
        }

        /**
         * light declaration 的恢复优先回到 symbol 所属 session 的 use-site module。
         *
         * 这里不再把“声明真实归属模块”当成前置条件：
         * 对 library / builtins 符号，use-site module 同样能提供完整查询语境，
         * 后续 `resolveContainingFile()` 再通过 decompiled support 去命中真实 binary file。
         */
        return symbol.containingModule
    }

    private fun collectModuleFiles(module: CaModule): List<CjFile> {
        val psiManager = PsiManager.getInstance(project)
        val roots = when (module) {
            is org.cangnova.cangjie.analysis.api.CaLibrarySourceModule -> module.sourceRoots
            is CaSourceModule -> module.psiRoots
            else -> emptyList()
        }

        val files = linkedSetOf<CjFile>()
        roots.forEach { root ->
            when (root) {
                is CjFile -> files += root
                is PsiFile -> (root as? CjFile)?.let(files::add)
                is PsiDirectory -> {
                    VfsUtilCore.iterateChildrenRecursively(root.virtualFile, null) { virtualFile ->
                        if (virtualFile.isDirectory || !virtualFile.isCangJieFileType()) return@iterateChildrenRecursively true
                        (psiManager.findFile(virtualFile) as? CjFile)?.let(files::add)
                        true
                    }
                }
            }
        }
        return files.toList()
    }

    private fun CaSession.restoreClassLikeSymbol(symbol: CaClassLikeSymbol): CaClassLikeSymbol? {
        val classId = symbol.classId ?: return null
        return getClassLikeSymbol(classId)
    }

    private fun CaSession.restoreCallableSymbol(symbol: CaCallableSymbol): CaCallableSymbol? {
        val callableId = symbol.callableId ?: return symbol
        val packageFqName = callableId.packageName
        val callableName = callableId.callableName
        return if (callableId.classId == null) {
            getTopLevelCallableSymbols(packageFqName, callableName).firstOrNull { it.callableId == callableId }
        } else {
            getClassLikeSymbol(callableId.classId!!)
                ?.declaredMemberScope
                ?.getCallableSymbols(callableName)
                ?.firstOrNull { it.callableId == callableId }
        }
    }

    private fun CaSession.restoreExtendSymbol(symbol: CaExtendSymbol): CaExtendSymbol? {
        val packageFqName = resolveContainingFile(symbol, containingModule = useSiteModule)?.packageFqName
            ?: symbol.targetClassId?.packageFqName
            ?: return null
        return getTopLevelExtendSymbols(packageFqName).firstOrNull { it.extendId == symbol.extendId }
    }

    private fun CaSession.buildPackageLightDeclaration(
        symbol: CaPackageSymbol,
        module: CaModule,
    ): CaLightDeclaration {
        val packageName = symbol.fqName.asString()
        return declarationCache.getOrPut(CaLightDeclarationCacheKey("pkg:$packageName")) {
            CaLightPackageDeclarationImpl(
                packageName = packageName,
                module = module,
                origin = sourceOrigin(
                    description = "package $packageName",
                    containingFile = null,
                    sourceElement = null,
                ),
                token = symbol.token,
            )
        }
    }

    private fun CaSession.buildClassLikeLightDeclaration(
        symbol: CaClassLikeSymbol,
        module: CaModule,
        containingFileHint: CjFile?,
    ): CaLightClassLikeDeclaration {
        val classId = requireNotNull(symbol.classId) { "Light declaration requires stable ClassId" }
        return declarationCache.getOrPut(CaLightDeclarationCacheKey("class:${classId.asString()}")) {
            val containingFile = containingFileHint ?: resolveContainingFile(symbol, module)
            val members = symbol.declaredMemberScope.symbols
                .mapNotNull { member -> buildMemberLightDeclaration(member, module, containingFile) }
                .distinctBy(::stableKey)

            CaLightClassLikeDeclarationImpl(
                name = symbol.name?.asString(),
                module = module,
                annotations = resolveAnnotations(symbol),
                origin = sourceOrigin(
                    description = classId.asString(),
                    containingFile = containingFile,
                    sourceElement = (symbol as? CaDeclarationSymbol)?.psi,
                ),
                token = symbol.token,
                classId = classId,
                typeParameters = resolveTypeParameterNames(symbol, containingFile),
                superTypes = (symbol as? org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol)?.superTypes.orEmpty(),
                members = members,
            )
        }
    }

    private fun CaSession.buildCallableLightDeclaration(
        symbol: CaCallableSymbol,
        module: CaModule,
        containingFileHint: CjFile?,
    ): CaLightCallableDeclaration {
        val stableId = symbol.callableId?.toString() ?: symbol.name?.asString() ?: "<anonymous>"
        return declarationCache.getOrPut(CaLightDeclarationCacheKey("callable:$stableId")) {
            val containingFile = containingFileHint ?: resolveContainingFile(symbol, module)
            CaLightCallableDeclarationImpl(
                name = symbol.name?.asString(),
                module = module,
                annotations = resolveAnnotations(symbol),
                origin = sourceOrigin(
                    description = stableId,
                    containingFile = containingFile,
                    sourceElement = (symbol as? CaDeclarationSymbol)?.psi,
                ),
                token = symbol.token,
                callableId = symbol.callableId,
                signature = symbol.signature,
            )
        }
    }

    private fun CaSession.buildExtendLightDeclaration(
        symbol: CaExtendSymbol,
        module: CaModule,
        containingFileHint: CjFile?,
    ): CaLightExtendDeclaration {
        return declarationCache.getOrPut(CaLightDeclarationCacheKey("extend:${symbol.extendId}")) {
            val containingFile = containingFileHint ?: resolveContainingFile(symbol, module)
            val members = symbol.declaredMemberScope.symbols
                .mapNotNull { member -> buildMemberLightDeclaration(member, module, containingFile) }
                .distinctBy(::stableKey)

            CaLightExtendDeclarationImpl(
                name = resolveExtendLightName(symbol),
                module = module,
                annotations = resolveAnnotations(symbol),
                origin = sourceOrigin(
                    description = symbol.extendId,
                    containingFile = containingFile,
                    sourceElement = (symbol as? CaDeclarationSymbol)?.psi,
                ),
                token = symbol.token,
                extendId = symbol.extendId,
                targetClassId = symbol.targetClassId,
                extendedType = symbol.extendedType,
                typeParameters = resolveTypeParameterNames(symbol, containingFile),
                superTypes = symbol.superTypes,
                members = members,
            )
        }
    }

    /**
     * extend 在 light declaration 层的可查询短名。
     *
     * 优先使用稳定的 `targetClassId.shortClassName`；
     * 如果目标类型当前没有 classId，再回退到已渲染类型文本的短名，
     * 这样 `findLightDeclarations(package, name, ...)` 仍能尽量覆盖 primitive / error extend。
     */
    private fun resolveExtendLightName(symbol: CaExtendSymbol): String? {
        symbol.targetClassId?.shortClassName?.asString()?.let { return it }
        val renderedType = symbol.extendedType.presentation
        return renderedType
            .substringBefore('<')
            .substringBefore('?')
            .substringAfterLast('.')
            .ifBlank { null }
    }

    private fun CaSession.buildMemberLightDeclaration(
        symbol: CaSymbol,
        module: CaModule,
        containingFile: CjFile?,
    ): CaLightDeclaration? {
        return when (symbol) {
            is CaClassLikeSymbol -> buildClassLikeLightDeclaration(symbol, module, containingFile)
            is CaExtendSymbol -> buildExtendLightDeclaration(symbol, module, containingFile)
            is CaCallableSymbol -> buildCallableLightDeclaration(symbol, module, containingFile)
            else -> null
        }
    }

    private fun CaSession.resolveAnnotations(symbol: CaSymbol): List<CaAnnotation> {
        return (symbol as? CaDeclarationSymbol)?.annotations ?: emptyList()
    }

    private fun CaSession.resolveContainingFile(
        symbol: CaSymbol,
        containingModule: CaModule,
    ): CjFile? {
        symbol.getContainingFile()?.let { return it }

        val packageFqName = when (symbol) {
            is CaPackageSymbol -> symbol.fqName
            is CaClassLikeSymbol -> symbol.classId?.packageFqName
            is CaExtendSymbol -> symbol.targetClassId?.packageFqName
            is CaCallableSymbol -> symbol.callableId?.packageName
            else -> null
        } ?: return null

        return decompiledLightSupport.findContainingFile(packageFqName, containingModule)
    }

    private fun CaSession.sameContainingFile(
        symbol: CaSymbol,
        expectedFile: CjFile,
        module: CaModule,
    ): Boolean {
        val actualFile = resolveContainingFile(symbol, module) ?: return false
        return actualFile.virtualFile?.url == expectedFile.virtualFile?.url
    }

    private fun stableKey(declaration: CaLightDeclaration): String {
        return when (declaration) {
            is CaLightClassLikeDeclaration -> "class:${declaration.classId?.asString() ?: declaration.name}"
            is CaLightExtendDeclaration -> "extend:${declaration.extendId}"
            is CaLightCallableDeclaration -> "callable:${declaration.callableId ?: declaration.name}"
            else -> "package:${declaration.name}"
        }
    }

    /**
     * decompiled 场景下，某些符号 facet 仍然可能要求 source PSI 才能恢复。
     *
     * light declaration 作为只读视图，不应因为这些 source-only facet 缺失而整体失败，
     * 因此这里对 type parameters 做显式边界收敛：source-backed 正常恢复，decompiled 边界
     * 则稳定退化为空列表。
     */
    private fun resolveTypeParameterNames(
        symbol: CaSymbol,
        containingFile: CjFile?,
    ): List<Name> {
        return runCatching {
            when (symbol) {
                is CaClassLikeSymbol -> symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name }
                is CaExtendSymbol -> symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name }
                else -> emptyList()
            }
        }.getOrElse { throwable ->
            if (containingFile?.isCompiled == true && throwable is IllegalStateException) {
                emptyList()
            } else {
                throw throwable
            }
        }
    }

    /**
     * 对 decompiled file 优先走 stub/custom stub builder 恢复包名，避免为了读 package
     * 触发整份反编译文本的 PSI 解析。
     */
    private fun CjFile.resolvePackageFqName(): FqName {
        if (!isCompiled) {
            return packageFqName
        }

        val stubPackage = (stub as? CangJieFileStub)?.getPackageFqName()
            ?: (customStubBuilder?.buildStubTree(this) as? CangJieFileStub)?.getPackageFqName()
        return stubPackage ?: packageFqName
    }
}
