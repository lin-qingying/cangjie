package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import java.util.Collections
import java.util.IdentityHashMap

/** SOURCE 及原 CJO FILE 各自保留身份；同包文件调度不能按名字混合装载事务。 */
internal sealed interface CjoDeclarationFile {
    data class Source(val file: CfirFile) : CjoDeclarationFile
    data class Loaded(val identity: CjoSnapshotTableEntry) : CjoDeclarationFile
    data class Unknown(val declaration: CfirDeclaration) : CjoDeclarationFile
}

/**
 * 一份冻结依赖合同服务 FINAL 文件 DFS、声明中的依赖 PreSave 和最终 FullId 投影。
 * NotCollected 不代表原 CJO vector 缺失；这里只选择已完成事实，不读 initializer/loader。
 */
internal class CjoDeclarationDependencyInputs(
    private val analysis: CfirDeclarationExportAnalysis,
    private val sources: CjoFrozenSourceInputs,
    private val family: CfirCjmpFamilySnapshot?,
) {
    private val dependencies = IdentityHashMap<CfirDeclaration, List<CjoFrozenDeclarationTarget>>()

    init {
        analysis.capturedDeclarations().forEach { declaration ->
            val targets = when (val state = analysis.initializationDependenciesOf(declaration)) {
                CjmpInitializationDependencies.NotCollected -> sources.formats[declaration]?.dependencies.orEmpty()
                is CjmpInitializationDependencies.Collected -> state.declarations.map { symbol ->
                    val target = sources.canonicalDeclarations.getValue(symbol.cfir)
                    if (analysis.precedingFactsOf(target).isImported) sources.externalDeclarations.getValue(target)
                    else CjoFrozenDeclarationTarget.Current(target)
                }
            }
            dependencies[declaration] = Collections.unmodifiableList(targets.toList())
        }
    }

    fun directTargets(declaration: CfirDeclaration): List<CjoFrozenDeclarationTarget> = dependencies.getValue(declaration)

    fun fileOf(declaration: CfirDeclaration): CjoDeclarationFile {
        analysis.sourceFileOf(declaration)?.let { return CjoDeclarationFile.Source(it) }
        sources.formats[declaration]?.begin?.file?.let { return CjoDeclarationFile.Loaded(it) }
        return CjoDeclarationFile.Unknown(declaration)
    }

    /** 当前定义及已绑定 counterpart 是 planned；其它中间节点仅沿冻结依赖展开。 */
    fun plannedTargets(declaration: CfirDeclaration, planned: Set<CfirDeclaration>): List<CjoFrozenDeclarationTarget> {
        val targets = ArrayList<CjoFrozenDeclarationTarget>()
        fun visit(target: CjoFrozenDeclarationTarget, path: Set<CfirDeclaration>) {
            if (target !is CjoFrozenDeclarationTarget.Current) {
                targets += target
                return
            }
            val canonical = sources.canonicalDeclarations.getValue(target.declaration)
            if (canonical in path) return
            val counterparts = family?.specificBindingsFor(canonical).orEmpty().filter { it in planned }
            when {
                counterparts.isNotEmpty() -> counterparts.forEach { targets += CjoFrozenDeclarationTarget.Current(it) }
                canonical in planned -> targets += CjoFrozenDeclarationTarget.Current(canonical)
                else -> {
                    val next = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>()).apply { addAll(path); add(canonical) }
                    directTargets(canonical).forEach { visit(it, next) }
                }
            }
        }
        val initial = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>()).apply { add(declaration) }
        directTargets(declaration).forEach { visit(it, initial) }
        return Collections.unmodifiableList(targets.toList())
    }
}

