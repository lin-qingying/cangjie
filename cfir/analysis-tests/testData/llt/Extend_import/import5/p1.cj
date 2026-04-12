// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

public class Foo<T> {}

extend<T> Foo<T> {
    public func foo(a: T) {
        print("hello")
    }
}

public interface I<T> {
    func test(a: T): Unit
}

extend<T> Foo<T> <: I<T> {
    public func test(a: T) {
        print("test")
    }
}
