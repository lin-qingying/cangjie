// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep2.cj dep2.cj.d

import dep2.*

main() {
   println(<!APILEVEL_REF_HIGHER!>aaaa<!>)
   println(<!APILEVEL_REF_HIGHER!>bbbb<!>)
   let a = <!APILEVEL_REF_HIGHER!>cccc<!>
}
