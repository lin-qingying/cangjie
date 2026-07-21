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

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.createParametersSubstitutor
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.containingAccessibleExtendOrNull
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutionMap
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitutionForConstraintDerivation
import org.cangnova.cangjie.cfir.resolve.providers.isBareOrDeclarationSelfTypeOf
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 对齐 Kotlin K2 FIR 的 `CreateFreshTypeVariableSubstitutorStage`。
 *
 * 本阶段负责初始化候选的约束系统：
 * - 为声明的类型参数创建新鲜推断类型变量
 * - 在约束系统中注册这些变量
 * - 添加类型参数的上界约束（declared upper bounds）
 * - 处理显式类型实参的相等约束
 *
 * 后续阶段（如 CfirCheckArguments）可在此基础上添加参数/期望类型约束并完成推断。
 */
object CfirCreateFreshTypeVariableSubstitutorStage : ResolutionStage() {
    /**
     * 初始化候选的 fresh type variable substitutor 和初始约束。
     */
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.cfir
        val enumConstructorReceiverOwnerTypeParameters =
            collectEnumConstructorReceiverOwnerTypeParameters(context.session, candidate, declaration)
        val enumConstructorReceiverOwnerLookupTags =
            enumConstructorReceiverOwnerTypeParameters.mapTo(mutableSetOf()) { it.symbol.toLookupTag() }
        val bareStaticExtendTypeParameters =
            collectBareStaticQualifierExtendTypeParameters(context.session, candidate, declaration)
        val knownOwnerSubstitutions = buildMap {
            if (!candidate.hasScopeOwnedInstanceMemberSubstitution(declaration) &&
                bareStaticExtendTypeParameters.isEmpty()
            ) {
                putAll(
                    createCallableOwnerUseSiteSubstitutionMap(
                        session = context.session,
                        callableSymbol = candidate.symbol as? CfirCallableSymbol<*>,
                        receiverType = candidate.useSiteReceiverType(),
                    ).filterKeys { it !in enumConstructorReceiverOwnerLookupTags }
                )
            }
            putAll(createBareStaticExtendTargetOwnerSubstitutionMap(context.session, candidate, declaration))
            putAll(createBareEnumConstructorQualifierOwnerSubstitutionMap(context.session, candidate, declaration))
            putAll(collectOriginScopeOwnerSubstitutions(context.session, candidate, declaration))
        }
        val typeParameters = (
                enumConstructorReceiverOwnerTypeParameters +
                        collectCandidateTypeParametersForFreshVariables(
                            context.session,
                            candidate,
                            declaration,
                            bareStaticExtendTypeParameters,
                        )
                )
            .distinctBy { it.symbol }
            .filterNot { typeParameter -> typeParameter.symbol.toLookupTag() in knownOwnerSubstitutions }
        if (typeParameters.isEmpty()) {
            val substitutor = knownOwnerSubstitutions
                .takeIf { it.isNotEmpty() }
                ?.let(::CfirTypeSubstitutorByMap)
                ?: ConeSubstitutor.Empty
            candidate.initializeSubstitutorAndVariables(substitutor, emptyList())
            return
        }

        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(
                declaration,
                context.session,
                typeParameters,
                knownOwnerSubstitutions,
                csBuilder,
        )
        candidate.initializeSubstitutorAndVariables(substitutor, freshVariables)

        // 声明侧存在矛盾（如上界冲突）——直接标记不可用
        if (csBuilder.hasContradiction) {
            sink.reportDiagnostic(InferenceConstraintError("declaration has contradicting upper bounds"))
            return
        }

        // 无显式类型实参时可提前返回
        if (candidate.typeArgumentMapping == TypeArgumentMapping.NoExplicitArguments) {
            return
        }

        // 处理显式类型实参：添加相等约束
        for (index in typeParameters.indices) {
            val freshVariable = freshVariables[index]
            val typeArgument = candidate.typeArgumentMapping[index]
            val argumentType = typeArgument.type

            if (argumentType is ConePlaceholderType) {
                // 占位符投影——不添加约束，等待推断
                continue
            }

            val sourceTypeArgument = candidate.typeArgumentMapping.sourceTypeRef(index)
                ?: continue

            csBuilder.addEqualityConstraint(
                freshVariable.defaultType,
                argumentType,
                ConeExplicitTypeParameterConstraintPosition(sourceTypeArgument),
            )
        }

