

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedDeclarationCheckers : DeclarationCheckers() {
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
        _basicDeclarationCheckers.addAll(checkers.basicDeclarationCheckers)
        _memberDeclarationCheckers.addAll(checkers.memberDeclarationCheckers)
        _callableDeclarationCheckers.addAll(checkers.callableDeclarationCheckers)
        _functionCheckers.addAll(checkers.functionCheckers)
        _enumConstructorCheckers.addAll(checkers.enumConstructorCheckers)
        _simpleFunctionCheckers.addAll(checkers.simpleFunctionCheckers)
        _propertyCheckers.addAll(checkers.propertyCheckers)
        _classLikeCheckers.addAll(checkers.classLikeCheckers)
        _anonymousFunctionCheckers.addAll(checkers.anonymousFunctionCheckers)
        _constructorCheckers.addAll(checkers.constructorCheckers)
        _fileCheckers.addAll(checkers.fileCheckers)
        _typeParameterCheckers.addAll(checkers.typeParameterCheckers)
        _extendCheckers.addAll(checkers.extendCheckers)
        _mainFunctionCheckers.addAll(checkers.mainFunctionCheckers)
        _patternVariableCheckers.addAll(checkers.patternVariableCheckers)
        _fieldVariableCheckers.addAll(checkers.fieldVariableCheckers)
        _typeAliasCheckers.addAll(checkers.typeAliasCheckers)
        _valueParameterCheckers.addAll(checkers.valueParameterCheckers)
        _invalidDeclarationCheckers.addAll(checkers.invalidDeclarationCheckers)
    }
}
