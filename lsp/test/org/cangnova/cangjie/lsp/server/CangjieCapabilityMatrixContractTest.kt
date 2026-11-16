package org.cangnova.cangjie.lsp.server

import java.lang.reflect.Modifier
import java.util.concurrent.ExecutionException
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession
import org.cangnova.cangjie.lsp.testkit.KNOWN_DEFECT_TAG
import org.cangnova.cangjie.lsp.testkit.LspAdvertisement
import org.cangnova.cangjie.lsp.testkit.LspCapabilityMatrix
import org.cangnova.cangjie.lsp.testkit.LspCapabilityStatus
import org.cangnova.cangjie.lsp.testkit.LspClientCapabilitiesBuilder
import org.cangnova.cangjie.lsp.testkit.LspMatrixStage
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.cangnova.cangjie.lsp.testkit.ProtocolContractConfiguration
import org.cangnova.cangjie.lsp.testkit.assertInjectedFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * 宣告能力的协议契约矩阵测试。
 *
 * 逐行驱动 [LspCapabilityMatrix]：能力开启时「声明 + 路由 + 正向结果」，能力关闭时
 * 「不声明 + 中性值 + 不进 facade」，文档缺失时「中性值 + 不进 facade」，
 * facade 抛错时「请求以协议错误结束」。
 *
 * 矩阵驱动的意义在于**新增能力必须补一行**：漂移检查用例会比对能力字段与协议枚举，
 * 漏写一行或写错能力名都会让测试失败，而不是悄悄少覆盖一项能力。
 */
class CangjieCapabilityMatrixContractTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：每个阶段需要不同的 facade 配置。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 矩阵漂移检查：能力字段、协议入口、状态与真实语义标记都必须自洽。
     */
    @Test
    fun `matrix covers every declared feature and protocol entry`() {
        val declaredFeatures = CangjieLspFeatureSet::class.java.declaredFields
            .filter { it.type == java.lang.Boolean.TYPE && !Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(
            declaredFeatures,
            LspCapabilityMatrix.featureGetters.keys,
            "能力字段与矩阵不一致：新增/改名能力必须同步更新 LspCapabilityMatrix.featureGetters",
        )

        val coveredFeatures = LspCapabilityMatrix.rows.mapNotNull { it.matrixFeature }.toSet()
        assertEquals(
            declaredFeatures,
            coveredFeatures,
            "有宣告能力没有任何矩阵行：$coveredFeatures",
        )

        val matrixMethods = LspCapabilityMatrix.rows.map { it.protocolMethod.lowercase() }.toSet()
        val requestMethods = CangjieLspRequest.entries
            .map { it.lspMethod }
            .filter { it !in LspCapabilityMatrix.lifecycleMethods }
            .toSet()
        requestMethods.forEach { method ->
            assertTrue(
                method.lowercase() in matrixMethods,
                "协议入口 $method 没有矩阵行；新增请求必须补一行契约测试",
            )
        }

        val knownMethods = requestMethods + LspCapabilityMatrix.lifecycleMethods + "workspace/executeCommand"
        LspCapabilityMatrix.rows.forEach { row ->
            assertTrue(
                row.protocolMethod.lowercase() in knownMethods.map { it.lowercase() }.toSet(),
                "矩阵行 ${row.id} 的协议方法 ${row.protocolMethod} 不在 CangjieLspRequest 中",
            )
            when (row.status) {
                LspCapabilityStatus.CONTRACT_AND_SEMANTIC ->
                    assertNotNull(row.semanticCoveredBy, "行 ${row.id} 标了真实语义覆盖却没写用例位置")

                LspCapabilityStatus.DEFECT_RECORDED ->
                    assertTrue(row.note.isNotBlank(), "行 ${row.id} 记录了缺陷，但没写缺陷说明")

                LspCapabilityStatus.CONTRACT_ONLY -> Unit
            }
            if (row.advertisement == LspAdvertisement.FORBIDDEN) {
                assertTrue(row.note.isNotBlank(), "行 ${row.id} 声明必须不广告，必须说明原因")
            }
            if (row.documentScoped) {
                assertTrue(
                    LspMatrixStage.MISSING_DOCUMENT in row.stages || row.note.isNotBlank(),
                    "文档级入口 ${row.id} 未覆盖文档缺失阶段，也没说明原因",
                )
            }
        }
    }

    /**
     * 能力开启：provider 必须声明，请求必须真正进入 facade，并返回契约结果。
     */
    @Test
    fun `enabled capabilities are advertised routed and answered`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createContractSession(factory).use { session ->
            session.openDocument(LspCapabilityMatrix.DOCUMENT_URI, CONTRACT_DOCUMENT_TEXT)
            val capabilities = session.initializeResult().capabilities
            val facade = factory.requireFacade()

            // 已宣告但实现未接入的行由 `declared capabilities honour their contract` 单独断言（预期失败）。
            LspCapabilityMatrix.rows
                .filter { LspMatrixStage.ENABLED in it.stages && it.status != LspCapabilityStatus.DEFECT_RECORDED }
                .forEach { row ->
                    when (row.advertisement) {
                        LspAdvertisement.REQUIRED -> assertTrue(
                            row.advertised(capabilities),
                            "行 ${row.id}：能力开启时必须在 initialize 结果里声明 provider",
                        )

                        LspAdvertisement.FORBIDDEN -> assertFalse(
                            row.advertised(capabilities),
                            "行 ${row.id}：服务端声明了「不广告」，provider 却出现在 initialize 结果里",
                        )
                    }
                    val result = row.invoke(session, LspCapabilityMatrix.target)
                    row.assertPositive(result)
                    row.facadeEntry?.let { entry ->
                        assertTrue(facade.wasInvoked(entry), "行 ${row.id}：请求没有进入 facade 入口 $entry")
                    }
                }
        }
    }

    /**
     * 能力关闭：不声明 provider、返回协议中性值、绝不进入 facade。
     */
    @Test
    fun `disabled capabilities are not advertised and return neutral results`() {
        val factory = ProtocolContractAnalysisFacadeFactory(
            ProtocolContractConfiguration(CangjieLspFeatureSet.none()),
        )
        createContractSession(factory).use { session ->
            session.openDocument(LspCapabilityMatrix.DOCUMENT_URI, CONTRACT_DOCUMENT_TEXT)
            val capabilities = session.initializeResult().capabilities
            val facade = factory.requireFacade()

            LspCapabilityMatrix.rows.filter { LspMatrixStage.DISABLED in it.stages }.forEach { row ->
                // 能力关闭时两类期望都收敛到「不得广告」。
                assertFalse(
                    row.advertised(capabilities),
                    "行 ${row.id}：能力关闭时不得声明 provider",
                )
                val result = row.invoke(session, LspCapabilityMatrix.target)
                row.assertNeutral(result)
                row.facadeEntry?.let { entry ->
                    assertFalse(facade.wasInvoked(entry), "行 ${row.id}：能力关闭时不应进入 facade 入口 $entry")
                }
            }
        }
    }

    /**
     * 文档缺失：返回协议中性值且不进入 facade（服务层必须自己兜住缺文档）。
     */
    @Test
    fun `missing documents return neutral results without facade calls`() {
        val factory = ProtocolContractAnalysisFacadeFactory()
        createContractSession(factory).use { session ->
            val facade = factory.requireFacade()

            LspCapabilityMatrix.rows
                .filter { it.documentScoped && LspMatrixStage.MISSING_DOCUMENT in it.stages }
                .forEach { row ->
                    val result = row.invoke(session, LspCapabilityMatrix.target)
                    row.assertNeutral(result)
                    row.facadeEntry?.let { entry ->
                        assertFalse(facade.wasInvoked(entry), "行 ${row.id}：文档缺失时不应进入 facade 入口 $entry")
                    }
                }
        }
    }

    /**
     * facade 抛错：请求必须以协议错误结束，不能挂起、也不能返回伪成功结果。
     */
    @Test
    fun `facade failures surface as protocol errors`() {
        val factory = ProtocolContractAnalysisFacadeFactory(
            ProtocolContractConfiguration(failRequestEntries = true),
        )
        createContractSession(factory).use { session ->
            session.openDocument(LspCapabilityMatrix.DOCUMENT_URI, CONTRACT_DOCUMENT_TEXT)

            LspCapabilityMatrix.rows.filter { LspMatrixStage.FACADE_FAILURE in it.stages }.forEach { row ->
                val failure = assertThrows(ExecutionException::class.java) {
                    row.invoke(session, LspCapabilityMatrix.target)
                }
                assertInjectedFailure(failure, row.facadeEntry!!)
            }

            // 失败注入后会话必须还能服务：错误不能把服务端执行器卡死（重复请求仍得到明确的协议错误）。
            val repeated = assertThrows(ExecutionException::class.java) {
                session.hover(session.hoverParams(LspCapabilityMatrix.DOCUMENT_URI, 0, 0))
            }
            assertInjectedFailure(repeated, "hover")
        }
    }

    /**
     * 已宣告但实现未接入的能力：按**已宣告契约**断言，用例预期失败。
     *
     * 计划要求「若失败，保留失败并记录缺陷，不改生产代码」。因此这里不写
     * `assertTrue(edits.isEmpty())` 之类的「顺着实现」的断言——那会把缺陷洗成绿灯，
     * 这份红灯就是缺陷本身的证据。
     *
     * 当前唯一一项：范围格式化（`textDocument/rangeFormatting`）。
     */
    @Test
    @Tag(KNOWN_DEFECT_TAG)
    fun `declared capabilities honour their contract`() {
        val declaredButUnwired = LspCapabilityMatrix.rows
            .filter { it.status == LspCapabilityStatus.DEFECT_RECORDED }
        assertTrue(declaredButUnwired.isNotEmpty(), "矩阵里没有 DEFECT_RECORDED 行，这个用例就该删掉")

        val factory = ProtocolContractAnalysisFacadeFactory()
        createContractSession(factory).use { session ->
            session.openDocument(LspCapabilityMatrix.DOCUMENT_URI, CONTRACT_DOCUMENT_TEXT)
            val capabilities = session.initializeResult().capabilities

            declaredButUnwired.forEach { row ->
                assertTrue(
                    row.advertised(capabilities),
                    "行 ${row.id} 被标记为「已宣告」，initialize 结果里却看不到 provider",
                )
                row.assertPositive(row.invoke(session, LspCapabilityMatrix.target))
            }
        }
    }

    /**
     * 用指定契约配置创建并初始化会话。
     */
    private fun createContractSession(
        factory: ProtocolContractAnalysisFacadeFactory,
    ): LspIntegrationTestSession = createSession(
        rootUri = "file:///workspace",
        capabilities = LspClientCapabilitiesBuilder.fullFeatured(),
        options = defaultServerOptions().copy(analysisFacadeFactory = factory::create),
    )

    companion object {
        /** 契约用例使用的文档文本：服务层只要求「文档存在」，语义由 facade 替身给出。 */
        private const val CONTRACT_DOCUMENT_TEXT: String = "contract body"
    }
}
