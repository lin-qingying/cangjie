// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    protected prop f: String {
        get() {
            return "C"
        }
    }
}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>(): Int64 {
        0
    }
}
