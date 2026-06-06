// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0

    public mut func foo1() {
        let f = {=> <!sema_capture_this_or_instance_field_in_func!>t<!>his}

        func goo() {
            <!sema_capture_this_or_instance_field_in_func!>t<!>his
        }
        return 0
    }

    public mut func foo2() {
        let f = {=> <!sema_capture_this_or_instance_field_in_func!>t<!>his.i = 2}

        func goo() {
            <!sema_capture_this_or_instance_field_in_func!>t<!>his.i = 2
        }
        return 0
    }

    public mut func foo3() {
        let f = {=> <!sema_capture_this_or_instance_field_in_func!>t<!>his.i}

        func goo() {
            <!sema_capture_this_or_instance_field_in_func!>t<!>his.i
        }
        return 0
    }

    public mut func foo4() {
        let f = {=> <!sema_capture_this_or_instance_field_in_func!>i<!>}

        func goo() {
            <!sema_capture_this_or_instance_field_in_func!>i<!>
        }
        return 0
    }

    public mut func foo5() {
        return <!sema_use_this_as_an_expression_in_func!>t<!>his
    }

    public mut func foo6() {
        var a = <!sema_use_this_as_an_expression_in_func!>t<!>his
        return a
    }
}

main() {
    return 1
}
