

class Data {
    var b: Float32
    public Data(let a: Int32) {
        b = 2.0
    }
}

func run(): Int64 {
    var value: Data = Data(1);
    if ((value.a == 1) && (value.b == 2.0)) {
        return 0
    } else {
        return 1
    }
}

main(): Int64 {
    return run()
}
