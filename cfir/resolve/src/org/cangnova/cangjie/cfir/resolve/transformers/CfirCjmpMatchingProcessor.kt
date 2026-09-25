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

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * CFIR CJMP_MATCHING 阶段主处理器。
 *
 * 对齐 Kotlin `FirExpectActualMatcherProcessor`（mpp/FirExpectActualMatcherTransformer.kt）：
 * 逐文件驱动 [CfirCjmpMatcherTransformer]，把 specific 声明的配对结果写入
 * [CfirCjmpMappingStorage]（specific session 组件，单侧写）。
 *
 * 门禁纪律（§8.5.5，门禁管行为不止管诊断）：
 * - 版本门（`LanguageFeature.CommonSpecificDeclarations`）关闭时不进入任何配对与写存储；
 * - 模式门（Phase 4 `CjmpSettingsComponent`）接线后在此并列判定。
 */
internal class CfirCjmpMatchingProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.CJMP_MATCHING,
) {
    /** 版本门判定结果（处理器生命周期内固定）。 */
    private val cjmpEnabled: Boolean =
        session.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)

    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(session, scopeSession)

    /**
     * 相位开始前清空配对存储（每轮 CJMP_MATCHING 一次；多文件模块共享同一存储，
     * 不能在逐文件遍历中清空）。
     */
    override fun beforePhase() {
        super.beforePhase()
        if (cjmpEnabled) {
            session.cjmpMappingStorageOrNull?.clear()
        }
    }

    /**
     * 版本门关闭时整阶段早退（不遍历、不写存储，保持 1.0.x 行为零变化）。
     */
    override fun processFile(file: CfirFile) {
        if (!cjmpEnabled) return
        // 未装配配对存储的轻量会话（手工构造的测试/桩会话）不参与 CJMP 配对
        if (session.cjmpMappingStorageOrNull == null) return
        super.processFile(file)
    }
}

/**
 * CJMP 配对阶段树转换器。
 *
 * 对齐 Kotlin `FirExpectActualMatcherTransformer`：
 * - 遍历模块内声明，对每个"可作为 specific 侧"的声明求 common 候选并写入存储；
 * - 匹配只做**结构面**判定（D2：不含返回类型与默认值；返回类型协变/注解/修饰符属验证期）；
 * - 不读写 common 侧 session 的任何声明（跨 session 单侧写，C25）。
 *
 * 说明：当前实现落在 `CfirCjmpResolver`（候选查找）与 `CfirCjmpMatcher`（结构匹配）
 * 两个原语上；本类只负责"何时、对谁、写什么"。
 */
open class CfirCjmpMatcherTransformer(
    override val session: CfirSession,
    private val scopeSession: ScopeSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.CJMP_MATCHING) {
    /** 当前 specific session 的配对结果存储。 */
    val storage: CfirCjmpMappingStorage get() = session.cjmpMappingStorage

    /**
     * 遍历入口：默认递归所有子节点（由 [CfirAbstractTreeTransformer] 提供）。
     *
     * 具体"对声明求配对并写存储"的钩子（transformNamedFunction / transformClass /
     * transformProperty / transformPatternVariable / transformFile 等）随匹配原语接线。
     */
    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        return super.transformFile(file, data)
    }

    // --------------------------- 声明遍历钩子 ---------------------------

    /** 转换 class 声明前执行配对。 */
    override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
        matchDeclaration(klass, data)
        return super.transformClass(klass, data)
    }

    /** 转换 struct 声明前执行配对。 */
    override fun transformStruct(struct: CfirStruct, data: Nothing?): CfirStruct {
        matchDeclaration(struct, data)
        return super.transformStruct(struct, data)
    }

    /** 转换 interface 声明前执行配对。 */
    override fun transformInterface(`interface`: CfirInterface, data: Nothing?): CfirInterface {
        matchDeclaration(`interface`, data)
        return super.transformInterface(`interface`, data)
    }

    /** 转换 enum 声明前执行配对。 */
    override fun transformEnum(enum: CfirEnum, data: Nothing?): CfirEnum {
        matchDeclaration(enum, data)
        return super.transformEnum(enum, data)
    }

    /** 转换 extend 前执行配对（官方 `MergeCJMPExtensions`：扩展类型 + 接口集为键）。 */
    override fun transformExtend(
        extend: org.cangnova.cangjie.cfir.declarations.CfirExtend,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirExtend {
        matchDeclaration(extend, data)
        return super.transformExtend(extend, data)
    }

    /** 转换命名函数前执行配对。 */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?) = run {
        matchDeclaration(namedFunction, data)
        super.transformNamedFunction(namedFunction, data)
    }

    /** 转换构造器前执行配对（secondary init；static init 由 parse 规则单列，匹配器内部跳过）。 */
    override fun transformConstructor(constructor: CfirConstructor, data: Nothing?) = run {
        matchDeclaration(constructor, data)
        super.transformConstructor(constructor, data)
    }

    /** 转换属性前执行配对。 */
    override fun transformProperty(property: CfirProperty, data: Nothing?): CfirProperty {
        matchDeclaration(property, data)
        return super.transformProperty(property, data)
    }

    /** 转换顶层/静态变量前执行配对（官方 `MatchCJMPVar`；实例成员变量随外层 nominal 的成员配对）。 */
    override fun transformFieldVariable(
        fieldVariable: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirFieldVariable {
        matchDeclaration(fieldVariable, data)
        return super.transformFieldVariable(fieldVariable, data)
    }

    /** 转换顶层模式变量前执行配对（`let v: T = e` 以模式变量承载；官方 `MatchCJMPVar` / 元组模式逐变量配对）。 */
    override fun transformPatternVariable(
        patternVariable: org.cangnova.cangjie.cfir.declarations.CfirPatternVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirPatternVariable {
        matchDeclaration(patternVariable, data)
        return super.transformPatternVariable(patternVariable, data)
    }

    // --------------------------- 配对核心 ---------------------------

    /**
     * 对单个声明执行配对并写存储。
     *
     * 只处理标记为 specific 的声明（官方：配对由 common/specific 标记驱动；未标记成员
     * 经外层已配对容器的成员配对路径覆盖）。写入方向恒为 specific 侧（C25 单侧存储）。
     */
    protected open fun matchDeclaration(declaration: CfirDeclaration, data: Nothing?) {
        CfirCjmpMatchRunner.matchDeclaration(declaration, session, storage)
    }
}
