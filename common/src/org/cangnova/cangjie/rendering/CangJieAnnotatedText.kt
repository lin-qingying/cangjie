package org.cangnova.cangjie.rendering

/** 仓颉渲染文本中的语言语义类别；不含配色或宿主信息。 */
enum class CangJieSemanticKind {
    CLASS,
    INTERFACE,
    STRUCT,
    ENUM,
    TYPE_ALIAS,
    TYPE_PARAMETER,
    FUNCTION,
    PARAMETER,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    PACKAGE,
    OTHER,
}

/** 使用 UTF-16 半开区间表示的语义标注。 */
data class CangJieTextAnnotation(
    val startOffset: Int,
    val endOffset: Int,
    val kind: CangJieSemanticKind,
) {
    init {
        require(startOffset >= 0) { "Text annotation start must be non-negative" }
        require(endOffset > startOffset) { "Text annotation range must not be empty" }
    }
}

/** 一次渲染得到的文本与同步收集的语义区间。 */
data class CangJieAnnotatedText(
    val text: String,
    val annotations: List<CangJieTextAnnotation>,
) {
    init {
        require(annotations.all { it.endOffset <= text.length }) {
            "Text annotation range extends past the rendered text"
        }
        require(annotations.all { '\n' !in text.substring(it.startOffset, it.endOffset) }) {
            "A semantic text annotation must not include a line break"
        }
        require(annotations.zipWithNext().all { (current, next) -> current.endOffset <= next.startOffset }) {
            "Text annotations must be ordered and non-overlapping"
        }
    }
}
