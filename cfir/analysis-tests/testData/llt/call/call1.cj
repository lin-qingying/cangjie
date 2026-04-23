// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo1(a: Int64, b: Int64, c: Int32): Int32 {
    1
}

func foo1(a: Int32, b: Int32, c: Int64): Int32 {
    2
}

open class A {}

open class B <: A {}

class C <: B {}

func foo2(a: ((A, B)) -> C): Int32 {
    1
}

func foo2(a: ((B, C)) -> B): Int32 {
    2
}

main() {
    let a = <!AMBIGUOUS_FUNCTION_CALL!>foo1<!>(1, 2, 3)
    let b = {x: (B, C) => C()}
    let c = <!AMBIGUOUS_FUNCTION_CALL!>foo2<!>(b)
    return 0
}
