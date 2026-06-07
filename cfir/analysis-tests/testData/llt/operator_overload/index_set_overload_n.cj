<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    operator func [](arg1: Int64, arg2: String, <!sema_invalid_subscript_assign_parameter!>x<!>!: Int64): Int64 {
        return 1
    }
}
