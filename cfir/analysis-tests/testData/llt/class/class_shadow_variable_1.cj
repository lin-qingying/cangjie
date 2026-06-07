// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public var a = 1
}

class B <: A {
    <!sema_member_variable_can_not_shadow!>p<!>ublic var a = 10
}

main() {
    return 1
}
