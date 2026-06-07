<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


extend Int64 {
    <!sema_operator_overload_built_in_binary_operator!>p<!>ublic operator func +(right: Int64) {
        return this
    }
    <!sema_operator_overload_built_in_binary_operator!>p<!>ublic operator func <!sema_return_type_incompatible!>==<!>(right: Int64) {
        return this
    }
    <!sema_operator_overload_built_in_binary_operator!>p<!>ublic operator func <<(right: Int64) {
        return this
    }
    <!sema_operator_overload_built_in_binary_operator!>p<!>ublic operator func -(right: Int64) {
        return this
    }
    <!sema_operator_overload_built_in_unary_operator!>p<!>ublic operator func -() {
        return this
    }
    public operator func [](right: Int64) { // OK
        return this
    }
    public operator func +(right: Int32) { // OK
        return this + Int64(right)
    }
}

extend Bool {
    <!sema_operator_overload_built_in_binary_operator!>p<!>ublic operator func <!sema_extend_member_cannot_shadow!>==<!>(right: Bool) {
        return true
    }
    public operator func +(right: Bool) { // OK
        return true
    }
}
