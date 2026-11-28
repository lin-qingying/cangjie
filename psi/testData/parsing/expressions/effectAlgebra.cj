// 代数效果：try { perform } handle { resume with }。
// 官方依据：Cangjie 文档《代数效果》；本仓库解析器已支持（见 raw-cfir 语料 effectTryHandleOfficial.cj）。
// 说明：cjc 1.0.5 尚不支持 perform / handle / resume（报 expected 'catch' or 'finally'），
// 本文件为解析器前瞻语法数据，cjc 审查属有意保留的例外。
class TextCommand {
    public init() {}
}

class LogCommand {
    public init() {}
}

func withEffects(): Int64 {
    let value = try {
        perform TextCommand()
        1
    } handle (cmd: TextCommand) {
        resume with 1
    }
    return value
}

func multiHandle(): Int64 {
    let v = try {
        perform TextCommand()
        perform LogCommand()
        0
    } handle (cmd: TextCommand) {
        resume with 1
    } handle (cmd: LogCommand) {
        resume with 2
    }
    return v
}
