// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 0
// DEPENDENCE: ./testa.cj ./testb.cj ./%n.cjnative.expect
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o b.%middle
// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %n.cjnative.expect

import a.*
import b.*

main() {
    A().f1()
    A().f2() // error
    A().f3()
}
