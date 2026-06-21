import std.deriving.Derive
@Derive[Equatable]
class A{
    var a = 1
    init(temp:Int64){
        a = temp
    }
}

main() {
}


