package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
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
    /** extend 所在包名。 */
    val packageFqName: FqName,
    /** extend 所在文件名。 */
    val fileName: String,
    /** extend 在文件内的声明序号。 */
    val declarationIndexInFile: Int,
    /** 被扩展目标的规范化 key。 */
    val targetKey: CfirExtendTargetKey?,
    /** 被扩展目标 classId。 */
    val targetClassId: ClassId?,
    /** 被扩展目标 class kind。 */
    val targetClassKind: CfirClassKind?,
    /** extend 继承接口的完整语义列表。 */
    val inheritedInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    /** extend 继承接口的 classId 列表。 */
    val inheritedInterfaceClassIds: List<ClassId>,
    /** extend 继承接口的稳定语义 key 列表。 */
    val inheritedInterfaceSemanticKeys: List<String>,
    /** 目标类型自身声明的接口语义列表。 */
    val targetOwnInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    /** 目标类型自身接口的稳定语义 key 列表。 */
    val targetOwnInterfaceSemanticKeys: List<String>,
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
