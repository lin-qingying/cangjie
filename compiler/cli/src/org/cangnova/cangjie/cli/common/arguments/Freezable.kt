package org.cangnova.cangjie.cli.common.arguments

/**
 * 可冻结的基类（对齐 K2 的 Freezable）
 */
abstract class Freezable {
    private var frozen = false

    fun checkFrozen() {
        if (frozen) {
            error("Cannot modify frozen object")
        }
    }

    fun freeze() {
        frozen = true
    }

    abstract fun copyOf(): Freezable
}
