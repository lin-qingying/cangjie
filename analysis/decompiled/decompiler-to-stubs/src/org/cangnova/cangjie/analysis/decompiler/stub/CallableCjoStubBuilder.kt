/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.psi.CjEnumConstructorTypeEntry
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjPropertyBody
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * `.cjo` callable stub 构建器。
 *
 * 这里只负责 callable / constructor / pattern variable 以及它们的附属 stub。
 */
internal fun createMainFunctionStub(
    parent: StubElement<*>,
    declaration: org.cangnova.cangjie.cfir.declarations.CfirMainFunction,
    context: CjoStubBuilderContext,
) {
    val fqName = context.packageFqName.firstSegment()?.let { firstSegment ->
        org.cangnova.cangjie.name.FqName(firstSegment.asString()).child(MAIN)
    } ?: context.packageFqName.child(MAIN)
    val functionStub = CangJieMainFunctionStubImpl(
        parent = parent,
        element = CjStubElementTypes.MAIN_FUNC,
        nameRef = StringRef.fromString(MAIN.asString()),
        fqName = fqName,
        origin = context.packageFacadeOrigin,
    )
    createEmptyDeclarationHeaderStubs(
        functionStub,
        createDeclarationModifierMask(declaration.status),
    )
    context.typeStubBuilder.createCallableParameterListStub(
        parent = functionStub,
        valueParameters = declaration.valueParameters,
        createEmptyList = true,
    )
    context.typeStubBuilder.createCallableReturnTypeReferenceStub(functionStub, declaration.returnTypeRef)
}

/**
 * 错误函数仍然需要参与 callable 级 stub 视图，
 * 否则包级名字索引会在出错情况下丢失声明轮廓。
 */
