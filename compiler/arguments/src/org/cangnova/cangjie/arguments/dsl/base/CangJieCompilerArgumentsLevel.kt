package org.cangnova.cangjie.arguments.dsl.base

import kotlin.properties.ReadOnlyProperty

data class CangJieCompilerArgumentsLevel(
    val name: String,
    val arguments: Set<CangJieCompilerArgument>,
    val nestedLevels: Set<CangJieCompilerArgumentsLevel>
) {
    internal fun mergeWith(another: CangJieCompilerArgumentsLevel): CangJieCompilerArgumentsLevel {
        require(name == another.name) {
            "Names for compiler arguments level should be the same! We are trying to merge $name with ${another.name}"
        }
        val argumentsWithTheSameNames = arguments.map { it.name }.intersect(another.arguments.map { it.name })
        require(argumentsWithTheSameNames.isEmpty()) {
            "Both levels with name $name contain compiler arguments with the same name(s): " +
                    argumentsWithTheSameNames.joinToString()
        }

        val intersectingNestedLevels = nestedLevels.filter { level -> another.nestedLevels.any { level.name == it.name } }.toSet()

        val mergedNestedLevels = nestedLevels.subtract(intersectingNestedLevels) +
                another.nestedLevels.filter { level -> intersectingNestedLevels.none { level.name == it.name } } +
                intersectingNestedLevels.map { level ->
                    level.mergeWith(another.nestedLevels.single { it.name == level.name })
                }
        return CangJieCompilerArgumentsLevel(
            name,
            (arguments + another.arguments).sortedBy { it.name }.toSet(),
            mergedNestedLevels
        )
    }
}

@CangJieArgumentsDslMarker
class KotlinCompilerArgumentsLevelBuilder(
    val name: String
) {
    private val arguments = mutableSetOf<CangJieCompilerArgument>()

    @OptIn(ExperimentalArgumentApi::class)
    fun compilerArgument(
        config: CangJieCompilerArgumentBuilder.() -> Unit
    ) {
        val argumentBuilder = CangJieCompilerArgumentBuilder()
        config(argumentBuilder)
        arguments.add(argumentBuilder.build())
    }

    fun addCompilerArguments(
        vararg compilerArguments: CangJieCompilerArgument
    ) {
        arguments.addAll(compilerArguments)
    }

    private val nestedLevels = mutableSetOf<CangJieCompilerArgumentsLevel>()

    fun subLevel(
        name: String,
        mergeWith: Set<CangJieCompilerArgumentsLevel> = emptySet(),
        config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        nestedLevels.add(
            mergeWith.fold(levelBuilder.build()) { current, mergingWith ->
                current.mergeWith(mergingWith)
            }
        )
    }

    fun build(): CangJieCompilerArgumentsLevel = CangJieCompilerArgumentsLevel(
        name,
        arguments,
        nestedLevels
    )
}

fun compilerArgumentsLevel(
    name: String,
    config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
) = ReadOnlyProperty<Any?, CangJieCompilerArgumentsLevel> { _, _ ->
    val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
    config(levelBuilder)
    levelBuilder.build()
}
