// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public interface MyInterface {
    static func foo1(): IntNative
    static func foo2<T>(a: IntNative): IntNative {
        foo1()
        return a + foo1()
    }
}
