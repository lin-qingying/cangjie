// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：类型别名上写 where 约束（官方只允许函数 / 类 / 接口 / 结构等声明）。
// 官方 cjc 1.0.5 / 1.1.3：parse_expected_character "expected ';' or '<NL>', found keyword 'where'"。
interface Bounded {}

type BoundedAlias<T> = T where T <: Bounded
