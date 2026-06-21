

internal import std.sync.*
internal import std.time.*
internal import std.convert.*

var reps = 180000000

func benchmarkAtomicSwapUInt32() {
    let x = AtomicUInt32(1)
    var y: UInt32 = 1

    for (i in 0..reps) {
        y = x.swap(y)
    }
}

main(): Int64 {
    var startTime: DateTime
    var endTime: DateTime
    var perTime: Float64

    startTime = DateTime.now()
    benchmarkAtomicSwapUInt32()
    endTime = DateTime.now()

    perTime = Float64((endTime - startTime).toNanoseconds()) / Float64(reps)
    println("BenchmarkAtomicSwapUInt32: " + perTime.format(".2") + " ns/op")
    return 0
}
