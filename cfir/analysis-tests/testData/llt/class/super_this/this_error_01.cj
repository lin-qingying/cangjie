// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C1 {
    public func foo1() {
        <!sema_invalid_assignment_to_this_expr!>t<!>his = C1()
    }
}

main() {
    return 1
}
