package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.Collections

/**
 * 语义准备 owner 提供的已完成依赖事实。来源选择可以据此建立自己的定义请求，
 * 但库存中的声明不因此取得输出资格；查询不解析类型、不展开 alias，也不装载 CJO。
 * 工作集视图只在 withCompletedInputs 回调中有效，最终 Result 则保持一次交付的输入域。
 */
interface CfirPreparedExportDependencyInputs {
    val declarations: List<CfirDeclaration>
    fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType?
    fun typeDependenciesOf(type: ConeCangJieType, domain: CfirSession): CfirPreparedExportTypeDependencies
    fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration?
    fun sourceFileOf(declaration: CfirDeclaration): CfirFile?
}

/** 原 type/session 域中的完成引用；列表只复制容器，所有 Cone/声明仍保留实际身份。 */
class CfirPreparedExportTypeDependencies internal constructor(
    val type: ConeCangJieType,
    val domain: CfirSession,
    children: List<ConeCangJieType>,
    val declaration: CfirDeclaration?,
    val abbreviatedType: ConeCangJieType?,
    val abbreviatedDeclaration: CfirDeclaration?,
    val expandedType: ConeCangJieType,
    val expandedDeclaration: CfirDeclaration?,
) {
    val children: List<ConeCangJieType> = Collections.unmodifiableList(children.toList())
}
