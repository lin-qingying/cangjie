// 字符串模板：短插值、长插值、多行字符串插值与原始字符串插值。
// 官方依据：Cangjie 文档《字符串类型》；cjc 1.0.5 / 1.1.3 零诊断。
class User {
    var name: String = "cj"

    public init() {}

    public func greet(): String {
        return "hi ${name}"
    }
}

func stringTemplates(u: User, xs: Array<String>, n: Int64): String {
    let short = "value: $n"
    let long = "sum ${n + 1} count ${xs.size} first ${xs[0]}"
    let member = "user ${u.name} greet ${u.greet()}"
    let multiline = """
        line ${n}
        raw ${xs.size}
    """
    let raw = #"hash ${n} quoted"#
    let concat = "a" + "b"
    let _ = (short, long, member, multiline, raw, concat)
    return long
}
