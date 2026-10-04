package completion.applicability

class Box {}

class Crate {}

extend Box {
    func unwrap(): Int64 {
        1
    }
}

func use(crate: Crate): Int64 {
    crate.unwrap()
}
