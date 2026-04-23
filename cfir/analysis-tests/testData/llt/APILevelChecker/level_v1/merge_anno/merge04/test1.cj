// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep1.cj dep.cj.d

import dep.*

main() {
    <!APILEVEL_REF_HIGHER!>A().foo()<!>
    <!APILEVEL_REF_HIGHER!>Option<Int64>.None.foo()<!>
    <!APILEVEL_REF_HIGHER!>1.foo()<!>
    0
}
