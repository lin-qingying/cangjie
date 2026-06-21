// FILE: change_abi.old.cj


package a

public enum A<T> {
    B | C | ...
}

public let m = A<Int64>.B

// FILE: change_abi.new.cj


package a

public enum A<T> {
    B | C | D(T) | ...
}

public let m = A<Int64>.D(3)

// FILE: change_abi.cj



import a.*

main() {
    match (m) {
        case B => 0
        case _ => 1
    }
}
