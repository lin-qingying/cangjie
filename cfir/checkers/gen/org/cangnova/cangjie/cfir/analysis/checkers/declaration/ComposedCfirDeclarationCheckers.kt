

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedCfirDeclarationCheckers(val predicate: (CfirCheckerWithDispatchKind) -> Boolean) : CfirDeclarationCheckers() {
    constructor(dispatchKind: CheckerDispatchKind) : this({ it.dispatchKind == dispatchKind })

    override val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker>
        get() = _basicDeclarationCheckers
    override val memberDeclarationCheckers: Set<CfirMemberDeclarationChecker>
        get() = _memberDeclarationCheckers
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = _callableDeclarationCheckers
    override val classLikeCheckers: Set<CfirClassLikeChecker>
        get() = _classLikeCheckers
    override val classCheckers: Set<CfirClassChecker>
        get() = _classCheckers
    override val fileCheckers: Set<CfirFileChecker>
        get() = _fileCheckers
    override val functionCheckers: Set<CfirFunctionChecker>
        get() = _functionCheckers
    override val mainFunctionCheckers: Set<CfirMainFunctionChecker>
        get() = _mainFunctionCheckers
    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = _propertyCheckers
    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = _fieldVariableCheckers
    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = _patternVariableCheckers
    override val typeAliasCheckers: Set<CfirTypeAliasChecker>
        get() = _typeAliasCheckers
    override val typeParameterCheckers: Set<CfirTypeParameterChecker>
        get() = _typeParameterCheckers
    override val valueParameterCheckers: Set<CfirValueParameterChecker>
        get() = _valueParameterCheckers
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = _invalidDeclarationCheckers

    private val _basicDeclarationCheckers: MutableSet<CfirBasicDeclarationChecker> = mutableSetOf()
    private val _memberDeclarationCheckers: MutableSet<CfirMemberDeclarationChecker> = mutableSetOf()
    private val _callableDeclarationCheckers: MutableSet<CfirCallableDeclarationChecker> = mutableSetOf()
    private val _classLikeCheckers: MutableSet<CfirClassLikeChecker> = mutableSetOf()
    private val _classCheckers: MutableSet<CfirClassChecker> = mutableSetOf()
    private val _fileCheckers: MutableSet<CfirFileChecker> = mutableSetOf()
    private val _functionCheckers: MutableSet<CfirFunctionChecker> = mutableSetOf()
    private val _mainFunctionCheckers: MutableSet<CfirMainFunctionChecker> = mutableSetOf()
    private val _propertyCheckers: MutableSet<CfirPropertyChecker> = mutableSetOf()
    private val _fieldVariableCheckers: MutableSet<CfirFieldVariableChecker> = mutableSetOf()
    private val _patternVariableCheckers: MutableSet<CfirPatternVariableChecker> = mutableSetOf()
    private val _typeAliasCheckers: MutableSet<CfirTypeAliasChecker> = mutableSetOf()
    private val _typeParameterCheckers: MutableSet<CfirTypeParameterChecker> = mutableSetOf()
    private val _valueParameterCheckers: MutableSet<CfirValueParameterChecker> = mutableSetOf()
    private val _invalidDeclarationCheckers: MutableSet<CfirInvalidDeclarationChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: CfirDeclarationCheckers) {
        checkers.basicDeclarationCheckers.filterTo(_basicDeclarationCheckers, predicate)
        checkers.memberDeclarationCheckers.filterTo(_memberDeclarationCheckers, predicate)
        checkers.callableDeclarationCheckers.filterTo(_callableDeclarationCheckers, predicate)
        checkers.classLikeCheckers.filterTo(_classLikeCheckers, predicate)
        checkers.classCheckers.filterTo(_classCheckers, predicate)
        checkers.fileCheckers.filterTo(_fileCheckers, predicate)
        checkers.functionCheckers.filterTo(_functionCheckers, predicate)
        checkers.mainFunctionCheckers.filterTo(_mainFunctionCheckers, predicate)
        checkers.propertyCheckers.filterTo(_propertyCheckers, predicate)
        checkers.fieldVariableCheckers.filterTo(_fieldVariableCheckers, predicate)
        checkers.patternVariableCheckers.filterTo(_patternVariableCheckers, predicate)
        checkers.typeAliasCheckers.filterTo(_typeAliasCheckers, predicate)
        checkers.typeParameterCheckers.filterTo(_typeParameterCheckers, predicate)
        checkers.valueParameterCheckers.filterTo(_valueParameterCheckers, predicate)
        checkers.invalidDeclarationCheckers.filterTo(_invalidDeclarationCheckers, predicate)
    }
}
