package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement

/**
 * 面向抽象 source element 的诊断定位策略集合。
 *
 * 每个策略把 LightTree 和 PSI 的定位实现组合起来，使诊断工厂不需要关心源元素来源。
 */
object SourceElementPositioningStrategies {
    /**
     * 默认 offset-only 定位策略。
     */
    val DEFAULT: AbstractSourceElementPositioningStrategy = OffsetsOnlyPositioningStrategy()
    /**
     * 标记声明的实际名称标识符。
     */
    val ACTUAL_DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ACTUAL_DECLARATION_NAME,
        PositioningStrategies.ACTUAL_DECLARATION_NAME
    )
    /**
     * 标记声明起始关键字到名称标识符的范围。
     */
    val DECLARATION_START_TO_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_START_TO_NAME,
        PositioningStrategies.DECLARATION_START_TO_NAME
    )
    /**
     * 标记可调用声明签名中不含修饰符的主体范围。
     */
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS,
        PositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS
    )
    /**
     * 标记声明上的可见性修饰符。
     */
    val VISIBILITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VISIBILITY_MODIFIER,
        PositioningStrategies.VISIBILITY_MODIFIER,
    )
    /**
     * 标记 override 或 redef 修饰符。
     */
    val OVERRIDE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OVERRIDE_MODIFIER,
        PositioningStrategies.OVERRIDE_MODIFIER,
    )
    /**
     * 标记 mut 修饰符。
     */
    val MUT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.MUT_MODIFIER,
        PositioningStrategies.MUT_MODIFIER,
    )
    /**
     * 标记 throw 表达式中的 throw 关键字。
     */
    val THROW_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.THROW_KEYWORD,
        PositioningStrategies.THROW_KEYWORD,
    )
    /**
     * 标记 for-in 表达式中的 for 关键字。
     */
    val FOR_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FOR_KEYWORD,
        PositioningStrategies.FOR_KEYWORD,
    )
    /**
     * 标记数组字面量左中括号。
     */
    val ARRAY_LITERAL_LEFT_BRACKET = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ARRAY_LITERAL_LEFT_BRACKET,
        PositioningStrategies.ARRAY_LITERAL_LEFT_BRACKET,
    )
    /**
     * 变量初始化器使用默认范围定位。
     */
    val VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = DEFAULT
    /**
     * 模式变量初始化器标记等号 token。
     */
    val PATTERN_VARIABLE_INITIALIZER: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INITIALIZER_EQ,
        PositioningStrategies.INITIALIZER_EQ
    )
    /**
     * 标记 import 路径最后一个被引用名称。
     */
    val IMPORT_LAST_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_LAST_NAME,
        PositioningStrategies.IMPORT_LAST_NAME
    )
    /**
     * 标记 import alias 的别名标识符。
     */
    val IMPORT_ALIAS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_ALIAS,
        PositioningStrategies.IMPORT_ALIAS
    )
    /**
     * 标记表达式中的操作符引用。
     */
    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PositioningStrategies.OPERATOR
    )
    /**
     * 标记具名实参的参数名。
     */
    val NAME_OF_NAMED_ARGUMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_OF_NAMED_ARGUMENT,
        PositioningStrategies.NAME_OF_NAMED_ARGUMENT
    )
    /**
     * 标记调用表达式中的实参范围。
     */
    val VALUE_ARGUMENTS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS,
        PositioningStrategies.VALUE_ARGUMENTS
    )
    /**
     * 标记调用表达式的实参列表节点。
     */
    val VALUE_ARGUMENTS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS_LIST,
        PositioningStrategies.VALUE_ARGUMENTS_LIST
    )
    /**
     * 标记限定表达式中最终被引用的名称。
     */
    val REFERENCED_NAME_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
        PositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

}
