package org.cangnova.cangjie.frontend.arguments

/**
 * 可冻结的前端参数基类（对齐 K2 的 Freezable）。
 */
abstract class Freezable {
    /**
     * 当前对象是否已经进入不可变状态。
     */
    private var frozen = false

    /**
     * 检查当前对象仍可修改。
     *
     * 冻结后继续修改属于前端参数构建流程错误，会直接抛出异常。
     */
    fun checkFrozen() {
        if (frozen) {
            error("Cannot modify frozen object")
        }
    }

    /**
     * 将当前对象标记为冻结状态。
     */
    fun freeze() {
        frozen = true
    }

    /**
     * 创建当前参数对象的可变副本。
     */
    abstract fun copyOf(): Freezable
}
