package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.cfir.serialization.provider.CfirClassLikeSymbolLookup
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/** 精确 snapshot 的查询绑定原 context，使用共享 provider traversal 并保持 public import/循环 guard。 */
internal class CjoSnapshotClassSymbolLookup(private val context: CfirDeserializationContext) : CfirClassLikeSymbolLookup {
    internal fun owns(candidate: CfirDeserializationContext): Boolean = candidate === context

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        val current = context.canonical()
        val target = if (classId.packageFqName.asString() == current.header.fullPkgName) current
            else current.packageContextOrNull(classId.packageFqName.asString()) ?: return null
        val provider = target.binding.dependencyManager.deserializedSymbolLookups
            .forBinding(target.binding, target.moduleData, target.mode)
        return target.materialization.read {
            provider.getClassLikeSymbolInContext(classId, target)
        }
    }
}
