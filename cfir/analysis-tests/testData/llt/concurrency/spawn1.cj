


var count: Int64 = 0

main(): Int64 {
    let f: Future<Unit> = spawn {
        => let fut: Future<Int64> = spawn {
            =>
            count = count + 1
            return 0
        }
    }
    return 0
}
