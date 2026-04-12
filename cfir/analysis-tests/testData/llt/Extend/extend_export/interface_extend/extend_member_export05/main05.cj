// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: ./testa.cj ./testb3.cj ./%n.cjnative.expect
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb3.cj -o b.%middle
// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %n.cjnative.expect

package b.a

import a.*
import b.c.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().f1() // error
    Foo<Int64>().f2() // error
    Foo<Int64>().f3() // ok
}
