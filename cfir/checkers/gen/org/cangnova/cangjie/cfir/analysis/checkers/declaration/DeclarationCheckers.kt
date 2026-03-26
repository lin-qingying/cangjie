

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@Suppress("UNCHECKED_CAST")
abstract class DeclarationCheckers {
    companion object {
        val EMPTY: DeclarationCheckers = object : DeclarationCheckers() {}
    }

    open val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker> = emptySet()
    open val memberDeclarationCheckers: Set<CfirMemberDeclarationChecker> = emptySet()
    open val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker> = emptySet()
    open val functionCheckers: Set<CfirFunctionChecker> = emptySet()
    open val enumConstructorCheckers: Set<CfirEnumConstructorChecker> = emptySet()
    open val simpleFunctionCheckers: Set<CfirSimpleFunctionChecker> = emptySet()
    open val propertyCheckers: Set<CfirPropertyChecker> = emptySet()
    open val classLikeCheckers: Set<CfirClassLikeChecker> = emptySet()
    open val anonymousFunctionCheckers: Set<CfirAnonymousFunctionChecker> = emptySet()
    open val constructorCheckers: Set<CfirConstructorChecker> = emptySet()
    open val fileCheckers: Set<CfirFileChecker> = emptySet()
    open val typeParameterCheckers: Set<CfirTypeParameterChecker> = emptySet()
    open val extendCheckers: Set<CfirExtendChecker> = emptySet()
    open val mainFunctionCheckers: Set<CfirMainFunctionChecker> = emptySet()
    open val patternVariableCheckers: Set<CfirPatternVariableChecker> = emptySet()
    open val fieldVariableCheckers: Set<CfirFieldVariableChecker> = emptySet()
    open val typeAliasCheckers: Set<CfirTypeAliasChecker> = emptySet()
    open val valueParameterCheckers: Set<CfirValueParameterChecker> = emptySet()
    open val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicDeclarationCheckers: Array<CfirBasicDeclarationChecker> by lazy { basicDeclarationCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allMemberDeclarationCheckers: Array<CfirMemberDeclarationChecker> by lazy { (memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirMemberDeclarationChecker> }
    @CheckersComponentInternal internal val allCallableDeclarationCheckers: Array<CfirCallableDeclarationChecker> by lazy { (callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirCallableDeclarationChecker> }
    @CheckersComponentInternal internal val allFunctionCheckers: Array<CfirFunctionChecker> by lazy { (functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirFunctionChecker> }
    @CheckersComponentInternal internal val allEnumConstructorCheckers: Array<CfirEnumConstructorChecker> by lazy { (enumConstructorCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirEnumConstructorChecker> }
    @CheckersComponentInternal internal val allSimpleFunctionCheckers: Array<CfirSimpleFunctionChecker> by lazy { (simpleFunctionCheckers + functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirSimpleFunctionChecker> }
    @CheckersComponentInternal internal val allPropertyCheckers: Array<CfirPropertyChecker> by lazy { (propertyCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirPropertyChecker> }
    @CheckersComponentInternal internal val allClassLikeCheckers: Array<CfirClassLikeChecker> by lazy { (classLikeCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirClassLikeChecker> }
    @CheckersComponentInternal internal val allAnonymousFunctionCheckers: Array<CfirAnonymousFunctionChecker> by lazy { (anonymousFunctionCheckers + functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirAnonymousFunctionChecker> }
    @CheckersComponentInternal internal val allConstructorCheckers: Array<CfirConstructorChecker> by lazy { (constructorCheckers + functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirConstructorChecker> }
    @CheckersComponentInternal internal val allFileCheckers: Array<CfirFileChecker> by lazy { (fileCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirFileChecker> }
    @CheckersComponentInternal internal val allTypeParameterCheckers: Array<CfirTypeParameterChecker> by lazy { (typeParameterCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirTypeParameterChecker> }
    @CheckersComponentInternal internal val allExtendCheckers: Array<CfirExtendChecker> by lazy { (extendCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirExtendChecker> }
    @CheckersComponentInternal internal val allMainFunctionCheckers: Array<CfirMainFunctionChecker> by lazy { (mainFunctionCheckers + functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirMainFunctionChecker> }
    @CheckersComponentInternal internal val allPatternVariableCheckers: Array<CfirPatternVariableChecker> by lazy { (patternVariableCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirPatternVariableChecker> }
    @CheckersComponentInternal internal val allFieldVariableCheckers: Array<CfirFieldVariableChecker> by lazy { (fieldVariableCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirFieldVariableChecker> }
    @CheckersComponentInternal internal val allTypeAliasCheckers: Array<CfirTypeAliasChecker> by lazy { (typeAliasCheckers + classLikeCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirTypeAliasChecker> }
    @CheckersComponentInternal internal val allValueParameterCheckers: Array<CfirValueParameterChecker> by lazy { (valueParameterCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirValueParameterChecker> }
    @CheckersComponentInternal internal val allInvalidDeclarationCheckers: Array<CfirInvalidDeclarationChecker> by lazy { (invalidDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirInvalidDeclarationChecker> }
}
