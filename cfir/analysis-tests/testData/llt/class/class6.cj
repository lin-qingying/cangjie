// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public init() {
        this
    }
    public func get(a: A) {
        1
    }
    public init(a: Int64) {
        get(<!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>)
    }
    public init(a: Bool) {
        this(<!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>)
    }
    public init(a: A) {}
}
