package sample.lowlevel.contextcollector.generated

func makePrinter(offset: Int64): (Int64) -> Int64 {
    return { value =>
        <expr>value + offset</expr>
    }
}
