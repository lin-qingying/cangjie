// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public enum ET {
    A | B | C(Int64)

    public func bar(a: Int64) {
        return a
    }
    public static func f(a: Int64): Int64 {
        return a
    }
}
