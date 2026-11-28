// A/M 故意未声明：只检查 raw 正文重建和根后处理失败，不期待语义诊断。
// 语法沿用 MacroRawOwnerSnapshotRawCfirTest.LOCAL_SOURCE。
package lowlevel.lazybodypublication

func outer(seed: Int64): Int64 {
    func local(@!A value!: Int64 = @M(seed)): Int64 { return @M(value) }
    return @M(seed)
}
