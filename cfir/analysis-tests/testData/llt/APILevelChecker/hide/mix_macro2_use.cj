// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_macro_dep.cj mix_macro_dep1.cj mix_macro2.cj

import ohos.dep.*

main() {
    <!UNRESOLVED_REFERENCE!>A()<!> // undefined
    <!UNRESOLVED_REFERENCE!>B()<!> // undefined
    <!UNRESOLVED_REFERENCE!>C()<!> // undefined
    <!UNRESOLVED_REFERENCE!>D()<!> // undefined
    0
}
