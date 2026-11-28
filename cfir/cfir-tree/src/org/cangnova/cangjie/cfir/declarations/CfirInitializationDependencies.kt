package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies

private object InitializationDependenciesKey : CfirDeclarationDataKey()
private var CfirDeclaration.initializationDependenciesAttribute: CjmpInitializationDependencies<CfirBasedSymbol<*>>?
    by CfirDeclarationDataRegistry.data(InitializationDependenciesKey)

/** 初始化检查与 binary reference completion 独立发布的原始依赖，不属于 CJMP 阶段根属性。 */
val CfirDeclaration.initializationDependencies: CjmpInitializationDependencies<CfirBasedSymbol<*>>
    get() = initializationDependenciesAttribute ?: CjmpInitializationDependencies.NotCollected

/** 只有本次依赖收集成功完成后才发布；集合中的符号保持真实声明身份。 */
fun CfirDeclaration.publishInitializationDependencies(declarations: Collection<CfirBasedSymbol<*>>) {
    val collected = CjmpInitializationDependencies.Collected(declarations)
    val previous = initializationDependenciesAttribute
    check(previous == null || previous == collected) {
        "Initialization dependencies may only be published once for the same declaration state"
    }
    initializationDependenciesAttribute = collected
}

/**
 * 收集 initializer 中解析到的真实非 static 实例字段目标。
 *
 * 解析引用已经确定声明身份，因此这里不按名称查找、不要求 this receiver、同一物理 owner 或同一包；
 * common CJO 是否保留某条边由序列化索引投影决定。默认 CFIR visitor 继续进入匿名函数体，
 * 结果按首次遍历顺序去重。
 */
fun CfirExpression.collectInstanceInitializationDependencySymbols(
): List<CfirBasedSymbol<*>> {
    check(this !is CfirLazyExpression) { "Initialization dependencies require a resolved initializer." }
    val dependencies = linkedSetOf<CfirBasedSymbol<*>>()
    accept(object : CfirDefaultVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            element.acceptChildren(this, null)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: CfirResolvedNamedReference) {
            val symbol = resolvedNamedReference.resolvedSymbol as? CfirFieldVariableSymbol ?: return
            if (symbol.callableId.classId != null && !symbol.cfir.status.isStatic) {
                dependencies += symbol
            }
        }
    }, null)
    return dependencies.toList()
}

/** 在字段自己的独立生命周期中收集并一次发布；已从 CJO 恢复的事实保持原样。 */
fun CfirFieldVariable.collectAndPublishInitializationDependencies(): CjmpInitializationDependencies<CfirBasedSymbol<*>> {
    if (status.isStatic) return CjmpInitializationDependencies.NotCollected
    initializationDependencies.let { current ->
        if (current !is CjmpInitializationDependencies.NotCollected) return current
    }
    val initializer = initializer ?: return CjmpInitializationDependencies.NotCollected
    if (initializer is CfirLazyExpression) return CjmpInitializationDependencies.NotCollected
    publishInitializationDependencies(
        initializer.collectInstanceInitializationDependencySymbols(),
    )
    return initializationDependencies
}
