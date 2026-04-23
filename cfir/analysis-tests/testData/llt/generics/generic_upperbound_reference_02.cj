// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_VARIABLE

interface I<T> {
    func test(a: T) {
        return a
    }
}

class A <: I<Rune> & I<Int32> {}

func test<T>(a: T) where T <: I<Rune> & I<Int32> {
    let f = <!AMBIGUOUS_USE!>a.test<!>
    <!AMBIGUOUS_USE!>a.test<!>
    return 1
}

main() {
    return test(A())
}