internal fun createErrorFunctionStub(
    parent: StubElement<*>,
    declaration: CfirErrorFunction,
    context: CjoStubBuilderContext,
) {
    val fallbackName = declaration.symbol.name
    val functionStub = CangJieNamedFunctionStubImpl(
        parent = parent,
        element = CjStubElementTypes.FUNCTION,
        nameRef = StringRef.fromString(fallbackName.asString()),
        isTopLevel = parent is CangJieFileStubImpl,
        fqName = callableFqName(parent, context, fallbackName),
        hasBlockBody = false,
        hasBody = false,
        hasTypeParameterListBeforeFunctionName = false,
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(functionStub)
}

/**
 * 错误 named-value 统一投影为 property 级 stub，
 * 保证索引层至少保留“名字 + 包归属 + 返回类型存在性”这组稳定轮廓。
 */
internal fun createErrorNamedValueStub(
    parent: StubElement<*>,
    declaration: CfirErrorNamedValue,
    context: CjoStubBuilderContext,
) {
    val propertyStub = CangJiePropertyStubImpl(
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        fqName = callableFqName(parent, context, declaration.name),
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(propertyStub)
}

/**
 * 为 CFIR finalizer 构建反编译 PSI stub。
 *
 * finalizer 只存在于类型成员上下文，参数列表固定为空，并以 compiled body 形式暴露给文本渲染器。
 */
internal fun createFinalizerStub(
    parent: StubElement<*>,
    declaration: CfirFinalizer,
    context: CjoStubBuilderContext,
) {
    val finalizerStub = CangJieFinalizerStubImpl(
        parent = parent,
        elementType = CjStubElementTypes.FINALIZER,
        containingClassName = StringRef.fromString(context.owningClassSimpleName ?: "finalizer"),
        hasBody = true,
    )
    createEmptyDeclarationHeaderStubs(finalizerStub)
    context.typeStubBuilder.createCallableParameterListStub(
        parent = finalizerStub,
        valueParameters = emptyList(),
        createEmptyList = true,
    )
}

/**
 * 为普通函数或特殊成员函数构建反编译 PSI stub。
 *
 * 成员名为 `~init` 或 `init` 时会投影为 finalizer/secondary constructor；
 * 其它函数保留名称、类型参数、值参数、返回类型和可见修饰符。
 */
internal fun createFunctionStub(
    parent: StubElement<*>,
    declaration: CfirNamedFunction,
    context: CjoStubBuilderContext,
) {
    if (context.owningClassSimpleName != null && declaration.name.asString() == "~init") {
        val finalizerStub = CangJieFinalizerStubImpl(
            parent = parent,
            elementType = CjStubElementTypes.FINALIZER,
            containingClassName = StringRef.fromString(context.owningClassSimpleName),
            hasBody = true,
        )
        createEmptyDeclarationHeaderStubs(finalizerStub)
        context.typeStubBuilder.createCallableParameterListStub(
            parent = finalizerStub,
            valueParameters = emptyList(),
            createEmptyList = true,
        )
        return
    }

    if (context.owningClassSimpleName != null && declaration.name.asString() == "init") {
        val constructorStub = CangJieConstructorStubImpl<CjSecondaryConstructor>(
            parent = parent,
            elementType = CjStubElementTypes.SECONDARY_CONSTRUCTOR,
            containingClassName = StringRef.fromString(context.owningClassSimpleName),
            hasBody = true,
            isPrimary = false,
        )
        createEmptyDeclarationHeaderStubs(constructorStub)
        context.typeStubBuilder.createCallableParameterListStub(
            parent = constructorStub,
            valueParameters = declaration.valueParameters,
            createEmptyList = true,
        )
        return
    }

    val hasBody = compiledCallableHasBody(declaration.status)
    val functionStub = CangJieNamedFunctionStubImpl(
        parent = parent,
        element = CjStubElementTypes.FUNCTION,
        nameRef = StringRef.fromString(declaration.name.asString()),
        isTopLevel = parent is CangJieFileStubImpl,
        fqName = callableFqName(parent, context, declaration.name),
        hasBlockBody = hasBody,
        hasBody = hasBody,
        hasTypeParameterListBeforeFunctionName = false,
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(
        functionStub,
        createDeclarationModifierMask(declaration.status, isOperator = declaration.status.isOperator),
    )
    createTypeParameterListStub(functionStub, declaration.typeParameters)
    context.typeStubBuilder.createCallableParameterListStub(functionStub, declaration.valueParameters, createEmptyList = true)
    context.typeStubBuilder.createCallableReturnTypeReferenceStub(functionStub, declaration.returnTypeRef)
}

/**
 * 为宏声明构建反编译 PSI stub。
 *
 * 宏与函数共享 callable 头部结构，但使用独立的 `MACRO` stub element type，
 * 以便索引和反编译文本保留仓颉宏声明形态。
 */
internal fun createMacroStub(
    parent: StubElement<*>,
    declaration: CfirMacroDeclaration,
    context: CjoStubBuilderContext,
) {
    val hasBody = compiledCallableHasBody(declaration.status)
    val macroStub = CangJieMacroStubImpl(
        parent = parent,
        element = CjStubElementTypes.MACRO,
        nameRef = StringRef.fromString(declaration.name.asString()),
        isTopLevel = parent is CangJieFileStubImpl,
        fqName = callableFqName(parent, context, declaration.name),
        hasBlockBody = hasBody,
        hasBody = hasBody,
        hasTypeParameterListBeforeFunctionName = false,
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(
        macroStub,
        createDeclarationModifierMask(declaration.status),
    )
    createTypeParameterListStub(macroStub, declaration.typeParameters)
    context.typeStubBuilder.createCallableParameterListStub(macroStub, declaration.valueParameters, createEmptyList = true)
    context.typeStubBuilder.createCallableReturnTypeReferenceStub(macroStub, declaration.returnTypeRef)
}

/**
 * 为属性声明构建反编译 PSI stub。
 *
 * 属性 stub 会记录名称、全限定名、声明修饰符、声明类型以及必要的 getter/setter body stub。
 */
internal fun createPropertyStub(
    parent: StubElement<*>,
    declaration: CfirProperty,
    context: CjoStubBuilderContext,
) {
    val propertyStub = CangJiePropertyStubImpl(
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        fqName = callableFqName(parent, context, declaration.name),
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(
        propertyStub,
        createDeclarationModifierMask(declaration.status),
    )
    context.typeStubBuilder.createDeclaredTypeReferenceStub(propertyStub, declaration.returnTypeRef)
    createPropertyBodyStub(propertyStub, declaration, context)
}

/**
 * 为字段变量构建反编译 PSI stub。
 *
 * 顶层字段在仓颉 PSI 中投影为 variable/binding pattern；类型成员字段则投影为 field stub，
 * 两条路径都保留 const/mut、initializer 与类型引用信息。
 */
internal fun createFieldStub(
    parent: StubElement<*>,
    declaration: CfirFieldVariable,
    context: CjoStubBuilderContext,
) {
    if (parent is CangJieFileStubImpl) {
        val variableStub = CangJieVariableStubImpl(
            parent = parent,
            patternKind = PatternKind.BINDING,
            isVar = declaration.isVar,
            isConst = declaration.status.isConst,
            isTopLevel = true,
            hasInitializer = declaration.initializer != null,
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
            origin = context.packageFacadeOrigin,
        )
        createEmptyDeclarationHeaderStubs(variableStub)
        val bindingPatternStub = CangJieBindingPatternStubImpl(
            parent = variableStub,
            nameRef = StringRef.fromString(declaration.name.asString()),
            fqName = context.packageFqName.child(declaration.name),
        )
        CangJieNameReferenceExpressionStubImpl(bindingPatternStub, StringRef.fromString(declaration.name.asString()))
        context.typeStubBuilder.createDeclaredTypeReferenceStub(variableStub, declaration.returnTypeRef)
        return
    }

    val fieldStub = CangJieFieldStubImpl(
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        fqName = callableFqName(parent, context, declaration.name),
        isVar = declaration.isVar,
        isConst = declaration.status.isConst,
        hasInitializer = declaration.initializer != null,
        hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(fieldStub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(fieldStub, declaration.returnTypeRef)
}

/**
 * 为模式变量构建反编译 PSI stub。
 *
 * 该入口保留变量层面的 mut/const/initializer/类型信息，并递归构造具体 pattern stub。
 */
internal fun createPatternVariableStub(
    parent: StubElement<*>,
    declaration: CfirPatternVariable,
    context: CjoStubBuilderContext,
) {
    val variableStub = CangJieVariableStubImpl(
        parent = parent,
        patternKind = declaration.pattern.toPatternKind(),
        isVar = declaration.isVar,
        isConst = declaration.status.isConst,
        isTopLevel = parent is CangJieFileStubImpl,
        hasInitializer = declaration.initializer != null,
        hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
        origin = context.packageFacadeOrigin.takeIf { parent is CangJieFileStubImpl },
    )
    createEmptyDeclarationHeaderStubs(variableStub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(variableStub, declaration.returnTypeRef)
    createPatternStub(declaration.pattern, variableStub)
}

/**
 * 为构造函数构建 primary 或 secondary constructor stub。
 *
 * 反序列化出的构造函数通过 CFIR 实现类名区分 primary/secondary，并使用上下文中的所属类型短名
 * 作为 constructor stub 的 containing class name。
 */
internal fun createConstructorStub(
    parent: StubElement<*>,
    declaration: CfirConstructor,
    context: CjoStubBuilderContext,
) {
    val containingClassSimpleName = context.owningClassSimpleName ?: declaration.symbol.callableId.callableName.asString()
    val isPrimary = declaration.javaClass.simpleName.contains("Primary", ignoreCase = true)
    if (isPrimary) {
        val constructorStub = CangJieConstructorStubImpl<CjPrimaryConstructor>(
            parent = parent,
            elementType = CjStubElementTypes.PRIMARY_CONSTRUCTOR,
            containingClassName = StringRef.fromString(containingClassSimpleName),
            hasBody = true,
            isPrimary = true,
        )
        createEmptyDeclarationHeaderStubs(constructorStub)
        context.typeStubBuilder.createCallableParameterListStub(
            parent = constructorStub,
            valueParameters = declaration.valueParameters,
            createEmptyList = true,
            includeParameterModifierList = true,
        )
    } else {
        val constructorStub = CangJieConstructorStubImpl<CjSecondaryConstructor>(
            parent = parent,
            elementType = CjStubElementTypes.SECONDARY_CONSTRUCTOR,
            containingClassName = StringRef.fromString(containingClassSimpleName),
            hasBody = true,
            isPrimary = false,
        )
        createEmptyDeclarationHeaderStubs(constructorStub)
        context.typeStubBuilder.createCallableParameterListStub(
            parent = constructorStub,
            valueParameters = declaration.valueParameters,
            createEmptyList = true,
        )
    }
}

/**
 * 为枚举构造项构建反编译 PSI stub。
 *
 * 枚举构造项参数在 stub 层以类型列表保存，后续文本渲染会把这些类型渲染为 enum entry 参数。
 */
internal fun createEnumConstructorStub(
    parent: StubElement<*>,
    declaration: CfirEnumConstructor,
    context: CjoStubBuilderContext,
) {
    val enumConstructorStub = CangJieEnumConstructorStubImpl(
        type = CjStubElementTypes.ENUM_CONSTRUCTOR,
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        typeCount = declaration.valueParameters.size,
        enumFqName = StringRef.fromString(context.owningClassFqName?.asString()),
    )
    if (declaration.valueParameters.isNotEmpty()) {
        val typeListStub = CangJiePlaceHolderStubImpl<CjEnumConstructorTypeEntry>(
            enumConstructorStub,
            CjStubElementTypes.TYPE_LIST,
        )
        declaration.valueParameters.forEach { valueParameter ->
            context.typeStubBuilder.createDeclaredTypeReferenceStub(typeListStub, valueParameter.returnTypeRef)
        }
    }
}

/**
 * 递归构建 CFIR pattern 对应的 PSI pattern stub。
 *
 * 该函数覆盖绑定、元组、枚举、通配、类型、常量和或模式等 compiled pattern 形态；
 * 表达式模式在当前 stub 视图中没有独立结构，因此保持为空操作。
 */
private fun createPatternStub(
    pattern: CfirPattern,
    parent: StubElement<*>,
) {
    when (pattern) {
        is CfirBindingPattern -> {
            val bindingStub = CangJieBindingPatternStubImpl(
                parent = parent,
                nameRef = StringRef.fromString(pattern.name.asString()),
                fqName = pattern.bindingVariable
                    ?.takeIf { !it.isLocal }
                    ?.symbol
                    ?.callableId
                    ?.packageName
                    ?.child(pattern.name),
            )
            CangJieNameReferenceExpressionStubImpl(bindingStub, StringRef.fromString(pattern.name.asString()))
            pattern.nestedPattern?.let { nestedPattern ->
                createPatternStub(nestedPattern, bindingStub)
            }
        }

        is CfirTuplePattern -> {
            val tupleStub = CangJieTuplePatternStubImpl(parent)
            pattern.elements.forEach { element ->
                createPatternStub(element, tupleStub)
            }
        }

        is CfirEnumPattern -> {
            val enumStub = CangJieEnumPatternStubImpl(parent)
            pattern.arguments.forEach { argument ->
                createPatternStub(argument, enumStub)
            }
        }

        is CfirWildcardPattern -> CangJieWildcardPatternStubImpl(parent)

        is CfirTypePattern -> {
            CangJieTypePatternStubImpl(
                parent = parent,
                name = StringRef.fromString(
                    pattern.bindingName?.asString() ?: pattern.bindingVariable?.name?.asString(),
                ),
            )
        }

        is CfirVarOrEnumPattern -> {
            CangJieVarOrEnumPatternStubImpl(
                parent = parent,
                nameRef = StringRef.fromString(pattern.name.asString()),
            )
        }

        is CfirConstPattern -> CangJieConstantPatternStubImpl(parent)
        is CfirExpressionPattern -> Unit
        is CfirOrPattern -> pattern.alternatives.forEach { alternative -> createPatternStub(alternative, parent) }
    }
}

/**
 * 为可带实现的 compiled property 创建 getter/setter body stub。
 *
 * abstract/foreign 属性不产生 body；mut 属性额外创建 setter accessor。
 */
private fun createPropertyBodyStub(
    propertyStub: CangJiePropertyStubImpl,
    declaration: CfirProperty,
    context: CjoStubBuilderContext,
) {
    if (!compiledPropertyHasBody(declaration.status)) return

    val propertyBodyStub = CangJiePlaceHolderStubImpl<CjPropertyBody>(
        propertyStub,
        CjStubElementTypes.PROPERTY_BODY,
    )
    createPropertyAccessorStub(
        parent = propertyBodyStub,
        isGetter = true,
        context = context,
    )
    if (declaration.status.isMut) {
        createPropertyAccessorStub(
            parent = propertyBodyStub,
            isGetter = false,
            context = context,
        )
    }
}

/**
 * 创建单个 property accessor stub。
 *
 * getter 没有参数列表；setter 会补一个未带类型的默认 `value` 参数，
 * 与反编译文本中的 `set(value)` 输出保持一致。
 */
private fun createPropertyAccessorStub(
    parent: StubElement<*>,
    isGetter: Boolean,
    context: CjoStubBuilderContext,
) {
    val accessorStub = CangJiePropertyAccessorStubImpl(
        parent = parent,
        isGetter = isGetter,
        hasBody = true,
        hasBlockBody = true,
    )
    if (isGetter) {
        return
    } else {
        context.typeStubBuilder.createSimpleParameterListStub(
            parent = accessorStub,
            parameterNames = listOf(PROPERTY_SETTER_PARAMETER_NAME),
            includeAnnotations = false,
        )
    }
}

/**
 * 判断 compiled property 是否应该在反编译文本中显示 body 占位。
 */
private fun compiledPropertyHasBody(status: CfirDeclarationStatus): Boolean {
    return !status.isAbstract && !status.isForeign
}

/**
 * 反编译 setter accessor 默认参数名。
 */
private const val PROPERTY_SETTER_PARAMETER_NAME = "value"

/**
 * 将 CFIR pattern 类型映射为 PSI variable stub 使用的 pattern kind。
 */
private fun CfirPattern.toPatternKind(): PatternKind = when (this) {
    is CfirBindingPattern -> PatternKind.BINDING
    is CfirTuplePattern -> PatternKind.TUPLE
    is CfirEnumPattern -> PatternKind.ENUM
    is CfirWildcardPattern -> PatternKind.WILDCARD
    is CfirTypePattern -> PatternKind.BINDING
    is CfirVarOrEnumPattern -> PatternKind.BINDING
    is CfirConstPattern -> PatternKind.BINDING
    is CfirExpressionPattern -> PatternKind.BINDING
    is CfirOrPattern -> PatternKind.BINDING
}

/**
 * 计算 callable 声明在反编译 stub 中应记录的全限定名。
 *
 * 顶层声明和 extend body 声明需要可索引的 FqName；普通成员声明由所属类型结构承载 owner，
 * 因此返回 `null`。
 */
private fun callableFqName(
    parent: StubElement<*>,
    context: CjoStubBuilderContext,
    name: org.cangnova.cangjie.name.Name,
): org.cangnova.cangjie.name.FqName? {
    return if (parent is CangJieFileStubImpl || context.isExtendBody) {
        composeQualifiedName(context.packageFqName, context.owningClassFqName, name)
    } else {
        null
    }
}

/**
 * 判断 compiled callable 是否应该在反编译文本中显示 body 占位。
 */
private fun compiledCallableHasBody(status: CfirDeclarationStatus): Boolean {
    return !status.isAbstract && !status.isForeign
}
