// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs004001a

public func Foo<T>(a: T): T {
    return a
}

public func Foo2<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo2<T, Int32>(a, b)
    return c
}
