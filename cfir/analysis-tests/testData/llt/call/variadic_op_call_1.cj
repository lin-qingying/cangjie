

enum ArgEnum {
    | First(Int64)
    | Second(String)
}

let v = VArg()

struct VArg {
    operator func ()(args: Array<ArgEnum>) {
        for (arg in args) {
            match (arg) {
                case First(x) => println(x)
                case Second(y) => println(y)
            }
        }
    }
}

func withArgs(body: (ArgEnum, ArgEnum, ArgEnum) -> Unit) {
    body(First(2), First(4), Second("hello"))
}

main() {
    withArgs {
        a, b, c => v(a, b, c)
    }
}
