// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep1.cj dep1.cj.d

import dep1.*

main() {
    <!APILEVEL_REF_HIGHER!>A()<!>
    <!APILEVEL_REF_HIGHER!>B()<!>
    <!APILEVEL_REF_HIGHER!>C.A.foo()<!>
    <!APILEVEL_REF_HIGHER!>C.B(1).foo()<!>
}
