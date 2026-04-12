// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

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
    println(!"BenchmarkAtomicSwapUInt32: " + perTime.format(".2") + " ns/op")
    return 0
}

/* SCAN
error: invalid unary operator '!' on type 'Struct-String'
  ==> err_unary_01.cj:34:13:
   | 
34 |     println(!"BenchmarkAtomicSwapUInt32: " + perTime.format(".2") + " ns/op")
   |             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func !()' for type 'Struct-String'

1 error generated, 1 error printed.
*/
