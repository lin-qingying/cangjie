package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.mock.MockProject
import org.cangnova.cangjie.codeinsight.api.members.CangJieMemberGenerationServiceRegistry

/**
 * 成员生成能力的 headless 装配入口。
 *
 * IDE 插件通过 plugin.xml 声明项目服务即可拿到生成实现；LSP 等 headless 容器
 * 没有插件描述符，需要显式把注册表与真实实现装进 MockProject。
 * 实现类本身保持 internal——宿主只依赖 [CangJieMemberGenerationService] 窄接口。
 */
object CangJieMemberGenerationHeadlessRegistrar {
    /**
     * 在 headless project 中注册生成服务注册表，并填入真实实现。
     *
     * 注册表已存在时不重复注册，也不覆盖宿主已填入的实现。
     */
    fun registerProjectServices(project: MockProject) {
        if (project.getService(CangJieMemberGenerationServiceRegistry::class.java) == null) {
            project.registerService(CangJieMemberGenerationServiceRegistry::class.java)
        }
        val registry = project.getService(CangJieMemberGenerationServiceRegistry::class.java)
        if (registry.service == null) {
            registry.service = CangJieMemberGenerationServiceImpl()
        }
    }
}
