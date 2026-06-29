package org.cangnova.cangjie.chir.core.transformation

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 CHIR 重写变换的基础行为。
 *
 * 该测试以可达性裁剪为代表，固定重写会话对包快照、函数块列表和不可达控制流的处理契约。
 */
class ChirTransformationBaselineTest {

    /**
     * 校验不可达基本块移除变换只保留入口可达的 CFG。
     *
     * 该用例确认变换结果成功，并且重写后的函数不再携带死块，防止后续优化阶段继续处理无效控制流。
     */
    @Test
    fun `remove unreachable blocks keeps only reachable cfg`() {
        val original = samplePackage()
        val session = ChirRewriteSession(original)
        val transform = RemoveUnreachableBlocksTransformation()

        val result = transform.apply(session)
        assertTrue(result.rewriteResult is ChirRewriteResult.Success)

        val rewritten = session.snapshot()
        val function = rewritten.modules.single().declarations.single() as DefaultChirFunctionDeclaration
        assertEquals(2, function.blocks.size)
        assertTrue(function.blocks.none { it.semanticId == ChirSemanticId("block:dead") })
    }

    /**
     * 构造包含入口、出口和死块的 CHIR 包样本。
     *
     * 样本显式保留一个不可达块，用于验证控制流裁剪变换是否依据入口块计算可达集合。
     */
    private fun samplePackage(): ChirPackage {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:remove-dead"),
            name = "removeDead",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:entry-exit"),
                        targetBlockId = ChirSemanticId("block:exit"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:dead"),
                    name = "dead",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:dead-return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:transform"),
            name = "transform.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:transform"),
                    name = "transform.mod",
                    declarations = listOf(function),
                ),
            ),
        )
    }
}
