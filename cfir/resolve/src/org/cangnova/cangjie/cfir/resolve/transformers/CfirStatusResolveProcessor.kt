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

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.util.PrivateForInline

/**
 * CFIR STATUS 阶段的主处理器。
 *
 * 该处理器把类型阶段之后的声明交给 [CfirStatusResolveTransformer]，
 * 统一补全可见性、模态以及 callable 覆盖关系相关的 resolved status。
 */
internal class CfirStatusResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.STATUS,
) {
    /**
     * STATUS 阶段实际使用的树转换器。
     *
     * 每个处理器实例持有独立的 [CfirStatusComputationSession]，
     * 用于记录本次遍历中的递归计算状态并避免 class-like 超类型解析环。
     */
    override val transformer: CfirStatusResolveTransformer = run {
        val statusComputationSession = CfirStatusComputationSession(session, scopeSession)
        CfirStatusResolveTransformer(statusComputationSession)
    }
}

/**
 * 一次 STATUS 计算中的共享状态。
 *
 * 它记录声明的 status 推进进度，并提供超类型 class-like 声明的强制解析入口。
 * 主干实现只面向当前 use-site session，low-level resolver 可以通过继承扩展多 session 搜索。
 */
open class CfirStatusComputationSession(
    /** 当前 STATUS 解析读取符号、scope 与 platform 服务时使用的会话。 */
    val useSiteSession: CfirSession,
    /** 当前 STATUS 解析复用的 scope 缓存会话。 */
    val useSiteScopeSession: ScopeSession,
) {
    /**
     * 声明到 STATUS 计算状态的缓存表。
     *
     * 默认值为 [StatusComputationStatus.NotComputed]，避免每次读取前显式初始化。
     */
    private val statusMap: MutableMap<CfirDeclaration, StatusComputationStatus> =
        hashMapOf<CfirDeclaration, StatusComputationStatus>()
            .withDefault { StatusComputationStatus.NotComputed }

    /** 返回指定声明在当前 STATUS 计算会话中的推进状态。 */
    operator fun get(declaration: CfirDeclaration): StatusComputationStatus = statusMap.getValue(declaration)

    /**
     * 标记指定声明开始进行 STATUS 计算。
     *
     * 返回进入前的状态；只要该状态仍要求计算，就立即写入 [StatusComputationStatus.Computing]
     * 作为同阶段递归的环路屏障。调用方必须基于返回值决定是否执行计算，不能读取写入后的
     * `Computing` 状态，否则首次 STATUS 计算会被误判成递归调用。
     */
    fun startComputing(declaration: CfirDeclaration): StatusComputationStatus {
        val previousStatus = statusMap.getValue(declaration)
        if (previousStatus.requiresComputation) {
            statusMap[declaration] = StatusComputationStatus.Computing
        }
        return previousStatus
    }

    /** 标记指定声明的 STATUS 已完整计算完成。 */
    fun endComputing(declaration: CfirDeclaration) {
        statusMap[declaration] = StatusComputationStatus.Computed
    }

    /**
     * 标记指定声明只完成了自身 declaration status 的计算。
     *
     * 该状态用于低阶 lazy resolve：声明自身 status 可先发布，
     * 但后续成员或子树仍允许继续推进到完整 STATUS。
     */
    fun computeOnlyDeclarationStatus(declaration: CfirDeclaration) {
        val existedStatus = statusMap.getValue(declaration)
        if (existedStatus < StatusComputationStatus.ComputedOnlyDeclarationStatus) {
            statusMap[declaration] = StatusComputationStatus.ComputedOnlyDeclarationStatus
        }
    }

    /**
     * 单个声明在 STATUS 计算中的生命周期状态。
     *
     * [requiresComputation] 表示再次遇到该声明时是否仍需继续进入真实计算。
     */
    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        /** 声明尚未进入 STATUS 计算。 */
        NotComputed(true),
        /** 声明正在计算中，用于打断递归环。 */
        Computing(false),
        /** 声明自身 status 已计算，但子树或成员仍可继续解析。 */
        ComputedOnlyDeclarationStatus(true),
        /** 声明及其 STATUS 子任务均已完成。 */
        Computed(false),
    }

    /**
     * 对齐 Kotlin `StatusComputationSession.forceResolveStatusesOfSupertypes`。
     *
     * STATUS 主干在进入当前 source class 之前，必须先把所有 super class 的 STATUS 推到稳定态，
     * 否则 override / accessor / enum constructor 都会在半初始化状态下读取上层信息。
     */
    open fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) {
        val classLikeDeclaration = declaration as? CfirClassLikeDeclaration ?: return
        for (superTypeRef in classLikeDeclaration.superTypeRefs + additionalSuperTypes(classLikeDeclaration)) {
            for (classifierSymbol in superTypeToSymbols(superTypeRef)) {
                forceResolveStatusOfCorrespondingClass(classifierSymbol)
            }
        }
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.superTypeToSymbols`。
     *
     * 主干默认只按当前 use-site session 查询 class-like symbol；
     * low-level 会在子类里扩展为多 session 搜索。
     */
    open fun superTypeToSymbols(typeRef: CfirTypeRef): Collection<CfirClassLikeSymbol<*>> {
        val classId = typeRef.coneType.classId ?: return emptyList()
        return listOfNotNull(useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId))
    }

    /**
     * 对一个超类型符号对应的 class-like 声明执行 STATUS 强制推进。
     *
     * typealias 会继续展开其 expanded type，直到落到真实 class-like 声明。
     */
    private fun forceResolveStatusOfCorrespondingClass(classLikeSymbol: CfirClassLikeSymbol<*>) {
        when (classLikeSymbol) {
            is CfirClassSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirInterfaceSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirStructSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirEnumSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirPrimitiveTypeSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirTypeAliasSymbol -> {
                for (expandedSymbol in superTypeToSymbols(classLikeSymbol.cfir.expandedTypeRef)) {
                    forceResolveStatusOfCorrespondingClass(expandedSymbol)
                }
            }
        }
    }

    /**
     * 强制推进 class-like 声明自身及其超类型的 STATUS。
     *
     * source 声明会重新进入目标 class 子树，非 source 声明只维护计算状态，
     * 因为它们通常已经由 provider / deserializer 提供稳定状态。
     */
    private fun forceResolveStatusesOfClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        if (classLikeDeclaration.origin != CfirDeclarationOrigin.Source) {
            val computationStatus = this[classLikeDeclaration]
            if (!computationStatus.requiresComputation) return

            startComputing(classLikeDeclaration)
            forceResolveStatusesOfSupertypes(classLikeDeclaration)
            endComputing(classLikeDeclaration)
            return
        }

        val computationStatus = this[classLikeDeclaration]
        if (!computationStatus.requiresComputation) return
        resolveClassForSuperType(classLikeDeclaration)
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.resolveClassForSuperType`。
     *
     * 主干必须真正推进 source class 的 STATUS，而不是永久返回 `false`。
     * 这里没有 Kotlin 的 designation 基础设施，因此直接对目标 class subtree 执行 STATUS resolve。
     */
    open fun resolveClassForSuperType(classLikeDeclaration: CfirClassLikeDeclaration): Boolean {
        classLikeDeclaration.transformSingle(CfirStatusResolveTransformer(this), null)
        return true
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.additionalSuperTypes`。
     *
     * 仓颉主干默认没有额外 platform-mapped super type。
     */
    open fun additionalSuperTypes(classLikeDeclaration: CfirClassLikeDeclaration): List<CfirTypeRef> = emptyList()
}

/**
 * STATUS 阶段树转换器的公共骨架。
 *
 * 该类负责维护当前 class-like 容器栈、声明 phase 推进和 resolved status 发布顺序；
 * 具体 status 默认值和覆盖关系计算委托给 [CfirStatusResolver]。
 */
open class AbstractCfirStatusResolveTransformer(
    /** 本次 STATUS 转换共享的计算状态与 use-site 会话信息。 */
    val statusComputationSession: CfirStatusComputationSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.STATUS) {
    /** 当前转换路径上的 class-like 容器栈。 */
    @PrivateForInline
    val classes: MutableList<CfirClassLikeDeclaration> = mutableListOf()
    /** 根据声明形态、容器和覆盖关系计算 resolved status 的服务对象。 */
    val statusResolver: CfirStatusResolver = CfirStatusResolver(session, statusComputationSession.useSiteScopeSession)
    /** 当前 STATUS 转换使用的 use-site session。 */
    override val session: CfirSession
        get() = statusComputationSession.useSiteSession
    /** 当前声明所在的最内层 class-like 容器。 */
    @OptIn(PrivateForInline::class)
    val containingClass: CfirClassLikeDeclaration?
        get() = classes.lastOrNull()

    /**
     * STATUS 以文件内声明为根，而不是把 [CfirFile] 当成可推进的 declaration。
     *
     * 文件只在 IMPORTS 阶段维护自己的 resolve state；若让通用 declaration guard 处理它，
     * 会因其未到 TYPES 而提前返回，整棵声明树都不会进入 STATUS。这里对齐 Kotlin FIR
     * 的 `transformFile -> transformDeclarationContent` 边界，只遍历该阶段实际拥有的顶层声明。
     */
    override fun transformFile(file: CfirFile, data: Nothing?): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            file.transformDeclarations(this, data)
            file
        }
    }

    /**
     * 在 class-like 容器栈中临时压入 [klass] 并执行 [computeResult]。
     *
     * 该 helper 保证子声明解析期间能读取正确的外层 class 上下文。
     */
    @OptIn(PrivateForInline::class)
    inline fun storeClass(
        klass: CfirClassLikeDeclaration,
        computeResult: () -> Unit,
    ) {
        classes += klass
        computeResult()
        classes.removeAt(classes.lastIndex)
    }

    /** 将所有声明节点转入 STATUS 专用路径，其它元素继续使用默认树遍历。 */
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    /** 为通用声明建立 STATUS phase guard 后继续遍历其子节点。 */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        return withResolvedStatusPhase(declaration) {
            declaration.transformChildren(this, data)
        }
    }

    /**
     * 统一 STATUS 发布入口。
     *
     * 这里负责 phase 迁移和最终 resolved-status 收敛；真正的“怎么解状态”由各个具体声明 override。
     */
    protected fun <D : CfirDeclaration> withResolvedStatusPhase(
        target: D,
        action: () -> Unit,
    ): D {
        if (target.resolvePhase < CfirResolvePhase.TYPES || target.resolvePhase >= CfirResolvePhase.STATUS) {
            return target
        }

        when (statusComputationSession.startComputing(target)) {
            CfirStatusComputationSession.StatusComputationStatus.Computed,
            CfirStatusComputationSession.StatusComputationStatus.Computing,
            -> return target

            else -> Unit
        }

        action()
        if (target is CfirMemberDeclaration) {
            target.publishResolvedStatusIfNeeded()
        }
        target.replaceResolvePhase(CfirResolvePhase.STATUS)
        statusComputationSession.endComputing(target)
        return target
    }

    /**
     * 按 STATUS 阶段顺序转换 class-like 成员。
     *
     * 非 class-like 成员先处理，嵌套 class-like 后处理，避免成员解析读取未稳定的外层状态。
     */
    protected fun transformClassLikeMembers(classLike: CfirClassLikeDeclaration) {
        val declarations = classLike.declarations
        declarations.forEach { declaration ->
            if (declaration !is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
        declarations.forEach { declaration ->
            if (declaration is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
    }

    /**
     * 按 STATUS 阶段顺序转换 extend 容器成员。
     *
     * extend 与 class-like 一样拥有成员列表，因此复用“普通成员优先、嵌套类型后置”的发布顺序。
     */
    protected fun transformExtendMembers(extend: CfirExtend) {
        val declarations = extend.declarations
        declarations.forEach { declaration ->
            if (declaration !is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
        declarations.forEach { declaration ->
            if (declaration is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
    }

    /** 推进 class 声明 STATUS，并在其容器上下文内处理类型参数和成员。 */
    override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
        val outerClass = containingClass
        return withResolvedStatusPhase(klass) {
            storeClass(klass) {
                statusComputationSession.forceResolveStatusesOfSupertypes(klass)
                klass.transformTypeParameters(this, null)
                transformClassStatus(klass, outerClass)
                transformClassLikeMembers(klass)
            }
        }
    }

    /** 推进 interface 声明 STATUS，并在其容器上下文内处理类型参数和成员。 */
    override fun transformInterface(`interface`: CfirInterface, data: Nothing?): CfirInterface {
        val outerClass = containingClass
        return withResolvedStatusPhase(`interface`) {
            storeClass(`interface`) {
                statusComputationSession.forceResolveStatusesOfSupertypes(`interface`)
                `interface`.transformTypeParameters(this, null)
                transformInterfaceStatus(`interface`, outerClass)
                transformClassLikeMembers(`interface`)
            }
        }
    }

    /** 推进 struct 声明 STATUS，并在其容器上下文内处理类型参数和成员。 */
    override fun transformStruct(struct: CfirStruct, data: Nothing?): CfirStruct {
        val outerClass = containingClass
        return withResolvedStatusPhase(struct) {
            storeClass(struct) {
                statusComputationSession.forceResolveStatusesOfSupertypes(struct)
                struct.transformTypeParameters(this, null)
                transformStructStatus(struct, outerClass)
                transformClassLikeMembers(struct)
            }
        }
    }

    /** 推进 enum 声明 STATUS，并在其容器上下文内处理类型参数和成员。 */
    override fun transformEnum(enum: CfirEnum, data: Nothing?): CfirEnum {
        val outerClass = containingClass
        return withResolvedStatusPhase(enum) {
            storeClass(enum) {
                statusComputationSession.forceResolveStatusesOfSupertypes(enum)
                enum.transformTypeParameters(this, null)
                transformEnumStatus(enum, outerClass)
                transformClassLikeMembers(enum)
            }
        }
    }

    /**
     * 只计算并发布 class 自身 STATUS。
     *
     * Low-level lazy resolver 会在自己的写锁中调用该入口，避免 class 目标重新落入 generic
     * `transformSingle` 路径后被 phase 状态短路。
     */
    fun transformClassStatus(
        klass: CfirClass,
        containingClass: CfirClassLikeDeclaration? = this.containingClass,
    ) {
        klass.replaceStatus(statusResolver.resolveStatus(klass, containingClass, isLocal = false))
    }

    /**
     * 只计算并发布 interface 自身 STATUS。
     *
     * 仓颉把 interface 从 class 节点中拆出，但 STATUS 阶段仍对位 Kotlin class-like
     * 声明路径，必须显式发布自身 resolved status 后再进入成员解析。
     */
    fun transformInterfaceStatus(
        interfaceDeclaration: CfirInterface,
        containingClass: CfirClassLikeDeclaration? = this.containingClass,
    ) {
        interfaceDeclaration.replaceStatus(statusResolver.resolveStatus(interfaceDeclaration, containingClass, isLocal = false))
    }

    /**
     * 只计算并发布 struct 自身 STATUS。
     *
     * struct 在仓颉语义里是独立 class-like 节点，但 STATUS 阶段仍需和 Kotlin regular class
     * 使用同一条 class-like 发布链，不能回落到 generic declaration children transform。
     */
    fun transformStructStatus(
        struct: CfirStruct,
        containingClass: CfirClassLikeDeclaration? = this.containingClass,
    ) {
        struct.replaceStatus(statusResolver.resolveStatus(struct, containingClass, isLocal = false))
    }

    /**
     * 只计算并发布 enum 自身 STATUS。
     *
     * enum 和 struct 一样属于仓颉额外拆出的 class-like 节点，必须在 STATUS 阶段显式发布
     * resolved status，后续 enum constructor / 成员解析才能读取稳定的宿主状态。
     */
    fun transformEnumStatus(
        enum: CfirEnum,
        containingClass: CfirClassLikeDeclaration? = this.containingClass,
    ) {
        enum.replaceStatus(statusResolver.resolveStatus(enum, containingClass, isLocal = false))
    }

    /** 推进 typealias 声明 STATUS。 */
    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Nothing?): CfirTypeAlias {
        return withResolvedStatusPhase(typeAlias) {
            transformTypeAliasStatusWithoutPhaseGuard(typeAlias)
        }
    }

    /**
     * 只计算并发布 typealias 自身 STATUS。
     *
     * 仓颉 low-level STATUS resolver 会像 `extend` / 非 `CfirNamedFunction` 一样，
     * 对某些声明直接走“写锁下的专用入口”，避免 generic `transformSingle` 路径
     * 在 phase 已被推进时跳过真实的 status 发布。
     */
    fun transformTypeAliasStatusWithoutPhaseGuard(typeAlias: CfirTypeAlias) {
        typeAlias.transformTypeParameters(this, null)
        typeAlias.replaceStatus(statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
    }

    /** 推进 extend 容器 STATUS，并继续处理其成员状态。 */
    override fun transformExtend(extend: CfirExtend, data: Nothing?): CfirExtend {
        return withResolvedStatusPhase(extend) {
            transformExtendStatusWithoutPhaseGuard(extend)
            transformExtendMembers(extend)
        }
    }

    /**
     * 只计算并发布 extend 自身 STATUS。
     *
     * `extend` 是仓颉特有的成员容器节点，LL STATUS resolver 需要在写锁中直接调用该入口，
     * 避免 generic `transformSingle` 路径被 phase 状态短路。
     */
    fun transformExtendStatusWithoutPhaseGuard(extend: CfirExtend) {
        extend.transformTypeParameters(this, null)
        extend.replaceStatus(statusResolver.resolveStatus(extend, containingClass, isLocal = false))
    }

    /** 推进普通函数节点的 STATUS。 */
    override fun transformFunction(function: CfirFunction, data: Nothing?): CfirFunction {
        return transformFunctionStatus(function)
    }

    /** 推进 main 函数节点的 STATUS。 */
    override fun transformMainFunction(mainFunction: CfirMainFunction, data: Nothing?): CfirMainFunction {
        return transformFunctionStatus(mainFunction) as CfirMainFunction
    }

    /** 推进宏声明函数节点的 STATUS。 */
    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: Nothing?): CfirMacroDeclaration {
        return transformFunctionStatus(macroDeclaration) as CfirMacroDeclaration
    }

    /** 推进 finalizer 函数节点的 STATUS。 */
    override fun transformFinalizer(finalizer: CfirFinalizer, data: Nothing?): CfirFinalizer {
        return transformFunctionStatus(finalizer) as CfirFinalizer
    }

    /**
     * 处理仓颉特有的 `CfirFunction` 直接子类。
     *
     * Kotlin FIR 的普通函数统一落在 `FirNamedFunction`，而仓颉 CFIR 还存在 main/macro/finalizer 等
     * 非 `CfirNamedFunction` 的函数节点；这些节点没有 overridden callable 语义，但仍必须发布 resolved status。
     */
    fun transformFunctionStatus(function: CfirFunction): CfirFunction {
        return withResolvedStatusPhase(function) {
            transformFunctionStatusWithoutPhaseGuard(function)
        }
    }

    /**
     * 只计算并发布函数自身 STATUS。
     *
     * 仓颉存在 `main` / macro / finalizer 等非 `CfirNamedFunction` 的函数节点；LL STATUS resolver
     * 需要像 Kotlin 的 named function 专门路径一样，在写锁中直接调用该入口。
     */
    fun transformFunctionStatusWithoutPhaseGuard(function: CfirFunction) {
        function.replaceStatus(statusResolver.resolveStatus(function, containingClass, isLocal = false))
        function.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    /** 推进具名函数 STATUS，并在解析前收集直接覆盖的函数状态。 */
    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?): CfirNamedFunction {
        return withResolvedStatusPhase(namedFunction) {
            val overriddenFunctions = statusResolver.getOverriddenFunctions(namedFunction, containingClass)
            transformNamedFunction(namedFunction, overriddenFunctions)
        }
    }

    /** 根据已确定的覆盖函数列表计算具名函数 STATUS。 */
    fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        overriddenFunctions: List<CfirNamedFunction>,
    ) {
        val overriddenStatuses = overriddenFunctions.mapNotNull { it.status as? CfirResolvedDeclarationStatus }
        namedFunction.replaceStatus(
            statusResolver.resolveStatus(
                namedFunction,
                containingClass,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )
        namedFunction.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    /** 推进普通构造器 STATUS，并同步发布构造器参数状态。 */
    override fun transformConstructor(constructor: CfirConstructor, data: Nothing?): CfirConstructor {
        return withResolvedStatusPhase(constructor) {
            constructor.replaceStatus(statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
            constructor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
        }
    }

    /** 推进 enum constructor STATUS，并同步发布构造器参数状态。 */
    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: Nothing?): CfirEnumConstructor {
        return withResolvedStatusPhase(enumConstructor) {
            enumConstructor.replaceStatus(statusResolver.resolveStatus(enumConstructor, containingClass, isLocal = false))
            enumConstructor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
        }
    }

    /** 推进属性 STATUS，并在解析前收集直接覆盖的属性状态。 */
    override fun transformProperty(property: CfirProperty, data: Nothing?): CfirProperty {
        return withResolvedStatusPhase(property) {
            val overriddenProperties = statusResolver.getOverriddenProperties(property, containingClass)
            transformProperty(property, overriddenProperties)
        }
    }

    /**
     * 根据已确定的覆盖属性列表计算属性及访问器 STATUS。
     *
     * setter 会单独继承被覆盖 setter 的状态集合，getter 使用属性自身 resolved status。
     */
    fun transformProperty(
        property: CfirProperty,
        overriddenProperties: List<CfirProperty>,
    ) {
        val overriddenStatuses = overriddenProperties.mapNotNull { it.status as? CfirResolvedDeclarationStatus }
        val overriddenSetters = overriddenProperties.mapNotNull { overriddenProperty ->
            overriddenProperty.setter?.status as? CfirResolvedDeclarationStatus
        }
        property.replaceStatus(
            statusResolver.resolveStatus(
                property,
                containingClass,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )

        property.getter?.let { transformPropertyAccessor(it, property) }
        property.setter?.let { transformPropertyAccessor(it, property, overriddenSetters) }
    }

    /**
     * 计算并发布属性访问器 STATUS。
     *
     * 访问器默认继承所属属性的可见性和模态上下文，setter 可额外接收覆盖链中的访问器状态。
     */
    protected fun transformPropertyAccessor(
        propertyAccessor: CfirPropertyAccessor,
        containingProperty: CfirProperty,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ) {
        propertyAccessor.replaceStatus(
            statusResolver.resolveStatus(
                propertyAccessor,
                containingClass,
                containingProperty,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )
        propertyAccessor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    /** 访问器被单独访问时，回到所属属性统一发布属性与访问器状态。 */
    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: Nothing?): CfirPropertyAccessor {
        transformProperty(propertyAccessor.propertySymbol.cfir, data)
        return propertyAccessor
    }

    /** 推进字段变量 STATUS。 */
    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: Nothing?): CfirFieldVariable {
        return withResolvedStatusPhase(fieldVariable) {
            transformVariableStatusWithoutPhaseGuard(fieldVariable)
        }
    }

    /** 推进模式绑定变量 STATUS。 */
    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: Nothing?,
    ): CfirPatternBindingVariable {
        return withResolvedStatusPhase(patternBindingVariable) {
            transformVariableStatusWithoutPhaseGuard(patternBindingVariable)
        }
    }

    /** 推进模式变量 STATUS。 */
    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: Nothing?,
    ): CfirPatternVariable {
        return withResolvedStatusPhase(patternVariable) {
            transformVariableStatusWithoutPhaseGuard(patternVariable)
        }
    }

    /** value parameter 的通用遍历入口；参数状态由 callable 专用路径集中发布。 */
    override fun transformValueParameter(valueParameter: CfirValueParameter, data: Nothing?): CfirValueParameter {
        return withResolvedStatusPhase(valueParameter) {}
    }

    /**
     * 只发布 value parameter 的 STATUS。
     *
     * LL STATUS resolver 的 callable 专门路径在写锁中直接调用 callable helper；
     * 参数不能再回到 generic phase guard，否则会因为宿主 callable 已推进 phase 而跳过 status 发布。
     */
    fun transformValueParameterStatusWithoutPhaseGuard(valueParameter: CfirValueParameter) {
        valueParameter.publishResolvedStatusIfNeeded()
        valueParameter.replaceResolvePhase(CfirResolvePhase.STATUS)
        statusComputationSession.endComputing(valueParameter)
    }

    /**
     * 只计算并发布变量自身 STATUS。
     *
     * 仓颉的 pattern variable / pattern binding variable 是可被符号恢复直接消费的
     * `CfirVariable`，但 Kotlin FIR 没有同名节点；它们在 STATUS 阶段对位 Kotlin
     * 普通 callable 的“仅发布自身 status”路径，不能依赖通用 declaration fallback。
     */
    fun transformVariableStatusWithoutPhaseGuard(variable: CfirVariable) {
        variable.replaceStatus(statusResolver.resolveStatus(variable, containingClass, isLocal = false))
    }

    /** 推进类型参数 bounds 并发布类型参数 STATUS phase。 */
    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: Nothing?): CfirTypeParameter {
        if (typeParameter.resolvePhase < CfirResolvePhase.TYPES || typeParameter.resolvePhase >= CfirResolvePhase.STATUS) {
            return typeParameter
        }
        typeParameter.transformBounds(this, null)
        typeParameter.replaceResolvePhase(CfirResolvePhase.STATUS)
        return typeParameter
    }
}

/**
 * 主干 STATUS 阶段使用的具体转换器。
 *
 * 当前没有额外状态，只固定继承 [AbstractCfirStatusResolveTransformer] 的完整行为。
 */
open class CfirStatusResolveTransformer(
    statusComputationSession: CfirStatusComputationSession,
) : AbstractCfirStatusResolveTransformer(
    statusComputationSession = statusComputationSession,
) 

/**
 * 在声明仍持有 raw status 时发布 resolved status。
 *
 * 该 helper 保留 raw status 中已经解析出的显式标志，只补齐 STATUS 阶段必须稳定的 modality。
 */
private fun CfirMemberDeclaration.publishResolvedStatusIfNeeded() {
    val currentStatus = status
    if (currentStatus is CfirResolvedDeclarationStatus) return

    if (this is CfirValueParameter) {
        // value parameter 仍沿用 statusless declaration 约定。
        replaceStatus(currentStatus.resolvedForStatuslessDeclaration())
        return
    }

    val currentModality = currentStatus.modality
        ?: error("Status modality must be initialized before publishing STATUS for ${this::class.simpleName}")

    replaceStatus(
        buildResolvedDeclarationStatus {
            source = currentStatus.source
            visibility = currentStatus.visibility
            isVisibilityExplicit = currentStatus.isVisibilityExplicit
            isModalityExplicit = currentStatus.isModalityExplicit
            isAbstractExplicit = currentStatus.isAbstractExplicit
            isOverride = currentStatus.isOverride
            isOperator = currentStatus.isOperator
            isStatic = currentStatus.isStatic
            isConst = currentStatus.isConst
            isMut = currentStatus.isMut
            isUnsafe = currentStatus.isUnsafe
            isForeign = currentStatus.isForeign
            isCommon = currentStatus.isCommon
            isSpecific = currentStatus.isSpecific
            isRedef = currentStatus.isRedef
            isDefault = currentStatus.isDefault
            isAbstract = currentStatus.isAbstract
            isOpen = currentStatus.isOpen
            isSealed = currentStatus.isSealed
            modality = currentModality
        }
    )
}

/**
 * 仓颉 STATUS 主干对位 Kotlin `FirStatusResolver` 的最小同构实现。
 *
 * 当前仓颉 tree 没有 Kotlin 的 `Unknown visibility/modality` raw 状态。raw builder 已按仓颉语法
 * 把普通声明缺省可见性物化为 `internal`、interface 成员物化为 `public`；STATUS 只能解析模态等
 * 尚未稳定的事实，不能把未显式写可见性的 override/redef 改写成父成员可见性。
 */
class CfirStatusResolver(
    /** 查询符号、scope 和可见性规则时使用的会话。 */
    private val session: CfirSession,
    /** 查询成员 scope 时复用的 scope 缓存会话。 */
    private val scopeSession: ScopeSession,
) {

    /** 查询属性在当前 class-like 容器中的直接覆盖属性。 */
    fun getOverriddenProperties(
        property: CfirProperty,
        containingClass: CfirClassLikeDeclaration?,
    ): List<CfirProperty> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()
        val result = linkedSetOf<CfirProperty>()
        scope.processDirectOverriddenPropertiesWithBaseScope(property.symbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    /** 查询具名函数在当前 class-like 容器中的直接覆盖函数。 */
    fun getOverriddenFunctions(
        function: CfirNamedFunction,
        containingClass: CfirClassLikeDeclaration?,
    ): List<CfirNamedFunction> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()
        val result = linkedSetOf<CfirNamedFunction>()
        scope.processDirectOverriddenFunctionsWithBaseScope(function.symbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    /** 计算 class 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirClass,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 interface 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirInterface,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 struct 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirStruct,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 enum 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirEnum,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 typealias 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirTypeAlias,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算非具名函数类声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirFunction,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算普通构造器声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirConstructor,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 enum constructor 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirEnumConstructor,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 根据覆盖属性状态计算属性声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirProperty,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 根据覆盖函数状态计算具名函数的 resolved status。 */
    fun resolveStatus(
        declaration: CfirNamedFunction,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 根据所属属性和覆盖访问器状态计算属性访问器的 resolved status。 */
    fun resolveStatus(
        declaration: CfirPropertyAccessor,
        containingClass: CfirClassLikeDeclaration?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = containingProperty,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 计算变量类声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirVariable,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 extend 容器声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirExtend,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /**
     * resolved status 的统一构造入口。
     *
     * 该函数保留 raw status 中的显式修饰符标志，并按声明种类、容器和覆盖状态补齐默认可见性与模态。
     */
    private fun resolveStatus(
        declaration: CfirDeclaration,
        status: CfirDeclarationStatus,
        containingClass: CfirClassLikeDeclaration?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ): CfirResolvedDeclarationStatus {
        if (status is CfirResolvedDeclarationStatus) return status

        val visibility = if (status.isVisibilityExplicit) {
            status.visibility
        } else {
            resolveVisibility(declaration, status, containingProperty, overriddenStatuses, isLocal)
        }

        val effectiveFlags = effectiveStatusFlags(status, declaration, containingClass)

        val modality = if (effectiveFlags.isModalityExplicit) {
            effectiveFlags.modality ?: resolveModality(
                declaration,
                containingProperty,
                containingClass,
                effectiveFlags.isOverride,
            )
        } else {
            resolveModality(
                declaration,
                containingProperty,
                containingClass,
                effectiveFlags.isOverride,
            )
        }

        val resolvedIsAbstract = effectiveFlags.isAbstract ||
                declaration is CfirCallableDeclaration && modality == Modality.ABSTRACT

        return buildResolvedDeclarationStatus {
            source = status.source
            this.visibility = visibility
            isVisibilityExplicit = status.isVisibilityExplicit
            isModalityExplicit = effectiveFlags.isModalityExplicit
            isAbstractExplicit = status.isAbstractExplicit
            isOverride = effectiveFlags.isOverride
            isOperator = effectiveFlags.isOperator
            isStatic = effectiveFlags.isStatic
            isConst = status.isConst
            isMut = status.isMut
            isUnsafe = status.isUnsafe
            isForeign = status.isForeign
            isCommon = status.isCommon
            isSpecific = status.isSpecific
            isRedef = status.isRedef
            isDefault = status.isDefault
            isAbstract = resolvedIsAbstract
            isOpen = effectiveFlags.isOpen
            isSealed = status.isSealed
            this.modality = modality
        }
    }

    /**
     * 解析默认可见性。
     *
     * 局部声明固定为 local，访问器继承所属属性；其它声明保留 raw builder / deserializer
     * 已按仓颉语法写入的默认可见性。与 Kotlin 的 Unknown raw visibility 不同，仓颉 override/redef
     * 未写可见性时 raw status 已是 internal，不能继承父成员的 public/protected。
     */
    private fun resolveVisibility(
        declaration: CfirDeclaration,
        status: CfirDeclarationStatus,
        containingProperty: CfirProperty?,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
        isLocal: Boolean,
    ) = when {
        isLocal -> Visibilities.Local
        declaration is CfirPropertyAccessor && containingProperty != null -> containingProperty.status.visibility
        else -> status.visibility
    }

    /**
     * 解析默认模态。
     *
     * class-like、顶层 callable、interface 成员和 override 成员各自遵循不同默认规则。
     */
    private fun resolveModality(
        declaration: CfirDeclaration,
        containingProperty: CfirProperty?,
        containingClass: CfirClassLikeDeclaration?,
        isOverride: Boolean,
    ): Modality {
        return when (declaration) {
            is CfirInterface -> Modality.ABSTRACT
            is CfirClass, is CfirStruct, is CfirEnum -> Modality.FINAL
            is CfirCallableDeclaration -> {
                val containingPropertyModality = containingProperty?.status?.modality
                when {
                    containingClass == null -> Modality.FINAL
                    declaration is CfirPropertyAccessor && containingPropertyModality != null -> containingPropertyModality
                    containingClass is CfirInterface -> {
                        when {
                            declaration.status.visibility == Visibilities.Private -> Modality.FINAL
                            !declaration.hasOwnBodyOrAccessorBody() -> Modality.ABSTRACT
                            else -> Modality.OPEN
                        }
                    }

                    isOverride -> Modality.OPEN
                    else -> Modality.FINAL
                }
            }

            else -> Modality.FINAL
        }
    }

    /**
     * 将 raw declaration status 投影为后续语义阶段可消费的有效状态。
     *
     * Raw CFIR 忠实保留源码的全部 modifier，供 modifier checker 报告组合或目标错误；
     * STATUS phase 则必须移除不能共同形成成员语义的状态位。这样继承、open 和缺失函数体
     * checker 都只消费同一个 resolved status，不再各自处理修饰符组合。
     */
    private fun effectiveStatusFlags(
        status: CfirDeclarationStatus,
        declaration: CfirDeclaration,
        containingClass: CfirClassLikeDeclaration?,
    ): EffectiveStatusFlags {
        val incompatibleStaticOverride = status.isStatic && status.isOverride
        val incompatibleStaticOpen = status.isStatic && status.isOpen
        val incompatibleStaticOperator = status.isStatic && status.isOperator
        val invalidStaticAbstractClassMember =
            declaration is CfirCallableDeclaration &&
                    containingClass is CfirClass &&
                    status.isStatic &&
                    status.isAbstractExplicit &&
                    !status.isForeign

        val effectiveStatic = status.isStatic &&
                !incompatibleStaticOverride &&
                !incompatibleStaticOpen &&
                !incompatibleStaticOperator
        val effectiveOverride = status.isOverride && !incompatibleStaticOverride
        val effectiveOpen = status.isOpen && !incompatibleStaticOpen
        val effectiveOperator = status.isOperator && !incompatibleStaticOperator
        val effectiveAbstract = status.isAbstract && !invalidStaticAbstractClassMember
        val effectiveModalityExplicit = status.isModalityExplicit &&
                !(status.isOpen && !effectiveOpen || status.isAbstract && !effectiveAbstract)
        val effectiveModality = status.modality?.takeIf {
            effectiveModalityExplicit
        }

        return EffectiveStatusFlags(
            isStatic = effectiveStatic,
            isOverride = effectiveOverride,
            isOperator = effectiveOperator,
            isAbstract = effectiveAbstract,
            isOpen = effectiveOpen,
            isModalityExplicit = effectiveModalityExplicit,
            modality = effectiveModality,
        )
    }

    /** STATUS phase 中 raw status 的有效成员修饰符视图。 */
    private data class EffectiveStatusFlags(
        val isStatic: Boolean,
        val isOverride: Boolean,
        val isOperator: Boolean,
        val isAbstract: Boolean,
        val isOpen: Boolean,
        val isModalityExplicit: Boolean,
        val modality: Modality?,
    )
}

/**
 * 读取声明当前 status，如果该声明类型本身不携带 status 则返回 null。
 *
 * 该函数仅作为默认可见性回退使用，不负责发布或推进 STATUS phase。
 */
private fun CfirDeclaration.statusOrNull(): CfirDeclarationStatus? {
    return when (this) {
        is CfirClass -> status
        is CfirInterface -> status
        is CfirStruct -> status
        is CfirEnum -> status
        is CfirFunction -> status
        is CfirProperty -> status
        is CfirEnumConstructor -> status
        is CfirVariable -> status
        is CfirExtend -> status
        is CfirTypeAlias -> status
        else -> null
    }
}

/**
 * 判断 callable 是否拥有自身实现体或属性访问器实现体。
 *
 * interface 成员默认模态需要区分抽象成员与带默认实现的成员。
 */
private fun CfirDeclaration.hasOwnBodyOrAccessorBody(): Boolean {
    return when (this) {
        is CfirFunction -> body != null
        is CfirProperty -> getter?.body != null || setter?.body != null
        else -> true
    }
}
