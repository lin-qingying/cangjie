// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct R1 {
    public var i = 0

    public mut func foo1() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his
        }
        return 0
    }

    public mut func foo2() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = 2}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = 2
        }
        return 0
    }

    public mut func foo3() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i
        }
        return 0
    }

    public mut func foo4() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>i<!>}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>i<!>
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
