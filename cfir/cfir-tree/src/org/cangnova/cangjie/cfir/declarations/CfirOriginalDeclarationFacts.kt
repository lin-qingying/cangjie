package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

internal object GenericNodePresentKey : CfirDeclarationDataKey()

/** 原始 Generic 语法节点是否存在；错误恢复中的空泛型列表也有节点，where 子句本身没有。 */
var CfirDeclarationAttributes.hasGenericNode: Boolean? by CfirDeclarationDataRegistry.attributesAccessor(GenericNodePresentKey)

private var CfirDeclaration.genericNodePresentAttribute: Boolean? by CfirDeclarationDataRegistry.data(GenericNodePresentKey)

/** raw builder 和 CJO 读取各自保存的原始节点事实，不从已转换的类型参数数量推断。 */
var CfirDeclaration.hasGenericNode: Boolean
    get() = genericNodePresentAttribute == true
    set(value) {
        genericNodePresentAttribute = value
    }

internal object CjmpSourceNominalDefaultKey : CfirDeclarationDataKey()

/** 源码容器解析完成时计算的默认实现事实；null 表示没有可用的源码事实。 */
var CfirDeclarationAttributes.cjmpSourceNominalHasDefault: Boolean? by
    CfirDeclarationDataRegistry.attributesAccessor(CjmpSourceNominalDefaultKey)

/** 直接成员经宏替换或参数字段反糖后，仍读取解析时的容器事实。 */
val CfirDeclaration.cjmpSourceNominalHasDefault: Boolean? by
    CfirDeclarationDataRegistry.data(CjmpSourceNominalDefaultKey)

internal object OriginalDoNotExportKey : CfirDeclarationDataKey()
private var CfirDeclaration.originalDoNotExportAttribute: Boolean? by CfirDeclarationDataRegistry.data(OriginalDoNotExportKey)

/**
 * 声明进入容器合并前的原始导出抑制位；普通源声明与从 CJO 物化的声明均为 false。
 * CJMP 合并产生的最终导出状态仅存在于聚合结果，不能回写本属性或从 common/private 推断。
 */
var CfirDeclaration.originalDoNotExport: Boolean
    get() = originalDoNotExportAttribute == true
    set(value) {
        originalDoNotExportAttribute = value
    }
