package org.cangnova.cangjie.analysis.light.declarations.symbol

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.asSignature
import org.cangnova.cangjie.analysis.api.components.combinedDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.components.declaredMemberScope
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.decompiled.light.declarations.CaDecompiledLightSupport
import org.cangnova.cangjie.analysis.light.declarations.CaLightCallableDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightClassLikeDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCache
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCacheKey
import org.cangnova.cangjie.analysis.light.declarations.CaLightExtendDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightPackageDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.sourceOrigin
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.isCangJieFileType

/**
 * 基于 Analysis API symbol 的轻量声明视图提供器。
 *
 * 实现分为三层：
 * 1. 通过 project-structure/decompiled support 收集模块或文件；
 * 2. 在统一 analyze session 中把 PSI 恢复为公开 symbol；
 * 3. 用 `analysis:light-declarations` 的只读模型投影 class-like、extend、callable 与 package。
 */
class CaSymbolLightDeclarationProvider(
    private val project: Project,
) : CaLightDeclarationProvider {
    private val psiManager: PsiManager = PsiManager.getInstance(project)

    private val projectStructure: CangJieProjectStructureProvider
        get() = CangJieProjectStructureProvider.getInstance(project)

    private val decompiledSupport: CaDecompiledLightSupport
        get() = CaDecompiledLightSupport(project)

    override fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration? {
        val pointer = symbol.createPointer()
        return analyze(symbol.containingModule) {
            val restoredSymbol = restoreSymbol(pointer) ?: return@analyze null
            LightDeclarationBuilder(this).build(restoredSymbol)
        }
    }

    override fun getLightDeclarations(file: CjFile, useSiteModule: CaModule?): List<CaLightDeclaration> {
        val module = useSiteModule ?: projectStructure.getModule(file)
        return analyze(module) {
            LightDeclarationBuilder(this).buildFile(file)
        }
    }

    override fun getLightDeclarations(module: CaModule): List<CaLightDeclaration> {
        val files = collectModuleFiles(module)
        if (files.isEmpty()) return emptyList()

        return analyze(module) {
            val builder = LightDeclarationBuilder(this)
            files.flatMap(builder::buildFile)
        }
    }

    override fun getPackageLightDeclaration(packageFqName: FqName, useSiteModule: CaModule): CaLightDeclaration? {
        val containingFile = findPackageFiles(packageFqName, useSiteModule).firstOrNull()
        if (containingFile == null && !decompiledSupport.hasPackage(packageFqName)) {
            return null
        }

        return analyze(useSiteModule) {
            CaLightPackageDeclarationImpl(
                packageName = packageFqName.asString(),
                module = useSiteModule,
                origin = sourceOrigin(
                    description = packageFqName.asString(),
                    containingFile = containingFile,
                    sourceElement = containingFile,
                ),
                token = token,
            )
        }
    }

    override fun findLightDeclarations(packageFqName: FqName, name: Name, useSiteModule: CaModule): List<CaLightDeclaration> {
        val files = findPackageFiles(packageFqName, useSiteModule)
        if (files.isEmpty()) return emptyList()

        return analyze(useSiteModule) {
            val builder = LightDeclarationBuilder(this)
            files.flatMap(builder::buildFile)
                .filter { declaration -> declaration.name == name.asString() }
        }
    }

    private fun collectModuleFiles(module: CaModule): List<CjFile> {
        val sourceFiles = collectSourceFiles()
            .filter { file -> projectStructure.getModule(file, module) == module }
        val decompiledFiles = decompiledSupport.getDecompiledFiles(module)
        return (sourceFiles + decompiledFiles).distinctBy { file -> file.virtualFile ?: file }
    }

    private fun findPackageFiles(packageFqName: FqName, useSiteModule: CaModule): List<CjFile> {
        val sourceFiles = collectModuleFiles(useSiteModule)
            .filter { file -> file.packageFqName == packageFqName }
        val decompiledFile = decompiledSupport.findContainingFile(packageFqName, useSiteModule)
        return (sourceFiles + listOfNotNull(decompiledFile)).distinctBy { file -> file.virtualFile ?: file }
    }

    private fun collectSourceFiles(): List<CjFile> {
        val files = linkedSetOf<CjFile>()
        projectStructure.allSourceFiles.forEach { item ->
            collectCangJieFiles(item, files)
        }
        return files.toList()
    }

    private fun collectCangJieFiles(item: PsiFileSystemItem, destination: MutableSet<CjFile>) {
        when (item) {
            is CjFile -> destination += item
            is PsiFile -> (item as? CjFile)?.let(destination::add)
            is PsiDirectory -> {
                VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { virtualFile ->
                    if (virtualFile.isDirectory || !virtualFile.isCangJieLightDeclarationCandidate()) {
                        return@iterateChildrenRecursively true
                    }
                    (psiManager.findFile(virtualFile) as? CjFile)?.let(destination::add)
                    true
                }
            }
        }
    }

    private fun com.intellij.openapi.vfs.VirtualFile.isCangJieLightDeclarationCandidate(): Boolean {
        return fileType == CangJieBuiltInFileType ||
            extension.equals("cjo", ignoreCase = true) ||
            isCangJieFileType()
    }

    private inner class LightDeclarationBuilder(
        private val session: CaSession,
    ) {
        private val cache = CaLightDeclarationCache()

        fun buildFile(file: CjFile): List<CaLightDeclaration> {
            return file.declarations.mapNotNull(::buildDeclaration)
        }

        private fun buildDeclaration(declaration: CjDeclaration): CaLightDeclaration? {
            return build(with(session) { declaration.symbol })
        }

        fun build(symbol: CaSymbol): CaLightDeclaration? {
            return when (symbol) {
                is CaExtendSymbol -> buildExtend(symbol)
                is CaClassLikeSymbol -> buildClassLike(symbol)
                is CaCallableSymbol -> buildCallable(symbol)
                else -> null
            }
        }

        private fun buildClassLike(symbol: CaClassLikeSymbol): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("classLike:${symbol.classId?.asString() ?: symbol.name?.asString() ?: symbol.hashCode()}")
            return cache.getOrPut(key) {
                CaLightClassLikeDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotations = symbol.annotations,
                    origin = symbol.origin("class-like"),
                    token = session.token,
                    classId = symbol.classId,
                    typeParameters = symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name },
                    superTypes = when (symbol) {
                        is CaClassSymbol -> symbol.superTypes
                        else -> emptyList()
                    },
                    members = when (symbol) {
                        is CaClassSymbol -> with(session) { symbol.combinedDeclaredMemberScope }
                            .declarations
                            .mapNotNull(::build)
                            .toList()
                        else -> emptyList()
                    },
                )
            }
        }

        private fun buildExtend(symbol: CaExtendSymbol): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("extend:${symbol.extendId}")
            return cache.getOrPut(key) {
                CaLightExtendDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotations = symbol.annotations,
                    origin = symbol.origin("extend"),
                    token = session.token,
                    extendId = symbol.extendId,
                    targetClassId = symbol.targetClassId,
                    extendedType = symbol.extendedType,
                    typeParameters = symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name },
                    superTypes = symbol.superTypes,
                    members = with(session) { symbol.declaredMemberScope }
                        .declarations
                        .mapNotNull(::build)
                        .toList(),
                )
            }
        }

        private fun buildCallable(symbol: CaCallableSymbol): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("callable:${symbol.callableId ?: symbol.name?.asString() ?: symbol.hashCode()}")
            return cache.getOrPut(key) {
                CaLightCallableDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotations = symbol.annotations,
                    origin = symbol.origin("callable"),
                    token = session.token,
                    callableId = symbol.callableId,
                    signature = with(session) { symbol.asSignature() },
                )
            }
        }

        private fun CaSymbol.origin(kind: String) = sourceOrigin(
            description = when (this) {
                is CaClassLikeSymbol -> classId?.asString() ?: name?.asString() ?: kind
                is CaExtendSymbol -> extendId
                is CaCallableSymbol -> callableId?.toString() ?: name?.asString() ?: kind
                else -> name?.asString() ?: kind
            },
            containingFile = psi?.containingFile as? CjFile,
            sourceElement = psi,
        )
    }
}
