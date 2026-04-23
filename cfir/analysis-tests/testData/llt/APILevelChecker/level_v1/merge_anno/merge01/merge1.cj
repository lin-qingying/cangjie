// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep1.cj dep1.cj.d


import dep1.*

main() {
    foo() // level 1
    foo([1]) // level 2
    <!APILEVEL_REF_HIGHER!>foo(["1"])<!> // level 3
}
