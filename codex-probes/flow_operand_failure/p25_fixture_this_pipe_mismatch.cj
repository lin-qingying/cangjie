func g(v: Int64): Int64 { v }

class FlowOperandFailure {
    var x = 1

    func thisLeftPipe(): Unit {
        this |> g
    }
}

main() {}
