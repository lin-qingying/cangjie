/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.declaration

import org.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangjie.cfir.analysis.checkers.MppCheckerKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class ComposedCfirDeclarationCheckers(val predicate: (CfirCheckerWithMppKind) -> Boolean) : CfirDeclarationCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

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
    override val variableCheckers: Set<CfirVariableChecker>
        get() = _variableCheckers
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
    private val _variableCheckers: MutableSet<CfirVariableChecker> = mutableSetOf()
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
        checkers.variableCheckers.filterTo(_variableCheckers, predicate)
        checkers.typeAliasCheckers.filterTo(_typeAliasCheckers, predicate)
        checkers.typeParameterCheckers.filterTo(_typeParameterCheckers, predicate)
        checkers.valueParameterCheckers.filterTo(_valueParameterCheckers, predicate)
        checkers.invalidDeclarationCheckers.filterTo(_invalidDeclarationCheckers, predicate)
    }
}
