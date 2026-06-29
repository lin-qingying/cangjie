package org.cangnova.cangjie.name

import kotlin.text.iterator

/**
 * 仓颉名称值对象。
 */
data class Name(private val name: String, val isSpecial: Boolean) : Comparable<Name> {

    /**
     * 返回名称原始字符串。
     */
    fun asString(): String = name

    /**
     * 返回普通标识符字符串；特殊名称会触发错误。
     */
    val identifier: String
        get() {
            check(!isSpecial) { "not identifier: $this" }
            return asString()
        }

    /**
     * 普通标识符返回字符串，特殊名称返回 null。
     */
    val identifierOrNullIfSpecial: String?
        get() = if (isSpecial) null else asString()

    /**
     * 特殊名称去掉尖括号后返回；普通名称原样返回。
     */
    fun asStringStripSpecialMarkers(): String =
        if (isSpecial) asString().substring(1, asString().length - 1) else asString()

    /**
     * 按名称字符串进行字典序比较。
     */
    override operator fun compareTo(other: Name): Int = name.compareTo(other.name)

    /**
     * 返回名称字符串。
     */
    override fun toString(): String = name

    /**
     * 按名称字符串和特殊名称标记判断相等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Name) return false
        if (isSpecial != other.isSpecial) return false
        return name == other.name
    }

    /**
     * 返回名称字符串和特殊标记组合后的哈希值。
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + if (isSpecial) 1 else 0
        return result
    }

    companion object {
        /**
         * 错误恢复使用的特殊名称。
         */
        @JvmField
        val ERROR_NAME = Name("<error>", true)

        /**
         * 创建普通标识符名称。
         */
        @JvmStatic
        fun identifier(name: String): Name = Name(name, false)

        /**
         * 判断字符串是否可作为普通标识符名称。
         */
        @JvmStatic
        fun isValidIdentifier(name: String): Boolean {
            if (name.isEmpty() || name.startsWith("<")) return false
            for (element in name) {
                if (element == '.' || element == '/' || element == '\\') {
                    return false
                }
            }
            return true
        }

        /**
         * 字符串合法时创建普通标识符名称，否则返回 null。
         */
        @JvmStatic
        fun identifierIfValid(name: String): Name? =
            if (!isValidIdentifier(name)) null else identifier(name)

        /**
         * 创建特殊名称，要求以 `<` 开头。
         */
        @JvmStatic
        fun special(name: String): Name {
            require(name.startsWith("<")) { "special name must start with '<': $name" }
            return Name(name, true)
        }

        /**
         * 根据首字符推断普通名称或特殊名称。
         */
        @JvmStatic
        fun guessByFirstCharacter(name: String): Name =
            if (name.startsWith("<")) special(name) else identifier(name)
    }
}
