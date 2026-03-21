package org.cangnova.cangjie.type

enum class EmptyIntersectionTypeKind(val description: String, val isDefinitelyEmpty: Boolean) {
    MULTIPLE_CLASSES("multiple incompatible classes", isDefinitelyEmpty = true),
    FINAL_CLASS_AND_INTERFACE("final class and interface", isDefinitelyEmpty = false)
}
