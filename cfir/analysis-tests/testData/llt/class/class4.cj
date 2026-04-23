// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface A {
    func foo1(input: Bool): Int32
}

interface B {
    func foo1(input: Bool): Int32
}

interface C <: A & B {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: C {}

abstract class E <: C {}
