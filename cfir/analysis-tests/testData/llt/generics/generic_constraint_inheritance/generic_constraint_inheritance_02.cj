// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

open class A {}

open class B <: A {}

class C <: B {}

interface I1 {
    static func test<T>(): Unit where T <: A {}
}

interface I2 {
    static func test<T>(): Unit where T <: B {}
}

class D <: I1 & I2 {
    public static func test<T>(): Unit where T <: C {}
}

class E <: I1 & I2 {
    public static func test<T>(): Unit where T <: B {}
}

class F <: I1 & I2 {
    public static func test<T>(): Unit where T <: A {}
}

main() {}

// ASSERT: scan-2 sema_generic_constraint_not_looser
// ASSERT: scan "Errors": 2
