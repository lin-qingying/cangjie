// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj

import ohos.dep.*

main() {
    A().foo() // ok
    <!UNRESOLVED_REFERENCE!>A().goo()<!> // error
    <!UNRESOLVED_REFERENCE!>A().koo()<!> // error
}
