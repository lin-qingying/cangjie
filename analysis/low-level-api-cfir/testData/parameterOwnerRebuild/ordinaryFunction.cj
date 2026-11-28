// A/M 故意未声明：只验证 raw 私有副本的结构 owner 与词法 owner，不期待语义诊断。
package lowlevel.parameterownerrebuild

func outer(@!A value!: Int64 = @M(1)): Int64 { return @M(value) }
