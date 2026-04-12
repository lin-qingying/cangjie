// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt testb.cj -o b.%middle
// EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

import a.*
import b.*

main() {
    A().f1
    A().f2 // error
    A().f3
}

// (not CJNATIVE) ASSERT: scan error: can not access field 'f2'
// (CJNATIVE) ASSERT: scan error: 'f2' is not a member of class 'A'
