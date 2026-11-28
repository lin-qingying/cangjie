@file:OptIn(org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import com.intellij.openapi.progress.ProcessCanceledException
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedToPhaseState
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirFunctionCallImpl
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildConstPattern
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 声明事务必须恢复模式的真实子图，保留 provider 与 symbol 所持的绑定身份。 */
class PatternStateKeeperTest {
    @Test
    fun `cancelled pattern variable body restores binding and shared initializer graph before retry`() {
        val initializer = NegativeConstant()
        val moduleData = moduleData()
        val originalType = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
        val binding = binding(moduleData).apply {
            replaceReturnTypeRef(originalType)
            replaceInitializer(initializer.call)
        }
        val pattern = buildBindingPattern {
            name = binding.name
            typeRef = originalType
            bindingVariable = binding
        }
        val root: CfirPatternVariable = buildPatternVariable {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            symbol = CfirPatternVariableSymbol(CallableId(FqName.ROOT, Name.special("<pattern-variable>")))
            isVar = false
            returnTypeRef = originalType
            this.pattern = pattern
            this.initializer = initializer.call
        }
        val originalRootState = root.resolveState
        val originalBindingState = binding.resolveState
        val designation = CfirDesignation(root)
        val completedType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        fun assertRestored() {
            assertSame(root, designation.target)
            assertSame(pattern, root.pattern)
            assertSame(binding, pattern.bindingVariable)
            assertSame(binding, binding.symbol.cfir)
            assertSame(root, root.symbol.cfir)
            assertSame(originalType, root.returnTypeRef)
            assertSame(originalType, pattern.typeRef)
            assertSame(originalType, binding.returnTypeRef)
            assertSame(initializer.call, root.initializer)
            assertSame(initializer.call, binding.initializer)
            assertSame(originalRootState, root.resolveState)
            assertSame(originalBindingState, binding.resolveState)
            initializer.assertRestored()
        }

        // 简单绑定在 body resolve 后与 root 共享整个 initializer；取消时不能仅恢复根引用。
        repeat(2) {
            assertThrows(ProcessCanceledException::class.java) {
                resolveWithKeeper(root, designation, BodyStateKeepers.PATTERN_VARIABLE) {
                    assertTrue(root.initializer is CfirLazyExpression)
                    assertSame(initializer.call, binding.initializer)
                    root.replaceReturnTypeRef(completedType)
                    root.replaceInitializer(initializer.replacement)
                    binding.replaceReturnTypeRef(completedType)
                    binding.replaceInitializer(initializer.replacement)
                    binding.resolveState = CfirResolvedToPhaseState(CfirResolvePhase.BODY_RESOLVE)
                    initializer.mutate()
                    throw ProcessCanceledException()
                }
            }
            assertRestored()
        }

        // 同一 designation 再次请求可以提交成功，恢复动作不能在成功后覆盖新结果。
        resolveWithKeeper(root, designation, BodyStateKeepers.PATTERN_VARIABLE) {
            assertSame(originalType, binding.returnTypeRef)
            assertSame(originalBindingState, binding.resolveState)
            initializer.assertRestored()
            root.replaceReturnTypeRef(completedType)
            root.replaceInitializer(initializer.call)
            binding.replaceReturnTypeRef(completedType)
            binding.resolveState = CfirResolvedToPhaseState(CfirResolvePhase.BODY_RESOLVE)
        }
        assertSame(completedType, root.returnTypeRef)
        assertSame(completedType, binding.returnTypeRef)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, binding.resolveState.resolvePhase)
        assertSame(binding, pattern.bindingVariable)
        assertSame(initializer.call, root.initializer)
    }

    @Test
    fun `constant pattern body keeper restores in place operator state on repeated cancellation`() {
        val initializer = NegativeConstant()
        val pattern = buildConstPattern { expression = initializer.call }

        repeat(2) {
            val state = PatternStateKeepers.BODY.prepare(pattern, Unit)
            assertThrows(ProcessCanceledException::class.java) {
                try {
                    state.postProcess()
                    initializer.mutate()
                    throw ProcessCanceledException()
                } catch (exception: ProcessCanceledException) {
                    state.restore()
                    throw exception
                }
            }
            assertSame(initializer.call, pattern.expression)
            initializer.assertRestored()
        }

        val retry = PatternStateKeepers.BODY.prepare(pattern, Unit)
        retry.postProcess()
        initializer.assertRestored()
        initializer.call.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        assertEquals(ConePrimitiveType.INT64, initializer.call.coneTypeOrNull)
    }

    /** 与 raw builder 的负常量一致：空实参 operator call，字面量为显式接收者。 */
    private class NegativeConstant {
        private val literal = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }
        private val reference = buildNamedReference { name = OperatorNameConventions.UNARY_MINUS }
        private val arguments = buildArgumentList {}
        val replacement = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 2
            coneTypeOrNull = ConePrimitiveType.INT64
        }
        val call = buildFunctionCall {
            calleeReference = reference
            explicitReceiver = literal
            argumentList = arguments
            origin = CfirFunctionCallOrigin.Operator
        }

        fun mutate() {
            call.replaceCalleeReference(buildNamedReference { name = OperatorNameConventions.UNARY_PLUS })
            call.replaceDispatchReceiver(replacement)
            // 生成节点的真实可变字段，与 body transformer 替换显式接收者的写入一致。
            (call as CfirFunctionCallImpl).explicitReceiver = replacement
            (arguments.arguments as MutableList<CfirExpression>).add(replacement)
            call.replaceArgumentList(buildArgumentList { this.arguments += replacement })
            call.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
        }

        fun assertRestored() {
            assertSame(reference, call.calleeReference)
            assertNull(call.dispatchReceiver)
            assertSame(literal, call.explicitReceiver)
            assertSame(arguments, call.argumentList)
            assertTrue(arguments.arguments.isEmpty())
            assertNull(call.coneTypeOrNull)
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
        }
    }

    private fun binding(moduleData: CfirModuleData): CfirPatternBindingVariable = buildPatternBindingVariable {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.IMPLICIT_TYPES
        origin = CfirDeclarationOrigin.Library
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        status = CfirDeclarationStatusImpl()
        symbol = CfirPatternBindingSymbol(CallableId(FqName.ROOT, Name.identifier("value")))
        name = Name.identifier("value")
        isVar = false
        returnTypeRef = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
    }

    /** keeper 仅需要真实声明所属模块，不注册任何替代 resolver 或功能组件。 */
    private fun moduleData(): CfirModuleData {
        val session = object : CfirSession(Kind.Source) {
            override fun toString(): String = "PatternStateKeeperTestSession"
        }
        return CfirSourceModuleData(
            name = Name.identifier("pattern-state-keeper"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        ).also {
            it.bindSession(session)
            session.register(CfirModuleData::class, it)
        }
    }
}
