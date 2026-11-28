

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedDeclarationCheckers(val predicate: (CfirCheckerWithMppKind) -> Boolean) : DeclarationCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker>
        get() = _basicDeclarationCheckers
    override val memberDeclarationCheckers: Set<CfirMemberDeclarationChecker>
        get() = _memberDeclarationCheckers
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = _callableDeclarationCheckers
    override val functionCheckers: Set<CfirFunctionChecker>
        get() = _functionCheckers
    override val enumConstructorCheckers: Set<CfirEnumConstructorChecker>
        get() = _enumConstructorCheckers
    override val simpleFunctionCheckers: Set<CfirSimpleFunctionChecker>
        get() = _simpleFunctionCheckers
    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = _propertyCheckers
    override val classLikeCheckers: Set<CfirClassLikeChecker>
        get() = _classLikeCheckers
    override val anonymousFunctionCheckers: Set<CfirAnonymousFunctionChecker>
        get() = _anonymousFunctionCheckers
    override val propertyAccessorCheckers: Set<CfirPropertyAccessorChecker>
        get() = _propertyAccessorCheckers
    override val constructorCheckers: Set<CfirConstructorChecker>
        get() = _constructorCheckers
    override val fileCheckers: Set<CfirFileChecker>
        get() = _fileCheckers
    override val typeParameterCheckers: Set<CfirTypeParameterChecker>
        get() = _typeParameterCheckers
    override val extendCheckers: Set<CfirExtendChecker>
        get() = _extendCheckers
    override val mainFunctionCheckers: Set<CfirMainFunctionChecker>
        get() = _mainFunctionCheckers
    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = _patternVariableCheckers
    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = _fieldVariableCheckers
    override val typeAliasCheckers: Set<CfirTypeAliasChecker>
        get() = _typeAliasCheckers
    override val valueParameterCheckers: Set<CfirValueParameterChecker>
        get() = _valueParameterCheckers
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = _invalidDeclarationCheckers

    private val _basicDeclarationCheckers: MutableSet<CfirBasicDeclarationChecker> = mutableSetOf()
    private val _memberDeclarationCheckers: MutableSet<CfirMemberDeclarationChecker> = mutableSetOf()
    private val _callableDeclarationCheckers: MutableSet<CfirCallableDeclarationChecker> = mutableSetOf()
    private val _functionCheckers: MutableSet<CfirFunctionChecker> = mutableSetOf()
    private val _enumConstructorCheckers: MutableSet<CfirEnumConstructorChecker> = mutableSetOf()
    private val _simpleFunctionCheckers: MutableSet<CfirSimpleFunctionChecker> = mutableSetOf()
    private val _propertyCheckers: MutableSet<CfirPropertyChecker> = mutableSetOf()
    private val _classLikeCheckers: MutableSet<CfirClassLikeChecker> = mutableSetOf()
    private val _anonymousFunctionCheckers: MutableSet<CfirAnonymousFunctionChecker> = mutableSetOf()
    private val _propertyAccessorCheckers: MutableSet<CfirPropertyAccessorChecker> = mutableSetOf()
    private val _constructorCheckers: MutableSet<CfirConstructorChecker> = mutableSetOf()
    private val _fileCheckers: MutableSet<CfirFileChecker> = mutableSetOf()
    private val _typeParameterCheckers: MutableSet<CfirTypeParameterChecker> = mutableSetOf()
    private val _extendCheckers: MutableSet<CfirExtendChecker> = mutableSetOf()
    private val _mainFunctionCheckers: MutableSet<CfirMainFunctionChecker> = mutableSetOf()
    private val _patternVariableCheckers: MutableSet<CfirPatternVariableChecker> = mutableSetOf()
    private val _fieldVariableCheckers: MutableSet<CfirFieldVariableChecker> = mutableSetOf()
    private val _typeAliasCheckers: MutableSet<CfirTypeAliasChecker> = mutableSetOf()
    private val _valueParameterCheckers: MutableSet<CfirValueParameterChecker> = mutableSetOf()
    private val _invalidDeclarationCheckers: MutableSet<CfirInvalidDeclarationChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: DeclarationCheckers) {
        checkers.basicDeclarationCheckers.filterTo(_basicDeclarationCheckers, predicate)
        checkers.memberDeclarationCheckers.filterTo(_memberDeclarationCheckers, predicate)
        checkers.callableDeclarationCheckers.filterTo(_callableDeclarationCheckers, predicate)
        checkers.functionCheckers.filterTo(_functionCheckers, predicate)
        checkers.enumConstructorCheckers.filterTo(_enumConstructorCheckers, predicate)
        checkers.simpleFunctionCheckers.filterTo(_simpleFunctionCheckers, predicate)
        checkers.propertyCheckers.filterTo(_propertyCheckers, predicate)
        checkers.classLikeCheckers.filterTo(_classLikeCheckers, predicate)
        checkers.anonymousFunctionCheckers.filterTo(_anonymousFunctionCheckers, predicate)
        checkers.propertyAccessorCheckers.filterTo(_propertyAccessorCheckers, predicate)
        checkers.constructorCheckers.filterTo(_constructorCheckers, predicate)
        checkers.fileCheckers.filterTo(_fileCheckers, predicate)
        checkers.typeParameterCheckers.filterTo(_typeParameterCheckers, predicate)
        checkers.extendCheckers.filterTo(_extendCheckers, predicate)
        checkers.mainFunctionCheckers.filterTo(_mainFunctionCheckers, predicate)
        checkers.patternVariableCheckers.filterTo(_patternVariableCheckers, predicate)
        checkers.fieldVariableCheckers.filterTo(_fieldVariableCheckers, predicate)
        checkers.typeAliasCheckers.filterTo(_typeAliasCheckers, predicate)
        checkers.valueParameterCheckers.filterTo(_valueParameterCheckers, predicate)
        checkers.invalidDeclarationCheckers.filterTo(_invalidDeclarationCheckers, predicate)
    }
}
