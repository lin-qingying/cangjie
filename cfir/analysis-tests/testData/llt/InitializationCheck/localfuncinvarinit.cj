main():Int64 {
    return 0
}

open class Boo{
    public var a: Int32 = 1
    public let b: Int32 = 2
}
class Aoo <: Boo{
    public var c = if(true){
        func foo1(x!:Int32=<!ILLEGAL_USAGE_OF_SUPER_MEMBER!>a<!>){return x}
    }

    let d = if(true) {
        func foo(_:Int32){
            func bar(x!:Int32=<!ILLEGAL_USAGE_OF_SUPER_MEMBER!>a<!>) {
                x
            }
            bar()
        }
    }
}

