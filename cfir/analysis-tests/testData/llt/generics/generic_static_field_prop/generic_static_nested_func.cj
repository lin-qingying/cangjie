// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

public interface From<S, T> {
    static func from(s: S): T
    func instance_from(s: S): T
}

extend Int64 <: From<Int64, Int64> {
    public static func from(s: Int64): Int64 {
        123i64
    }

    public func instance_from(s: Int64): Int64 {
        -123
    }
}

open class C1<R> {
    static init() {
        func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
        foo<Int64>()
    }
    static var a1 = {
        =>
        func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
        foo<Int64>()
    }
    public static prop foo: Int64 {
        get() {
            func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
            foo<Int64>()
            1
        }
    }
}

struct C2<R> {
    static init() {
        func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
        foo<Int64>()
    }
    static var a1 = {
        =>
        func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
        foo<Int64>()
    }
    public static prop foo: Int64 {
        get() {
            func foo<T>(a!: T = T.from(123)) where T <: From<Int64, T> {}
            foo<Int64>()
            1
        }
    }
}

main() {
    C1<Int64>()
    C2<Int64>()
    0
}
