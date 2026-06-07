<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// extend member functions cannot be shadowed by it sub classes
open class A {}

extend A {
    func foo() {}
}

class B <: A {
    func <!sema_extend_function_cannot_overridden!>foo<!>() {}
}
