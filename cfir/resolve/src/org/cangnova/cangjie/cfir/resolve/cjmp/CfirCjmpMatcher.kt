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
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CjmpMismatchKind
import org.cangnova.cangjie.cfir.session.CjmpParameterMismatch
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 匹配结论。 */
sealed class CjmpMatchResult {
    /** 匹配成功（可绑定）。 */
    data object Matched : CjmpMatchResult()

    /** 声明级失配（无参数锚点）。 */
    data class Mismatched(val kind: CjmpMismatchKind) : CjmpMatchResult()

    /** 参数级失配（官方在该参数上报告诊断后返回 false）。 */
    data class ParameterMismatched(val mismatch: CjmpParameterMismatch) : CjmpMatchResult()
}

/**
 * CJMP 匹配（逐条对位 cjc 1.1.3 `src/Sema/CJMP/CheckCJMP.cpp`，计划风险 3：以 1.1.3 实测与源码为准）。
 *
 * - class-like：`MatchNominativeDecl`——仅种类（class/struct/interface/enum）相等即配对；种类不同
 *   不绑定（[CjmpMismatchKind.CLASS_KIND]，检查器报 `SPECIFIC_HAS_DIFFERENT_KIND`）；
 * - 函数/构造器：`MatchCJMPFunction`——
 *   1. 泛型参数个数相等（`IsCJMPDeclMatchable`）；
 *   2. 函数类型 specific ≤ common（`IsFuncDeclSubType`：参数类型**逐个相同**，返回类型**协变**），
 *      不满足即静默失败（只报 NOT_MATCHED）；
 *   3. 逐参数：命名性/命名参数名不同 → 参数级失败（`SPECIFIC_HAS_DIFFERENT_PARAMETER`）；
 *      两侧同时带默认值 → 参数级失败（`CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES`）；
 * - 变量/属性：`MatchCJMPVar` / `MatchCJMPProp`——同名同种类即配对，类型差异由检查器在已配对对上报告。
 *
 * 泛型形参按位置映射（D6：重命名合法）。跨 session 类型同一性按 ClassId 结构判定（风险 9）。
 */
