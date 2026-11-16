package org.cangnova.cangjie.documentation.builtin

import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.documentation.api.DocumentationBody
import org.cangnova.cangjie.documentation.api.DocumentationContent
import org.cangnova.cangjie.documentation.resolve.DocumentationMode
import org.cangnova.cangjie.psi.CjPsiBundle

/** 为没有声明符号的基本类型提供独立、可本地化的快速文档正文。 */
internal object BasicTypeDocumentation {
    fun content(typeName: String, mode: DocumentationMode): DocumentationContent? {
        val primitiveType = PrimitiveType.entries.singleOrNull { type ->
            type.typeName.asString() == typeName
        } ?: return null

        val body = when (mode) {
            DocumentationMode.FULL -> DocumentationBody(
                sections = emptyList(),
                description = CjPsiBundle.message(primitiveType.documentationMessageKey),
            )

            DocumentationMode.HINT -> null
        }

        return DocumentationContent(
            definition = primitiveType.typeName.asString(),
            body = body,
            container = null,
        )
    }

    private val PrimitiveType.documentationMessageKey: String
        get() = when (this) {
            PrimitiveType.BOOL -> "quick.doc.basic.type.bool"
            PrimitiveType.Rune -> "quick.doc.basic.type.rune"
            PrimitiveType.INT8 -> "quick.doc.basic.type.int8"
            PrimitiveType.INT16 -> "quick.doc.basic.type.int16"
            PrimitiveType.INT32 -> "quick.doc.basic.type.int32"
            PrimitiveType.INT64 -> "quick.doc.basic.type.int64"
            PrimitiveType.INTNATIVE -> "quick.doc.basic.type.int.native"
            PrimitiveType.UINT8 -> "quick.doc.basic.type.uint8"
            PrimitiveType.UINT16 -> "quick.doc.basic.type.uint16"
            PrimitiveType.UINT32 -> "quick.doc.basic.type.uint32"
            PrimitiveType.UINT64 -> "quick.doc.basic.type.uint64"
            PrimitiveType.UINTNATIVE -> "quick.doc.basic.type.uint.native"
            PrimitiveType.FLOAT16 -> "quick.doc.basic.type.float16"
            PrimitiveType.FLOAT32 -> "quick.doc.basic.type.float32"
            PrimitiveType.FLOAT64 -> "quick.doc.basic.type.float64"
            PrimitiveType.Unit -> "quick.doc.basic.type.unit"
            PrimitiveType.Nothing -> "quick.doc.basic.type.nothing"
        }
}
