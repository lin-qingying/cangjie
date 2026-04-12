// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix 2>&1 | compare %f
// ASSERT: scan set T.a
// ASSERT: scan call T.f

interface I1 {
    mut static prop a: Int64
    static func f(): Unit
}

interface I2 <: I1 {}

abstract class I3 <: I2 {
    public mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {
            println("set T.a")
        }
    }
    public static func f(): Unit {
        println("call T.f")
    }
}

func f<T>(): Unit where T <: I2 {
    T.a = 1
    T.f()
}

main() {
    f<I3>()
}
