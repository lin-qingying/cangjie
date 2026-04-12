// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1.cj pkg2.cj 
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg1.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg1.%middle pkg2.cj -o pkg2.%middle
// EXEC: %compiler %cmp_opt %enableO2 %f pkg1.%middle pkg2.%middle -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan 2

package pkg

import pkg1.*
import pkg2.*

main() {
    var a = A()
    println(a.test())
    return
}
