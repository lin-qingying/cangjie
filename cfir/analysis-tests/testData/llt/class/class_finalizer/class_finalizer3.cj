// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// parse diag
// class A {
//      ~init(a:Int32){}
// }

class B {
    ~init<!sema_cannot_currying!>(<!>)() {}
}

class C {
    ~init() {
        return <!sema_cannot_convert_literal!>0<!>
    }
}

class D {
    ~init() {}
}

class F<<!sema_upper_bound_must_be_class_or_interface!>T<!>> where T <: String {
    <!sema_forbid_generic_finalizer!>~<!>init<<!sema_upper_bound_must_be_class_or_interface!>T<!>>() where T <: String {}
}

main() {
    0
}
