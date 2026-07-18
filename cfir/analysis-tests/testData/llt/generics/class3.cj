

func codegenUnitTest(): Int32 {
    return <!RETURN_TYPE_MISMATCH!>run()<!>
}

struct Roo<T> {
    public var r: T
    public init(x: T) {
        r = x
    }
}

class Data<T> {
    public let count: T
    public init(x: T) {
        count = x
    }
    public func getFoo<V>(x: Roo<V>): Roo<V> {
        return Roo<V><!NO_VALUE_FOR_PARAMETER!>()<!>
    }
}

func run(): Int64 {
    var a: Int32 = 3
    let roo: Roo<Int32> = Roo<Int32>(a)
    var data: Data<Int32> = Data<Int32>(a)
    var foo: Roo<Roo<Int32>> = data.getFoo<Roo<Int32>>(<!TYPE_MISMATCH!>roo<!>)
    var res: Int32 = <!TYPE_MISMATCH!>foo.r<!>
    if (res == 3) {
        return 0
    }
    return 1
}
