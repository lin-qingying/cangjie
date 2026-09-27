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

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver as CfirCjmpCallableResolver
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping

/**
 * CFIR CJMP_MATCHING 阶段主处理器。
 *
 * 对齐 Kotlin `FirExpectActualMatcherProcessor`（mpp/FirExpectActualMatcherTransformer.kt）：
 * 逐文件驱动 [CfirCjmpMatcherTransformer]，把 specific 声明的配对结果写入
 * [CfirCjmpMappingStorage]（specific session 组件，单侧写）。
 *
 * 门禁纪律（§8.5.5，门禁管行为不止管诊断）：
 * - 版本门（`LanguageFeature.CommonSpecificDeclarations`）关闭时不进入任何配对与写存储；
 * - 模式门只允许 specific 编译执行配对，其它模式保持存储为空。
 */
internal class CfirCjmpMatchingProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.CJMP_MATCHING,
) {
    /** 配对只在 specific 编译中执行；测试/IDE 可在会话创建后注入最终模式。 */
    private fun isCjmpMatchingEnabled(): Boolean = session.isCjmpSpecificCompilationEnabled()

    override val transformer: CfirTransformer<Nothing?> = CfirCjmpMatcherTransformer(session, scopeSession)

    /**
     * 相位开始前清空配对存储（每轮 CJMP_MATCHING 一次；多文件模块共享同一存储，
     * 不能在逐文件遍历中清空）。
     */
    override fun beforePhase() {
        super.beforePhase()
        if (isCjmpMatchingEnabled()) {
            session.cjmpMappingStorage.clear()
        }
    }

    /**
     * 版本门关闭或当前不是 specific 编译时整阶段早退（不遍历、不写存储）。
     */
    override fun processFile(file: CfirFile) {
        if (!isCjmpMatchingEnabled()) return
        super.processFile(file)
    }
}

/**
 * CJMP 配对阶段树转换器。
 *
 * 对齐 Kotlin `FirExpectActualMatcherTransformer`：
 * - 遍历模块内声明，对每个"可作为 specific 侧"的声明求 common 候选并写入存储；
 * - 兼容性由 resolution.common 的共享 matcher 执行；对齐 cjc 1.1.3 的参数类型、返回协变与双侧默认值规则；
 * - 不读写 common 侧 session 的任何声明（跨 session 单侧写，C25）。
 *
 * 候选查找由 `CfirCjmpResolver` 适配，通用兼容性算法由 resolution.common
 * `AbstractCjmpMatcher` 执行，存储由 `CfirCjmpMatchingContext` 适配。
 */
