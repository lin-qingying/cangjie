// MODULE: dep
// FILE: dep.cj
package recovery.dep

public func recoverTarget(): Int64 { 7 }

// MODULE: main(dep)
// MAIN_MODULE
// FILE: main.cj
package recovery.main

// 声明级导入：cjc 1.1.3 实测可编译，且 `recoverTarget` 直接可用。
import recovery.dep.recoverTarget

func site(): Int64 {
    recoverTarget
}
