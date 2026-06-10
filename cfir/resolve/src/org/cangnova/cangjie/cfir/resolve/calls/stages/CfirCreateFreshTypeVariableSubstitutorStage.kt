package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation

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
        val typeParameters = collectCandidateTypeParametersForFreshVariables(context.session, candidate, declaration)
        if (typeParameters.isEmpty()) {
            candidate.initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
            return
        }

        val csBuilder = candidate.system.getBuilder()
        val (substitutor, freshVariables) =
            createToFreshVariableSubstitutorAndAddInitialConstraints(typeParameters, csBuilder)
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

            csBuilder.addEqualityConstraint(
                freshVariable.defaultType,
                argumentType,
                ConeExplicitTypeParameterConstraintPosition(typeArgument),
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
        typeParameters: List<CfirTypeParameterRef>,
        csBuilder: ConstraintSystemOperation,
    ): Pair<ConeSubstitutor, List<ConeTypeVariable>> {
        val freshTypeVariables = typeParameters.map { ConeTypeParameterBasedTypeVariable(it.symbol) }

        // 构建替代器：类型参数名 → 新鲜类型变量的默认类型
        val replacements = freshTypeVariables.associate {
            it.typeParameterSymbol.name.asString() to (it.defaultType as ConeCangJieType)
        }
        val toFreshVariables = CfirTypeSubstitutorByMap(replacements)

        // 在约束系统中注册所有新鲜变量
        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        // 添加上界约束：freshVariable <: substituted(bound)
        for (freshVariable in freshTypeVariables) {
            for (bound in freshVariable.typeParameterSymbol.resolvedBounds) {
                val substitutedBound = toFreshVariables.substituteOrSelf(bound.coneType)
                csBuilder.addSubtypeConstraint(
                    freshVariable.defaultType,
                    substitutedBound,
                    ConeDeclaredUpperBoundConstraintPosition,
                )
            }
        }

        return toFreshVariables to freshTypeVariables
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
    ): List<CfirTypeParameterRef> {
        val ownTypeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
        val ownerTypeParameters = collectBareStaticQualifierOwnerTypeParameters(session, candidate, declaration)
        if (ownerTypeParameters.isNotEmpty()) {
            if (candidate.callInfo.typeArguments.isEmpty()) {
                return ownerTypeParameters + ownTypeParameters
            }
            if (ownTypeParameters.isEmpty()) {
                return ownerTypeParameters
            }
        }
        if (ownTypeParameters.isNotEmpty()) return ownTypeParameters
        if (declaration !is org.cangnova.cangjie.cfir.declarations.CfirConstructor &&
            declaration !is CfirEnumConstructor
        ) {
            return emptyList()
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

        val receiver = candidate.callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return emptyList()
        if (receiver.typeArguments.isNotEmpty()) return emptyList()

        val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return emptyList()
        val ownerDeclaration = ownerSymbol.cfir as? CfirClassLikeDeclaration ?: return emptyList()
        return ownerDeclaration.typeParameters
    }

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
    }
}
