// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<T> {
    func foo(a: T) {}
}

interface I2 {}

open class A {}

open class B <: A {}

class C <: B & I2 {}

class D <: I<B> & I<I2> {} // exist func foo(a: B);  func foo(a: I2)

main() {
    let d = D()
    d.<!AMBIGUOUS_FUNCTION_CALL!>foo<!>(C())
    return 1
}
