// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

// Test for nested type error can be reported

public interface fatherInterface<T> where T <: UInt32 {
    static func foo1(): UInt32
    static func foo2<K>(a: K): UInt32 {
        foo1()
    }
}

public interface MyInterface<T> <: fatherInterface<T> where T <: UInt32 {
    static func foo3(): UInt32 {
        foo1() + foo1()
    }
}

public enum enum1<T> <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        1
    }
}

enum enum2<T> <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        2
    }
}

main() {
    var en1: enum1<MyInterface<UInt32>> = enum1<MyInterface<UInt32>>.A(
        enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() + enum1<MyInterface<UInt16>>.foo1()))
}

// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-2 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 3
