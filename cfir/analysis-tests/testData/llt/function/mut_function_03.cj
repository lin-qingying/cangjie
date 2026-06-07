// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct S {
    var x = 0

    init() {
        func g() {
            <!sema_illegal_capture_this!>x<!> += 1
        }
        g()
    }

    mut func f() {
        func g() {
            <!sema_capture_this_or_instance_field_in_func!>x<!> += 1
        }
        g()
        x += 1
    }
}

main() {
    var x = S()
    x.f()
    x.x
}
