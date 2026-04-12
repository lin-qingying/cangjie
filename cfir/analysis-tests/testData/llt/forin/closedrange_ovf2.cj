// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f %overflowOpt -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args | compare %f

class Counter {
    var c = 0
    func count(): Unit {
        c++
        if (c >= 100) {
            throw Exception("count exceeded")
        }
    }
}

func rangem2e(bottom: Int64, top: Int64): Unit {
    let c = Counter()
    for (i in top..=bottom : -2) {
        c.count()
    }
}

func rangem1e(bottom: Int64, top: Int64): Unit {
    let c = Counter()
    for (i in top..=bottom : -1) {
        c.count()
    }
}

func test(name: String, fun: (Int64, Int64) -> Unit, bottom: Int64, top: Int64) {
    print("${name}: ")
    try {
        fun(bottom, top)
        println("ok")
    } catch (e: Exception) {
        println("${e}")
    }
}

main() {
    let max = 0x7FFFFFFFFFFFFFFF
    let min = -max - 1
    test("rangem1e", rangem1e, min, min + 1)
    test("rangem2e", rangem2e, min, min + 1)
}

/* SCAN
rangem1e: ok
rangem2e: ok
*/
