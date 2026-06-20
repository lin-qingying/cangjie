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
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutionMap
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitutionForConstraintDerivation
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
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
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.cfir
        val knownOwnerSubstitutions = createCallableOwnerUseSiteSubstitutionMap(
            session = context.session,
            callableSymbol = candidate.symbol as? CfirCallableSymbol<*>,
            receiverType = candidate.useSiteReceiverType(),
        ) + createBareEnumConstructorQualifierOwnerSubstitutionMap(context.session, candidate, declaration)
        val typeParameters = collectCandidateTypeParametersForFreshVariables(context.session, candidate, declaration)
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
            freshTypeVariables.associate {
                it.typeParameterSymbol.toLookupTag() to it.defaultType as ConeCangJieType
            }
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
                    for (bound in parameter.symbol.resolvedBounds) {
                        val upperBound = toFreshVariables.substituteOrSelf(bound.coneType)
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
                for (bound in freshVariable.typeParameterSymbol.resolvedBounds) {
                    add(freshVariable.defaultType to toFreshVariables.substituteOrSelf(bound.coneType))
                }
            }
        }
    }

    private class ChainedCfirSubstitutor(
        private val first: ConeSubstitutor,
        private val second: ConeSubstitutor,
    ) : ConeSubstitutor() {
        override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType {
            return second.substituteOrSelf(first.substituteOrSelf(type))
        }

        override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
            val afterFirst = first.substituteOrNull(type)
            val afterSecond = second.substituteOrNull(afterFirst ?: type)
            return afterSecond ?: afterFirst
        }

        override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
            val afterFirst = first.substituteArgument(projection, index)
            val afterSecond = second.substituteArgument(afterFirst ?: projection, index)
            return afterSecond ?: afterFirst
        }
    }

    private fun chainSubstitutors(vararg substitutors: ConeSubstitutor?): ConeSubstitutor {
        return substitutors
            .filterNotNull()
            .filterNot { it === ConeSubstitutor.Empty }
            .reduceOrNull { first, second -> ChainedCfirSubstitutor(first, second) }
            ?: ConeSubstitutor.Empty
    }

    /**
     * 将 extend 条件父类型派生成约束系统可求解的 where 约束。
     *
     * 例如 `extend<T> Option<T> <: I where T <: I` 使 `Option<X> <: I`
     * 等价于初始约束 `X <: I`。普通父类型查询仍由 providers 层按已满足
     * 约束过滤；这里仅服务于声明上界进入推断系统的阶段。
     */
    private fun collectExtendDerivedUpperBoundConstraints(
        session: CfirSession,
        lowerType: ConeCangJieType,
        upperBound: ConeCangJieType,
    ): List<Pair<ConeCangJieType, ConeCangJieType>>? {
        val semanticLowerType = lowerType.fullyExpandedType(session)
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
                AbstractTypeChecker.isSubtypeOf(session.typeContext, extendSupertype, upperBound)
            }
            if (!matchesUpperBound) continue

            matchedExtendSupertype = true
            for (typeParameter in extend.typeParameters) {
                typeParameter.symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
                val actualType = substitution.substitutor.substituteOrSelf(typeParameter.symbol.constructType())
                for (bound in typeParameter.symbol.resolvedBounds) {
                    val substitutedBound = substitution.substitutor.substituteOrSelf(bound.coneType)
                    if (actualType !is ConeErrorType && substitutedBound !is ConeErrorType) {
                        constraints += actualType to substitutedBound
                    }
                }
            }
        }
        return constraints.takeIf { matchedExtendSupertype }
    }

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

    private fun Candidate.useSiteReceiverType(): ConeCangJieType? =
        dispatchReceiverExpression()?.coneTypeOrNull
            ?: chosenExtensionReceiverExpression()?.coneTypeOrNull
            ?: callInfo.explicitReceiver?.coneTypeOrNull

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
    ): List<CfirTypeParameterRef> {
        val ownTypeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        if ((declaration as? CfirConstructor)?.typeAliasConstructorInfo != null) {
            return ownTypeParameters
        }

        val extendTypeParameters = collectBareStaticQualifierExtendTypeParameters(session, candidate, declaration)
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
        if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirConstructor &&
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
        val ownerExtend = session.extendProvider.getContainingExtend(callableSymbol.unwrapSubstitutionOverrides())
            ?.takeIf(session.extendProvider::isExtendAccessible)
            ?: return emptyList()

        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyList()
        if (receiver.typeArguments.isNotEmpty()) return emptyList()

        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyList()
        val ownerDeclaration = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return emptyList()
        if (ownerDeclaration.typeParameters.isEmpty()) return emptyList()

        val receiverType = receiver.coneTypeOrNull as? ConeLookupTagBasedType ?: return emptyList()
        if (receiverType.typeArguments.isNotEmpty()) return emptyList()

        val extendTargetType = ownerExtend.extendedTypeRef.coneTypeOrNull as? ConeLookupTagBasedType
            ?: return emptyList()
        if (extendTargetType.typeArguments.isEmpty()) return emptyList()
        if (extendTargetType.classIdOrPrimitiveClassId != ownerSymbol.classId) return emptyList()

        return ownerExtend.typeParameters
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
        if (!callable.status.isStatic) return emptyList()

        val receiver = candidate.bareStaticQualifierExpression() ?: return emptyList()
        if (receiver.typeArguments.isNotEmpty()) return emptyList()

        collectBareTypeAliasQualifierTypeParameters(receiver)?.let { return it }

        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyList()
        val ownerDeclaration = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return emptyList()
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
        if (callable.status.isStatic) return emptyList()
        val receiverType = candidate.dispatchReceiverExpression()?.coneTypeOrNull as? ConeTypeVariableType
            ?: return emptyList()
        if (receiverType.typeConstructor.originalTypeParameter != null) return emptyList()

        val ownerClassId = ownerClassIdForCallable(session, candidate)
            ?: return emptyList()
        val ownerDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: session.cfirProvider.getCfirClassifierByFqName(ownerClassId)
            ?: return emptyList()

        return (ownerDeclaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
    }

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

    private fun CfirQualifiedAccessExpression.resolvedQualifierTypeAliasSymbol(): CfirTypeAliasSymbol? {
        val resolvedReference = calleeReference as? CfirResolvedNamedReference ?: return null
        return resolvedReference.resolvedSymbol as? CfirTypeAliasSymbol
    }

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
        val callableSymbol = candidate.symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*> ?: return null

        return session.cfirProvider.getContainingClass(callableSymbol)?.classId
            ?: (callableSymbol.cfir as? CfirEnumConstructor)?.let {
                val receiver = candidate.bareStaticQualifierExpression()
                receiver?.coneTypeOrNull
                    ?.fullyExpandedType(session)
                    ?.expandedClassIdOrPrimitiveClassId
                    ?: receiver?.resolvedQualifierClassifier(session)?.classId
            }
    }
}
