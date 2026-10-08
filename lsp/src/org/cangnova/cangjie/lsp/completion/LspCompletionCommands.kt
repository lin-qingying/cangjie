package org.cangnova.cangjie.lsp.completion

/**
 * LSP 补全用到的命令与数据约定。
 *
 * 集中在一处，避免「转换端写了一个字符串、路由端比了另一个」这类静默失效——
 * 命令名不匹配的表现是客户端接受候选后什么都没发生，且不报错。
 */
object LspCompletionCommands {
    /**
     * 「接受候选并插入」的命令名。
     *
     * 走 LSP 标准路径：候选带**空** `textEdit` + 本命令，客户端在接受候选后**自动执行**该命令，
     * 由服务端计算编辑并用 `workspace/applyEdit` 请求落盘。
     * 服务端因此需要在 `workspace/executeCommand` 的 `commands` 能力里声明它（设计 §7.11.3）。
     */
    const val APPLY_COMPLETION: String = "cangjie.completion.apply"

    /**
     * 候选项 `data` 里携带的会话键字段名。
     *
     * `data` 是结构化 JSON：只放**定位信息**（会话键 + 下标），不放候选内容——
     * 候选必须由服务端按会话键去会话存储里取，不能由客户端回传后直接信任。
     */
    const val SESSION_KEY_FIELD: String = "sessionKey"

    /**
     * 候选项 `data` 里携带的候选下标字段名。
     *
     * `resolve`（延迟文档）只拿到一个 `CompletionItem`，靠这个下标才能回到是哪个候选；
     * 用标签反查在同名重载上会挑错。
     */
    const val INDEX_FIELD: String = "index"

    /**
     * 命令实参里的候选下标字段位序。
     *
     * 实参是 `[会话键, 下标]` 两个位置参数：LSP 命令实参没有字段名，
     * 因此用位置约定，并由本对象与转换端、路由端共用一个常量，避免错位。
     */
    const val ARG_INDEX_OFFSET: Int = 1
}
