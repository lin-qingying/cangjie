package org.cangnova.cangjie.descriptors

enum class Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    FINAL,
    SEALED,
    OPEN,
    ABSTRACT,
    ;

    companion object {
        fun convertFromFlags(sealed: Boolean, abstract: Boolean, open: Boolean): Modality = when {
            sealed -> SEALED
            abstract -> ABSTRACT
            open -> OPEN
            else -> FINAL
        }
    }
}
