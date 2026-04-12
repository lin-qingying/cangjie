// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

open class A {}

open class B <: A {}

class C <: B {}

interface I<X> {
    static func test<T>(): Unit where T <: B & Equatable<X> {}
}

class D <: I<B> {
    public static func test<T>(): Unit where T <: C & Equatable<B> {}
}

class E <: I<B> {
    public static func test<T>(): Unit where T <: Equatable<C> {}
}

class F <: I<B> {
    public static func test<T>(): Unit where T <: A & Equatable<B> {}
}

main() {}

// ASSERT: scan-2 sema_generic_constraint_not_looser
// ASSERT: scan "Errors": 2
