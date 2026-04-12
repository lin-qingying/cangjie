// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan 1

open class A {}

class B <: A {}

interface I {
    static func f1(): A
    static func f2(): Unit {
        f1()
    }
}

interface I1 <: I {
    static func f1(): B {
        println(1)
        B()
    }
}

main() {
    I1.f2()
}
