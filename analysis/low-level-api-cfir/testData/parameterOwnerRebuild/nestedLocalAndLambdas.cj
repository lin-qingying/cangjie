// 局部/default 语法沿用 raw owner 夹具；lambda 参数遵循官方文法。A/M 未声明，不期待语义诊断。
package lowlevel.parameterownerrebuild

func outer(@!A seed!: Int64 = combine({ value: Int64 => return @M(value) }, @M(0))): Int64 {
    func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
    combine({ value: Int64 => return @M(value) }, @M(seed))
    return @M(seed)
}

func combine(action: (Int64) -> Int64, value: Int64): Int64 { return action(value) }
