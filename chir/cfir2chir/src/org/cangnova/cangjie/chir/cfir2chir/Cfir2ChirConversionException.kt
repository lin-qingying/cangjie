package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.CfirElement

/**
 * CFIR 到 CHIR 转换过程中遇到不可降级结构时抛出的异常。
 */
class Cfir2ChirConversionException(
    message: String,
    /**
     * 触发失败的 CFIR 元素；无法定位到具体元素时为空。
     */
    val element: CfirElement? = null,
) : IllegalStateException(message)
