

var a = 0
let b = c()

class c {}

struct d {
    public init(e: Float32, f: c) {}
    public init() {
        match {
            case _ => d((<!TYPE_MISMATCH!>a++<!>), match {
                case _ => b
            })
        }
    }
}
