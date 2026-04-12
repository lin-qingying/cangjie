// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public open class A {
    public static var a: Int64 = 1
    public static let r: Range<Int64>
    static init() {
        r = a..5
    }
}
