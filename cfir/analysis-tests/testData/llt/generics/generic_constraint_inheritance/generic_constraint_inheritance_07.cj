// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface A {}

interface B <: A {}

interface C <: B {}

interface I1 {
    static func test<T>(): Unit where T <: A & B {}
}

class D <: I1 {
    public static func test<T>(): Unit where T <: <!GENERIC_CONSTRAINT_NOT_LOOSER!>C<!> {}
}

class E <: I1 {
    public static func test<T>(): Unit where T <: B {}
}

class F <: I1 {
    public static func test<T>(): Unit where T <: A {}
}

main() {}
