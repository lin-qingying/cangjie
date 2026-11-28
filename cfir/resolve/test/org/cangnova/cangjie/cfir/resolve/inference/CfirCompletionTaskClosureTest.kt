@file:OptIn(org.cangnova.cangjie.util.PrivateForInline::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferGenericFunctionTypeParameterType
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.type.model.TypeVariableInferenceScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 通过原 FULL 完成循环检验任务闭包，不通过枚举全 CS、手工固定子 VM 或 writer 补值完成。 */
class CfirCompletionTaskClosureTest {
    @Test
    fun fixedRootRetainsBothActualChildEqualityTasks() {
        val f = Fixture()
        val (root, left, right) = f.rootWithChildren()
        val functionType = ConeFunctionType(listOf(root.defaultType), ConePrimitiveType.UNIT)
        val expression = f.lambdaExpression(root.defaultType)
        val argument = ConeResolutionAtomWithPostponedChild(expression)
        val atom = f.candidate(f.seed, "root", root.defaultType, listOf(root), listOf(argument), listOf(functionType))
        val delayed = ConeResolvedLambdaAtom(
            expression, expression.anonymousFunction, atom.candidate, functionType,
            listOf(root.defaultType), ConePrimitiveType.UNIT,
        )
        argument.setPostponedSubAtom(delayed)
        atom.candidate.addPostponedAtom(delayed)
        val actual = atom.candidate.system
        with(actual) {
            val first = f.session.inferenceComponents.variableFixationFinder.findFirstVariableForFixation(
                listOf(root.typeConstructor, left.typeConstructor, right.typeConstructor),
                listOf(delayed), ConstraintSystemCompletionMode.FULL, root.defaultType,
            )
            assertSame(root.typeConstructor, first?.variable)
            assertTrue(first?.isReady == true)
        }

        f.complete(atom, actual) { analyzed, withPCLA ->
            assertSame(delayed, analyzed)
            assertFalse(withPCLA)
            val before = actual.captureStorage()
            assertEquals(ConePrimitiveType.INT64, before.fixedTypeVariables[root.typeConstructor])
            for (variable in listOf(left, right)) {
                val pending = before.notFixedTypeVariables.getValue(variable.typeConstructor)
                assertSame(variable, pending.typeVariable)
                assertTrue(pending.constraints.any {
                    it.kind == ConstraintKind.EQUALITY && it.type == ConePrimitiveType.INT64
                })
            }
            delayed.analyzed = true
        }
        val completed = actual.asReadOnlyStorage()

        for (variable in listOf(root, left, right)) {
            assertSame(variable, completed.allTypeVariables[variable.typeConstructor])
            assertEquals(ConePrimitiveType.INT64, completed.fixedTypeVariables[variable.typeConstructor])
            assertFalse(variable.typeConstructor in completed.notFixedTypeVariables)
        }
        assertTrue(completed.errors.isEmpty())
        assertThrows(IllegalStateException::class.java) { actual.getBuilder() }
    }

    @Test
    fun analyzedActualAtomAddsLaterOwnedRootAndConstraintTasks() {
        val f = Fixture()
        val root = ConstraintSystemTestHarness.newVariable(f.seed, "Root")
        f.seed.addEqualityConstraint(root.defaultType, ConePrimitiveType.INT64, SimpleConstraintSystemConstraintPosition)
        val functionType = ConeFunctionType(listOf(root.defaultType), ConePrimitiveType.UNIT)
        val expression = f.lambdaExpression(root.defaultType)
        val argument = ConeResolutionAtomWithPostponedChild(expression)
        val atom = f.candidate(f.seed, "root", root.defaultType, listOf(root), listOf(argument), listOf(functionType))
        val delayed = ConeResolvedLambdaAtom(
            expression, expression.anonymousFunction, atom.candidate, functionType,
            listOf(root.defaultType), ConePrimitiveType.UNIT,
        )
        argument.setPostponedSubAtom(delayed)
        atom.candidate.addPostponedAtom(delayed)
        val actual = atom.candidate.system
        lateinit var laterRoot: ConeTypeVariable
        lateinit var laterChild: ConeTypeVariable
        var analyses = 0

        f.complete(atom, actual) { analyzed, withPCLA ->
            assertSame(delayed, analyzed)
            assertFalse(withPCLA)
            assertEquals(0, analyses++)
            val before = actual.captureStorage()
            assertSame(root, before.allTypeVariables[root.typeConstructor])
            assertEquals(ConePrimitiveType.INT64, before.fixedTypeVariables[root.typeConstructor])
            laterRoot = ConstraintSystemTestHarness.newVariable(actual, "Later")
            laterChild = ConstraintSystemTestHarness.newVariable(actual, "Later")
            actual.addSubtypeConstraint(laterChild.defaultType, laterRoot.defaultType, SimpleConstraintSystemConstraintPosition)
            actual.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, laterChild.defaultType, SimpleConstraintSystemConstraintPosition)
            actual.addEqualityConstraint(laterRoot.defaultType, ConePrimitiveType.BOOLEAN, SimpleConstraintSystemConstraintPosition)
            // 后续任务来自这个原 owner 的实际新 atom；同名子 VM 只能沿真实约束进入闭包。
            atom.candidate.postponedPCLACalls +=
                f.candidate(actual, "later", laterRoot.defaultType, listOf(laterRoot))
            delayed.analyzed = true
        }
        val completed = actual.asReadOnlyStorage()

        assertEquals(1, analyses)
        assertSame(laterRoot, completed.allTypeVariables[laterRoot.typeConstructor])
        assertSame(laterChild, completed.allTypeVariables[laterChild.typeConstructor])
        assertEquals(ConePrimitiveType.BOOLEAN, completed.fixedTypeVariables[laterRoot.typeConstructor])
        assertEquals(ConePrimitiveType.BOOLEAN, completed.fixedTypeVariables[laterChild.typeConstructor])
        assertTrue(completed.notFixedTypeVariables.isEmpty())
        assertTrue(completed.errors.isEmpty())
        assertThrows(IllegalStateException::class.java) { actual.asConstraintSystemCompleterContext() }
    }

    @Test
    fun retainedOwnedClosureLeavesUnrelatedAndActualOuterVariablesPending() {
        val f = Fixture()
        val parent = f.session.inferenceComponents.createConstraintSystem()
        val outer = ConstraintSystemTestHarness.newVariable(parent, "T")
        parent.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, outer.defaultType, SimpleConstraintSystemConstraintPosition)
        f.seed.addOuterSystem(parent.captureStorage())
        val (root, left, right) = f.rootWithChildren()
        val unrelated = ConstraintSystemTestHarness.newVariable(f.seed, "T")
        f.seed.addSubtypeConstraint(ConePrimitiveType.BOOLEAN, unrelated.defaultType, SimpleConstraintSystemConstraintPosition)
        val atom = f.candidate(f.seed, "root", root.defaultType, listOf(root))
        val actual = atom.candidate.system

        f.complete(atom, actual)
        val completed = actual.asReadOnlyStorage()

        for (variable in listOf(root, left, right)) {
            assertSame(variable, completed.allTypeVariables[variable.typeConstructor])
            assertEquals(ConePrimitiveType.INT64, completed.fixedTypeVariables[variable.typeConstructor])
        }
        for (variable in listOf(outer, unrelated)) {
            assertSame(variable, completed.allTypeVariables[variable.typeConstructor])
            assertSame(variable, completed.notFixedTypeVariables[variable.typeConstructor]?.typeVariable)
            assertFalse(variable.typeConstructor in completed.fixedTypeVariables)
        }
        assertTrue(parent.captureStorage().fixedTypeVariables.isEmpty())
        assertTrue(completed.errors.isEmpty())
    }

    @Test
    fun ordinaryDeclaredGenericFailureKeepsItsOriginalDiagnostic() {
        val f = Fixture()
        val parameter = ExtendTestFixtures.newTypeParameter(f.session.moduleData, "T")
        val variable = ConeTypeParameterBasedTypeVariable(parameter.symbol, TypeVariableInferenceScope())
        f.seed.registerVariable(variable)
        val atom = f.candidate(f.seed, "ordinary", variable.defaultType, listOf(variable), typeParameters = listOf(parameter))
        val actual = atom.candidate.system

        f.complete(atom, actual)
        val completed = actual.asReadOnlyStorage()

        assertSame(variable, completed.allTypeVariables[variable.typeConstructor])
        assertEquals(1, completed.errors.filterIsInstance<NotEnoughInformationForTypeParameter<*>>().size)
        val error = completed.fixedTypeVariables[variable.typeConstructor] as ConeErrorType
        assertTrue(error.diagnostic is ConeCannotInferGenericFunctionTypeParameterType)
        assertFalse(variable.typeConstructor in completed.notFixedTypeVariables)
    }

    /** 使用已有 session、Candidate、atom、mapping 和原 completer；冻结后只读显式 actual storage。 */
    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession()
        val seed = session.inferenceComponents.createConstraintSystem()
        private val file = CallResolutionTestFixtures.buildCallInfo(session, "tasks").containingFile
        private val context = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()).apply {
            this.file = this@Fixture.file
        }
        private val transformer = CfirBodyResolveTransformer(session, ScopeSession(), outerBodyResolveContext = context)

        fun rootWithChildren(): Triple<ConeTypeVariable, ConeTypeVariable, ConeTypeVariable> {
            val root = ConstraintSystemTestHarness.newVariable(seed, "T")
            val left = ConstraintSystemTestHarness.newVariable(seed, "T")
            val right = ConstraintSystemTestHarness.newVariable(seed, "T")
            seed.addSubtypeConstraint(left.defaultType, root.defaultType, SimpleConstraintSystemConstraintPosition)
            seed.addSubtypeConstraint(right.defaultType, root.defaultType, SimpleConstraintSystemConstraintPosition)
            seed.addSubtypeConstraint(ConePrimitiveType.INT64, left.defaultType, SimpleConstraintSystemConstraintPosition)
            seed.addSubtypeConstraint(ConePrimitiveType.INT64, right.defaultType, SimpleConstraintSystemConstraintPosition)
            seed.addEqualityConstraint(root.defaultType, ConePrimitiveType.INT64, SimpleConstraintSystemConstraintPosition)
            return Triple(root, left, right)
        }

        fun candidate(
            sourceSystem: ConstraintSystemImpl,
            name: String,
            returnType: ConeCangJieType,
            variables: List<ConeTypeVariable>,
            argumentAtoms: List<ConeResolutionAtom> = emptyList(),
            parameterTypes: List<ConeCangJieType> = emptyList(),
            typeParameters: List<CfirTypeParameter> = emptyList(),
        ): ConeAtomWithCandidate {
            val argumentExpressions = argumentAtoms.map { it.expression }
            val prototype = CallResolutionTestFixtures.buildCallInfo(session, name, argumentExpressions)
            val call = buildFunctionCall {
                source = prototype.callSite.source
                coneTypeOrNull = returnType
                calleeReference = buildNamedReference { this.name = prototype.name }
                argumentList = buildArgumentList { this.arguments += argumentExpressions }
            }
            val info = CallInfo(
                callSite = call,
                callKind = prototype.callKind,
                name = prototype.name,
                explicitReceiver = null,
                arguments = argumentExpressions,
                isUsedAsGetClassReceiver = false,
                typeArguments = emptyList(),
                session = session,
                containingFile = file,
                containingDeclarations = emptyList(),
                resolutionMode = prototype.resolutionMode,
            )
            val target = CallResolutionTestFixtures.buildFunctionSymbol(
                session, name, returnType, parameterTypes, typeParameters = typeParameters,
            )
            val candidate = Candidate(
                symbol = target,
                dispatchReceiver = null,
                givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
                baseSystem = sourceSystem.captureStorage(),
                callInfo = info,
                originScope = null,
                bodyResolveContext = context,
            ).apply {
                initializeSubstitutorAndVariables(ConeSubstitutor.Empty, variables)
                initializeArgumentMapping(argumentAtoms, LinkedHashMap(argumentAtoms.zip(target.cfir.valueParameters).toMap()))
            }
            call.replaceCalleeReference(CfirNamedReferenceWithCandidate(call.source, info.name, candidate))
            return ConeAtomWithCandidate(call, candidate)
        }

        fun lambdaExpression(input: ConeCangJieType): CfirAnonymousFunctionExpression =
            buildAnonymousFunctionExpression {
                source = CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.UNIT).source
                anonymousFunction = buildAnonymousFunction {
                    source = this@buildAnonymousFunctionExpression.source
                    moduleData = session.moduleData
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true
                    status = CfirDeclarationStatusImpl()
                    returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef()
                    typeRef = ConeFunctionType(listOf(input), ConePrimitiveType.UNIT).toCfirResolvedTypeRef()
                    val lambdaSymbol = CfirAnonymousFunctionSymbol()
                    symbol = lambdaSymbol
                    valueParameters += CallResolutionTestFixtures.buildValueParameter(
                        session, "input", input, containingDeclarationSymbol = lambdaSymbol,
                    )
                    body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
                    hasExplicitParameterList = true
                    isLambda = true
                }
                isTrailingLambda = false
            }

        fun complete(
            atom: ConeAtomWithCandidate,
            actual: ConstraintSystemImpl,
            analyzer: ConstraintSystemCompleter.PostponedAtomAnalyzer =
                ConstraintSystemCompleter.PostponedAtomAnalyzer { _, _ -> error("No postponed argument is expected") },
        ) {
            ConstraintSystemCompleter(transformer.components).complete(
                actual.asConstraintSystemCompleterContext(), ConstraintSystemCompletionMode.FULL,
                listOf(atom), atom.candidate.substitutedReturnType(), transformer.resolutionContext, analyzer,
            )
        }
    }
}
