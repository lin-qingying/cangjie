<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct Test {
    private Test(var name: String) {}
    <!sema_class_uninitialized_field!><!sema_overload_conflicts!>i<!>nit<!>(name: String) {}
}
