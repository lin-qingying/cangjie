// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

interface I {}

extend Int32 <: I {}

main(): Int64 {
    if (Coo.foo(1, 2) != 2 || Boo<Int32>.foo(3, 4) != 4) {
        return 1
    }
    return 0
}

open class Boo<T> {
    public static func foo<V>(x: T, y: V) where V <: I {
        return y
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<V>(x: Int32, y: V) where V <: I {
        return y
    }
}
