// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Base {
    func f1(): Int32
    func f2(): Int32 {
        return f1()
    }
}

class Data <: Base {
    public var m: Int32
    public init() {
        m = <!USED_BEFORE_INITIALIZATION!>f2<!>()
    }

    public func f1(): Int32 {
        return m
    }
}
