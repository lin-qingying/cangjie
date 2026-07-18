// FILE: other.cj
import std.deriving.Derive

@Derive[Equatable]
class B{
    var a = 1
    init(temp:Int64){
        a = temp
    }
}



// FILE: unused003.cj
import <!UNUSED_IMPORT!>std.deriving.Derive<!>

main() {
}

