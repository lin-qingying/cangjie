enum Color {
    Red | Green | Blue

    public operator func ()(brightness: Float64): Float64 {
        return brightness
    }
}

func testEnumInvoke(): Unit {
    let c = Red
    Red(0.5)
    c(0.5)
}
