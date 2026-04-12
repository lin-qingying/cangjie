// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output

package my_test

public interface From<S, T> {
    static func from(s: S): T
}

extend Unit <: From<Int64, Unit> {
    public static func from(s: Int64): Unit {
        ()
    }
}

extend Int64 <: From<Int64, Int64> {
    public static func from(s: Int64): Int64 {
        123i64
    }
}

extend Bool <: From<Int64, Bool> {
    public static func from(s: Int64): Bool {
        true
    }
}

public class TestStaticFunction<E> where E <: From<Int64, E> {
    public static func f0<F>(p01_named_with_default!: F = F.from(114514)): Unit where F <: From<Int64, F> {}
}

main() {}
