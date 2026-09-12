// CFIR 检查同一客户端分别依赖旧、新库版本时的语义；两个版本属于独立模块。
// MODULE: oldLibrary
// FILE: change_abi_recursive.old.cj


package a

public enum A {
    B | C(Int64) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.B

// MODULE: newLibrary
// FILE: change_abi_recursive.new.cj


package a

public enum A {
    B | C(Int64) | D(X) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.D(X(B))

// MODULE: oldClient(oldLibrary)
// FILE: change_abi_recursive.old_client.cj

import a.*

main() {
    match (m) {
        case B => 0
        case C(_) => 1
        case _ => 2
    }
}

// MODULE: newClient(newLibrary)
// FILE: change_abi_recursive.cj



import a.*

main() {
    match (m) {
        case B => 0
        case C(_) => 1
        case _ => 2
    }
}
