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
 */

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 声明成员图的解析视图。CJMP 实际化视图必须与 STATUS 使用的原始视图分别缓存，
 * 因为它消费 CJMP_MATCHING 发布的成员快照。
 */
enum class CfirCjmpMemberScopeView {
    RAW,
    CJMP_ACTUALIZED,
    /** 实例化建表读取未导出过滤的 FINAL 合并声明，缓存域独立于调用候选。 */
    CJMP_INSTANTIATION,
}

/** 提供 CJMP common 物理 owner 到 specific 逻辑 owner 的 use-site 类型替换。 */
interface CfirCjmpMemberSubstitutorProvider {
    /** 已准备成员图的容器投影；空值仅表示该成员没有 CJMP 容器投影。 */
    fun cjmpMemberOwnerFor(symbol: CfirCallableSymbol<*>): CfirCjmpMemberOwnerProjection?

    fun cjmpOwnerSubstitutorFor(symbol: CfirCallableSymbol<*>): ConeSubstitutor?

    /** 为声明级 override 图保留符号 owner 类型参数，只应用 common→specific 的 owner 映射。 */
    fun cjmpOwnerSubstitutorFor(
        symbol: CfirCallableSymbol<*>,
        declarationOwnerSubstitutor: ConeSubstitutor,
    ): ConeSubstitutor? = null
}

/** 原声明身份与合并声明体身份分别保存，不通过符号所属 session 重建逻辑容器。 */
data class CfirCjmpMemberOwnerProjection(
    val physicalOwner: CfirDeclaration,
    val logicalOwner: CfirDeclaration,
)
