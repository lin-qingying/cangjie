// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

let n: Int64 = A().a

struct B {
    public let a: Int64 = n
    public let b: Int64 = this.a
    public var x: Int64 = 1
    public let g: () -> Int64 = {=> return this.x * this.x}
    public func h() {
        return this.a
    }
}
