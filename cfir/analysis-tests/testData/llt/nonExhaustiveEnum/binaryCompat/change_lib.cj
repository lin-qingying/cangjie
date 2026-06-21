// FILE: change_lib.old.cj


package a

public enum A {
    B | C | ...
}

public var m = C

// FILE: change_lib.new.cj


package a

public enum A {
    B | C | D | ...
}

public var m = D

// FILE: change_lib.cj


// not CJNATIVE does not support binary compatibility

import a.*

main() {
    match (m) {
        case B => 0
        case C => 1
        case _ => 2
    }
}
