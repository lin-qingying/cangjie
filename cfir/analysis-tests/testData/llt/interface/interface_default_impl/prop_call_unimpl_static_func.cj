// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE: %run %run_opt %output %run_args | compare %f
// ASSERT: scan set 10
// ASSERT: scan pass

interface I {
    static func foo(): Int64
    static func foo(a: Int64): Int64
    mut static prop a: Int64 {
        get() {
            foo()
        }
        set(x) {
            foo(x)
        }
    }
}

interface I2 <: I {
    static func foo(a: Int64): Int64 {
        println("set ${a}")
        a
    }
    static func foo(): Int64 {
        2
    }
}

main() {
    I2.a = 10
    if (I2.a == 2) {
        println("pass")
    }
    0
}
