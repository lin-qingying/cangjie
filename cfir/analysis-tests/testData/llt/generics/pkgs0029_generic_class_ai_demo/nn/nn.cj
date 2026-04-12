// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package nn

public struct A<T> {
    public var a: B<T> = B<T>()
    public func abc() {
        return 1
    }
}

public struct B<T> {
    public var b: Int64 = 1
    public func abc() {
        return 1
    }
}

func foo() {
    A<Float64>()
}
