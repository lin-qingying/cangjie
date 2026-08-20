package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
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
    /**
     * 当前 provider 所属的解析会话。
     */
    private val session: CfirSession,
    /**
     * extend 语义模型与索引的会话级存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {

    /**
     * 查询目标键对应的所有 extend 声明。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
        return indexStore.modelsForTarget(targetKey).map(CfirExtendSemanticModel::declaration)
    }

    /**
     * 查询指定类或接口目标上的 extend 声明。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        return getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))
    }

    /**
     * 查询指定包内声明的 extend。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        return indexStore.modelsInPackage(packageFqName).map(CfirExtendSemanticModel::declaration)
    }

    /**
     * 查询内建基础类型可匹配的 extend 声明。
     *
     * 基础类型可能同时具有语言层类型和运行时类标识，因此需要遍历所有可用于 extend 查找的目标种类。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        return kind.extendLookupKinds
            .flatMap { indexStore.modelsForTarget(CfirExtendTargetKey.ClassLike(it.classId)) }
            .map(CfirExtendSemanticModel::declaration)
            .distinct()
    }

    /**
     * 返回成员符号所属的 extend 声明。
     */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        return indexStore.containingExtendOf(symbol)
    }

    /**
     * 返回 extend 声明所在包名。
     */
    override fun getPackageFqName(extend: CfirExtend): FqName? {
        return indexStore.modelForDeclaration(extend)?.packageFqName
    }

    /** 返回索引中记录的源码 extend 所属文件。 */
    override fun getContainingFile(extend: CfirExtend): CfirFile? {
        return indexStore.modelForDeclaration(extend)?.containingFile
    }

}
