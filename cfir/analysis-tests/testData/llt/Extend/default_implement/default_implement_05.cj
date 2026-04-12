// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum enum3<T> <: MyInterface {
    A(Int16)

    public static func foo1(): Int16 {
        3
    }
}

enum enum2<T> <: MyInterface {
    A(Int16)

    public static func foo1(): Int16 {
        2
    }
}

public interface faterInterface<T> {
    static func foo1(): T
    static func foo2(a: Int16): Int16 {
        a
    }
}

main() {
    var en1: enum1<faterInterface<Int16>> = enum1<faterInterface<Int16>>.A(
        enum1<faterInterface<Int16>>.foo2(enum1<faterInterface<Int16>>.foo1()))
    var en2: enum2<faterInterface<Int16>> = enum2<faterInterface<Int16>>.A(
        enum2<faterInterface<Int16>>.foo2(enum2<faterInterface<Int16>>.foo1()))
    var en3: enum3<faterInterface<Int16>> = enum3<faterInterface<Int16>>.A(
        enum3<faterInterface<Int16>>.foo2(enum3<faterInterface<Int16>>.foo1()))
    var e1 = match (en1) {
        case A(x) => x
    }
    var e2 = match (en2) {
        case A(x) => x
    }
    var e3 = match (en3) {
        case A(x) => x
    }
    if (e1 == 1 && e2 == 2 && e3 == 3) {
        return 0
    } else {
        return 1
    }
}

interface MyInterface <: faterInterface<Int16> {}

enum enum1<T> <: MyInterface {
    A(Int16)

    public static func foo1(): Int16 {
        1
    }
}
