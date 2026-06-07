// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public open func f(): String {
        return ""
    }
    public open prop a: Int {
        get() {
            0
        }
    }
}

abstract class B <: A {
    <!sema_invalid_override_member_in_class!>p<!>ublic func f(): String
    <!sema_invalid_override_member_in_class!>p<!>ublic prop a: Int
}

main() {}
