// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_anno_dep.cj

import ohos.dep.*

main() {
    <!UNRESOLVED_REFERENCE!>A()<!> // apilevel check error, hide check error.
    <!UNRESOLVED_REFERENCE!>B()<!> // apilevel check error, hide check error.
    0
}
