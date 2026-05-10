package org.cangnova.cangjie.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.psi.CjEnumConstructorTypeEntry
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieBindingPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstantPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumConstructorStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFieldStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFinalizerStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMacroStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieMainFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTuplePatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypePatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVarOrEnumPatternStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieVariableStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieWildcardPatternStubImpl

/**
 * `.cjo` callable stub 构建器。
 *
 * 这里只负责 callable / constructor / pattern variable 以及它们的附属 stub。
 */
internal fun createMainFunctionStub(
    parent: StubElement<*>,
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
        origin = null,
    )
    createEmptyDeclarationHeaderStubs(functionStub)
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
        origin = null,
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
    )
    createEmptyDeclarationHeaderStubs(propertyStub)
}

internal fun createFinalizerStub(
    parent: StubElement<*>,
    declaration: CfirFinalizer,
    context: CjoStubBuilderContext,
) {
    val finalizerStub = CangJieFinalizerStubImpl(
        parent = parent,
        elementType = CjStubElementTypes.FINALIZER,
        containingClassName = StringRef.fromString(context.owningClassSimpleName ?: "finalizer"),
        hasBody = declaration.body != null,
    )
    createEmptyDeclarationHeaderStubs(finalizerStub)
    context.typeStubBuilder.createCallableParameterListStub(
        parent = finalizerStub,
        valueParameters = emptyList(),
        createEmptyList = true,
    )
}

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
            hasBody = declaration.body != null,
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

    val functionStub = CangJieNamedFunctionStubImpl(
        parent = parent,
        element = CjStubElementTypes.FUNCTION,
        nameRef = StringRef.fromString(declaration.name.asString()),
        isTopLevel = parent is CangJieFileStubImpl,
        fqName = callableFqName(parent, context, declaration.name),
        hasBlockBody = declaration.body != null,
        hasBody = declaration.body != null,
        hasTypeParameterListBeforeFunctionName = false,
        origin = null,
    )
    createEmptyDeclarationHeaderStubs(functionStub, createCallableModifierMask(declaration.status.isOperator))
    context.typeStubBuilder.createCallableParameterListStub(functionStub, declaration.valueParameters)
    context.typeStubBuilder.createCallableReturnTypeReferenceStub(functionStub, declaration.returnTypeRef)
}

internal fun createMacroStub(
    parent: StubElement<*>,
    declaration: CfirMacroDeclaration,
    context: CjoStubBuilderContext,
) {
    val macroStub = CangJieMacroStubImpl(
        parent = parent,
        element = CjStubElementTypes.MACRO,
        nameRef = StringRef.fromString(declaration.name.asString()),
        isTopLevel = parent is CangJieFileStubImpl,
        fqName = callableFqName(parent, context, declaration.name),
        hasBlockBody = declaration.body != null,
        hasBody = declaration.body != null,
        hasTypeParameterListBeforeFunctionName = false,
        origin = null,
    )
    createEmptyDeclarationHeaderStubs(macroStub)
    context.typeStubBuilder.createCallableParameterListStub(macroStub, declaration.valueParameters)
    context.typeStubBuilder.createCallableReturnTypeReferenceStub(macroStub, declaration.returnTypeRef)
}

internal fun createPropertyStub(
    parent: StubElement<*>,
    declaration: CfirProperty,
    context: CjoStubBuilderContext,
) {
    val propertyStub = CangJiePropertyStubImpl(
        parent = parent,
        name = StringRef.fromString(declaration.name.asString()),
        fqName = callableFqName(parent, context, declaration.name),
    )
    createEmptyDeclarationHeaderStubs(propertyStub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(propertyStub, declaration.returnTypeRef)
}

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
            isTopLevel = true,
            hasInitializer = declaration.initializer != null,
            hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
            origin = null,
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
        origin = null,
    )
    createEmptyDeclarationHeaderStubs(fieldStub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(fieldStub, declaration.returnTypeRef)
}

internal fun createPatternVariableStub(
    parent: StubElement<*>,
    declaration: CfirPatternVariable,
    context: CjoStubBuilderContext,
) {
    val variableStub = CangJieVariableStubImpl(
        parent = parent,
        patternKind = declaration.pattern.toPatternKind(),
        isVar = declaration.isVar,
        isTopLevel = parent is CangJieFileStubImpl,
        hasInitializer = declaration.initializer != null,
        hasReturnTypeRef = declaration.returnTypeRef !is CfirImplicitTypeRef,
        origin = null,
    )
    createEmptyDeclarationHeaderStubs(variableStub)
    context.typeStubBuilder.createDeclaredTypeReferenceStub(variableStub, declaration.returnTypeRef)
    createPatternStub(declaration.pattern, variableStub)
}

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
