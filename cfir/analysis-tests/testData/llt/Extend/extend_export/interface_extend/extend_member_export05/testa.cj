// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

private interface I0 {
    public func f0(): Unit
}

public class Foo<T> {}

// The extension is exported.
extend<T> Foo<T> <: I0 {
    public func f0(): Unit {}
}
