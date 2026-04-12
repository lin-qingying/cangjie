// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A <: ToString {
    public func toString() {
        "A"
    }
}

interface I0 {
    func test(): ToString
}

interface I1 {
    func test(): A {
        A()
    }
}

class B <: I0 & I1 {}

main(): Int64 {
    let ins = B()
    if (ins.test().toString() != "A") {
        return 1
    }
    return 0
}
