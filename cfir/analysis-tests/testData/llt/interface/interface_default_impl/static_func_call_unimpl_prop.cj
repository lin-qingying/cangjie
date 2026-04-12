// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan 21

interface I {
    mut static prop a: Int64
    static func fset(): Unit {
        a = 1
    }
    static func fget(): Int64 {
        a
    }
}

interface I2 <: I {
    mut static prop a: Int64 {
        get() {
            print("1")
            0
        }
        set(x) {
            print("2")
        }
    }
}

main() {
    I2.fset()
    I2.fget()
    0
}
