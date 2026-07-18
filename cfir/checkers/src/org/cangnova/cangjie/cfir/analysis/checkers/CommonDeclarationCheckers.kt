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

/** CFIR 默认声明 checker 注册表，按声明节点类别汇总主干语义检查器。 */
object CommonDeclarationCheckers : DeclarationCheckers() {
    /** 对所有声明节点都会执行的基础声明 checker 集合。 */
    override val basicDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker>
        get() = setOf(
            CfirBuiltInAnnotationDeclarationChecker,
            CfirConflictsDeclarationChecker,
            CfirModifierChecker,
            CfirTypeConstraintsChecker,
        )

    /** 对错误声明节点执行的 checker 集合；当前默认主干不注册额外错误声明 checker。 */
    override val invalidDeclarationCheckers: Set<CfirInvalidDeclarationChecker>
        get() = emptySet()

    /** 对模式变量声明执行的 checker 集合。 */
    override val patternVariableCheckers: Set<CfirPatternVariableChecker>
        get() = setOf(CfirPatternVariableInitializerTypeMismatchChecker)

    /** 对 callable 声明通用语义执行的 checker 集合。 */
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker>
        get() = setOf(
            CfirConstVariableInitializerChecker,
            CfirVariableLambdaInitializerTypeMismatchChecker,
            CfirVArrayExtraChecker,
            CfirDeprecatedDeclarationChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirJavaInteropTypePropagationChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCustomAnnotationPlaceChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnnotationArgNumberCallableChecker,
        )

    /** 对函数声明本体、返回类型和函数级限制执行的 checker 集合。 */
    override val functionCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFunctionChecker>
        get() = setOf(
            CfirFunctionInitializationChecker,
            CfirForeignFunctionParameterTypeChecker,
            CfirForeignFunctionReturnTypeChecker,
            CfirFunctionReturnTypeInferenceChecker,
            CfirFinalizerDeclarationChecker,
            CfirConstFunctionBodyChecker,
        )

    /** 对程序入口 `main` 的参数和返回类型签名执行官方入口约束检查。 */
    override val mainFunctionCheckers: Set<CfirMainFunctionChecker>
        get() = setOf(CfirMainFunctionSignatureChecker)

    /** 对类型参数及其边界执行的 checker 集合。 */
    override val typeParameterCheckers: Set<CfirTypeParameterChecker>
        get() = setOf(
            CfirGenericDeepChecker,
            CfirTypeParameterBoundsChecker,
        )

    /** 对普通命名函数声明的修饰符、重载和默认参数规则执行的 checker 集合。 */
    override val simpleFunctionCheckers: Set<CfirSimpleFunctionChecker>
        get() = setOf(
            CfirOperatorDeclarationChecker,
            CfirFunctionDeclarationStatusChecker,
            CfirFunctionOverloadChecker,
            CfirDefaultParameterChecker,
        )

    /** 对文件级结构、导入、泛型实例化和入口约束执行的 checker 集合。 */
    override val fileCheckers: Set<CfirFileChecker>
        get() = setOf(
            CfirImportsChecker,
            CfirGeneralSemanticsChecker,
            CfirGenericInstantiationChecker,
            CfirFileStaticGlobalInitializationChecker,
            CfirCommonPackageMainChecker,
        )

    /** 对值参数默认值和构造器参数限制执行的 checker 集合。 */
    override val valueParameterCheckers: Set<CfirValueParameterChecker>
        get() = setOf(
            CfirValueParameterDefaultValueTypeMismatchChecker,
            CfirConstructorParameterThisOrSuperDefaultValueChecker,
        )

    /** 对字段变量初始化和 `this`/`super` 使用规则执行的 checker 集合。 */
    override val fieldVariableCheckers: Set<CfirFieldVariableChecker>
        get() = setOf(
            CfirFieldVariableInitializerTypeMismatchChecker,
            CfirFieldVariableThisOrSuperInitializerChecker,
        )

    /** 对 `extend` 声明的目标合法性、继承关系、泛型和附加限制执行的 checker 集合。 */
    override val extendCheckers: Set<CfirExtendChecker>
        get() = setOf(
            CfirExtendTargetLegalityChecker,
            CfirExtendInterfaceKindChecker,
            CfirExtendDuplicateInterfaceChecker,
            CfirExtendCheckSequenceChecker,
            CfirExtendOrphanRuleChecker,
            CfirExtendGenericUsageChecker,
            CfirExtendImmutableMutInterfaceChecker,
            CfirExtendImmutableMemberChecker,
            CfirExtendSpecializationConflictChecker,
            CfirExtendInheritanceDeepChecker,
            CfirExtendExtraChecker,
            CfirConstExtendDeclarationChecker,
        )

    /** 对构造器委托和初始化规则执行的 checker 集合。 */
    override val constructorCheckers: Set<CfirConstructorChecker>
        get() = setOf(
            CfirConstructorDelegationChecker,
            CfirConstructorInitializationChecker,
        )

    /** 对成员声明的通用成员体规则执行的 checker 集合。 */
    override val memberDeclarationCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirMemberDeclarationChecker>
        get() = setOf(
            CfirMemberBodyDeclarationChecker,
        )

    /** 对类型别名循环、展开类型和类型参数使用执行的 checker 集合。 */
    override val typeAliasCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirTypeAliasChecker>
        get() = setOf(
            CfirTypeAliasCFuncLegalityChecker,
            CfirTypeAliasCycleChecker,
            CfirTypeAliasExpandedTypeChecker,
            CfirTypeAliasUnusedTypeParameterChecker,
        )

    /** 对 class-like 声明的继承、覆盖、注解、互操作和值类型规则执行的 checker 集合。 */
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

    /** 对属性声明语义执行的 checker 集合。 */
    override val propertyCheckers: Set<CfirPropertyChecker>
        get() = setOf(
            CfirPropertySemanticsChecker,
        )

    /** 对属性访问器声明语义执行的 checker 集合。 */
    override val propertyAccessorCheckers: Set<CfirPropertyAccessorChecker>
        get() = setOf(
            CfirPropertyAccessorDeclarationChecker,
        )

    /** 对匿名函数和 lambda 参数推断相关规则执行的 checker 集合。 */
    override val anonymousFunctionCheckers: Set<org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirAnonymousFunctionChecker>
        get() = setOf(
            CfirLambdaParameterTypeChecker,
        )
}
