// 主构造器名字与名义类型一致；A/M 故意未声明，只验证 raw owner，不期待语义诊断。
package lowlevel.parameterownerrebuild

struct Owner {
    public Owner(@!A value!: Int64 = @M(1)) { @M(value) }
}
