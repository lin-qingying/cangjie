package org.cangnova.cangjie.arguments.dsl.base

import org.cangnova.cangjie.arguments.dsl.types.AllCangJieArgumentTypes

/**
 * 编译器参数 schema 的根模型。
 */
data class KotlinCompilerArguments(
    /**
     * 参数 schema 格式版本。
     */
    val schemaVersion: Int = 1,
    /**
     * 当前 schema 覆盖的仓颉发布版本集合。
     */
    val releases: Set<CangJieReleaseVersion> = CangJieReleaseVersion.entries.toSet(),
    /**
     * 当前 schema 可引用的参数值类型注册对象。
     */
    val types: AllCangJieArgumentTypes = AllCangJieArgumentTypes(),
    /**
     * 参数层级树的根节点。
     */
    val topLevel: CangJieCompilerArgumentsLevel,
)

/**
 * 构造编译器参数 schema 根模型的 DSL builder。
 */
@CangJieArgumentsDslMarker
class KotlinCompilerArgumentsBuilder {
    /**
     * DSL 中配置完成的根参数层级。
     */
    private lateinit var topLevel: CangJieCompilerArgumentsLevel

    /**
     * 配置根参数层级，并可与已有同名层级合并。
     */
    fun topLevel(
        name: String,
        mergeWith: Set<CangJieCompilerArgumentsLevel> = emptySet(),
        config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        topLevel = mergeWith.fold(levelBuilder.build()) { init, level -> init.mergeWith(level) }
    }

    /**
     * 将 DSL builder 状态构造成不可变 schema 根模型。
     */
    fun build(): KotlinCompilerArguments = KotlinCompilerArguments(
        topLevel = topLevel
    )
}

/**
 * 编译器参数 schema 的顶层 DSL 入口。
 */
fun compilerArguments(
    config: KotlinCompilerArgumentsBuilder.() -> Unit,
): KotlinCompilerArguments {
    val kotlinArguments = KotlinCompilerArgumentsBuilder()
    config(kotlinArguments)
    return kotlinArguments.build()
}
