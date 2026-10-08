package org.cangnova.cangjie.ide.completion.impl.shared.resolve

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot

/**
 * 按候选身份**重新进入分析**恢复出声明符号。
 *
 * 用途是补全的「延迟文档」：候选快照按契约**不持有**符号或会话
 * （弹窗生命周期长于分析会话），因此文档不能在生成候选时顺手算好存下来——
 * 那会让每个候选都背一份渲染结果，而绝大多数候选永远不会被查看。
 * 用户真要文档时（LSP `completionItem/resolve`、IDE 快速文档）再按身份恢复一次。
 *
 * 是普通 interface 而不是 `fun interface`：抽象方法带类型参数（`<T>`），
 * 而 Kotlin 的 `fun interface` 不允许（编译期直接报错）。
 *
 * **恢复必须用与快照工厂同一套身份算法**，因此实现放在生成候选的那一层
 * （`impl-cfir`），消费端只拿窄接口。放在这里而不是 `contracts`：
 * 接口签名带 [CaDeclarationSymbol]，而 `contracts` 刻意不依赖 Analysis API。
 */
interface CompletionDeclarationResolver {
    /**
     * 在**调用方的分析上下文内**按身份恢复声明并立刻使用它；恢复不出来时返回 `null`。
     *
     * 签名刻意是「恢复 + 立即使用」而不是「返回符号」：分析符号**不得跨会话带出**
     * （实测：返回符号后在会话外读它的任何成员都会抛
     * `CaInaccessibleLifetimeOwnerAccessException: Called outside an analyze context`）。
     * 调用方因此必须自己处在 `analyze(...)` 块里，把「拿它干什么」（渲染文档等）交给 [use]。
     *
     * 会话用**显式参数**而不是接收者：接口成员带接收者时要求两个接收者同时可解析，
     * 调用点会变成 `with(resolver) { ... }` 的绕法，不如把会话直接传进来直白。
     *
     * @param session 调用方所在的分析会话（符号只在这个会话内有效）。
     * @param snapshot 待恢复的候选。
     * @param use 在会话内消费恢复出的符号。
     */
    fun <T> resolveDeclaration(
        session: CaSession,
        snapshot: CompletionSnapshot,
        use: (CaDeclarationSymbol) -> T,
    ): T?
}

/**
 * 项目级解析器装配点。
 *
 * 与成员生成窄接口同一形态：宿主或上游实现在装配阶段装入，消费端取不到时**降级为不填文档**。
 */
@Service(Service.Level.PROJECT)
class CompletionDeclarationResolverRegistry {
    /**
     * 当前生效的解析器；未装配时为 `null`。
     */
    @Volatile
    var service: CompletionDeclarationResolver? = null
}

/**
 * 取解析器装配点；宿主没注册该服务时返回 `null`（装配方据此跳过装配，而不是抛异常）。
 */
fun completionDeclarationResolverRegistry(project: Project): CompletionDeclarationResolverRegistry? =
    project.serviceOrNull()

/**
 * 取当前工程的候选声明解析器；未装配（或宿主没注册该服务）时返回 `null`。
 *
 * 与统计接缝同一纪律：**旁路能力缺失不得影响主路径**——取不到解析器时
 * `resolve` 只是不填文档，绝不抛异常（实测过统计那处忘了这条会让 8 个用例一起红）。
 */
fun completionDeclarationResolverOrNull(project: Project): CompletionDeclarationResolver? =
    project.serviceOrNull<CompletionDeclarationResolverRegistry>()?.service
