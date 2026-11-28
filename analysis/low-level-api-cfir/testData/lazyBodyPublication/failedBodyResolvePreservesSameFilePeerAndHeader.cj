// A/M 故意未声明：只检查 raw 同文件独立发布和 header 保留，不期待语义诊断。
// 局部声明/default/表达式宏沿用 MacroRawOwnerSnapshotRawCfirTest；注解使用 raw 方括号实参语法。
package lowlevel.lazybodypublication

@!A[@M(9)]
func outer(@!A seed: Int64): Int64 {
    func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
    return @M(seed)
}

func peer(@!A seed: Int64): Int64 {
    func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
    return @M(seed)
}
