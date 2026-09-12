// CFIR 检查同一客户端分别依赖旧、新库版本时的语义；两个版本属于独立模块。
// MODULE: oldLibrary
// FILE: change_lib.old.cj


package a

public enum A {
    B | C | ...
}

public var m = C

// MODULE: newLibrary
// FILE: change_lib.new.cj


package a

public enum A {
    B | C | D | ...
}

public var m = D

// MODULE: oldClient(oldLibrary)
// FILE: change_lib.old_client.cj

import a.*

main() {
    match (m) {
        case B => 0
        case C => 1
        case _ => 2
    }
}

// MODULE: newClient(newLibrary)
// FILE: change_lib.cj


import a.*

main() {
    match (m) {
        case B => 0
        case C => 1
        case _ => 2
    }
}
