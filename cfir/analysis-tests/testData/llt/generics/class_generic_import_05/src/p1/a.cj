// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

public open class A<T> {}

public open class B<T> {
    public var b: T
    public init(x: T) {
        b = x
    }
}
