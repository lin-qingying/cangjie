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
 */

package org.cangnova.cangjie.cfir.analysis.checkers.annotation

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable

/**
 * 推导仓颉 `@Annotation` 目标分类。
 *
 * **与官方编译器一字对齐**：翻译自
 * `external/cangjie_compiler/src/CHIR/Checker/AnnotationChecker.cpp` 的
 * `AnnotationChecker::GetTarget(const decl&)` 与 `GetTarget(const CustomTypeDef&)`。
 *
 * 判定顺序与官方保持一致——特别是官方注释明示的
 * "enum constructor must be checked before var and func decl"，
 * 翻译时保序，不可调换。
 *
 * 与官方差异：CFIR 无 `Attribute::GLOBAL` bitmask，改用 `CfirVariable.isLocal`
 * 取反判顶层；构造器无 `Attribute::CONSTRUCTOR`，改用 `CfirConstructor` 类型识别。
 * 语义等价。
 *
 * @return 该声明对应的单一 `CangjieAnnotationTarget`；不在注解目标体系内的声明返回 `null`。
 */
public fun CfirDeclaration.annotationTargetFor(): CangjieAnnotationTarget? = when (this) {
    // 对齐官方 `Is<FuncParam>` 分支
    is CfirValueParameter -> CangjieAnnotationTarget.PARAMETER

    // 对齐官方 `Is<PropDecl>` 分支（成员 property）
    is CfirProperty -> CangjieAnnotationTarget.MEMBER_PROPERTY

    // 对齐官方 `decl.TestAttr(AST::Attribute::ENUM_CONSTRUCTOR)` 分支
    // 必须先于 VarDecl 和 FuncDecl：enum 构造器在官方 AST 上也可能被认作 var/func，
    // 此处通过具体类型 CfirEnumConstructor 识别，保序由 when 分支顺序保证。
    is CfirEnumConstructor -> CangjieAnnotationTarget.ENUM_CONSTRUCTOR

    // 对齐官方 `Is<VarDeclAbstract>` 分支
    // 官方以 `Attribute::GLOBAL` 区分顶层与成员；CFIR 用 `isLocal` 取反等价表达。
    is CfirPatternVariable, is CfirPatternBindingVariable, is CfirFieldVariable ->
        if (!isLocal) CangjieAnnotationTarget.GLOBAL_VARIABLE else CangjieAnnotationTarget.MEMBER_VARIABLE

    // 对齐官方 `func->TestAttr(CONSTRUCTOR)` 分支
    is CfirConstructor -> CangjieAnnotationTarget.INIT

    // 对齐官方 `func` + `TestAttr(GLOBAL)` 分支
    // CFIR 无 Attribute::GLOBAL bitmask，顶层函数用 CfirMainFunction 或非成员的 CfirNamedFunction 表达。
    is CfirMainFunction -> CangjieAnnotationTarget.GLOBAL_FUNCTION
    is CfirNamedFunction, is CfirFinalizer ->
        if (!isLocal) CangjieAnnotationTarget.GLOBAL_FUNCTION else CangjieAnnotationTarget.MEMBER_FUNCTION

    // 对齐官方 fallback：`return AnnotationTarget::MEMBER_FUNCTION`
    // CFIR 的匿名函数和宏声明无 isLocal 字段，按官方语义归为成员函数目标。
    is CfirFunction -> CangjieAnnotationTarget.MEMBER_FUNCTION

    // 对齐官方 `GetTarget(const CustomTypeDef&)`：
    //   `Is<ExtendDef>(type) ? AnnotationTarget::EXTEND : AnnotationTarget::TYPE`
    is CfirExtend -> CangjieAnnotationTarget.EXTEND
    is CfirClass, is CfirStruct, is CfirInterface, is CfirTypeAlias -> CangjieAnnotationTarget.TYPE

    // 未在官方注解目标体系内的声明（类型参数、文件、宏等）返回 null。
    else -> null
}
