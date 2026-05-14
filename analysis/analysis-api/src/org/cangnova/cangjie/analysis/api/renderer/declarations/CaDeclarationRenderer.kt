package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaClassifierBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaFunctionLikeBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaParameterDefaultValueRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeSorter
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaVariableInitializerRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableParameterRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReceiverRenderer
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
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeAliasSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeListRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypesFilter
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.name

/**
 * 声明渲染器配置入口。
 *
 * - 控制声明(类、函数、属性、字段等)被序列化为字符串时的整体策略;
 * - 通过子组件(`modifiersRenderer`、`bodyRenderer`、`typeRenderer` 等)实现细粒度定制,
 *   每种声明 kind 对应一个专属子 renderer;
 * - 通过 [CaDeclarationRendererForSource] / [CaDeclarationRendererForDebug] 等 object 预设直接复用常用配置;
 * - 通过 [Builder] / [with] 在已有 preset 基础上派生新的配置。
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationRenderer`。
 */
class CaDeclarationRenderer private constructor(
    val nameRenderer: CaDeclarationNameRenderer,
    val keywordsRenderer: CaKeywordsRenderer,
    val codeStyle: CaRendererCodeStyle,
    val typeRenderer: CaTypeRenderer,
    val annotationRenderer: CaAnnotationRenderer,
    val modifiersRenderer: CaDeclarationModifiersRenderer,
    val declarationTypeApproximator: CaRendererTypeApproximator,
    val classifierBodyRenderer: CaClassifierBodyRenderer,
    val superTypeRenderer: CaSuperTypeRenderer,
    val superTypeListRenderer: CaSuperTypeListRenderer,
    val superTypesFilter: CaSuperTypesFilter,
    val bodyMemberScopeProvider: CaRendererBodyMemberScopeProvider,
    val bodyMemberScopeSorter: CaRendererBodyMemberScopeSorter,
    val functionLikeBodyRenderer: CaFunctionLikeBodyRenderer,
    val variableInitializerRenderer: CaVariableInitializerRenderer,
    val parameterDefaultValueRenderer: CaParameterDefaultValueRenderer,
    val propertyAccessorsRenderer: CaPropertyAccessorsRenderer,
    val accessorBodyRenderer: CaPropertyAccessorBodyRenderer,
    val returnTypeRenderer: CaCallableReturnTypeRenderer,
    val callableReceiverRenderer: CaCallableReceiverRenderer,
    val valueParametersRenderer: CaCallableParameterRenderer,
    val typeParametersRenderer: CaTypeParametersRenderer,
    val typeParametersFilter: CaTypeParametersFilter,
    val callableSignatureRenderer: CaCallableSignatureRenderer,
    val returnTypeFilter: CaCallableReturnTypeFilter,
    val functionLikeKeywordRenderer: CaFunctionLikeKeywordRenderer,
    val classLikeRenderer: CaClassLikeSymbolRenderer,
    val typeAliasRenderer: CaTypeAliasSymbolRenderer,
    val extendRenderer: CaExtendSymbolRenderer,
    val namedFunctionRenderer: CaNamedFunctionSymbolRenderer,
    val constructorRenderer: CaConstructorSymbolRenderer,
    val propertyRenderer: CaPropertySymbolRenderer,
    val getterRenderer: CaPropertyGetterSymbolRenderer,
    val setterRenderer: CaPropertySetterSymbolRenderer,
    val fieldRenderer: CaFieldSymbolRenderer,
    val localVariableRenderer: CaLocalVariableSymbolRenderer,
    val enumConstructorRenderer: CaEnumConstructorSymbolRenderer,
    val valueParameterRenderer: CaValueParameterSymbolRenderer,
    val typeParameterRenderer: CaTypeParameterSymbolRenderer,
) {
    /** 将声明 [symbol] 渲染为字符串。 */
    fun renderDeclaration(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
    ): String = prettyPrint {
        renderDeclaration(analysisSession, symbol, this)
    }

    /**
     * 将声明 [symbol] 渲染到给定的 [printer]。
     *
     * 实际渲染按 symbol 的具体 kind 分发到对应的子 renderer。
     */
    fun renderDeclaration(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
        printer: PrettyPrinter,
    ) {
        when (symbol) {
            is CaClassSymbol -> classLikeRenderer.renderSymbol(
                analysisSession,
                symbol,
                symbol.classKind.keyword(),
                this,
                printer,
            )

            is CaTypeAliasSymbol -> typeAliasRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaExtendSymbol -> extendRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaNamedFunctionSymbol -> namedFunctionRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaConstructorSymbol -> constructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFinalizerSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "finalizer", this, printer)
            is CaAnonymousFunctionSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "func", this, printer)
            is CaEnumConstructorSymbol -> enumConstructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertySymbol -> propertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertyGetterSymbol -> getterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertySetterSymbol -> setterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFieldSymbol -> fieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaValueParameterSymbol -> valueParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaVariableSymbol -> localVariableRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaTypeParameterSymbol -> typeParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFunctionSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "func", this, printer)
            else -> {
                modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer)
                symbol.name?.let { name ->
                    nameRenderer.renderName(analysisSession, name, null, this, printer)
                } ?: printer.append("<unnamed>")
            }
        }
    }

    /**
     * 基于当前 renderer 派生一个新配置。
     *
     * 未在 [action] 中显式覆盖的字段沿用当前实例的设置, 便于在 preset 基础上局部定制。
     */
    fun with(action: Builder.() -> Unit): CaDeclarationRenderer {
        val current = this
        return Builder().apply {
            nameRenderer = current.nameRenderer
            keywordsRenderer = current.keywordsRenderer
            codeStyle = current.codeStyle
            typeRenderer = current.typeRenderer
            annotationRenderer = current.annotationRenderer
            modifiersRenderer = current.modifiersRenderer
            declarationTypeApproximator = current.declarationTypeApproximator
            classifierBodyRenderer = current.classifierBodyRenderer
            superTypeRenderer = current.superTypeRenderer
            superTypeListRenderer = current.superTypeListRenderer
            superTypesFilter = current.superTypesFilter
            bodyMemberScopeProvider = current.bodyMemberScopeProvider
            bodyMemberScopeSorter = current.bodyMemberScopeSorter
            functionLikeBodyRenderer = current.functionLikeBodyRenderer
            variableInitializerRenderer = current.variableInitializerRenderer
            parameterDefaultValueRenderer = current.parameterDefaultValueRenderer
            propertyAccessorsRenderer = current.propertyAccessorsRenderer
            accessorBodyRenderer = current.accessorBodyRenderer
            returnTypeRenderer = current.returnTypeRenderer
            callableReceiverRenderer = current.callableReceiverRenderer
            valueParametersRenderer = current.valueParametersRenderer
            typeParametersRenderer = current.typeParametersRenderer
            typeParametersFilter = current.typeParametersFilter
            callableSignatureRenderer = current.callableSignatureRenderer
            returnTypeFilter = current.returnTypeFilter
            functionLikeKeywordRenderer = current.functionLikeKeywordRenderer
            classLikeRenderer = current.classLikeRenderer
            typeAliasRenderer = current.typeAliasRenderer
            extendRenderer = current.extendRenderer
            namedFunctionRenderer = current.namedFunctionRenderer
            constructorRenderer = current.constructorRenderer
            propertyRenderer = current.propertyRenderer
            getterRenderer = current.getterRenderer
            setterRenderer = current.setterRenderer
            fieldRenderer = current.fieldRenderer
            localVariableRenderer = current.localVariableRenderer
            enumConstructorRenderer = current.enumConstructorRenderer
            valueParameterRenderer = current.valueParameterRenderer
            typeParameterRenderer = current.typeParameterRenderer
            action()
        }.build()
    }

    /**
     * 声明 renderer 构建器, 以 DSL 方式装配各种子组件。
     *
     * 所有字段必须在调用 [build] 前赋值, 否则会在使用时抛出 lateinit 异常。
     */
    class Builder {
        /** 声明名称渲染策略。 */
        lateinit var nameRenderer: CaDeclarationNameRenderer
        /** 关键字渲染策略(含过滤)。 */
        lateinit var keywordsRenderer: CaKeywordsRenderer
        /** 代码风格(缩进、分隔符等)。 */
        lateinit var codeStyle: CaRendererCodeStyle
        /** 类型渲染策略。 */
        lateinit var typeRenderer: CaTypeRenderer
        /** 注解渲染策略。 */
        lateinit var annotationRenderer: CaAnnotationRenderer
        /** 声明修饰符渲染策略。 */
        lateinit var modifiersRenderer: CaDeclarationModifiersRenderer
        /** 类型近似化策略(用于隐藏内部类型)。 */
        lateinit var declarationTypeApproximator: CaRendererTypeApproximator
        /** 类/接口/struct/enum 主体(成员)渲染策略。 */
        lateinit var classifierBodyRenderer: CaClassifierBodyRenderer
        /** 单个超类型渲染策略。 */
        lateinit var superTypeRenderer: CaSuperTypeRenderer
        /** 超类型列表(继承/实现部分)渲染策略。 */
        lateinit var superTypeListRenderer: CaSuperTypeListRenderer
        /** 超类型过滤策略(决定哪些需要写出)。 */
        lateinit var superTypesFilter: CaSuperTypesFilter
        /** 成员作用域提供者(决定渲染哪些成员)。 */
        lateinit var bodyMemberScopeProvider: CaRendererBodyMemberScopeProvider
        /** 成员排序策略。 */
        lateinit var bodyMemberScopeSorter: CaRendererBodyMemberScopeSorter
        /** 函数体渲染策略。 */
        lateinit var functionLikeBodyRenderer: CaFunctionLikeBodyRenderer
        /** 变量初始化器渲染策略。 */
        lateinit var variableInitializerRenderer: CaVariableInitializerRenderer
        /** 函数参数默认值渲染策略。 */
        lateinit var parameterDefaultValueRenderer: CaParameterDefaultValueRenderer
        /** 属性 get/set 访问器整体渲染策略。 */
        lateinit var propertyAccessorsRenderer: CaPropertyAccessorsRenderer
        /** 单个访问器函数体渲染策略。 */
        lateinit var accessorBodyRenderer: CaPropertyAccessorBodyRenderer
        /** callable 返回类型渲染策略。 */
        lateinit var returnTypeRenderer: CaCallableReturnTypeRenderer
        /** callable 接收者(`this`/扩展前缀)渲染策略。 */
        lateinit var callableReceiverRenderer: CaCallableReceiverRenderer
        /** callable 形参列表渲染策略。 */
        lateinit var valueParametersRenderer: CaCallableParameterRenderer
        /** 类型形参列表渲染策略。 */
        lateinit var typeParametersRenderer: CaTypeParametersRenderer
        /** 类型形参过滤策略。 */
        lateinit var typeParametersFilter: CaTypeParametersFilter
        /** callable 整体签名渲染策略。 */
        lateinit var callableSignatureRenderer: CaCallableSignatureRenderer
        /** callable 返回类型是否输出的过滤策略。 */
        lateinit var returnTypeFilter: CaCallableReturnTypeFilter
        /** 函数关键字(func/init/finalizer 等)渲染策略。 */
        lateinit var functionLikeKeywordRenderer: CaFunctionLikeKeywordRenderer
        /** class-like(类/接口/struct/enum)符号渲染策略。 */
        lateinit var classLikeRenderer: CaClassLikeSymbolRenderer
        /** typealias 符号渲染策略。 */
        lateinit var typeAliasRenderer: CaTypeAliasSymbolRenderer
        /** extend 块符号渲染策略。 */
        lateinit var extendRenderer: CaExtendSymbolRenderer
        /** 顶层/成员函数符号渲染策略。 */
        lateinit var namedFunctionRenderer: CaNamedFunctionSymbolRenderer
        /** 构造器符号渲染策略。 */
        lateinit var constructorRenderer: CaConstructorSymbolRenderer
        /** 属性符号渲染策略。 */
        lateinit var propertyRenderer: CaPropertySymbolRenderer
        /** 属性 getter 渲染策略。 */
        lateinit var getterRenderer: CaPropertyGetterSymbolRenderer
        /** 属性 setter 渲染策略。 */
        lateinit var setterRenderer: CaPropertySetterSymbolRenderer
        /** 字段符号渲染策略。 */
        lateinit var fieldRenderer: CaFieldSymbolRenderer
        /** 局部变量符号渲染策略。 */
        lateinit var localVariableRenderer: CaLocalVariableSymbolRenderer
        /** enum 构造子符号渲染策略。 */
        lateinit var enumConstructorRenderer: CaEnumConstructorSymbolRenderer
        /** 形参符号渲染策略。 */
        lateinit var valueParameterRenderer: CaValueParameterSymbolRenderer
        /** 类型形参符号渲染策略。 */
        lateinit var typeParameterRenderer: CaTypeParameterSymbolRenderer

        /** 构建最终的声明渲染器。 */
        fun build(): CaDeclarationRenderer = CaDeclarationRenderer(
            nameRenderer = nameRenderer,
            keywordsRenderer = keywordsRenderer,
            codeStyle = codeStyle,
            typeRenderer = typeRenderer,
            annotationRenderer = annotationRenderer,
            modifiersRenderer = modifiersRenderer,
            declarationTypeApproximator = declarationTypeApproximator,
            classifierBodyRenderer = classifierBodyRenderer,
            superTypeRenderer = superTypeRenderer,
            superTypeListRenderer = superTypeListRenderer,
            superTypesFilter = superTypesFilter,
            bodyMemberScopeProvider = bodyMemberScopeProvider,
            bodyMemberScopeSorter = bodyMemberScopeSorter,
            functionLikeBodyRenderer = functionLikeBodyRenderer,
            variableInitializerRenderer = variableInitializerRenderer,
            parameterDefaultValueRenderer = parameterDefaultValueRenderer,
            propertyAccessorsRenderer = propertyAccessorsRenderer,
            accessorBodyRenderer = accessorBodyRenderer,
            returnTypeRenderer = returnTypeRenderer,
            callableReceiverRenderer = callableReceiverRenderer,
            valueParametersRenderer = valueParametersRenderer,
            typeParametersRenderer = typeParametersRenderer,
            typeParametersFilter = typeParametersFilter,
            callableSignatureRenderer = callableSignatureRenderer,
            returnTypeFilter = returnTypeFilter,
            functionLikeKeywordRenderer = functionLikeKeywordRenderer,
            classLikeRenderer = classLikeRenderer,
            typeAliasRenderer = typeAliasRenderer,
            extendRenderer = extendRenderer,
            namedFunctionRenderer = namedFunctionRenderer,
            constructorRenderer = constructorRenderer,
            propertyRenderer = propertyRenderer,
            getterRenderer = getterRenderer,
            setterRenderer = setterRenderer,
            fieldRenderer = fieldRenderer,
            localVariableRenderer = localVariableRenderer,
            enumConstructorRenderer = enumConstructorRenderer,
            valueParameterRenderer = valueParameterRenderer,
            typeParameterRenderer = typeParameterRenderer,
        )
    }

    companion object {
        /** DSL 入口, 等价于 `Builder().apply(action).build()`。 */
        operator fun invoke(action: Builder.() -> Unit): CaDeclarationRenderer =
            Builder().apply(action).build()
    }
}

/** 把 [CaClassKind] 映射为仓颉源码中的对应关键字。 */
private fun CaClassKind.keyword(): String = when (this) {
    CaClassKind.CLASS -> "class"
    CaClassKind.INTERFACE -> "interface"
    CaClassKind.STRUCT -> "struct"
    CaClassKind.ENUM -> "enum"
}
