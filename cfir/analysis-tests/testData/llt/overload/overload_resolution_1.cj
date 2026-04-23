// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class ConsTest {
    public static func f(a: Bool): Unit {}
    public static func f(): Unit {}
}

func g(a: Bool): Unit {}

func g(): Unit {}

let a: (Bool) -> Unit = g // OK
let b: () -> Unit = ConsTest.f // OK
let c: (Bool) -> Unit = ConsTest.f // OK
let d = ConsTest.<!AMBIGUOUS_USE!>f<!>
let lst = [(ConsTest.<!AMBIGUOUS_USE!>f<!>, 1), (ConsTest.<!AMBIGUOUS_USE!>f<!>, 2)]

main(): Int64 {
    let x = lst[0][0]
    x + 1
    0
}
