package org.cangnova.cangjie.lsp.quickfix

/** 由服务端能力声明并通过 workspace/executeCommand 路由的 quick-fix 命令名。 */
object LspQuickFixCommands {
    const val APPLY_MOD_COMMAND: String = "cangjie.quickfix.applyModCommand"
}
