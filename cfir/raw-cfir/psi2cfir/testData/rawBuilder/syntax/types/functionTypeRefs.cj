// RUN_PIPELINE_TILL: FRONTEND
// 函数类型：无参、多参、具名形参（参数列表要么全具名要么全无名）、返回值是函数类型（高阶，嵌套 lambda 用花括号块体）、
// 出现在形参 / 返回值 / prop / 类型别名中。
// 官方依据：Cangjie 文档《函数类型》；cjc 1.0.5 / 1.1.3 零诊断。
func noArgs(f: () -> Unit): Unit { f() }
func oneArg(f: (Int64) -> Int64): Int64 { return f(1) }
func twoArgs(f: (Int64, String) -> Bool): Bool { return f(1, "x") }
func namedArgs(f: (x: Int64, y: String) -> Bool): Bool { return f(1, "x") }
func returningFunction(): (Int64) -> (String) -> Int64 { return { a: Int64 => { b: String => a } } }

class WithFuncProp {
    private var handler: (Int64) -> Int64 = { a => a }

    mut prop handlerProp: (Int64) -> Int64 {
        get() {
            return handler
        }

        set(v) {
            handler = v
        }
    }
}

type Callback = (Int64) -> Unit
type HigherOrder = ((Int64) -> Int64) -> Int64
type NamedHigherOrder = (f: (Int64) -> Int64) -> Int64
type NamedTuple = (x: Int64, y: String)
