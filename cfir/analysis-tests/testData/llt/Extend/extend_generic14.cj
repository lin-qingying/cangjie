// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output

class X {}

open class A<T> {}

extend<T> A<T> {
    func foo() {
        123
    }
}

extend<T> A<T> where T <: X {
    func coo() {
        234
    }
}

class B <: A<X> {}

main() {
    let b = B()
    if (b.foo() != 123) {
        return 1
    }
    if (b.coo() != 234) {
        return 2
    }
    return 0
}
