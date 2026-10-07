@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.codeinsight.api.members.CangJieGeneratableMember
import org.cangnova.cangjie.codeinsight.api.members.CangJieMemberGenerationService
import org.cangnova.cangjie.codeinsight.api.members.CangJieMemberGenerationServiceRegistry
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.services.TestServices

/**
 * 为声明建议区段测试装配生成服务。
 *
 * 注册的是**假实现**而不是真实生成器：本区段的契约是「消费窄接口」，
 * 真实的成员收集与渲染由 `override-implement` 自己的用例验证
 * （`CangJieMemberGenerationServiceImplTest`）。两边分开测，
 * 区段测试就不会因为生成器换了实现而变红。
 */
object DeclarationSuggestionTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 注册生成服务注册表并填入假实现。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        // 注册表本身是项目服务；先把服务注册进去，再取回实例填实现。
        project.registerService(CangJieMemberGenerationServiceRegistry::class.java)
        project.getService(CangJieMemberGenerationServiceRegistry::class.java).service = FakeGenerationService
    }
}

/**
 * 固定的生成服务：无论问哪个类型都返回同一条成员。
 */
private object FakeGenerationService : CangJieMemberGenerationService {
    override fun collectUnimplementedMembers(typeStatement: CjTypeStatement): List<CangJieGeneratableMember> =
        listOf(FakeMember)
}

/**
 * 固定的可生成成员。
 */
private object FakeMember : CangJieGeneratableMember {
    override val name: String = "greet"

    override val renderedText: String = "func greet(name: String): String"
}
