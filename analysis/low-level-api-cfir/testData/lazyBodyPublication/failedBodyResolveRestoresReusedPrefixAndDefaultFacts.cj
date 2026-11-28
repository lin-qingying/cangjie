// A/M 故意未声明：只检查 raw 旧前缀/default 复用与撤销，不期待语义诊断。
// 语法沿用 MacroRawOwnerSnapshotRawCfirTest.LOCAL_SOURCE。
package lowlevel.lazybodypublication

func outer(@!A seed!: Int64 = @M(0)): Int64 {
    func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
    return @M(seed)
}
