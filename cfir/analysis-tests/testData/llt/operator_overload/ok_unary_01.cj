// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

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
