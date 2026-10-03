// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：泛型形参上的变型标注 `in T` / `out T`（官方 1.0.5 / 1.1.3 的泛型形参只接受类型名，`in` / `out` 是保留字）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_name "expected a generic type name after '<' in generic" / parse_expected_right_delimiter。
class Variance<in T> {
    public init() {}
}

interface VarianceOut<out T> {
}
