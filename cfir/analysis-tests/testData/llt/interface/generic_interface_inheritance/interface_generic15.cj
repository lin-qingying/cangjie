// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

interface I<T> {
    static func test(): Int64
}

open class Base <: I<Base> {
    public static func test(): Int64 {
        return 1
    }
}

open class A <: Base & I<A> {
    public static func test(): Int64 {
        return 2
    }
}

class B <: A & I<B> {
    public static func test(): Int64 {
        return 3
    }
}

func get1<T>() where T <: I<T> {
    T.test()
}

func get2<T>() where T <: I<Base> {
    T.test()
}

main() {
    let v1 = get1<B>()
    if (v1 != 3) {
        return 1
    }
    let v2 = get2<B>()
    if (v2 != 3) {
        return 1
    }
    return 0
}
