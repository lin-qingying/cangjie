package org.cangnova.cangjie.descriptors

interface SourceFile {
    val name: String?

    companion object {
        val NO_SOURCE_FILE: SourceFile = object : SourceFile {
            override val name: String? get() = null
        }
    }
}
