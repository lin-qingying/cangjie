package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeInvalidCatchTypeError
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildHandleClause
import org.cangnova.cangjie.cfir.expressions.builder.buildLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchBranch
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildOptionalExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildSubscriptExpression
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.patterns.restorePatternTreeRoots
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** 快照恢复真实可变类型状态，不向错误表达式的派生类型 getter 写回。 */
class CfirResolutionSnapshotTest {
    /** 已检查的 optional payload 不能跨失败候选保留，原接收者的类型也必须恢复。 */
    @Test
    fun `restore discards a checked optional subject type`() {
        val receiver = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val subject = buildOptionalExpression { expression = receiver }
        val snapshot = CfirResolutionSnapshot.capture(subject)
        receiver.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        subject.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        snapshot.restore()
        assertNull(subject.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT32, receiver.coneTypeOrNull)
    }

    /** 复合赋值的语义调用可回指同一个下标；快照不能递归失控或遗留失败候选的绑定。 */
    @Test
    fun `restore preserves operator bindings in a shared subscript graph`() {
        val subscript = buildSubscriptExpression {
            receiver = buildErrorExpression { diagnostic = ConeSimpleDiagnostic("receiver") }
        }
        val originalReference = buildNamedReference { name = Name.identifier("set") }
        val setCall = buildFunctionCall {
            calleeReference = originalReference
            argumentList = buildArgumentList { arguments += subscript }
        }
        subscript.replaceResolvedSetCall(setCall)
        val snapshot = CfirResolutionSnapshot.capture(subscript)
        repeat(2) {
            subscript.replaceResolvedSetCall(null)
            subscript.replaceResolvedGetCall(setCall)
            setCall.replaceCalleeReference(buildNamedReference { name = Name.identifier("failed") })
            snapshot.restore()
            assertNull(subscript.resolvedGetCall)
            assertSame(setCall, subscript.resolvedSetCall)
            assertSame(originalReference, setCall.calleeReference)
        }
    }

    /** 候选试跑检查了 catch 后，回滚必须恢复原类型引用和“尚未检查”的整体状态。 */
    @Test
    fun `restore discards catch pattern typing from a failed candidate`() {
        val originalType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        val pattern = buildCatchPattern {
            isWildcard = true
            typeRefs += originalType
        }
        val snapshot = CfirResolutionSnapshot.capture(pattern)
        repeat(2) {
            val errorType = ConeErrorType(ConeInvalidCatchTypeError).toCfirResolvedTypeRef()
            pattern.replaceTypeRefs(listOf(errorType))
            pattern.replaceResolvedTypeRef(errorType)
            snapshot.restore()
            assertNull(pattern.resolvedTypeRef)
            assertEquals(1, pattern.typeRefs.size)
            assertSame(originalType, pattern.typeRefs.single())
        }
    }

    /** 延迟解析替换的 let pattern 根必须在失败候选回滚时恢复，并重新绑定原树。 */
    @Test
    fun `restore reinstates the let pattern root and owner`() {
        val originalPattern = buildWildcardPattern()
        val letExpression = buildLetPatternExpression {
            initializer = buildLiteralExpression {
                kind = CfirLiteralKind.INT
                value = 1
            }
            pattern = originalPattern
        }
        val snapshot = CfirResolutionSnapshot.capture(letExpression)
        val candidatePattern = buildWildcardPattern()

        letExpression.replacePattern(candidatePattern)
        snapshot.restore()

        assertSame(originalPattern, letExpression.pattern)
        assertSame(letExpression, originalPattern.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
    }

    /** Handle 的命令类型引用和 owner 根都属于候选 body 的可变 CFIR 状态。 */
    @Test
    fun `restore reinstates handle pattern root and command type references`() {
        val originalTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        val originalPattern = buildCommandTypePattern {
            bindingName = Name.identifier("value")
            isWildcard = false
            typeRefs += originalTypeRef
        }
        val handleClause = buildHandleClause {
            commandPattern = originalPattern
            body = buildBlock()
        }
        val snapshot = CfirResolutionSnapshot.capture(handleClause)
        val candidateTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
        val candidatePattern = buildCommandTypePattern {
            isWildcard = true
        }

        originalPattern.replaceTypeRefs(listOf(candidateTypeRef))
        handleClause.replaceCommandPattern(candidatePattern)
        snapshot.restore()

        assertSame(originalPattern, handleClause.commandPattern)
        assertSame(handleClause, originalPattern.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
        assertSame(originalTypeRef, originalPattern.typeRefs.single())
    }

    /** 从外层表达式捕获多个 owner；跨 owner 接管后的根与反向链接必须整体恢复。 */
    @Test
    fun `restore reinstates multiple nested roots after a pattern tree transfer`() {
        val firstPattern = buildBindingPattern { name = Name.identifier("first") }
        val secondPattern = buildBindingPattern { name = Name.identifier("second") }
        val firstBinding = createPatternBindingVariable("first")
        val secondBinding = createPatternBindingVariable("second")
        firstPattern.replaceBindingVariable(firstBinding)
        secondPattern.replaceBindingVariable(secondBinding)
        val firstBranch = buildMatchBranch {
            pattern = firstPattern
            body = buildBlock()
        }
        val secondBranch = buildMatchBranch {
            pattern = secondPattern
            body = buildBlock()
        }
        val match = buildMatchExpression {
            branches += firstBranch
            branches += secondBranch
        }
        val snapshot = CfirResolutionSnapshot.capture(match)
        val candidatePattern = buildBindingPattern { name = Name.identifier("candidate") }

        firstBranch.replacePattern(candidatePattern)
        secondBranch.replacePattern(firstPattern)
        assertSame(secondBranch, firstBinding.patternTreeOwner)
        assertNull(secondBinding.patternTreeOwner)
        assertSame(firstBranch, candidatePattern.patternTreeOwner)
        snapshot.restore()

        assertSame(firstPattern, firstBranch.pattern)
        assertSame(firstBranch, firstPattern.patternTreeOwner)
        assertSame(firstBranch, firstBinding.patternTreeOwner)
        assertSame(secondPattern, secondBranch.pattern)
        assertSame(secondBranch, secondPattern.patternTreeOwner)
        assertSame(secondBranch, secondBinding.patternTreeOwner)
        assertNull(candidatePattern.patternTreeOwner)
    }

    /** 所有根类型必须在事务开始前验证，避免较早的 owner 已被部分恢复。 */
    @Test
    fun `restore validates every owner root type before changing any owner`() {
        val originalBranchPattern = buildWildcardPattern()
        val branch = buildMatchBranch {
            pattern = originalBranchPattern
            body = buildBlock()
        }
        val originalHandlePattern = buildCommandTypePattern { isWildcard = true }
        val handle = buildHandleClause {
            commandPattern = originalHandlePattern
            body = buildBlock()
        }
        val candidateBranchPattern = buildWildcardPattern()
        val invalidHandlePattern = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    branch to candidateBranchPattern,
                    handle to invalidHandlePattern,
                ),
            )
        }

