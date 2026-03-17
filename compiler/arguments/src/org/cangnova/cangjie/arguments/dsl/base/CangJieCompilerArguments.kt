package org.cangnova.cangjie.arguments.dsl.base

import org.cangnova.cangjie.arguments.dsl.types.AllCangJieArgumentTypes

data class KotlinCompilerArguments(
    val schemaVersion: Int = 1,
    val releases: Set<CangJieReleaseVersion> = CangJieReleaseVersion.entries.toSet(),
    val types: AllCangJieArgumentTypes = AllCangJieArgumentTypes(),
    val topLevel: CangJieCompilerArgumentsLevel,
)

@CangJieArgumentsDslMarker
class KotlinCompilerArgumentsBuilder {
    private lateinit var topLevel: CangJieCompilerArgumentsLevel

    fun topLevel(
        name: String,
        mergeWith: Set<CangJieCompilerArgumentsLevel> = emptySet(),
        config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        topLevel = mergeWith.fold(levelBuilder.build()) { init, level -> init.mergeWith(level) }
    }

    fun build(): KotlinCompilerArguments = KotlinCompilerArguments(
        topLevel = topLevel
    )
}

fun compilerArguments(
    config: KotlinCompilerArgumentsBuilder.() -> Unit,
): KotlinCompilerArguments {
    val kotlinArguments = KotlinCompilerArgumentsBuilder()
    config(kotlinArguments)
    return kotlinArguments.build()
}
