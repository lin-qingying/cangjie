package org.cangnova.cangjie.chir.core.expression

/**
 * CHIR 支持的操作名称集合。
 */
object ChirOperationSets {
    /**
     * 所有一元操作名称。
     */
    val unaryOperators: Set<String> = ChirUnaryOperator.acceptedNames

    /**
     * 所有二元操作名称。
     */
    val binaryOperators: Set<String> = ChirBinaryOperator.acceptedNames

    /**
     * 所有内存操作名称。
     */
    val memoryOperations: Set<String> = ChirMemoryOperation.acceptedNames

    /**
     * 所有其他操作名称。
     */
    val otherOperations: Set<String> = ChirOtherOperation.acceptedNames
}
