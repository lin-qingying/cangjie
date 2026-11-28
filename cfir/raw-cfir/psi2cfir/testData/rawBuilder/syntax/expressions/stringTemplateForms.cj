// RUN_PIPELINE_TILL: FRONTEND
// 字符串运算与插值（与 lexical 组互补）：+ 拼接、== / != 比较、多行字符串插值、原始字符串插值与拼接、插值中的容器 / 成员 / 调用链。
// 官方依据：Cangjie 文档《字符串类型》；cjc 1.0.5 / 1.1.3 零诊断。
class User {
    var name: String = "cj"

    func greet(): String {
        return "hi ${name}"
    }
}

func stringTemplateForms(u: User, xs: Array<String>, n: Int64): String {
    let a = "a" + "b"
    let eq = "a" == "b"
    let ne = "a" != "b"
    let c = "sum ${n + 1} count ${xs.size} first ${xs[0]}"
    let d = """
        name ${u.name}, greet ${u.greet()}
    """
    let e = #"raw ${n}"#
    let f = e + "plain"
    let g = "chain ${u.greet()}!"
    let h = if (eq || ne) {
        "!"
    } else {
        "?"
    }
    return a + c + d + f + g + h
}
