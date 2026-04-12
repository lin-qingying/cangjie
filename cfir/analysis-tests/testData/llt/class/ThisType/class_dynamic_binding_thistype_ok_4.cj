// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

open class A {
    func test() {
        return this
    }
    protected open func foo() {
        1
    }
}

class B <: A {
    protected override func foo() {
        2
    }
}

main() {
    let b = B()
    let x: () -> B = b.test
    let y = x() // Type of y is B
    if (y.foo() != 2) {
        return 1
    }
    return 0
}
