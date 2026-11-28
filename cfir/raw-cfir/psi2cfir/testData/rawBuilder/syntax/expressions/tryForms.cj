// RUN_PIPELINE_TILL: FRONTEND
// try 表达式：catch（带类型 / 匿名 catch(e)）、多 catch、finally、资源（try (r = expr)）、try 作为表达式值与语句。
// 官方依据：Cangjie 文档《异常处理》；cjc 1.0.5 / 1.1.3 零诊断（catch 必须带括号，形如 catch (e: Exception)）。
class MyError <: Exception {
    public init() {
        super()
    }
}

func risky(): Int64 {
    return 1
}

func tryForms(): Int64 {
    let a = try {
        risky()
    } catch (e: MyError) {
        0
    }
    let b = try {
        risky()
    } catch (e: MyError) {
        0
    } catch (e: IllegalArgumentException) {
        1
    } finally {
        println("done")
    }
    try {
        risky()
    } catch (e: Exception) {
        println("failed")
    }
    return a + b
}
