package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/** extend 语义模型的声明来源分类。 */
enum class CfirExtendSemanticOrigin {
    SOURCE,
    LIBRARY,
    SYNTHETIC,
    OTHER,
}

/** 单个 extend 声明归一化后的语义索引模型。 */
data class CfirExtendSemanticModel(
    /** 原始 CFIR extend 声明。 */
    val declaration: CfirExtend,
    /** extend 声明实际所属的源文件。 */
    val containingFile: CfirFile,
    /** extend 所在包名。 */
    val packageFqName: FqName,
    /** extend 所在文件名。 */
    val fileName: String,
    /** extend 在文件内的声明序号。 */
    val declarationIndexInFile: Int,
    /** 被扩展目标的规范化 key。 */
    val targetKey: CfirExtendTargetKey?,
    /** 被扩展目标完整实例化模式的稳定语义 key。 */
    val targetSemanticKey: String?,
    /**
     * 供官方重复接口汇总使用的目标身份。
     *
     * 该键只做 typealias 展开和 extend 类型参数 alpha-renaming，不编码 where bound；
     * [targetSemanticKey] 仍保留完整约束信息，不能用本字段替代特化匹配键。
     */
    val duplicateTargetSemanticKey: String?,
    /** 源码中声明的目标 key；typealias 目标保留 alias 声明身份。 */
    val declaredTargetKey: CfirExtendTargetKey?,
    /** 被扩展真实目标的 classId。 */
    val targetClassId: ClassId?,
    /** 源码中声明的目标 classId；typealias 目标为 alias 自身 classId。 */
    val declaredTargetClassId: ClassId?,
    /** 被扩展目标 class kind。 */
    val targetClassKind: CfirClassKind?,
    /** extend 继承接口的完整语义列表。 */
    val inheritedInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    /** extend 继承接口的 classId 列表。 */
    val inheritedInterfaceClassIds: List<ClassId>,
    /** extend 继承接口的稳定语义 key 列表。 */
    val inheritedInterfaceSemanticKeys: List<String>,
    /**
     * extend 直接接口用于重复汇总的无 bounds 语义 key 列表，与 [inheritedInterfaces] 对齐。
     */
    val duplicateInterfaceSemanticKeys: List<String>,
    /** extend 继承接口连同其传递父接口的语义列表（实例化后）。 */
    val inheritedInterfaceClosure: List<CfirExtendInheritedInterfaceSemantic>,
    /** extend 声明来源。 */
    val origin: CfirExtendSemanticOrigin,
)

/** 将 CFIR 声明 origin 映射为 extend 语义索引使用的来源分类。 */
internal fun CfirDeclarationOrigin.toExtendSemanticOrigin(): CfirExtendSemanticOrigin = when (this) {
    CfirDeclarationOrigin.Source -> CfirExtendSemanticOrigin.SOURCE
    CfirDeclarationOrigin.Library -> CfirExtendSemanticOrigin.LIBRARY
    CfirDeclarationOrigin.Synthetic.Default,
    CfirDeclarationOrigin.ImplicitDefault,
    CfirDeclarationOrigin.Extension -> CfirExtendSemanticOrigin.SYNTHETIC
    else -> CfirExtendSemanticOrigin.OTHER
}
