// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class int16 {}

open class int32 <: int16 {}

open class int64 <: int32 {}

open class float16 {}

open class float32 <: float16 {}

open class float64 <: float32 {}

open class bool {}

interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<int32> & I<float64> {
    func foo(a: int32): int32
    static func fs(a: int32): int32

    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface III1<T> <: II<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface III2 <: II<int64> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III3<T> <: II<T> & II<int64> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III4<T> <: II<T> & II<int64> & I<T> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III5<T> <: II<T> & II<int64> & I<int32> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III6<T> <: II<T> & II<int64> & I<int64> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III7<T> <: II<T> & II<int64> & I<float64> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface IIII1<T> <: III1<T> & II<T> & I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface IIII2 <: III2 & II<int64> & I<int64> {
    func foo(a: int32): int32
    static func fs(a: int32): int32
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface IIII3 <: III5<bool> {
    func foo(a: bool): bool
    static func fs(a: bool): bool
}

interface IIII4<T> <: III7<bool> & III6<int16> & III4<T> & II<bool> & II<int16> & II<T> & I<float32> & I<T> {
    func foo(a: bool): bool
    static func fs(a: bool): bool
    func foo(a: int16): int16
    static func fs(a: int16): int16
    func foo(a: float32): float32
    static func fs(a: float32): float32
}

main() {
    return 0
}
