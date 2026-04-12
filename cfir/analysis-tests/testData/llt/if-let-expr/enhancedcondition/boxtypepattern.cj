// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC-PIPE: %run %run_opt %output %run_args | compare %f

class A {
    public var test: Any = "any"
    public var value: Int64 = 0
    public init() {
        super()
    }
}

main() {
    var testA: Option<A> = A()
    if (let Some(v: String) <- testA?.test) {
        println("1")
    }
    while (let Some(v: String) <- testA?.test) {
        println("2")
        break
    }
    0
}

/* SCAN
1
2
*/
