package org.cangnova.cangjie.arguments.dsl.base

import org.cangnova.cangjie.arguments.dsl.types.CangJieArgumentValueType
import kotlin.properties.ReadOnlyProperty

@OptIn(ExperimentalArgumentApi::class)
data class CangJieCompilerArgument(
    val name: String,
    val shortName: String? = null,
    val deprecatedName: String? = null,
    val description: ReleaseDependent<String>,
    val delimiter: Delimiter?,
    val valueType: CangJieArgumentValueType<*>,
    val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),
    override val releaseVersionsMetadata: CangJieReleaseVersionLifecycle,

    val argumentType: CangJieArgumentValueType<*> = valueType,
    val additionalAnnotations: List<Annotation> = emptyList(),
    val compilerName: String? = null,
    val isObsolete: Boolean = false,
) : WithCangJieReleaseVersionsMetadata {

    enum class Delimiter(val constantName: String) {
        Default("default"),
        None("none"),
        PathSeparator("path-separator"),
        Space("space"),
        Semicolon("semicolon"),
    }
}
@ExperimentalArgumentApi
@CangJieArgumentsDslMarker
class CangJieCompilerArgumentBuilder {
    lateinit var name: String
    var shortName: String? = null
    var deprecatedName: String? = null
    lateinit var description: ReleaseDependent<String>
    lateinit var valueType: CangJieArgumentValueType<*>
    var valueDescription: ReleaseDependent<String?> = null.asReleaseDependent()

    var argumentType: CangJieArgumentValueType<*>? = null
    var compilerName: String? = null
    var delimiter: CangJieCompilerArgument.Delimiter? = null
    private lateinit var releaseVersionsMetadata: CangJieReleaseVersionLifecycle
    private val additionalAnnotations: MutableList<Annotation> = mutableListOf()

    fun lifecycle(
        introducedVersion: CangJieReleaseVersion,
        stabilizedVersion: CangJieReleaseVersion? = null,
        deprecatedVersion: CangJieReleaseVersion? = null,
        removedVersion: CangJieReleaseVersion? = null,
    ) {
        releaseVersionsMetadata = CangJieReleaseVersionLifecycle(
            introducedVersion,
            stabilizedVersion,
            deprecatedVersion,
            removedVersion
        )
    }

    fun additionalAnnotations(vararg annotation: Annotation) {
        additionalAnnotations.addAll(annotation)
    }

    @OptIn(ExperimentalArgumentApi::class)
    fun build(): CangJieCompilerArgument = CangJieCompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        argumentType = argumentType ?: valueType,
        releaseVersionsMetadata = releaseVersionsMetadata,
        additionalAnnotations = additionalAnnotations,
        compilerName = compilerName,
        delimiter = delimiter,
    )
}
@ExperimentalArgumentApi
fun compilerArgument(
    config: CangJieCompilerArgumentBuilder.() -> Unit,
) = ReadOnlyProperty<Any?, CangJieCompilerArgument> { _, _ ->
    val builder = CangJieCompilerArgumentBuilder()
    config(builder)
    builder.build()
}
