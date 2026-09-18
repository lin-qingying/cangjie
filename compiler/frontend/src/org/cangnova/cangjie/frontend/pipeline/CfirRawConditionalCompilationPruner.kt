package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.resolve.conditional.CfirConditionalCompilationEvaluator
import org.cangnova.cangjie.cfir.resolve.conditional.CfirConditionalCompilationResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.withCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.withSurfaces
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationFailure
import org.cangnova.cangjie.cfir.session.CfirRawBuiltinAnnotationNames
import org.cangnova.cangjie.cfir.session.conditionalCompilationSettingsOrNull

/**
 * Parse 后、宏分类前的 raw-CFIR `@When` 裁剪 owner。
 *
 * 当前 owner 先覆盖 file/import/top-level/member 声明；它只在 session 显式注入
 * 条件环境时运行，绝不从 JVM 或 target platform 合成条件值。非法条件不会被
 * 当作 false；失败事实写回 CfirFile，由 file checker 发布结构化诊断。
 */
public fun PreMacroRawBuildResult.pruneConditionalCompilation(): PreMacroRawBuildResult {
    val settings = session.conditionalCompilationSettingsOrNull ?: return this
    return mapFiles { preFile ->
        val file = preFile.cfirFile
        val context = ConditionalPruningContext(CfirConditionalCompilationEvaluator(settings))
        file.replaceImports(file.imports.filter { keepImport(it, context) })
        (file.declarations as MutableList<CfirDeclaration>).removeIf { declaration ->
            !keepDeclaration(declaration, context)
        }
        file.declarations.forEach { pruneNestedDeclarations(it, context) }
        file.replaceConditionalCompilationFailures(
            file.conditionalCompilationFailures + context.failures,
        )
        val reachable = collectReachableDeclarations(file)
        preFile.withCfirFile(file).withSurfaces {
            val carrier = it.replaceHandle.carrier as? CfirDeclaration
            carrier == null || carrier in reachable
        }
    }
}

/**
 * 单个文件的条件编译裁剪上下文。
 *
 * 持有会话注入环境构建的 evaluator，并累积裁剪过程中收集的全部
 * 条件编译失败事实，供裁剪完成后统一写回 [CfirFile]。
 */
private class ConditionalPruningContext(
    /** 基于显式条件环境的 `@When` 求值器。 */
    val evaluator: CfirConditionalCompilationEvaluator,
) {
    /** 本次裁剪收集的失败事实；非法条件不视为 false，而是记录后同样移除。 */
    val failures: MutableList<CfirConditionalCompilationFailure> = mutableListOf()
}

/**
 * 判断一条 import 是否应保留。
 *
 * 无条件或条件求值为 Enabled 时保留；Disabled 时移除；
 * 非法条件记录失败事实后同样移除，诊断由 file checker 后续发布。
 */
private fun keepImport(
    import: org.cangnova.cangjie.cfir.declarations.CfirImport,
    context: ConditionalPruningContext,
): Boolean = when (val result = import.condition?.let(context.evaluator::evaluate)) {
    null, CfirConditionalCompilationResult.Enabled -> true
    CfirConditionalCompilationResult.Disabled -> false
    is CfirConditionalCompilationResult.Invalid -> {
        context.failures += result.failure(import.condition?.source ?: import.source)
        false
    }
}

/**
 * 判断一条声明是否应保留，保留时顺手摘除其 `@When` 注解。
 *
 * 注解已消费完毕，不摘除会让后续阶段把条件编译语法当作普通注解重复处理。
 */
private fun keepDeclaration(
    declaration: CfirDeclaration,
    context: ConditionalPruningContext,
): Boolean {
    val condition = declaration.whenAnnotation()?.conditionExpression()
    val keep = when (val result = condition?.let(context.evaluator::evaluate)) {
        null, CfirConditionalCompilationResult.Enabled -> true
        CfirConditionalCompilationResult.Disabled -> false
        is CfirConditionalCompilationResult.Invalid -> {
            context.failures += result.failure(condition.source ?: declaration.source)
            false
        }
    }
    if (keep) declaration.removeWhenAnnotation()
    return keep
}

