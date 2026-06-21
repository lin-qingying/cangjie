

main(): Int64 {
    return run()
}

func run(): Int64 {
    if (Foo() == "good") {
        return 0
    }
    return 1
}

var a: Int32 = 3
var b: Int32 = 4
let score: Int32 = 90

func Foo(c!: String = match (score) {
        case 0 => "zero"
        case 90 => "good"
        case _ => "not a valid score"
    }): String {
    return c
}
