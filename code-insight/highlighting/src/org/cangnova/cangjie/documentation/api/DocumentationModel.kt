@file:OptIn(
    org.cangnova.cangjie.psi.CjNonPublicApi::class,
    org.cangnova.cangjie.psi.CjImplementationDetail::class,
)

package org.cangnova.cangjie.documentation.api

import org.cangnova.cangjie.rendering.CangJieAnnotatedText
import org.cangnova.cangjie.rendering.CangJieTextAnnotation

/** 快速文档的完整、格式无关内容。 */
data class DocumentationContent(
    /** 由共享 Analysis renderer policy 生成的规范定义头文本。 */
    val definition: String,
    /** HINT 使用 null；完整文档目标即使没有 CDoc 也使用空章节列表。 */
    val body: DocumentationBody?,
    /** 声明所在的包或类容器；由输出 renderer 决定如何展示。 */
    val container: DocumentationContainer?,
    /** Analysis renderer 同步产出的语言语义区间；输出 renderer 决定是否消费。 */
    val definitionAnnotations: List<CangJieTextAnnotation> = emptyList(),
) {
    init {
        CangJieAnnotatedText(definition, definitionAnnotations)
    }
}

/** 快速文档摘要正文与可选的原生 enum 序号。 */
data class DocumentationBody(
    val sections: List<DocumentationSection>,
    /** CDoc 默认主节摘要或内建类型文档正文；标签主节不重复映射为摘要。 */
    val description: String? = null,
    /** 原生 enum 条目序号；HINT 不携带正文，因此该值不可达。 */
    val enumOrdinal: Int? = null,
)

/** 声明的可展示容器信息。 */
sealed interface DocumentationContainer {
    val fqName: String

    /** 顶层声明所在的包及可选文件名。 */
    data class Package(
        override val fqName: String,
        val fileName: String?,
    ) : DocumentationContainer

    /** 嵌套在类式声明中的成员。 */
    data class ClassLike(
        override val fqName: String,
    ) : DocumentationContainer
}

/** 快速文档的唯一输出扩展点。 */
fun interface DocumentationRenderer<R> {
    fun render(content: DocumentationContent): R
}

/** CDoc 标签映射后的章节类型。 */
sealed interface DocumentationSection

/** 具有名称与描述条目的章节家族。 */
sealed interface NamedEntrySection : DocumentationSection {
    val entries: List<NamedEntry>
}

data class ParametersSection(override val entries: List<NamedEntry>) : NamedEntrySection

data class TypeParametersSection(override val entries: List<NamedEntry>) : NamedEntrySection

data class PropertiesSection(override val entries: List<NamedEntry>) : NamedEntrySection

/** `@throws` 与 `@exception` 合并，保留源码顺序。 */
data class ThrowsSection(override val entries: List<NamedEntry>) : NamedEntrySection

/** 只包含一段 Markdown 文本的章节家族。 */
sealed interface TextSection : DocumentationSection {
    val markdown: String
}

data class ConstructorSection(override val markdown: String) : TextSection

data class ReturnsSection(override val markdown: String) : TextSection

data class SinceSection(override val markdown: String) : TextSection

data class SuppressSection(override val markdown: String) : TextSection

data class AuthorsSection(val names: List<String>) : DocumentationSection

data class SeeAlsoSection(val entries: List<LinkEntry>) : DocumentationSection

data class SamplesSection(val entries: List<SampleEntry>) : DocumentationSection

/** `@param`、`@property`、`@throws` 等标签中的名称与说明。 */
data class NamedEntry(
    val name: String,
    val markdown: String,
    val link: DocumentationLink?,
)

/** `@see` 标签中的展示文本、说明与可选链接。 */
data class LinkEntry(
    val label: String,
    val markdown: String,
    val link: DocumentationLink?,
)

/** `@sample` 的标签与可选解析目标。示例源码由宿主按需提取。 */
data class SampleEntry(
    val label: String,
    val link: DocumentationLink?,
)

/** 文档链接的纯文本身份，不持有 PSI 或平台对象。 */
data class DocumentationLink(
    val label: String,
    /** 限定名；单段名没有足够上下文时为 null。 */
    val fqName: String?,
)
