// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public enum ET<T> {
    A | B | C

    public func bar(a: T) {
        return a
    }
    public static func f<U>(a: T, b: U): T {
        return a
    }
}
