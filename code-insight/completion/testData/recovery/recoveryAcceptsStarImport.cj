// MODULE: dep
// FILE: dep.cj
package recovery.dep

public func recoverTarget(): Int64 { 7 }

// MODULE: main(dep)
// MAIN_MODULE
// FILE: main.cj
package recovery.main

// 星号导入：cjc 1.1.3 实测可编译，且包内声明直接可用。
import recovery.dep.*

func site(): Int64 {
    recoverTarget
}
