// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
package b.c

import a.*
import b.*

main() {
    <!NO_VALUE_FOR_PARAMETER!>Foo().f1()<!> // error
    Foo().f1(1) // ok
    Foo().f2() // ok
}
