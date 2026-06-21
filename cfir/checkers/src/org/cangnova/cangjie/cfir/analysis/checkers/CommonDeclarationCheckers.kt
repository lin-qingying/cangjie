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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.*

object CommonDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker>
        get() = setOf(
            CfirBuiltInAnnotationDeclarationChecker,
            CfirConflictsDeclarationChecker,
            CfirModifierChecker,
            CfirTypeConstraintsChecker,
            CfirUnusedExpressionChecker,
        )

    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = setOf(CfirPatternVariableInitializerTypeMismatchChecker)

    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = setOf(
            CfirConstVariableInitializerChecker,
            CfirVArrayExtraChecker,
            CfirDeprecatedDeclarationChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirJavaInteropTypePropagationChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCustomAnnotationPlaceChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnnotationArgNumberCallableChecker,
        )

    override val functionCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFunctionChecker>
        get() = setOf(
            CfirFunctionInitializationChecker,
            CfirForeignFunctionParameterTypeChecker,
            CfirForeignFunctionReturnTypeChecker,
            CfirFunctionReturnTypeInferenceChecker,
            CfirFinalizerDeclarationChecker,
            CfirConstFunctionBodyChecker,
        )

    override val typeParameterCheckers: Set<CfirTypeParameterChecker>
        get() = setOf(
            CfirTypeParameterBoundsChecker,
            CfirGenericDeepChecker,
        )

    override val simpleFunctionCheckers: Set<CfirSimpleFunctionChecker>
        get() = setOf(
            CfirOperatorDeclarationChecker,
            CfirFunctionDeclarationStatusChecker,
            CfirFunctionOverloadChecker,
            CfirDefaultParameterChecker,
        )

    override val fileCheckers: Set<CfirFileChecker>
        get() = setOf(
            CfirImportsChecker,
            CfirGeneralSemanticsChecker,
            CfirGenericInstantiationChecker,
            CfirCommonPackageMainChecker,
        )

    override val valueParameterCheckers: Set<CfirValueParameterChecker>
        get() = setOf(
            CfirValueParameterDefaultValueTypeMismatchChecker,
            CfirConstructorParameterThisOrSuperDefaultValueChecker,
        )

    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = setOf(
            CfirFieldVariableInitializerTypeMismatchChecker,
            CfirFieldVariableThisOrSuperInitializerChecker,
        )

    override val extendCheckers: Set<CfirExtendChecker>
        get() = setOf(
            CfirExtendTargetLegalityChecker,
            CfirExtendInterfaceKindChecker,
            CfirExtendDuplicateInterfaceChecker,
            CfirExtendOrphanRuleChecker,
            CfirExtendGenericUsageChecker,
            CfirExtendImmutableMutInterfaceChecker,
            CfirExtendImmutableMemberChecker,
            CfirExtendSpecializationConflictChecker,
            CfirExtendDefaultImplementationConflictChecker,
            CfirExtendInheritanceDeepChecker,
            CfirExtendExtraChecker,
            CfirConstExtendDeclarationChecker,
        )

    override val constructorCheckers: Set<CfirConstructorChecker>
        get() = setOf(
            CfirConstructorDelegationChecker,
            CfirConstructorInitializationChecker,
        )

    override val memberDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirMemberDeclarationChecker>
        get() = setOf(
            CfirMemberBodyDeclarationChecker,
        )

    override val typeAliasCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirTypeAliasChecker>
        get() = setOf(
            CfirTypeAliasCFuncLegalityChecker,
            CfirTypeAliasCycleChecker,
            CfirTypeAliasUnusedTypeParameterChecker,
        )

    override val classLikeCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirClassLikeChecker>
        get() = setOf(
            CfirAnnotationDeclarationChecker,
            CfirClassLikeInitializationChecker,
            CfirInteropAnnotationChecker,
            CfirSupertypesChecker,
            CfirOverrideChecker,
            CfirNotImplementedOverrideChecker,
            CfirClassStructSemanticsChecker,
            CfirOpenMemberChecker,
            CfirRecursiveConstructorCallChecker,
            CfirValueTypeRecursiveChecker,
            CfirConstDeclarationChecker,
            CfirInheritanceDeepChecker,
            CfirCommonSpecificChecker,
            CfirMockSemanticsChecker,
            CfirGenericJavaInteropChecker,
            CfirInheritanceThreadContextChecker,
            CfirCJMappingChecker,
            CfirObjCCJMappingChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCommonCtorImmutableAssignChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnnotationArgNumberClassChecker,
        )

    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = setOf(
            CfirPropertySemanticsChecker,
        )

    override val propertyAccessorCheckers: Set<CfirPropertyAccessorChecker>
        get() = setOf(
            CfirPropertyAccessorDeclarationChecker,
        )

    override val anonymousFunctionCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnonymousFunctionChecker>
        get() = setOf(
            CfirLambdaParameterTypeChecker,
        )
}
