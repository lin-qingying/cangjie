// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// Test for nested type error can be reported

public interface fatherInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UInt32 {
    static func foo1(): UInt32
    static func foo2<K>(a: K): UInt32 {
        foo1()
    }
}

public interface MyInterface<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> <: fatherInterface<T> where T <: UInt32 {
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
        enum1<MyInterface<UInt32>>.foo2<UInt32>(enum1<MyInterface<UInt32>>.foo1() + enum1<<!GENERIC_ARGUMENT_NO_MATCH!>MyInterface<UInt16><!>>.foo1()))
}
