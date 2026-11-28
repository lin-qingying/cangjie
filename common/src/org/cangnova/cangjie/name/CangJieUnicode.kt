package org.cangnova.cangjie.name

import com.ibm.icu.text.Normalizer2

/** 仓颉源码名称使用 Unicode 15.0 NFC；源码范围由调用方独立保留。 */
object CangJieUnicode {
    private val nfc: Normalizer2 = Normalizer2.getNFCInstance()

    /** 返回规范组合形式，不修改原始源码或位置。 */
    fun normalizeIdentifier(value: String): String = nfc.normalize(value)

    /** 将源码中的完整标识符转换为语义名称，原始拼写和反引号仍由语法节点保留。 */
    fun sourceIdentifier(value: String): String = normalizeIdentifier(value.removeSurrounding("`"))
}
