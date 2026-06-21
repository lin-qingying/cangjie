package org.cangnova.cangjie.chir.core.serializer

object ChirSerializationSchema {
    const val CURRENT_VERSION: Int = 1

    object Header {
        const val SCHEMA = "S"
        const val PACKAGE = "P"
    }

    object Entity {
        const val MODULE = "M"
        const val FUNCTION = "F"
        const val PARAMETER = "A"
        const val BLOCK = "B"
        const val EXPRESSION = "X"
        const val TERMINATOR = "T"
    }
}

class ChirSerializationException(message: String) : IllegalArgumentException(message)
