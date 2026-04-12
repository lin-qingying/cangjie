// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC-PIPE: %run %run_opt %output %run_args | compare %f
// EXEC: %compiler %f -o %output %overflowOpt
// RUN-EXEC-PIPE: %run %run_opt %output %run_args | compare %f

class Counter {
    var c = 0
    func count(): Unit {
        c++
        if (c >= 100) {
            throw Exception("count exceeded")
        }
    }
}

func range2e(bottom: Int64, top: Int64): Unit {
    let c = Counter()
    for (_ in bottom..=top : 2) {
        c.count()
    }
}

func test(fun: (Int64, Int64) -> Unit, bottom: Int64, top: Int64) {
    try {
        fun(bottom, top)
        println(": ok")
    } catch (e: Exception) {
        println("${e}")
    }
}

main() {
    let max = 0x7FFFFFFFFFFFFFFF
    test(range2e, max - 1, max)
}

// ASSERT: scan : ok
