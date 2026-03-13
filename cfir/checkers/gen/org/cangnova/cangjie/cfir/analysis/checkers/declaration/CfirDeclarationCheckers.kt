

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@Suppress("UNCHECKED_CAST")
abstract class CfirDeclarationCheckers {
    companion object {
        val EMPTY: CfirDeclarationCheckers = object : CfirDeclarationCheckers() {}
    }

    open val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker> = emptySet()
    open val memberDeclarationCheckers: Set<CfirMemberDeclarationChecker> = emptySet()
    open val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker> = emptySet()
    open val classLikeCheckers: Set<CfirClassLikeChecker> = emptySet()
    open val classCheckers: Set<CfirClassChecker> = emptySet()
    open val fileCheckers: Set<CfirFileChecker> = emptySet()
    open val functionCheckers: Set<CfirFunctionChecker> = emptySet()
    open val mainFunctionCheckers: Set<CfirMainFunctionChecker> = emptySet()
    open val propertyCheckers: Set<CfirPropertyChecker> = emptySet()
    open val variableCheckers: Set<CfirVariableChecker> = emptySet()
    open val typeAliasCheckers: Set<CfirTypeAliasChecker> = emptySet()
    open val typeParameterCheckers: Set<CfirTypeParameterChecker> = emptySet()
    open val valueParameterCheckers: Set<CfirValueParameterChecker> = emptySet()
    open val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker> = emptySet()

    @CheckersComponentInternal internal val allBasicDeclarationCheckers: Array<CfirBasicDeclarationChecker> by lazy { basicDeclarationCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allMemberDeclarationCheckers: Array<CfirMemberDeclarationChecker> by lazy { (memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirMemberDeclarationChecker> }
    @CheckersComponentInternal internal val allCallableDeclarationCheckers: Array<CfirCallableDeclarationChecker> by lazy { (callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirCallableDeclarationChecker> }
    @CheckersComponentInternal internal val allClassLikeCheckers: Array<CfirClassLikeChecker> by lazy { (classLikeCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirClassLikeChecker> }
    @CheckersComponentInternal internal val allClassCheckers: Array<CfirClassChecker> by lazy { (classCheckers + classLikeCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirClassChecker> }
    @CheckersComponentInternal internal val allFileCheckers: Array<CfirFileChecker> by lazy { (fileCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirFileChecker> }
    @CheckersComponentInternal internal val allFunctionCheckers: Array<CfirFunctionChecker> by lazy { (functionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirFunctionChecker> }
    @CheckersComponentInternal internal val allMainFunctionCheckers: Array<CfirMainFunctionChecker> by lazy { (mainFunctionCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirMainFunctionChecker> }
    @CheckersComponentInternal internal val allPropertyCheckers: Array<CfirPropertyChecker> by lazy { (propertyCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirPropertyChecker> }
    @CheckersComponentInternal internal val allVariableCheckers: Array<CfirVariableChecker> by lazy { (variableCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirVariableChecker> }
    @CheckersComponentInternal internal val allTypeAliasCheckers: Array<CfirTypeAliasChecker> by lazy { (typeAliasCheckers + classLikeCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirTypeAliasChecker> }
    @CheckersComponentInternal internal val allTypeParameterCheckers: Array<CfirTypeParameterChecker> by lazy { (typeParameterCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirTypeParameterChecker> }
    @CheckersComponentInternal internal val allValueParameterCheckers: Array<CfirValueParameterChecker> by lazy { (valueParameterCheckers + callableDeclarationCheckers + memberDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirValueParameterChecker> }
    @CheckersComponentInternal internal val allInvalidDeclarationCheckers: Array<CfirInvalidDeclarationChecker> by lazy { (invalidDeclarationCheckers + basicDeclarationCheckers).toTypedArray() as Array<CfirInvalidDeclarationChecker> }
}
