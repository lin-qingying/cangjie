// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public operator func ()(x: Int32): Int64 {
        Int64(x)
    }
}

class B {
    public operator func ()(x: Int64): Int32 {
        Int32(x)
    }
}

func run() {
    let x: Int32 = 1
    let a: A = A()
    let b: B = B()
    let f = a ~> b
    let res = f(x)
    print("res: ${res}")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 1) {
        return 0
    }
    return 1
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