open class CfirCjmpMatcherTransformer(
    override val session: CfirSession,
    private val scopeSession: ScopeSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.CJMP_MATCHING) {
    /** eager 遍历的外围声明上下文；ThreadLocal 允许不同文件并行驱动同一阶段 visitor。 */
    private val matchingContainer = ThreadLocal<CfirDeclaration?>()

    /** 当前 specific session 的配对结果存储。 */
    val storage: CfirCjmpMappingStorage get() = session.cjmpMappingStorage

    /**
     * 遍历入口：文件和声明容器递归声明，非声明节点到此为止。
     *
     * 文件与 class-like/extend 容器只递归其声明列表；其它节点由 [transformElement] 截止。
     */
    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            withMatchingContainer(null) {
                file.transformDeclarations(this, data)
                file
            }
        }
    }

    /** eager 与 LL 共同使用的声明级匹配入口。 */
    fun transformMemberDeclaration(
        memberDeclaration: CfirMemberDeclaration,
        containingContainer: CfirDeclaration? = matchingContainer.get(),
    ) {
        when (memberDeclaration) {
            is CfirPatternVariable ->
                CfirCjmpCallableResolver.findSpecificTopLevelPatternVariablesInMatchOrder(memberDeclaration, session)
                    .forEach { matchDeclaration(it, null, containingContainer) }

            is CfirCallableDeclaration ->
                CfirCjmpCallableResolver.findSpecificCallablesInMatchOrder(memberDeclaration, session, containingContainer)
                    .forEach { matchDeclaration(it, null, containingContainer) }

            else -> matchDeclaration(memberDeclaration, null, containingContainer)
        }
    }

    /** CJMP 匹配不进入函数体、表达式或类型节点。 */
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E = element

    // --------------------------- 声明遍历钩子 ---------------------------

    /** 转换 class 声明前执行配对。 */
    override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
        transformMemberDeclaration(klass)
        return withMatchingContainer(klass) {
            klass.transformDeclarations(this, data)
            klass
        }
    }

    /** 转换 struct 声明前执行配对。 */
    override fun transformStruct(struct: CfirStruct, data: Nothing?): CfirStruct {
        transformMemberDeclaration(struct)
        return withMatchingContainer(struct) {
            struct.transformDeclarations(this, data)
            struct
        }
    }

    /** 转换 interface 声明前执行配对。 */
    override fun transformInterface(`interface`: CfirInterface, data: Nothing?): CfirInterface {
        transformMemberDeclaration(`interface`)
        return withMatchingContainer(`interface`) {
            `interface`.transformDeclarations(this, data)
            `interface`
        }
    }

    /** 转换 enum 声明前执行配对。 */
    override fun transformEnum(enum: CfirEnum, data: Nothing?): CfirEnum {
        transformMemberDeclaration(enum)
        return withMatchingContainer(enum) {
            enum.transformDeclarations(this, data)
            enum
        }
    }

    /** 转换 extend 前执行配对（官方 `MergeCJMPExtensions`：扩展类型 + 接口集为键）。 */
    override fun transformExtend(
        extend: org.cangnova.cangjie.cfir.declarations.CfirExtend,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirExtend {
        transformMemberDeclaration(extend)
        return withMatchingContainer(extend) {
            extend.transformDeclarations(this, data)
            extend
        }
    }

    /** 转换命名函数前执行配对。 */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?) = run {
        transformMemberDeclaration(namedFunction)
        namedFunction
    }

    /** 转换构造器前执行配对（secondary init；static init 由 parse 规则单列，匹配器内部跳过）。 */
    override fun transformConstructor(constructor: CfirConstructor, data: Nothing?) = run {
        transformMemberDeclaration(constructor)
        constructor
    }

    /** enum 构造器在自己的声明级入口按官方 CJMP enum-constructor 规则配对。 */
    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: Nothing?): CfirEnumConstructor {
        transformMemberDeclaration(enumConstructor)
        return enumConstructor
    }

    /** 转换属性前执行配对。 */
    override fun transformProperty(property: CfirProperty, data: Nothing?): CfirProperty {
        transformMemberDeclaration(property)
        return property
    }

    /** 转换变量前执行配对（官方 `MatchCJMPVar`；实例成员使用已配对父容器的泛型映射）。 */
    override fun transformFieldVariable(
        fieldVariable: org.cangnova.cangjie.cfir.declarations.CfirFieldVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirFieldVariable {
        transformMemberDeclaration(fieldVariable)
        return fieldVariable
    }

    /** 转换顶层模式变量前执行配对（`let v: T = e` 以模式变量承载；官方 `MatchCJMPVar` / 元组模式逐变量配对）。 */
    override fun transformPatternVariable(
        patternVariable: org.cangnova.cangjie.cfir.declarations.CfirPatternVariable,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirPatternVariable {
        transformMemberDeclaration(patternVariable)
        return patternVariable
    }

    // --------------------------- 配对核心 ---------------------------

    /**
     * 对单个声明执行配对并写存储。
     *
     * 只处理标记为 specific 的声明（官方：配对由 common/specific 标记驱动；成员独立进入此入口，
     * 并通过已配对父容器查找 common 候选）。写入方向恒为 specific 侧（C25 单侧存储）。
     */
    protected open fun matchDeclaration(
        declaration: CfirDeclaration,
        data: Nothing?,
        containingContainer: CfirDeclaration? = matchingContainer.get(),
    ) {
        CfirCjmpMatchRunner.matchDeclaration(declaration, session, storage, containingContainer)
    }

    private inline fun <T> withMatchingContainer(container: CfirDeclaration?, action: () -> T): T {
        val previous = matchingContainer.get()
        if (container == null) {
            matchingContainer.remove()
        } else {
            matchingContainer.set(container)
        }
        return try {
            action()
        } finally {
            if (previous == null) {
                matchingContainer.remove()
            } else {
                matchingContainer.set(previous)
            }
        }
    }
}
