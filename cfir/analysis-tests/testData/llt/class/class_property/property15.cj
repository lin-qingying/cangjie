// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public mut prop a: Int64 {
        get() {
            1
        }
        <!sema_cannot_currying!>s<!>et(v)(x) {}
    }
}

class B {
    public mut prop a: Int64 {
        <!sema_cannot_have_parameter!>g<!>et(v) {
            1
        }
        set(v) {}
    }
}

class C {
    public mut prop a: Int64 {
        <!sema_cannot_currying!>g<!>et()() {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}
