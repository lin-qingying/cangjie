


func getHeight(height: Int64): Int64 {
    return height * 2
}

class C {}

class Data<T> {
    public let width: T

    public init(x: C) {
        width = <!TYPE_MISMATCH!>C<!>
    }
}

func run(): Int32 {
    let data: Data<(Int64) -> Int64> = <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>D<!>ata<(Int64) -> Int64>(<!TYPE_MISMATCH!>getHeight<!>)
    var res = data.width(5)
    if (res == 10) {
        return 0
    }
    return 1
}

main(): Int32 {
    return run()
}
