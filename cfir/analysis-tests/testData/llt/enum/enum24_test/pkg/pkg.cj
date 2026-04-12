// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public enum ET {
    A | B | C

    public func bar<T>(a: T) {
        return a
    }
    public static func f<T>(a: T): T {
        return a
    }
}
