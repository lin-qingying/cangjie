// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

struct A {
    private var b: Int64 = 1
    public func test() {
        foo().b
    }
}

func foo() {
    A()
}

class B {
    private var b: Int64 = 1
    public func test() {
        foo1().b
    }
}

func foo1() {
    B()
}

main(): Int64 {
    if (A().test() == 1 && B().test() == 1) {
        return 0
    }
    return 1
}

/* SCAN
*/ 
