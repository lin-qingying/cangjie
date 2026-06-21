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
<!UNUSED_IMPORT!>import std.deriving.Derive<!>

main() {
}