/**
 * 递归裁剪声明内部的嵌套 `@When` 目标。
 *
 * 覆盖函数（形参 + 直接 body block）、类族与 extend 的成员声明；
 * 其余声明种类没有可裁剪的嵌套声明位置。
 */
private fun pruneNestedDeclarations(
    declaration: CfirDeclaration,
    context: ConditionalPruningContext,
) {
    when (declaration) {
        is CfirFunction -> {
            declaration.replaceValueParameters(
                declaration.valueParameters.filter { keepDeclaration(it, context) },
            )
            declaration.body?.let { pruneBlock(it, context) }
        }
        is CfirClassLikeDeclaration -> {
            (declaration.declarations as MutableList<CfirDeclaration>).removeIf { child ->
                !keepDeclaration(child, context)
            }
            declaration.declarations.forEach { pruneNestedDeclarations(it, context) }
        }
        is CfirExtend -> {
            (declaration.declarations as MutableList<CfirDeclaration>).removeIf { child ->
                !keepDeclaration(child, context)
            }
            declaration.declarations.forEach { pruneNestedDeclarations(it, context) }
        }
        else -> Unit
    }
}

/** 裁剪函数直接 block 中的 local declaration；嵌套 block 由后续表达式 owner 继续递归。 */
private fun pruneBlock(block: CfirBlock, context: ConditionalPruningContext) {
    block.replaceStatements(
        block.statements.filter { statement ->
            val declaration = statement as? CfirDeclaration ?: return@filter true
            keepDeclaration(declaration, context)
        },
    )
    block.statements.forEach { statement ->
        val nested = statement as? CfirBlock ?: return@forEach
        pruneBlock(nested, context)
    }
}

/** 提取声明上未被 `@!` 强制的 `@When` 注解调用；无则返回 `null`。 */
private fun CfirDeclaration.whenAnnotation(): CfirAnnotationCall? =
    annotations.asSequence()
        .filterIsInstance<CfirAnnotationCall>()
        .firstOrNull { !it.forcedCustom && it.annotationSourceName == CfirRawBuiltinAnnotationNames.WHEN }

/** 读取 `@When` 注解的唯一条件表达式；实参个数不为 1 时返回 `null`。 */
private fun CfirAnnotationCall.conditionExpression(): org.cangnova.cangjie.cfir.expressions.CfirExpression? =
    argumentList.arguments.singleOrNull()

/** 从声明注解列表中移除已消费的 `@When` 调用（保留 `@!` 强制形式与其他注解）。 */
private fun CfirDeclaration.removeWhenAnnotation() {
    replaceAnnotations(
        annotations.filterNot { annotation ->
            val call = annotation as? CfirAnnotationCall
            call != null && !call.forcedCustom && call.annotationSourceName == CfirRawBuiltinAnnotationNames.WHEN
        },
    )
}

/**
 * 收集裁剪后文件中仍然可达的全部声明（含嵌套成员）。
 *
 * raw builder 按声明句柄构造成员 surface；surface 的可达性由该集合判定，
 * 避免为已被裁剪的声明保留悬空句柄。
 */
private fun collectReachableDeclarations(file: CfirFile): Set<CfirDeclaration> {
    val result = linkedSetOf<CfirDeclaration>()
    fun visit(declaration: CfirDeclaration) {
        if (!result.add(declaration)) return
        when (declaration) {
            is CfirClassLikeDeclaration -> declaration.declarations.forEach(::visit)
            is CfirExtend -> declaration.declarations.forEach(::visit)
            else -> Unit
        }
    }
    file.declarations.forEach(::visit)
    return result
}

/** 把 Invalid 求值结果转换为结构化失败事实，定位信息缺省时回退到声明 source。 */
private fun CfirConditionalCompilationResult.Invalid.failure(
    source: org.cangnova.cangjie.source.CjSourceElement?,
): CfirConditionalCompilationFailure = CfirConditionalCompilationFailure(
    reason = reason,
    source = source,
    conditionName = conditionName,
    rightValue = rightValue,
    operator = operator,
)
