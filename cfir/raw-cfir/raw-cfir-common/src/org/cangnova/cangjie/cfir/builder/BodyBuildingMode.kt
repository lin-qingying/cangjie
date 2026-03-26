package org.cangnova.cangjie.cfir.builder

enum class BodyBuildingMode {
    NORMAL,
    LAZY_BODIES,

    ;

    companion object {
        fun lazyBodies(lazyBodies: Boolean): BodyBuildingMode = if (lazyBodies) LAZY_BODIES else NORMAL
    }
}
