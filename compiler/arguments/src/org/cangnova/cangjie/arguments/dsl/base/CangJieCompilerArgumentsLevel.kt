package org.cangnova.cangjie.arguments.dsl.base

import kotlin.properties.ReadOnlyProperty

/**
 * 编译器参数 schema 中的一个层级节点。
 */
data class CangJieCompilerArgumentsLevel(
    /**
     * 当前参数层级的稳定名称。
     */
    val name: String,
    /**
     * 当前层级直接包含的编译器参数集合。
     */
    val arguments: Set<CangJieCompilerArgument>,
    /**
     * 当前层级下的嵌套参数层级集合。
     */
    val nestedLevels: Set<CangJieCompilerArgumentsLevel>
) {
    /**
     * 合并两个同名层级，并递归合并同名子层级。
     */
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

/**
 * 构造单个参数层级及其子层级的 DSL builder。
 */
@CangJieArgumentsDslMarker
class KotlinCompilerArgumentsLevelBuilder(
    /**
     * 当前 builder 负责构造的参数层级名称。
     */
    val name: String
) {
    /**
     * 当前层级直接收集到的参数集合。
     */
    private val arguments = mutableSetOf<CangJieCompilerArgument>()

    /**
     * 在当前层级内声明一个新的编译器参数。
     */
    @OptIn(ExperimentalArgumentApi::class)
    fun compilerArgument(
        config: CangJieCompilerArgumentBuilder.() -> Unit
    ) {
        val argumentBuilder = CangJieCompilerArgumentBuilder()
        config(argumentBuilder)
        arguments.add(argumentBuilder.build())
    }

    /**
     * 向当前层级批量追加已经构造好的参数定义。
     */
    fun addCompilerArguments(
        vararg compilerArguments: CangJieCompilerArgument
    ) {
        arguments.addAll(compilerArguments)
    }

    /**
     * 当前层级下收集到的嵌套层级集合。
     */
    private val nestedLevels = mutableSetOf<CangJieCompilerArgumentsLevel>()

    /**
     * 在当前层级下声明一个子层级，并可与已有同名子层级合并。
     */
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

    /**
     * 将当前 builder 状态构造成不可变参数层级。
     */
    fun build(): CangJieCompilerArgumentsLevel = CangJieCompilerArgumentsLevel(
        name,
        arguments,
        nestedLevels
    )
}

/**
 * 创建可作为 DSL 委托属性使用的参数层级定义。
 */
fun compilerArgumentsLevel(
    name: String,
    config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
) = ReadOnlyProperty<Any?, CangJieCompilerArgumentsLevel> { _, _ ->
    val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
    config(levelBuilder)
    levelBuilder.build()
}
