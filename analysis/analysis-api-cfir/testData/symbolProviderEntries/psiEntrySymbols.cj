package sample.symbol.provider

macro trace(value: Int64): Int64 {
    return value
}

enum Status {
    | Ready(Int64)
}

class Box {
    let backing: Int64 = 1

    prop state: Int64 {
        get() {
            backing
        }
        set(value) {
        }
    }

    init(seed: Int64) {
    }

    ~init() {
    }
}

extend Box {
    func prettyPrint(): Int64 {
        return this.state
    }
}

func useAll(box: Box): Int64 {
    let (left, right) = (1, 2)
    let lambda = {input => input + left + right}
    return trace(box.state + left + right)
}
