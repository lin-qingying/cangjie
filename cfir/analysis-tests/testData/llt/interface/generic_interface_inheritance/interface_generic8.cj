// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<Int32> & I<Float64> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32

    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface III1<T> <: II<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface III2 <: II<Int64> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III3<T> <: II<T> & II<Int64> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III4<T> <: II<T> & II<Int64> & I<T> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III5<T> <: II<T> & II<Int64> & I<Int32> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III6<T> <: II<T> & II<Int64> & I<Int64> {
    func foo(a: T): T
    static func fs(a: T): T
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III7<T> <: II<T> & II<Int64> & I<Float64> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface IIII1<T> <: III1<T> & II<T> & I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface IIII2 <: III2 & II<Int64> & I<Int64> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface IIII3 <: III5<Bool> {
    func foo(a: Bool): Bool
    static func fs(a: Bool): Bool
}

interface IIII4<T> <: III7<Bool> & III6<Int16> & III4<T> & II<Bool> & II<Int16> & II<T> & I<Float32> & I<T> {
    func foo(a: Bool): Bool
    static func fs(a: Bool): Bool
    func foo(a: Int16): Int16
    static func fs(a: Int16): Int16
    func foo(a: Float32): Float32
    static func fs(a: Float32): Float32
}

main() {
    return 0
}
