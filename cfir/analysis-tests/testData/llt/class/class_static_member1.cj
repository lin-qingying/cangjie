// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public static var a: Int64 = 1
    public var b: Int64 = 2
    <!sema_static_variable_cannot_access_non_static_member!>p<!>ublic static var c: Int64 = a + <!USED_BEFORE_INITIALIZATION!>b<!>
}

main(): Int64 {
    return 0
}
