// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

open class A {}

open class B <: A {}

open class C <: B {}

open class Base {
    static func f<T>(a: T): Unit where T <: B {}
    static func g<T>(): Unit where T <: B {}
    static func p<T>(): Unit {}
}

class D <: Base {
    static redef func f<T>(a: T): Unit where T <: C {} // Error，stricter constraint
    static redef func g<T>(): Unit where T <: C {} // Error，stricter constraint
    static redef func p<T>(): Unit where T <: C {} // Error，stricter constraint
}

class E <: Base {
    static redef func f<T>(a: T): Unit where T <: A {} // OK, looser constraint
    static redef func g<T>(): Unit where T <: A {} // OK, looser constraint
}

class F <: Base {
    static redef func f<T>(a: T): Unit where T <: B {} // OK，same constraint
    static redef func g<T>(): Unit where T <: B {} // OK，same constraint
}

main() {}

// ASSERT: scan-3 sema_generic_constraint_not_looser
// ASSERT: scan "Errors": 3
