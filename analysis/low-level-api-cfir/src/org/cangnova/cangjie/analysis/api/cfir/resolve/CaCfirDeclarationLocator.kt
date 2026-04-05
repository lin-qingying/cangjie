package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclarationContainer
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.ConcurrentHashMap

/**
 * use-site 模块闭包上的源码声明定位器。
 *
 * 这层职责对应 low-level 里的 PSI-aware declaration lookup，但语义必须保持仓颉约束：
 * 1. 只在当前 use-site 模块闭包可见的源码根中工作。
 * 2. `ClassId` 只定位顶层 class-like 声明。
 * 3. callable 只定位顶层 callable，或顶层 class-like 的直接成员 callable。
 */
internal class CaCfirDeclarationLocator(
    private val moduleResolveComponents: CaCfirModuleResolveComponents,
) {
    private val classLikeDeclarationCache = ConcurrentHashMap<ClassId, CjClassLikeDeclaration?>()
    private val callableDeclarationCache = ConcurrentHashMap<CallableId, CjCallableDeclaration?>()

    private val visibleSourceFiles: List<CjFile> by lazy(LazyThreadSafetyMode.NONE) {
        moduleResolveComponents.allModules.asSequence()
            .flatMap { module ->
                moduleResolveComponents.globalResolveComponents.getVisibleRoots(module).asSequence()
            }
            .filterIsInstance<CjFile>()
            .distinct()
            .toList()
    }

    fun findClassLikeDeclaration(classId: ClassId): CjClassLikeDeclaration? =
        classLikeDeclarationCache.computeIfAbsent(classId, ::locateClassLikeDeclaration)

    fun findCallableDeclaration(callableId: CallableId): CjCallableDeclaration? =
        callableDeclarationCache.computeIfAbsent(callableId, ::locateCallableDeclaration)

    private fun locateClassLikeDeclaration(classId: ClassId): CjClassLikeDeclaration? {
        return visibleSourceFiles.asSequence()
            .filter { file -> file.packageFqName == classId.packageFqName }
            .flatMap(::topLevelClassLikeDeclarations)
            .firstOrNull { declaration -> declaration.getClassId() == classId }
    }

    private fun locateCallableDeclaration(callableId: CallableId): CjCallableDeclaration? {
        val declarations = when (val ownerClassId = callableId.classId) {
            null -> visibleSourceFiles.asSequence()
                .filter { file -> file.packageFqName == callableId.packageName }
                .flatMap(::directCallableDeclarations)

            else -> {
                val container = findClassLikeDeclaration(ownerClassId) as? CjDeclarationContainer ?: return null
                directCallableDeclarations(container)
            }
        }

        return declarations.firstOrNull { declaration ->
            declaration.fqName == callableId.asSingleFqName()
        }
    }
}

private fun topLevelClassLikeDeclarations(file: CjFile): Sequence<CjClassLikeDeclaration> =
    file.declarations.asSequence().filterIsInstance<CjClassLikeDeclaration>()

private fun directCallableDeclarations(container: CjDeclarationContainer): Sequence<CjCallableDeclaration> =
    container.declarations.asSequence().filterIsInstance<CjCallableDeclaration>()
