// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb5.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb5.cj -o b.%middle
// EXEC-PIPE-1: %compiler %cmp_opt b.%middle a.%middle %f -o %output 2>&1 | compare %f

package d

import <!package_search_error!>a<!>.*
import <!package_search_error!>b.c<!>.*

main() {
    Foo<Int64>().f0() // ok
    Foo<Int64>().f1() // error
    Foo<Int64>().f2() // error
    Foo<Int64>().f3() // error
    Foo<Int64>().f4() // ok
}
