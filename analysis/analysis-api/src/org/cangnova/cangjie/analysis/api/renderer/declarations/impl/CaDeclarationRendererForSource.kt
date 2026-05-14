package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReturnTypeFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaRecommendedRendererCodeStyle
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaClassifierBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaFunctionLikeBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaParameterDefaultValueRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeSorter
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaVariableInitializerRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableParameterRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReceiverRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReturnTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableSignatureRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaEnumConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFieldSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFunctionLikeKeywordRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaLocalVariableSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaNamedFunctionSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertyGetterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertySymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertySetterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaValueParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaClassLikeSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaExtendSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeAliasSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeListRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypesFilter
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource

/**
 * 面向源码展示的声明渲染预设集合。
 *
 * - 以 `WITH_QUALIFIED_NAMES` 为基线: 修饰符、类型、注解、超类型等均按"贴近仓颉源码"风格输出;
 * - 通过 `_WITH_MEMBERS` / `_WITH_BODY` / `_WITH_INITIALIZERS` 等后缀启用更多细节;
 * - 通过 `WITH_SHORT_NAMES_*` 系列将类型与注解切换为短名版本;
 * - 通过 [CaDeclarationRenderer.with] 派生自定义配置。
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationRendererForSource`。
 */
object CaDeclarationRendererForSource {
    /** 在已有 renderer 上切换为短名类型/注解, 内部复用。 */
    private fun CaDeclarationRenderer.withShortNames(): CaDeclarationRenderer = with {
        typeRenderer = CaTypeRendererForSource.WITH_SHORT_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    /** 基线预设: 类型、注解、超类型使用全限定名, 不输出函数体/初始化器/默认值。 */
    @OptIn(CaExperimentalApi::class)
    val WITH_QUALIFIED_NAMES: CaDeclarationRenderer = CaDeclarationRenderer {
        nameRenderer = CaDeclarationNameRenderer.QUOTED
        keywordsRenderer = CaKeywordsRenderer.AS_WORD
        codeStyle = CaRecommendedRendererCodeStyle
        typeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        modifiersRenderer = CaDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
        declarationTypeApproximator = CaRendererTypeApproximator.NO_APPROXIMATION
        classifierBodyRenderer = CaClassifierBodyRenderer.NO_BODY

        superTypeRenderer = CaSuperTypeRendererForSource.WITH_OUT_APPROXIMATION
        superTypeListRenderer = CaSuperTypeListRendererForSource.AS_LIST
        superTypesFilter = CaSuperTypesFilter.ALL
        bodyMemberScopeProvider = CaRendererBodyMemberScopeProvider.ALL_DECLARED
        bodyMemberScopeSorter = CaRendererBodyMemberScopeSorter.ENUM_CONSTRUCTORS_AT_BEGINNING
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.NO_DEFAULT_VALUE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
        returnTypeRenderer = CaCallableReturnTypeRenderer.WITH_OUT_APPROXIMATION
        callableReceiverRenderer = CaCallableReceiverRendererForSource.AS_TYPE_WITH_IN_APPROXIMATION
        valueParametersRenderer = CaCallableParameterRenderer.PARAMETERS_IN_PARENS
        typeParametersRenderer = CaTypeParametersRendererForSource.WITH_BOUNDS_IN_WHERE_CLAUSE
        typeParametersFilter = CaTypeParametersFilter.ALL
        callableSignatureRenderer = CaCallableSignatureRenderer.FOR_SOURCE
        returnTypeFilter = CaCallableReturnTypeFilter.NO_UNIT_FOR_FUNCTIONS
        functionLikeKeywordRenderer = CaFunctionLikeKeywordRenderer.AS_SOURCE
        classLikeRenderer = CaClassLikeSymbolRenderer.AS_SOURCE
        typeAliasRenderer = CaTypeAliasSymbolRenderer.AS_SOURCE
        extendRenderer = CaExtendSymbolRenderer.AS_SOURCE
        namedFunctionRenderer = CaNamedFunctionSymbolRenderer.AS_SOURCE
        constructorRenderer = CaConstructorSymbolRenderer.AS_SOURCE
        propertyRenderer = CaPropertySymbolRenderer.AS_SOURCE
        getterRenderer = CaPropertyGetterSymbolRenderer.AS_SOURCE
        setterRenderer = CaPropertySetterSymbolRenderer.AS_SOURCE
        fieldRenderer = CaFieldSymbolRenderer.AS_SOURCE
        localVariableRenderer = CaLocalVariableSymbolRenderer.AS_SOURCE
        enumConstructorRenderer = CaEnumConstructorSymbolRenderer.AS_SOURCE
        valueParameterRenderer = CaValueParameterSymbolRenderer.AS_SOURCE
        typeParameterRenderer = CaTypeParameterSymbolRenderer.AS_SOURCE
    }

    /** 在基线基础上输出 classifier 主体成员(成员为空则省略主体)。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS
    }

    /** 与 `_WITH_MEMBERS` 类似, 但即便成员为空也保留 `{ }`。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS_OR_EMPTY_BRACES
    }

    /** 渲染函数/属性访问器的真实源码 body。 */
    val WITH_QUALIFIED_NAMES_WITH_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    /** 同时渲染类成员和函数/访问器 body。 */
    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    /** 用 `{ ... }` / `= ...` 占位形式渲染 body, 便于 hover 等场景。 */
    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_PLACEHOLDER
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_PLACEHOLDER
        setterRenderer = CaPropertySetterSymbolRenderer.WITH_PARAMETER_PLACEHOLDER
    }

