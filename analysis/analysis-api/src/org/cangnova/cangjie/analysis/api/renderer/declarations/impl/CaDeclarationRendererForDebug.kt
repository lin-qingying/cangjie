package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForDebug

/**
 * 面向 debug 输出的声明渲染预设集合。
 *
 * - 在 [CaDeclarationRendererForSource] 各预设基础上, 把:
 *   - 修饰符切换为"包含隐式修饰符"模式(便于排查),
 *   - 类型切换为 debug 风格(显示更多内部信息);
 * - 命名约定与 source 预设一一对应, 仅修饰符/类型/注解差异。
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationRendererForDebug`。
 */
object CaDeclarationRendererForDebug {
    /** 在已有 renderer 上启用 debug 风格的全限定名类型/注解, 内部复用。 */
    private fun CaDeclarationRenderer.withDebugQualifiedNames(): CaDeclarationRenderer = with {
        modifiersRenderer = CaDeclarationModifiersRendererForSource.WITH_IMPLICIT_MODIFIERS
        typeRenderer = CaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
    }

    /** 在已有 renderer 上启用 debug 风格的短名类型/注解, 内部复用。 */
    private fun CaDeclarationRenderer.withDebugShortNames(): CaDeclarationRenderer = with {
        typeRenderer = CaTypeRendererForDebug.WITH_SHORT_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    /** debug 基线: 全限定名 + 显示隐式修饰符。 */
    val WITH_QUALIFIED_NAMES: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        modifiersRenderer = CaDeclarationModifiersRendererForSource.WITH_IMPLICIT_MODIFIERS
        typeRenderer = CaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
    }

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS`。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES`。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_BODY`。 */
    val WITH_QUALIFIED_NAMES_WITH_BODY: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_BODY.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY`。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES`。 */
    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_INITIALIZERS`。 */
    val WITH_QUALIFIED_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES`。 */
    val WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS`。 */
    val WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS`。 */
    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_RAW_SIGNATURES`。 */
    val WITH_QUALIFIED_NAMES_RAW_SIGNATURES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES`。 */
    val WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withDebugQualifiedNames()

    /** debug 版的 `WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS`。 */
    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withDebugQualifiedNames()

    /** debug 短名版本系列, 与 `WITH_QUALIFIED_NAMES_*` 一一对应。 */
    val WITH_SHORT_NAMES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.withDebugShortNames()

    /** debug 短名版本: 输出成员声明。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_MEMBERS.withDebugShortNames()

    /** debug 短名版本: 输出成员声明或在没有成员时输出空 `{}`。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withDebugShortNames()

    /** debug 短名版本: 输出可调用声明的完整函数体。 */
    val WITH_SHORT_NAMES_WITH_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_BODY.withDebugShortNames()

    /** debug 短名版本: 同时输出成员声明与各自的函数体。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withDebugShortNames()

    /** debug 短名版本: 函数体以占位符代替, 仅保留签名形态。 */
    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withDebugShortNames()

    /** debug 短名版本: 输出变量初始化器。 */
    val WITH_SHORT_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withDebugShortNames()

    /** debug 短名版本: 输出形参默认值。 */
    val WITH_SHORT_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withDebugShortNames()

    /** debug 短名版本: 默认值/函数体/初始化器等细节全部展开。 */
    val WITH_SHORT_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withDebugShortNames()

    /** debug 短名版本: 细节以占位符代替, 仅保留位置信息。 */
    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withDebugShortNames()

    /** debug 短名版本: 输出 raw 签名 (尽量贴近内部表示)。 */
    val WITH_SHORT_NAMES_RAW_SIGNATURES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withDebugShortNames()

    /** debug 短名版本: 省略超类型列表。 */
    val WITH_SHORT_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withDebugShortNames()

    /** debug 短名版本: 省略类型形参列表。 */
    val WITH_SHORT_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withDebugShortNames()
}
