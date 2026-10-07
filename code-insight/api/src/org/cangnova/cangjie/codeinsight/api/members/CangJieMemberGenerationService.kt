package org.cangnova.cangjie.codeinsight.api.members

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 一个可生成的成员：**身份 + 已渲染的声明文本**。
 *
 * 刻意只暴露这两样：补全需要「叫什么」用于匹配与展示，需要「渲染成什么」用于预览与插入。
 * 内部的 chooser 对象、符号指针、analysis 会话都不在此列——它们属于生成器的实现细节，
 * 一旦泄漏，补全就得跟着生成器的重构走。
 */
interface CangJieGeneratableMember {
    /** 成员的短名。 */
    val name: String

    /** 渲染后的声明源码，供候选预览与插入。 */
    val renderedText: String
}

/**
 * 成员生成能力的**窄接口**。
 *
 * 补全的「声明建议」候选要用到「这个类型还缺哪些成员」以及「生成出来长什么样」，
 * 但不需要知道 chooser、UI、写事务与插入位置——那些留在动作处理器里。
 *
 * 提取这条接口的目的是把补全与 `override-implement` 模块的**内部实现**解耦：
 * 补全只依赖本接口，模块重构（换 chooser、换插入策略）不会波及补全。
 * 模块间也不得访问 `internal` 实现。
 *
 * **只覆盖 implement 语义**（收集未实现的抽象成员）。模块名字里的 override 一半
 * 目前**没有具体处理器**：`GenerateMembersHandler(toImplement = false)` 是抽象基类，
 * 既无具体子类也无注册。接口因此不提供 `toImplement` 开关——
 * 加一个恒走空分支的开关，正是本变更里反复出现的「声明了但不工作」。
 * 等 override 侧真的有处理器时，再作为独立方法加进来。
 */
interface CangJieMemberGenerationService {
    /**
     * 收集该类型中**尚未实现**的抽象成员。
     *
     * 类型不可分析（未解析、不是类状声明）时返回空列表，由调用方决定是否提示。
     */
    fun collectUnimplementedMembers(typeStatement: CjTypeStatement): List<CangJieGeneratableMember>
}

/**
 * 生成能力的注册表。
 *
 * 与 `CangJieCompletionServiceRegistry` 同形：宿主侧把实现注册进来，
 * 消费方（补全）只依赖接口，不依赖 `override-implement` 模块。
 * 未注册时补全不产出声明建议候选——这是明确的降级，不是静默失败。
 */
@Service(Service.Level.PROJECT)
class CangJieMemberGenerationServiceRegistry {
    /**
     * 当前生效的生成服务；未注册时为 `null`。
     */
    @Volatile
    var service: CangJieMemberGenerationService? = null
}

/**
 * 取得项目级成员生成注册表。
 */
fun memberGenerationRegistry(project: Project): CangJieMemberGenerationServiceRegistry =
    project.service()
