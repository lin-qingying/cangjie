package org.cangnova.cangjie.chir.core.serializer

/**
 * CHIR 序列化 schema 常量集合。
 */
object ChirSerializationSchema {
    /**
     * 当前支持的 schema 版本。
     */
    const val CURRENT_VERSION: Int = 1

    /**
     * 顶层记录头标记。
     */
    object Header {
        /**
         * schema 记录标记。
         */
        const val SCHEMA = "S"

        /**
         * 包记录标记。
         */
        const val PACKAGE = "P"
    }

    /**
     * 实体记录类型标记。
     */
    object Entity {
        /**
         * 模块实体标记。
         */
        const val MODULE = "M"

        /**
         * 函数实体标记。
         */
        const val FUNCTION = "F"

        /**
         * 参数实体标记。
         */
        const val PARAMETER = "A"

        /**
         * 基本块实体标记。
         */
        const val BLOCK = "B"

        /**
         * 表达式实体标记。
         */
        const val EXPRESSION = "X"

        /**
         * 终结指令实体标记。
         */
        const val TERMINATOR = "T"
    }
}

/**
 * CHIR 序列化或反序列化失败异常。
 */
class ChirSerializationException(message: String) : IllegalArgumentException(message)
