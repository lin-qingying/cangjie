package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendSemanticModel
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.extendLookupKinds
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Session-scoped extend provider backed by [CfirExtendIndexStore].
 *
 * 这里同时暴露目标类型索引与成员 owner 索引，确保 providers 层就能完成
 * extend 成员的语义归属判定，而不是把 owner 反推逻辑泄漏到解析阶段。
 */
class CfirSessionExtendProvider(
    private val session: CfirSession,
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {

    private val accessibilityChecker by lazy { CfirExtendAccessibilityChecker(session) }

    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
        return indexStore.modelsForTarget(targetKey).map(CfirExtendSemanticModel::declaration)
    }

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        return getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))
    }

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        return indexStore.modelsInPackage(packageFqName).map(CfirExtendSemanticModel::declaration)
    }

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        return kind.extendLookupKinds
            .flatMap { indexStore.modelsForTarget(CfirExtendTargetKey.ClassLike(it.classId)) }
            .map(CfirExtendSemanticModel::declaration)
            .distinct()
    }

    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        return indexStore.containingExtendOf(symbol)
    }

    override fun isExtendAccessible(extend: CfirExtend): Boolean {
        val file = CfirAccessibilityFileScope.get() ?: return true
        return accessibilityChecker.isAccessible(file, extend)
    }
}
