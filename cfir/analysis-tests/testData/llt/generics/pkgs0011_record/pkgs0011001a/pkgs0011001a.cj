// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs0011001a

public struct Test<T> {
    public var member: T
    public init(a: T) {
        member = a
    }
    public func testNormal(a: Bool): Bool {
        return a
    }
    public func testGeneric1(t: T): T {
        return t
    }
    public func testGeneric2(t: T, j: Bool): Bool {
        return j
    }
}
