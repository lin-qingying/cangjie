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
    ~init<!CANNOT_CURRYING!>(<!>)() {}
}

class C {
    ~init() {
        return <!RETURN_TYPE_MISMATCH!>0<!>
    }
}

class D {
    ~init() {}
}

class F<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: String {
    <!FORBID_GENERIC_FINALIZER!>~<!>init<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: String {}
}

main() {
    0
}
