// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg2

internal import pkg1.*

public class myClass1<T> <: MyInterface {
    public static var x: IntNative = 20
    public static func f() {
        x = 10
        return x
    }
    public static func foo1(): IntNative {
        f()
    }
}

public class myClass2<T> <: MyInterface {
    public static var x: IntNative = 20
    public static func foo1(): IntNative {
        x = 11
        return x
    }
    public static func foo3() {
        foo2<IntNative>(11)
    }
}

public class myClass3<T> <: MyInterface {
    public static var x: IntNative = 10
    public static func foo1(): IntNative {
        x = 30
        return x
    }
}
