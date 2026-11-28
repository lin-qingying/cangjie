package completion.applicability

class Box {}

extend Box {
    func unwrap(): Int64 {
        1
    }
}

func use(box: Box): Int64 {
    box.unwrap()
    return unknownValue()
}
