// 析构器使用 ~init() 文法；M 故意未声明，只验证 raw 重建，不期待语义诊断。
package lowlevel.parameterownerrebuild

class Owner {
    ~init() { @M() }
}
