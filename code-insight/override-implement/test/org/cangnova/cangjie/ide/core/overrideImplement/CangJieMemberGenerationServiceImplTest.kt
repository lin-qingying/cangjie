package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 成员生成窄接口的回归。
 *
 * 断言到「收集结果 + 渲染文本」为止：接口的职责就是把这两样交给消费方，
 * 至于插入、写事务与 UI 属于动作处理器，不在接口契约里。
 *
 * 这条用例同时锁住一个**接口边界**：适配器不得从渲染文本里截取名字
 * （`func greet(name: String)` 里截出来的会是 `func greet`），
 * 名字必须来自符号。
 */
@OptIn(CaExperimentalApi::class)
class CangJieMemberGenerationServiceImplTest : AbstractAnalysisApiExecutionTest(
    "code-insight/override-implement/testData/members",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun unimplementedMembersAreRendered(unimplementedMembersAreRendered: CjFile) {
        val typeStatement = PsiTreeUtil
            .findChildrenOfType(unimplementedMembersAreRendered, CjTypeStatement::class.java)
            .first { it.name == "Host" }

        val members = CangJieMemberGenerationServiceImpl().collectUnimplementedMembers(typeStatement)

        assertTrue(
            members.any { it.name == "greet" },
            "未实现的抽象成员必须被收集出来，实际 ${members.map { it.name }}",
        )
        val greet = members.first { it.name == "greet" }
        assertTrue(
            greet.renderedText.contains("func greet"),
            "必须给出渲染后的声明文本，实际 ${greet.renderedText}",
        )
    }
}
