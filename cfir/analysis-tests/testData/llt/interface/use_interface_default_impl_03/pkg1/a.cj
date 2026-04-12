// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public interface fatherInterface<T> {
    static func foo1(): UInt32
    static func foo2<K>(a: K): UInt32 {
        foo1()
    }
}

public interface MyInterface<T> <: fatherInterface<T> {
    static func foo3(): UInt32 {
        foo1() + foo1()
    }
}

public enum enum1 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        1
    }
}

public enum enum2 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        2
    }
}

public enum enum3 <: MyInterface<UInt32> {
    A(UInt32)

    public static func foo1(): UInt32 {
        3
    }
}