object CfirCjmpMatcher {
    /** 判定 [specific] 与 [common] 是否配对。调用方保证名称与容器一致（候选查找）。 */
    fun match(specific: CfirDeclaration, common: CfirDeclaration, session: CfirSession): CjmpMatchResult {
        if (specific is CfirClassLikeDeclaration || common is CfirClassLikeDeclaration) {
            if (specific !is CfirClassLikeDeclaration || common !is CfirClassLikeDeclaration) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
            return if (nominalKind(specific) == nominalKind(common)) {
                CjmpMatchResult.Matched
            } else {
                CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
            }
        }
        if (specific is org.cangnova.cangjie.cfir.declarations.CfirExtend ||
            common is org.cangnova.cangjie.cfir.declarations.CfirExtend
        ) {
            // extend 由候选键（扩展类型 + 接口集）保证同一性
            return if (specific is org.cangnova.cangjie.cfir.declarations.CfirExtend &&
                common is org.cangnova.cangjie.cfir.declarations.CfirExtend
            ) CjmpMatchResult.Matched else CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
        }
        if (callableKind(specific) != callableKind(common)) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND)
        }
        if (specific is CfirFunction && common is CfirFunction) {
            return matchFunctions(specific, common, session)
        }
        return CjmpMatchResult.Matched
    }

    /** 官方 `MatchEnumFuncTypes`：带参 enum 构造器参数类型逐个相同（无参构造器按名即配对）。 */
    fun matchEnumConstructors(
        specific: org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor,
        common: org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor,
    ): Boolean {
        if (specific.valueParameters.size != common.valueParameters.size) return false
        return specific.valueParameters.indices.all { i ->
            identical(
                specific.valueParameters[i].returnTypeRef.coneTypeOrNull(),
                common.valueParameters[i].returnTypeRef.coneTypeOrNull(),
                emptyMap(),
            )
        }
    }

    /** 官方 `MatchCJMPFunction` 对位。 */
    private fun matchFunctions(specific: CfirFunction, common: CfirFunction, session: CfirSession): CjmpMatchResult {
        if (specific.typeParameters.size != common.typeParameters.size) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE_PARAMETER_COUNT)
        }
        val specificParameters = specific.valueParameters
        val commonParameters = common.valueParameters
        if (specificParameters.size != commonParameters.size) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_COUNT)
        }

        // 泛型形参按位置映射（common 的 T → specific 的 T'）
        val typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter> =
            common.typeParameters.zip(specific.typeParameters).toMap()

        // IsFuncDeclSubType：参数类型逐个相同
        for (i in specificParameters.indices) {
            if (!identical(
                    specificParameters[i].returnTypeRef.coneTypeOrNull(),
                    commonParameters[i].returnTypeRef.coneTypeOrNull(),
                    typeParameterMapping,
                )
            ) {
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_TYPES)
            }
        }
        // IsFuncDeclSubType：返回类型协变（构造器无返回类型比较面）
        if (specific !is CfirConstructor && !returnTypeCompatible(specific, common, typeParameterMapping, session)) {
            return CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE)
        }

        for (i in specificParameters.indices) {
            val specificParameter = specificParameters[i]
            val commonParameter = commonParameters[i]
            if (specificParameter.isNamed != commonParameter.isNamed ||
                (specificParameter.isNamed && specificParameter.name != commonParameter.name)
            ) {
                return CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_NAMES, i),
                )
            }
            if (specificParameter.defaultValue != null && commonParameter.defaultValue != null) {
                return CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, i),
                )
            }
        }
        return CjmpMatchResult.Matched
    }

    /**
     * 返回类型 specific ≤ common（官方 `IsSubtype(retTy1, retTy2)`）。
     *
     * 类型不可得（未解析/隐式）时不在此否决；泛型形参按位置映射后按身份比较。
     */
    private fun returnTypeCompatible(
        specific: CfirFunction,
        common: CfirFunction,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        session: CfirSession,
    ): Boolean {
        val specificType = specific.returnTypeRef.coneTypeOrNull() ?: return true
        val commonType = common.returnTypeRef.coneTypeOrNull() ?: return true
        if (specificType is ConeErrorType || commonType is ConeErrorType) return true
        if (identical(specificType, commonType, typeParameterMapping)) return true
        return runCatching {
            AbstractTypeChecker.isSubtypeOf(session.typeContext, specificType, commonType)
        }.getOrDefault(false)
    }

    /**
     * 类型相同（官方 `IsTyEqual` 对位；跨 session 按 ClassId 结构比较，风险 9）。
     *
     * - 两侧都是类型参数：common 形参按位置映射到 specific 形参；
     * - 两侧都是 class-like：ClassId 相等 + 实参递归；
     * - 两侧都是原生类型：kind 相等；
     * - 其余形态退化为 Cone 结构相等；类型不可得时不否决。
     */
    private fun identical(
        specificType: ConeCangJieType?,
        commonType: ConeCangJieType?,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): Boolean {
        if (specificType == null || commonType == null) return true

        if (specificType is ConeTypeParameterType && commonType is ConeTypeParameterType) {
            val specificTag = specificType.lookupTag as? ConeTypeParameterLookupTag ?: return false
            val commonTag = commonType.lookupTag as? ConeTypeParameterLookupTag ?: return false
            val mappedSpecificSymbol = typeParameterMapping.entries
                .firstOrNull { it.key.symbol == commonTag.typeParameterSymbol }
                ?.value?.symbol
            return mappedSpecificSymbol == specificTag.typeParameterSymbol ||
                    specificTag.typeParameterSymbol == commonTag.typeParameterSymbol
        }

        if (specificType is ConeClassLikeType && commonType is ConeClassLikeType) {
            if (specificType.classId != commonType.classId) return false
            val specificArguments = specificType.typeArguments
            val commonArguments = commonType.typeArguments
            if (specificArguments.size != commonArguments.size) return false
            return specificArguments.indices.all { i ->
                identical(specificArguments[i].type, commonArguments[i].type, typeParameterMapping)
            }
        }

        if (specificType is ConePrimitiveType && commonType is ConePrimitiveType) {
            return specificType.kind == commonType.kind
        }

        return specificType == commonType
    }

    /** 官方 `ASTKind` 的 nominal 种类面（class/struct/interface/enum）。 */
    fun nominalKind(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirClass -> "class"
        is CfirStruct -> "struct"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        else -> "class"
    }

    /**
     * 可调用者种类（官方 `ASTKind`：FUNC_DECL（含构造器，构造器只与构造器同名）/ PROP_DECL / VAR_DECL）。
     */
    private fun callableKind(declaration: CfirDeclaration): String = when (declaration) {
        is CfirConstructor -> "constructor"
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        is CfirVariable -> "variable"
        is CfirCallableDeclaration -> declaration::class.simpleName.orEmpty()
        else -> "other"
    }

    /**
     * 安全读取 cone 类型：未解析/隐式引用返回 null（匹配阶段不因类型未就绪崩溃；调用方按"不否决"处理）。
     */
    private fun CfirTypeRef.coneTypeOrNull(): ConeCangJieType? {
        if (this is CfirResolvedTypeRef) return coneType
        return runCatching { coneType }.getOrNull()
    }

    /** 声明是否为 specific 侧（供运行核心判断成员是否参与配对）。 */
    fun isSpecific(declaration: CfirDeclaration): Boolean =
        (declaration as? CfirMemberDeclaration)?.status?.isSpecific == true
}
