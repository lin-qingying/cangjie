// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

public interface faterInterface<T> {
    static func foo1(): T
    static func foo2(a: Int16): Int16 {
        a
    }
}

struct enum1<T> <: MyInterface {
    public static func A(a: Int16): enum1<T> {
        enum1<T>()
    }
    public static func foo1(): Int16 {
        1
    }
}

interface MyInterface <: faterInterface<Int16> {}

main() {
    var en1: enum1<faterInterface<Int16>> = enum1<faterInterface<Int16>>.A(
        enum1<faterInterface<Int16>>.foo2(enum1<faterInterface<Int16>>.foo1()))
    0
}
