<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public mut prop a: Int64 {
        get() {
            return b
        }
        set(v) {
            return ()
        }
    }
    public var b = <!USED_BEFORE_INITIALIZATION!>a<!>
}
