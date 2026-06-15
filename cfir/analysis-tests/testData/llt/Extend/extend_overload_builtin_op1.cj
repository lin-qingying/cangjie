<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


extend Int64 {
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>p<!>ublic operator func +(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>p<!>ublic operator func <!RETURN_TYPE_INCOMPATIBLE!>==<!>(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>p<!>ublic operator func <<(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>p<!>ublic operator func -(right: Int64) {
        return this
    }
    <!OPERATOR_OVERLOAD_BUILT_IN_UNARY_OPERATOR!>p<!>ublic operator func -() {
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
    <!OPERATOR_OVERLOAD_BUILT_IN_BINARY_OPERATOR!>p<!>ublic operator func <!EXTEND_MEMBER_CANNOT_SHADOW!>==<!>(right: Bool) {
        return true
    }
    public operator func +(right: Bool) { // OK
        return true
    }
}
