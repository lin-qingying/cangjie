package org.cangnova.cangjie.chir.core.expression

object ChirOperationSets {
    val unaryOperators: Set<String> = ChirUnaryOperator.acceptedNames

    val binaryOperators: Set<String> = ChirBinaryOperator.acceptedNames

    val memoryOperations: Set<String> = ChirMemoryOperation.acceptedNames

    val otherOperations: Set<String> = ChirOtherOperation.acceptedNames
}
