package org.cangnova.cangjie.descriptors

/**
 * 声明的可继承性与抽象性分类。
 */
enum class Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    /**
     * 不允许被覆盖或继承的最终声明。
     */
    FINAL,
    /**
     * 只能在受限层级中扩展的密封声明。
     */
    SEALED,
    /**
     * 允许被继承或覆盖的开放声明。
     */
    OPEN,
    /**
     * 必须由具体实现补全的抽象声明。
     */
    ABSTRACT,
    ;

    companion object {
        /**
         * 根据序列化或前端标志组合转换为 modality。
         */
        fun convertFromFlags(sealed: Boolean, abstract: Boolean, open: Boolean): Modality = when {
            sealed -> SEALED
            abstract -> ABSTRACT
            open -> OPEN
            else -> FINAL
        }
    }
}
