// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

// DEPENDENCE: ./testa.cj ./testb4.cj
package d

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().<!NOT_MEMBER_OF!>f1<!>() // error
    Foo<Int64>().<!NOT_MEMBER_OF!>f2<!>() // error
    Foo<Int64>().<!NOT_MEMBER_OF!>f3<!>() // error
}
