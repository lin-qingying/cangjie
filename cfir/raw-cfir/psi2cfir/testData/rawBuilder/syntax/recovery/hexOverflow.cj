// RUN_PIPELINE_TILL: FRONTEND
// 恢复场景：十六进制字面量超出类型值域（官方 sema_exceed_num_value_range "the number '0xFFFFFFFFFFFFFFFF' exceeds the value range of type 'UInt64'"，锚字面量）。
// 同类的十进制 / UInt8 越界见 integerOverflow.cj。
// 官方 cjc 1.0.5 / 1.1.3：sema_exceed_num_value_range。
func hexOverflow(): Unit {
    let a = 0xFFFFFFFFFFFFFFFF
    let _ = a
}
