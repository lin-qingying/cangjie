// FILE: change_abi_recursive.old.cj


package a

public enum A {
    B | C(Int64) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.B

// FILE: change_abi_recursive.new.cj


package a

public enum A {
    B | C(Int64) | D(X) | ...
}

public struct X {
    public X(let value: A) {}
}

public let m = A.D(X(B))

// FILE: change_abi_recursive.cj



import a.*

main() {
    match (m) {
        case B => 0
        case C(_) => 1
        case _ => 2
    }
}
