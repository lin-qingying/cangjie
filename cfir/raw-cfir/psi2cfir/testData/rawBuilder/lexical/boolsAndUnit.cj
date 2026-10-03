// RUN_PIPELINE_TILL: FRONTEND
// 布尔与 Unit 字面量：true / false / ()。
// 官方依据：Cangjie 文档《基本数据类型》；cjc 1.0.5 / 1.1.3 零诊断。
func boolAndUnit(): Unit {
    let t = true
    let f = false
    let notT = !t
    let and = t && f
    let or = t || f
    let unit: Unit = ()
    let unitCall = unit
    let _ = (notT, and, or, unitCall)
}
