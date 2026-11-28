package org.cangnova.cangjie.cfir

/**
 * CFIR 声明附加数据的键基类。
 *
 * 每个 key 对应 [org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes]
 * 中的一类扩展数据，用于在不扩展声明接口的情况下保存阶段性元信息。
 */
abstract class CfirDeclarationDataKey {
    /** 非空声明数据只有登记了类型化策略才可导入 RAW 图；扩展 key 默认明确未分类。 */
    open val rawCopyPolicy: CfirRawDeclarationDataPolicy<*>? get() = null
}
