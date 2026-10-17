package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry

/**
 * 变量常量求值结果在声明属性表中的键。
 */
private object EvaluatedValue : CfirDeclarationDataKey()

/** 已编译变量中是否保留了可读取的 initializer 表达式。 */
private object SerializedInitializerAvailability : CfirDeclarationDataKey()

/**
 * 变量初始化表达式的常量求值结果。
 */
var CfirVariable.evaluatedInitializer: CfirEvaluatorResult? by CfirDeclarationDataRegistry.data(EvaluatedValue)

/**
 * CJO 是否为变量声明保留了指向有效 initializer wire 节点的引用。
 *
 * CJO 可按导出策略省略 initializer 表达式，因此有效索引只证明二进制中存在表达式节点，不能证明
 * 源代码一定声明了 initializer。该属性描述的是二进制数据可用性，不替代源级 initializer 语义。
 */
var CfirVariable.hasSerializedInitializer: Boolean
    get() = serializedInitializerAvailability ?: (initializer != null)
    set(value) {
        serializedInitializerAvailability = value
    }

private var CfirVariable.serializedInitializerAvailability: Boolean? by
    CfirDeclarationDataRegistry.data(SerializedInitializerAvailability)
