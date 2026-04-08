package sample.symbol.equivalence

interface Printable {
    func prettyPrint(): Int64
}

enum TimeUnit {
    | Year(Int32)
    | Month(Int32, Int32)
}

class Document {
}

class Holder {
    prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }
}

extend Document <: Printable {
    func prettyPrint(): Int64 {
        return 1
    }
}

func run(holder: Holder, time: TimeUnit): Int64 {
    let lambda = {input => input + holder.state}
    return holder.state + match (time) {
        case Year(y) => y
        case Month(y, _) => y
    }
}
