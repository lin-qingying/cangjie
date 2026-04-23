// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj dep_ok.cj

import dep.*

main() {
    f9()
    <!APILEVEL_REF_HIGHER!>f11()<!>
    f0()
}
