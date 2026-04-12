// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public func TestFn1<T>(): Int64 {
    var x = 0
    func TestFn2(): Unit {
        x = 2
    }
    TestFn2()
    x
}
