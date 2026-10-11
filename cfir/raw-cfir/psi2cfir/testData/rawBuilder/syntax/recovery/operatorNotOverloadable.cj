// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：不可重载的操作符名（get / set / 前缀自增自减 / ~ / 逻辑与或）。
// 官方 cjc 1.0.5 / 1.1.3：parse_invalid_overloaded_operator "cannot overload operator 'get'" 等。
class NotOverloadable {
    public operator func get(i: Int64): Int64 {
        return 0
    }

    public operator func ++(): Int64 {
        return 0
    }

    public operator func ~(): Int64 {
        return 0
    }
}