        if (csBuilder.hasContradiction) {
            for (error in csBuilder.errors) {
                sink.reportDiagnostic(InferenceConstraintError(error.toString()))
            }
        }
    }

    /**
     * 为声明的类型参数创建新鲜类型变量，构建替代器，并添加上界约束。
     */
    private fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        declaration: Any?,
        session: CfirSession,
        typeParameters: List<CfirTypeParameterRef>,
        knownSubstitutions: Map<TypeConstructorMarker, ConeCangJieType>,
        csBuilder: ConstraintSystemOperation,
    ): Pair<ConeSubstitutor, List<ConeTypeVariable>> {
        val freshTypeVariables = typeParameters.map { ConeTypeParameterBasedTypeVariable(it.symbol) }

        val useSiteSubstitutor = knownSubstitutions
            .takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeSubstitutorByMap)
        val freshVariableSubstitutor = CfirTypeSubstitutorByMap(
            buildFreshVariableSubstitutionMap(declaration, freshTypeVariables)
        )
        val typeAliasConstructorSubstitutor = (declaration as? CfirConstructor)
            ?.typeAliasConstructorInfo
            ?.substitutor
        val toFreshVariables = chainSubstitutors(
            typeAliasConstructorSubstitutor,
            useSiteSubstitutor,
            freshVariableSubstitutor,
        )

        // 在约束系统中注册所有新鲜变量
        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        for ((lower, upper) in collectInitialUpperBoundConstraints(
            declaration = declaration,
            session = session,
            toFreshVariables = toFreshVariables,
            freshTypeVariables = freshTypeVariables,
        )) {
            csBuilder.addSubtypeConstraint(lower, upper, ConeDeclaredUpperBoundConstraintPosition)
        }

        return toFreshVariables to freshTypeVariables
    }

    /**
     * 候选签名中的 owner 类型参数可能已经被 use-site scope 替换成外来 fresh variable。
     *
     * 例如 `T1.a16(i16)` 会先把 enum constructor receiver `T1` 解析成 `Test<X>`，
     * 随后成员 scope 中的 `a16(x: Array<T>)` 签名可能表现为 `Array<X>`。外层成员候选
     * 会重新为 owner `T` 创建当前约束系统内的 fresh variable；这里把签名里遗留的
     * `T` lookup tag 和外来 `X` constructor 都别名到同一个当前 fresh variable，保证
     * 参数、receiver、返回值和 expected type 约束进入同一个候选约束系统。
     */
    private fun buildFreshVariableSubstitutionMap(
        declaration: Any?,
        freshTypeVariables: List<ConeTypeParameterBasedTypeVariable>,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        val substitution = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
        val freshBySymbol = freshTypeVariables.associateBy { it.typeParameterSymbol }
        val uniqueFreshByName = freshTypeVariables
            .groupBy { it.typeParameterSymbol.name }
            .mapNotNull { (name, variables) -> variables.singleOrNull()?.let { name to it } }
            .toMap()

        fun freshVariableFor(lookupTag: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag): ConeTypeParameterBasedTypeVariable? {
            return freshBySymbol[lookupTag.typeParameterSymbol]
                ?: uniqueFreshByName[lookupTag.typeParameterSymbol.name]
        }

        for (freshVariable in freshTypeVariables) {
            substitution[freshVariable.typeParameterSymbol.toLookupTag()] = freshVariable.defaultType as ConeCangJieType
        }

        val callable = declaration as? CfirCallableDeclaration
        if (callable != null) {
            val valueParameterTypes = when (callable) {
                is CfirFunction -> callable.valueParameters.mapNotNull { it.returnTypeRef.coneTypeOrNull }
                is CfirConstructor -> callable.valueParameters.mapNotNull { it.returnTypeRef.coneTypeOrNull }
                is CfirEnumConstructor -> callable.valueParameters.mapNotNull { it.returnTypeRef.coneTypeOrNull }
                else -> emptyList()
            }
            val signatureTypes = valueParameterTypes +
                    listOfNotNull(callable.returnTypeRef.coneTypeOrNull, callable.dispatchReceiverType)
            for (type in signatureTypes) {
                type.collectTypeParameterAliases { aliasConstructor, originalLookupTag ->
                    val freshVariable = freshVariableFor(originalLookupTag) ?: return@collectTypeParameterAliases
                    substitution.putIfAbsent(aliasConstructor, freshVariable.defaultType as ConeCangJieType)
                }
            }
        }

        return substitution
    }

    /**
     * 遍历类型中出现的类型参数别名。
     */
    private fun ConeCangJieType.collectTypeParameterAliases(
        consume: (
            aliasConstructor: TypeConstructorMarker,
            originalLookupTag: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag,
        ) -> Unit,
    ) {
        when (this) {
            is ConeTypeParameterType -> consume(lookupTag, lookupTag)
            is ConeTypeVariableType -> {
                val originalLookupTag =
                    typeConstructor.originalTypeParameter as? org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
                        ?: return
                consume(typeConstructor, originalLookupTag)
                consume(originalLookupTag, originalLookupTag)
            }
            is ConeLookupTagBasedType -> typeArguments.forEach { it.type.collectTypeParameterAliases(consume) }
            is ConeTypeAliasType -> {
                expandedType?.collectTypeParameterAliases(consume)
                typeArguments.forEach { it.type.collectTypeParameterAliases(consume) }
            }
            is ConeFunctionType -> {
                parameterTypes.forEach { it.collectTypeParameterAliases(consume) }
                returnType.collectTypeParameterAliases(consume)
            }
            is ConeTupleType -> elementTypes.forEach { it.collectTypeParameterAliases(consume) }
            is ConeVArrayType -> elementType.collectTypeParameterAliases(consume)
            is ConePointerType -> pointeeType.collectTypeParameterAliases(consume)
            is ConeIntersectionType -> intersectedTypes.forEach { it.collectTypeParameterAliases(consume) }
            is ConeUnionType -> unionTypes.forEach { it.collectTypeParameterAliases(consume) }
            else -> Unit
        }
    }

    /**
     * typealias 构造器的 bounds 来自展开后真实类的类型参数。
     *
     * 对齐 Kotlin `CreateFreshTypeVariableSubstitutorStage.addConstraintsProperly`：
     * `type A<T> = Cl<Option<T>>` 调用 `A<X>()` 时，约束系统必须检查
     * `Option<X>` 是否满足 `Cl` 的声明上界，而不是只看 `A` 自身是否声明上界。
     */
    private fun collectInitialUpperBoundConstraints(
        declaration: Any?,
        session: CfirSession,
        toFreshVariables: ConeSubstitutor,
        freshTypeVariables: List<ConeTypeParameterBasedTypeVariable>,
    ): List<Pair<ConeCangJieType, ConeCangJieType>> {
        val typeAliasConstructorInfo = (declaration as? CfirConstructor)?.typeAliasConstructorInfo
        if (typeAliasConstructorInfo != null) {
            val expandedReturnType = declaration.returnTypeRef.coneTypeOrNull
                ?.fullyExpandedType(session) as? ConeLookupTagBasedType
                ?: return emptyList()
            val expandedDeclaration = (expandedReturnType.toSymbol(session) as? CfirClassLikeSymbol<*>)?.cfir
                ?: return emptyList()
            val expandedArguments = expandedReturnType.typeArguments.map { argument ->
                toFreshVariables.substituteOrSelf(argument.type)
            }

            return buildList {
                for ((index, parameter) in expandedDeclaration.typeParameters.withIndex()) {
                    val argumentType = expandedArguments.getOrNull(index) ?: continue
                    for (bound in parameter.symbol.toDeclaredUpperBoundTypes(session)) {
                        val upperBound = toFreshVariables.substituteOrSelf(bound)
                        val extendConstraints = collectExtendDerivedUpperBoundConstraints(session, argumentType, upperBound)
                        if (extendConstraints != null) {
                            addAll(extendConstraints)
                        } else {
                            add(argumentType to upperBound)
                        }
                    }
                }
            }
        }

        return buildList {
            for (freshVariable in freshTypeVariables) {
                for (bound in freshVariable.typeParameterSymbol.toDeclaredUpperBoundTypes(session)) {
                    add(freshVariable.defaultType to toFreshVariables.substituteOrSelf(bound))
                }
            }
        }
    }

    /**
     * 顺序组合两个 CFIR substitutor。
     */
    private class ChainedCfirSubstitutor(
        /**
         * 先执行的 substitutor。
         */
        private val first: ConeSubstitutor,
        /**
         * 后执行的 substitutor。
         */
        private val second: ConeSubstitutor,
    ) : ConeSubstitutor() {
        /**
         * 对类型依次应用两个 substitutor。
         */
        override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType {
            return second.substituteOrSelf(first.substituteOrSelf(type))
        }

        /**
         * 对类型执行可空替换，并保留任一 substitutor 的有效结果。
         */
        override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
            val afterFirst = first.substituteOrNull(type)
            val afterSecond = second.substituteOrNull(afterFirst ?: type)
            return afterSecond ?: afterFirst
        }

        /**
         * 对类型实参投影依次应用两个 substitutor。
         */
        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
            val afterFirst = first.substituteArgument(projection, index)
            val afterSecond = second.substituteArgument(afterFirst ?: projection, index)
            return afterSecond ?: afterFirst
        }
    }

    /**
     * 将多个 substitutor 串联为一个 substitutor。
     */
    private fun chainSubstitutors(vararg substitutors: ConeSubstitutor?): ConeSubstitutor {
        return substitutors
            .filterNotNull()
            .filterNot { it === ConeSubstitutor.Empty }
            .reduceOrNull { first, second -> ChainedCfirSubstitutor(first, second) }
            ?: ConeSubstitutor.Empty
    }

    /**
     * 将用户 extend 的条件父类型派生成约束系统可求解的 where 约束。
     *
     * 例如 `extend<T> Option<T> <: I where T <: I` 使 `Option<X> <: I`
     * 等价于初始约束 `X <: I`。标准库 `Array<T>` 不在这里下沉元素约束：
     * 官方 `cjc` 对 `GennericClassA<Array<Int64>, ...>` 的 typealias 构造
     * 不会因此要求 `Int64` 满足 `Array` 的 PrettyPrintable 派生条件。
     */
    private fun collectExtendDerivedUpperBoundConstraints(
        session: CfirSession,
        lowerType: ConeCangJieType,
        upperBound: ConeCangJieType,
    ): List<Pair<ConeCangJieType, ConeCangJieType>>? {
        val semanticLowerType = lowerType.fullyExpandedType(session)
        if (semanticLowerType.isArray) return null

        val candidateExtends = when (semanticLowerType) {
            is ConePrimitiveType -> session.extendProvider.getExtendsForBuiltinType(semanticLowerType.kind)
            else -> {
                val classId = semanticLowerType.classIdOrPrimitiveClassId ?: return null
                session.extendProvider.getExtendsForClass(classId)
            }
        }
        if (candidateExtends.isEmpty()) return null

        val constraints = mutableListOf<Pair<ConeCangJieType, ConeCangJieType>>()
        var matchedExtendSupertype = false
        for (extend in candidateExtends) {
            val targetPattern = resolveExtendTypeRef(session, extend, extend.extendedTypeRef) ?: continue
            val substitution = createExtendDeclarationSubstitutionForConstraintDerivation(
                session = session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = semanticLowerType,
            ) ?: continue

            val matchesUpperBound = extend.superTypeRefs.any { superTypeRef ->
                val extendSupertype = resolveExtendTypeRef(session, extend, superTypeRef)
                    ?.let(substitution.substitutor::substituteOrSelf)
                    ?: return@any false
                AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                    session.typeContext,
                    extendSupertype,
                    upperBound,
                )
            }
            if (!matchesUpperBound) continue

            matchedExtendSupertype = true
            for (typeParameter in extend.typeParameters) {
                typeParameter.symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
                val actualType = substitution.substitutor.substituteOrSelf(typeParameter.symbol.constructType())
                for (bound in typeParameter.symbol.toDeclaredUpperBoundTypes(session)) {
                    val substitutedBound = substitution.substitutor.substituteOrSelf(bound)
                    if (actualType !is ConeErrorType && substitutedBound !is ConeErrorType) {
                        constraints += actualType to substitutedBound
                    }
                }
            }
        }
        return constraints.takeIf { matchedExtendSupertype }
    }

    /**
     * 提取可参与调用约束求解的声明上界。
     *
     * 非 class/interface 等非法声明上界由声明 checker 报告；这里不能通过
     * `resolvedBounds` 强制读取，否则非法 function/tuple 上界会在进入诊断前触发内部异常。
     */
    private fun CfirTypeParameterSymbol.toDeclaredUpperBoundTypes(session: CfirSession): List<ConeCangJieType> {
        lazyResolveToPhase(CfirResolvePhase.TYPES)
        val bounds = toLookupTag()
            .declaredUpperBoundRefsAfterTypeResolve()
            .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
            .filterNot { it is ConeErrorType }
        val effectiveBounds = bounds.filter { bound -> bound.isLegalDeclaredUpperBound(session) }
            .ifEmpty { bounds }
        return effectiveBounds.filter { bound ->
            when (bound.fullyExpandedType(session)) {
                is ConeClassLikeType,
                is ConeEnumType,
                is ConeStructType,
                is ConePrimitiveType,
                -> true
                else -> false
            }
        }
    }

    /**
     * 在 extend 声明语境下解析目标或父类型引用。
     */
    private fun resolveExtendTypeRef(
        session: CfirSession,
        extend: CfirExtend,
        typeRef: CfirTypeRef,
    ): ConeCangJieType? {
        if (typeRef is CfirResolvedTypeRef) return typeRef.coneType

        return session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = CfirTypeResolutionConfiguration.EMPTY
                .withTopContainer(extend)
                .withAdditionalTypeParameters(extend.typeParameters),
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = false,
            supertypeSupplier = SupertypeSupplier.Default,
        ).type
    }

    /**
     * 取得候选 use-site receiver 的类型。
     */
    private fun Candidate.useSiteReceiverType(): ConeCangJieType? =
        dispatchReceiverExpression()?.coneTypeOrNull
            ?: chosenExtensionReceiverExpression()?.coneTypeOrNull
            ?: callInfo.explicitReceiver?.coneTypeOrNull

    /**
     * 判断普通实例成员的 owner substitution 是否已经由候选来源 scope 完整应用。
     *
     * [CfirClassSubstitutionScope] 产出的 substitution override 已拥有最终参数和返回签名；
     * fresh-variable 阶段若再 unwrap 到原声明并按 receiver 注入 owner map，会把签名中保留的
     * lexical generic 参数误当成原 owner 参数进行二次替换。构造器、enum constructor 与 static
     * 成员仍保留既有路径：它们可能来自 classifier/qualifier 调用，而非具体实例成员 scope。
     */
    private fun Candidate.hasScopeOwnedInstanceMemberSubstitution(declaration: Any?): Boolean {
        val callable = declaration as? CfirCallableDeclaration ?: return false
        if (callable.status.isStatic || callable is CfirConstructor || callable is CfirEnumConstructor) return false
        if (originScope !is CfirClassSubstitutionScope) return false

        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        return callableSymbol.unwrapSubstitutionOverrides() !== callableSymbol
    }

    /**
     * 收集产生候选的 substitution scope 已经确定的 constructor owner 映射。
     *
     * 这是“owner 已实例化”的结构事实：构造器 symbol 即使能回溯到原始声明，其 owner 类型参数
     * 也已经在 scope 层替换完成。把该映射并入候选初始 substitutor，可阻止后续 owner 回查把它们
     * 再次 fresh 化；普通 classifier 构造调用不来自该 scope，仍按原有调用推断规则处理。
     */
    private fun collectOriginScopeOwnerSubstitutions(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        if (declaration !is CfirConstructor) return emptyMap()
        val substitutionScope = candidate.originScope as? CfirClassSubstitutionScope ?: return emptyMap()
        val ownerClassId = ownerClassIdForCallable(session, candidate) ?: return emptyMap()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyMap()
        val ownerTypeParameters = (ownerDeclaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()

        return ownerTypeParameters.mapNotNull { typeParameter ->
            val substitutedType = substitutionScope
                .substitutedOwnerTypeParameterOrNull(typeParameter.symbol)
                ?: return@mapNotNull null
            typeParameter.symbol.toLookupTag() to substitutedType
        }.toMap()
    }

    /**
     * 返回调用候选参与显式类型实参映射和 fresh-variable 初始化的类型参数。
     *
     * 普通函数使用声明自身类型参数；构造器调用使用 owner class/enum 的类型参数。
     * 该集合必须和 `CfirMapTypeArguments` 的计数逻辑保持一致，否则
     * `Array<Int64>(...)` 这类构造器显式类型实参会先被误判为数量错误。
     */
    internal fun collectCandidateTypeParametersForFreshVariables(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
        precomputedBareStaticExtendTypeParameters: List<CfirTypeParameterRef>? = null,
    ): List<CfirTypeParameterRef> {
        val ownTypeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if ((declaration as? CfirConstructor)?.typeAliasConstructorInfo != null) {
            return ownTypeParameters
        }

        val extendTypeParameters = precomputedBareStaticExtendTypeParameters
            ?: collectBareStaticQualifierExtendTypeParameters(session, candidate, declaration)
        if (extendTypeParameters.isNotEmpty()) {
            if (candidate.callInfo.typeArguments.isEmpty()) {
                return extendTypeParameters + ownTypeParameters
            }
            if (ownTypeParameters.isEmpty()) {
                return extendTypeParameters
            }
        }
        val ownerTypeParameters = collectBareStaticQualifierOwnerTypeParameters(session, candidate, declaration)
        if (ownerTypeParameters.isNotEmpty()) {
            if (candidate.callInfo.typeArguments.isEmpty()) {
                return ownerTypeParameters + ownTypeParameters
            }
            if (ownTypeParameters.isEmpty()) {
                return ownerTypeParameters
            }
        }
        val typeVariableReceiverOwnerTypeParameters =
            collectTypeVariableReceiverOwnerTypeParameters(session, candidate, declaration)
        if (typeVariableReceiverOwnerTypeParameters.isNotEmpty()) {
            return typeVariableReceiverOwnerTypeParameters + ownTypeParameters
        }
        if (ownTypeParameters.isNotEmpty()) return ownTypeParameters
        if (declaration !is CfirConstructor &&
            declaration !is CfirEnumConstructor
        ) {
            return emptyList()
        }

        val receiver = candidate.bareStaticQualifierExpression()
        if (receiver != null && receiver.typeArguments.isEmpty()) {
            collectBareTypeAliasQualifierTypeParameters(receiver)?.let { return it + ownTypeParameters }
        }

        val ownerClassId = ownerClassIdForCallable(session, candidate)
            ?: return emptyList()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyList()

        return when (ownerDeclaration) {
            is CfirTypeParameterRefsOwner -> ownerDeclaration.typeParameters
            else -> emptyList()
        }
    }

    /**
     * static extend 成员在裸泛型类名上调用时，extend 声明自身的类型参数参与调用推断。
     *
     * 官方 `GetAllGenericTys` 会把 static member 所在外层泛型声明加入候选泛型集合；
     * 对 CFIR 而言，extend 成员的外层声明是 owner extend，而不是 qualifier class。
     */
    private fun collectBareStaticQualifierExtendTypeParameters(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): List<CfirTypeParameterRef> {
        val callable = declaration as? CfirCallableDeclaration ?: return emptyList()
        if (!callable.status.isStatic) return emptyList()

        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return emptyList()
        val ownerExtend = callableSymbol.containingAccessibleExtendOrNull(session) ?: return emptyList()

        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyList()
        if (receiver.typeArguments.isNotEmpty()) return emptyList()

        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyList()
        val extendTargetType = resolveExtendTypeRef(session, ownerExtend, ownerExtend.extendedTypeRef)
            ?.fullyExpandedType(session) as? ConeLookupTagBasedType
            ?: return emptyList()
        if (extendTargetType.typeArguments.isEmpty()) return emptyList()
        if (extendTargetType.classIdOrPrimitiveClassId != ownerSymbol.classId) return emptyList()

        return ownerExtend.typeParameters
    }

    /**
     * static extend 成员通过裸泛型 owner 调用时，把 extend 目标类型反映射到 owner 类型参数。
     *
     * 官方 `RelayMappingFromExtendToExtended` 会把 `extend E<Rune>` 中的目标实参固定为
     * `E` 的 owner 映射 `T -> Rune`；泛型 extend 则形成 `T -> R`，随后 `R` 再进入
     * 当前候选的 fresh-variable substitutor。该映射属于声明 owner 的结构事实，不能把
     * concrete extend 的 owner 参数再次当作无约束 fresh variable。
     */
    private fun createBareStaticExtendTargetOwnerSubstitutionMap(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        val callable = declaration as? CfirCallableDeclaration ?: return emptyMap()
        if (!callable.status.isStatic || callable is CfirConstructor || callable is CfirEnumConstructor) {
            return emptyMap()
        }
        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return emptyMap()
        val ownerExtend = callableSymbol.containingAccessibleExtendOrNull(session) ?: return emptyMap()
        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyMap()
        if (receiver.typeArguments.isNotEmpty()) return emptyMap()
        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyMap()

        val targetType = resolveExtendTypeRef(session, ownerExtend, ownerExtend.extendedTypeRef)
            ?.fullyExpandedType(session) as? ConeLookupTagBasedType
            ?: return emptyMap()
        if (targetType.classIdOrPrimitiveClassId != ownerSymbol.classId) return emptyMap()
        val ownerTypeParameters = (ownerSymbol.cfir as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if (ownerTypeParameters.size != targetType.typeArguments.size) return emptyMap()

        return ownerTypeParameters.zip(targetType.typeArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.toLookupTag() to argument.type
        }
    }

    /**
     * 泛型类型的静态成员可通过裸类名参与调用推断，例如 `Box.create()`。
     *
     * 官方 Cangjie 在调用路径为 owner class 的类型参数创建待推断变量；如果没有
     * 实参、返回类型或期望类型能约束这些变量，后续约束系统会报告“无法推断泛型实参”，
     * 而不是把 `Box` 当作类型位置的裸泛型类型诊断。
     */
    private fun collectBareStaticQualifierOwnerTypeParameters(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): List<CfirTypeParameterRef> {
        val callable = declaration as? CfirCallableDeclaration ?: return emptyList()
        if (callable is CfirConstructor || callable is CfirEnumConstructor) return emptyList()
        if (!callable.status.isStatic) return emptyList()

        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyList()
        if (receiver.typeArguments.isNotEmpty()) return emptyList()

        collectBareTypeAliasQualifierTypeParameters(receiver)?.let { return it }

        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyList()
        val ownerDeclaration = ownerSymbol.cfir
        val receiverType = receiver.coneTypeOrNull as? ConeLookupTagBasedType ?: return emptyList()
        if (!receiverType.isBareOrDeclarationSelfTypeOf(ownerSymbol)) return emptyList()
        return ownerDeclaration.typeParameters
    }

    /**
     * fresh type variable 接收者的成员候选需要把 owner 泛型参数纳入同一候选约束系统。
     *
     * 官方 `TryEnforceCandidate` 在泛型接收者候选上会用 placeholder tyvars 填充 type arguments；
     * 在 CFIR 中，这些 placeholder 对应 owner class-like type parameters 创建出的 fresh variables。
     */
    private fun collectTypeVariableReceiverOwnerTypeParameters(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): List<CfirTypeParameterRef> {
        val callable = declaration as? CfirCallableDeclaration ?: return emptyList()
        val dispatchReceiverExpression = candidate.dispatchReceiverExpression()
        val extensionReceiverExpression = candidate.chosenExtensionReceiverExpression()
        val receiverType = (dispatchReceiverExpression ?: extensionReceiverExpression)?.coneTypeOrNull as? ConeTypeVariableType
            ?: return emptyList()
        if (receiverType.typeConstructor.originalTypeParameter != null) return emptyList()

        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*>
        if (extensionReceiverExpression != null && dispatchReceiverExpression == null) {
            val ownerExtend = callableSymbol
                ?.unwrapSubstitutionOverrides()
                ?.let(session.extendProvider::getContainingExtend)
                ?.takeIf(session.extendProvider::isExtendAccessible)
            if (ownerExtend != null) {
                return ownerExtend.typeParameters
            }
        }

        if (callable.status.isStatic) return emptyList()
        val ownerClassId = ownerClassIdForCallable(session, candidate)
            ?: callableSymbol?.dispatchReceiverType
                ?.fullyExpandedType(session)
                ?.classIdOrPrimitiveClassId
            ?: return emptyList()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyList()

        return (ownerDeclaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
    }

    /**
     * 无参 enum constructor 作为成员 receiver 时，owner 泛型实参属于外层成员调用候选。
     *
     * `T1.a16(i16)` 这类调用中，`T1` 自身没有实参，真正能约束 enum owner `Test<T>` 的
     * 是成员 `a16` 的参数、返回值和外层 expected type。若把 `T1` 预解析时遗留的 foreign
     * fresh variable 当作已知 use-site substitution，成员候选的约束系统就无法完成该变量。
     * 因此这里为成员候选重新收集 owner 类型参数，让后续 dispatch receiver 检查把 `T1`
     * 定型到同一组 fresh variables 上。
     */
    private fun collectEnumConstructorReceiverOwnerTypeParameters(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): List<CfirTypeParameterRef> {
        val callable = declaration as? CfirCallableDeclaration ?: return emptyList()
        if (callable.status.isStatic || callable is CfirEnumConstructor) return emptyList()

        val ownerClassId = ownerClassIdForCallable(session, candidate)
            ?: return emptyList()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyList()
        val ownerTypeParameters = (ownerDeclaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if (ownerTypeParameters.isEmpty()) return emptyList()

        val receiver = candidate.enumConstructorMemberAccessReceiverExpression()
            ?: return emptyList()
        val isEnumReceiver = receiver.isNoArgEnumConstructorReceiverOf(session, ownerDeclaration)
        if (!isEnumReceiver) return emptyList()

        return ownerTypeParameters
    }

    /** 成员访问语法中的 enum constructor receiver，以调用点显式 receiver 为准。 */
    private fun Candidate.enumConstructorMemberAccessReceiverExpression(): CfirQualifiedAccessExpression? =
        callInfo.explicitReceiver as? CfirQualifiedAccessExpression
            ?: dispatchReceiverExpression() as? CfirQualifiedAccessExpression

    /**
     * 判断 member receiver 是否为当前 owner 的无参 enum constructor sugar。
     *
     * 外层成员候选创建 fresh variables 时，receiver 可能仍处在 `ContextDependent`
     * 的裸名状态，不能只依赖 resolved reference；已解析到非 enum symbol 时则必须拒绝，
     * 避免把普通变量/属性 receiver 当作 enum constructor。
     */
    private fun CfirQualifiedAccessExpression.isNoArgEnumConstructorReceiverOf(
        session: CfirSession,
        ownerDeclaration: CfirDeclaration,
    ): Boolean {
        if (typeArguments.isNotEmpty()) return false

        val symbol = enumConstructorSymbolOrNull()
        if (symbol != null) {
            val constructor = symbol.cfir
            val expectedOwnerClassId = (ownerDeclaration as? CfirClassLikeDeclaration)?.symbol?.classId
                ?: return false
            val constructorOwnerClassId = session.cfirProvider.getContainingClass(symbol)?.classId
                ?: return false
            return constructor.valueParameters.isEmpty() && constructorOwnerClassId == expectedOwnerClassId
        }

        // 只有尚未解析 symbol 的裸名 fallback 才能依赖当前 owner 的 lexical enum scope。
        if (explicitReceiver != null) return false
        val reference = calleeReference
        if (reference !is org.cangnova.cangjie.cfir.references.CfirNamedReference) return false
        if (reference is CfirResolvedNamedReference) return false
        if (reference is CfirNamedReferenceWithCandidateBase) return false

        val owner = ownerDeclaration as? CfirClassLikeDeclaration ?: return false
        return owner.declarations
            .asSequence()
            .filterIsInstance<CfirEnumConstructor>()
            .any { constructor ->
                constructor.name == reference.name && constructor.valueParameters.isEmpty()
            }
    }

    /** 提取 receiver 表达式已经解析到的 enum constructor symbol。 */
    private fun CfirQualifiedAccessExpression.enumConstructorSymbolOrNull(): CfirEnumConstructorSymbol? =
        when (val reference = calleeReference) {
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirEnumConstructorSymbol
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirEnumConstructorSymbol
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirEnumConstructorSymbol
            else -> null
        }

    /**
     * 从裸 typealias qualifier 中收集实际参与展开类型的类型参数。
     */
    private fun collectBareTypeAliasQualifierTypeParameters(
        receiver: CfirQualifiedAccessExpression,
    ): List<CfirTypeParameterRef>? {
        val typeAliasSymbol = receiver.resolvedQualifierTypeAliasSymbol() ?: return null
        val typeAlias = typeAliasSymbol.cfir
        if (typeAlias.typeParameters.isEmpty()) return emptyList()
        val expandedType = typeAlias.expandedTypeRef.coneTypeOrNull ?: return emptyList()
        return typeAlias.typeParameters.filter { parameter ->
            expandedType.referencesTypeParameter(parameter.symbol)
        }
    }

    /**
     * 从限定访问表达式中提取已解析的 typealias 符号。
     */
    private fun CfirQualifiedAccessExpression.resolvedQualifierTypeAliasSymbol(): CfirTypeAliasSymbol? {
        val resolvedReference = calleeReference as? CfirResolvedNamedReference ?: return null
        return resolvedReference.resolvedSymbol as? CfirTypeAliasSymbol
    }

    /**
     * 判断类型是否引用了指定类型参数符号。
     */
    private fun ConeCangJieType.referencesTypeParameter(symbol: CfirTypeParameterSymbol): Boolean = when (this) {
        is ConeTypeParameterType -> lookupTag.typeParameterSymbol == symbol
        is ConeLookupTagBasedType -> typeArguments.any { it.type.referencesTypeParameter(symbol) }
        is ConeTypeAliasType -> expandedType?.referencesTypeParameter(symbol) == true ||
            typeArguments.any { it.type.referencesTypeParameter(symbol) }
        else -> false
    }

    /**
     * enum constructor 通过裸 class/typealias qualifier 访问时，owner enum 的类型参数
     * 来自 qualifier 的 use-site 展开类型，而不是 enum constructor 自身声明。
     */
    private fun createBareEnumConstructorQualifierOwnerSubstitutionMap(
        session: CfirSession,
        candidate: Candidate,
        declaration: Any?,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        if (declaration !is CfirEnumConstructor) return emptyMap()

        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyMap()
        if (receiver.typeArguments.isEmpty() && receiver.resolvedQualifierTypeAliasSymbol() == null) {
            /*
             * 裸类名 enum qualifier（如 `TimeUnit.Year`）没有 use-site 类型实参。
             * owner 泛型必须作为 fresh variables 进入候选约束系统，由 payload 实参和 expected type 共同推断。
             */
            return emptyMap()
        }
        val expandedReceiverType = receiver.expandedTypeAliasQualifierType(session)
            ?: receiver.coneTypeOrNull
                ?.fullyExpandedType(session)
                ?.let { type ->
                    when (type) {
                        is ConeLookupTagBasedType -> type
                        is ConeTypeAliasType -> type.expandedType?.fullyExpandedType(session) as? ConeLookupTagBasedType
                        else -> null
                    }
                }
        expandedReceiverType ?: return emptyMap()
        val ownerClassId = expandedReceiverType.expandedClassIdOrPrimitiveClassId ?: return emptyMap()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyMap()
        val ownerTypeParameters = (ownerDeclaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if (ownerTypeParameters.isEmpty()) return emptyMap()
        if (ownerTypeParameters.size != expandedReceiverType.typeArguments.size) return emptyMap()

        return ownerTypeParameters.zip(expandedReceiverType.typeArguments).associate { (typeParameter, argument) ->
            typeParameter.symbol.toLookupTag() to argument.type
        }
    }

    /**
     * 解析裸 typealias qualifier 展开后的 class-like 类型。
     */
    private fun CfirQualifiedAccessExpression.expandedTypeAliasQualifierType(
        session: CfirSession,
    ): ConeLookupTagBasedType? {
        val typeAlias = resolvedQualifierTypeAliasSymbol()?.cfir ?: return null
        val expandedType = typeAlias.expandedTypeRef.coneTypeOrNull ?: return null
        val typeArgumentTypes = typeArguments.mapNotNull { it.coneTypeOrNull }
        val appliedExpandedType = if (
            typeArgumentTypes.isNotEmpty() &&
            typeArgumentTypes.size == typeAlias.typeParameters.size
        ) {
            val abbreviatedType = ConeTypeAliasType(
                classId = typeAlias.symbol.classId,
                expandedType = expandedType,
                typeArguments = typeArgumentTypes,
            )
            typeAlias.createParametersSubstitutor(abbreviatedType, session).substituteOrSelf(expandedType)
        } else {
            expandedType
        }

        return appliedExpandedType.fullyExpandedType(session) as? ConeLookupTagBasedType
    }

    /**
     * 取得裸 static qualifier 表达式。
     */
    private fun Candidate.bareStaticQualifierExpression(): CfirQualifiedAccessExpression? =
        callInfo.explicitReceiver as? CfirQualifiedAccessExpression
            ?: dispatchReceiverExpression() as? CfirQualifiedAccessExpression

    /**
     * 构造器调用需要把 owner class 的类型参数也纳入候选约束系统。
     *
     * enum constructor 之前已经做了单独补齐；普通 class/struct constructor 若不走这里，
     * `Box<T>()` 这类调用里的 `T` 就不会注册成 fresh variable，后续同构造器泛型约束无法下沉到实参级。
     */
    private fun ownerClassIdForCallable(
        session: CfirSession,
        candidate: Candidate,
    ): org.cangnova.cangjie.name.ClassId? {
        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return null
        val originalCallableSymbol = callableSymbol.unwrapSubstitutionOverrides()

        return session.cfirProvider.getContainingClass(originalCallableSymbol)?.classId
            ?: (originalCallableSymbol.cfir as? CfirEnumConstructor)?.let {
                val receiver = candidate.bareStaticQualifierExpression()
                receiver?.coneTypeOrNull
                    ?.fullyExpandedType(session)
                    ?.expandedClassIdOrPrimitiveClassId
                    ?: receiver?.resolvedQualifierClassifier(session)?.classId
            }
    }
}