        assertSame(originalBranchPattern, branch.pattern)
        assertSame(originalHandlePattern, handle.commandPattern)
        assertNull(candidateBranchPattern.patternTreeOwner)
        assertNull(invalidHandlePattern.patternTreeOwner)
    }

    /** 恢复输入不能重复声明同一 owner，否则根字段顺序会产生歧义。 */
    @Test
    fun `restore rejects duplicate owners before changing roots`() {
        val originalPattern = buildWildcardPattern()
        val branch = buildMatchBranch {
            pattern = originalPattern
            body = buildBlock()
        }
        val firstCandidate = buildWildcardPattern()
        val secondCandidate = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    branch to firstCandidate,
                    branch to secondCandidate,
                ),
            )
        }

        assertSame(originalPattern, branch.pattern)
        assertNull(firstCandidate.patternTreeOwner)
        assertNull(secondCandidate.patternTreeOwner)
    }

    /** 不同 owner 的目标根不能共享同一模式节点，验证失败必须保持当前图不变。 */
    @Test
    fun `restore rejects a pattern node shared by different target owners`() {
        val firstOriginal = buildWildcardPattern()
        val secondOriginal = buildWildcardPattern()
        val firstBranch = buildMatchBranch {
            pattern = firstOriginal
            body = buildBlock()
        }
        val secondBranch = buildMatchBranch {
            pattern = secondOriginal
            body = buildBlock()
        }
        val sharedTarget = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            restorePatternTreeRoots(
                listOf(
                    firstBranch to sharedTarget,
                    secondBranch to sharedTarget,
                ),
            )
        }

        assertSame(firstOriginal, firstBranch.pattern)
        assertSame(secondOriginal, secondBranch.pattern)
        assertNull(sharedTarget.patternTreeOwner)
    }

    private fun createPatternBindingVariable(name: String): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            moduleData = CfirBinaryDependenciesModuleData(Name.identifier("pattern-snapshot-test"))
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            isVar = false
            symbol = CfirPatternBindingSymbol(CallableId(Name.identifier(name)))
            returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            this.name = Name.identifier(name)
        }

    @Test
    fun `restore keeps a fixed error diagnostic and restores sibling types`() {
        val diagnostic = ConeSimpleDiagnostic("unexpanded macro")
        val error = buildErrorExpression { this.diagnostic = diagnostic }
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val block = buildBlock { statements.addAll(listOf(error, literal)) }
        val snapshot = CfirResolutionSnapshot.capture(block)

        repeat(2) {
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            snapshot.restore()
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
            val restoredDiagnostic = (error.coneTypeOrNull as ConeErrorType).diagnostic as ConeUnreportedDuplicateDiagnostic
            assertSame(diagnostic, restoredDiagnostic.original)
        }
    }

    @Test
    fun `restore visits the expression supplying an error wrapper type`() {
        val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        val diagnostic = ConeSimpleDiagnostic("erroneous wrapper")
        val error = buildErrorExpression {
            this.diagnostic = diagnostic
            expression = literal
        }
        val snapshot = CfirResolutionSnapshot.capture(error)
        literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)

        snapshot.restore()

        assertEquals(ConePrimitiveType.INT32, error.coneTypeOrNull)
        assertSame(literal, error.expression)
        assertSame(diagnostic, error.diagnostic)
    }
}
