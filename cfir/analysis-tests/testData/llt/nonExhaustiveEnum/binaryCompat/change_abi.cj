// CFIR 检查同一客户端分别依赖旧、新库版本时的语义；两个版本属于独立模块。
// MODULE: oldLibrary
// FILE: change_abi.old.cj


package a

public enum A<T> {
    B | C | ...
}

public let m = A<Int64>.B

// MODULE: newLibrary
// FILE: change_abi.new.cj


package a

public enum A<T> {
    B | C | D(T) | ...
}

public let m = A<Int64>.D(3)

// MODULE: oldClient(oldLibrary)
// FILE: change_abi.old_client.cj

import a.*

main() {
    match (m) {
        case B => 0
        case _ => 1
    }
}

// MODULE: newClient(newLibrary)
// FILE: change_abi.cj



import a.*

main() {
    match (m) {
        case B => 0
        case _ => 1
    }
}