    /** 渲染变量初始化器源码文本。 */
    val WITH_QUALIFIED_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.AS_SOURCE
    }

    /** 渲染函数参数默认值源码文本。 */
    val WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.AS_SOURCE
    }

    /** 一次性启用 body / 初始化器 / 默认值等"全细节"渲染。 */
    val WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.AS_SOURCE
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.AS_SOURCE
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    /** 与 `_WITH_ALL_DETAILS` 相同范围, 但所有具体值替换为 `...` 占位。 */
    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.AS_PLACEHOLDER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.THREE_DOTS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_PLACEHOLDER
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_DEFAULT
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_PLACEHOLDER
        setterRenderer = CaPropertySetterSymbolRenderer.WITH_PARAMETER_PLACEHOLDER
    }

    /** 仅输出"原始签名": 函数/构造器/属性使用 raw signature 形态, 适合 mangling 等场景。 */
    val WITH_QUALIFIED_NAMES_RAW_SIGNATURES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        namedFunctionRenderer = CaNamedFunctionSymbolRenderer.AS_RAW_SIGNATURE
        constructorRenderer = CaConstructorSymbolRenderer.AS_RAW_SIGNATURE
        propertyRenderer = CaPropertySymbolRenderer.AS_RAW_SIGNATURE
    }

    /** 在基线之上隐藏所有超类型, 便于 IDE 简化展示。 */
    val WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        superTypesFilter = CaSuperTypesFilter.NONE
    }

    /** 隐藏所有类型形参列表(`<T>` / `where` 部分)。 */
    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        typeParametersFilter = CaTypeParametersFilter.NONE
    }

    /** 短名版本的 `WITH_QUALIFIED_NAMES`。 */
    val WITH_SHORT_NAMES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS`。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_MEMBERS.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES`。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_BODY`。 */
    val WITH_SHORT_NAMES_WITH_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_BODY.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY`。 */
    val WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES`。 */
    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_INITIALIZERS`。 */
    val WITH_SHORT_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES`。 */
    val WITH_SHORT_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS`。 */
    val WITH_SHORT_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS`。 */
    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_RAW_SIGNATURES`。 */
    val WITH_SHORT_NAMES_RAW_SIGNATURES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES`。 */
    val WITH_SHORT_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withShortNames()

    /** 短名版本的 `WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS`。 */
    val WITH_SHORT_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withShortNames()
}
