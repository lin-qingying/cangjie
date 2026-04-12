// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg2

internal import pkg1.*

public class myClass<K> {
    public static var x: Int64 = 10
    static func f() {
        x = 20
        return foo
    }
}

extend<K> myClass<K> <: MyInterface<() -> Int64> {
    public static func foo1(): () -> Int64 {
        f()
    }
}
