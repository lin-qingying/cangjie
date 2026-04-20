package org.cangnova.cangjie.analysis.api.resolution

enum class CaCallOrigin {
    REGULAR,
    OPERATOR,
    CONSTRUCTOR_DELEGATION_THIS,
    CONSTRUCTOR_DELEGATION_SUPER,
}
