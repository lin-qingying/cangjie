// 普通构造器使用 init 文法；A/M 故意未声明，只验证 raw owner，不期待语义诊断。
package lowlevel.parameterownerrebuild

struct Owner {
    public init(@!A value!: Int64 = @M(1)) { @M(value) }
}
