// RUN_PIPELINE_TILL: FRONTEND
// 原始标识符：反引号包裹，可把关键字、数字开头的名字用作普通标识符（函数名 / 局部变量 / 成员名 / 形参名 / 成员访问）。
// 注意：中文原始标识符放在类型位置（返回值类型）时 cjc 1.0.5 报 undeclared type name，故这里只在值位置使用。
// 官方依据：Cangjie 文档《标识符》「原始标识符」；cjc 1.0.5 / 1.1.3 零诊断。
func `if`(): Int64 {
    let `while` = 1
    var `a1b2c3`: Int64 = 2
    let `_abc` = 3
    return `while` + `a1b2c3` + `_abc`
}

class `class` {
    var `var`: Int64 = 0

    public init() {
        this.`var` = 1
    }
}

func `for`(`in`: Int64): Int64 {
    let y = `in` + 1
    let z = `for`(y)
    return z
}

func cjkRawId(): Int64 {
    let `仓颉` = 7
    return `仓颉`
}
