// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: test.cj
// (CJNATIVE) EXEC: %compiler %cmp_opt %compile_lib_opt test.cj -o test.%middle %enableO2
// (CJNATIVE) EXEC: %compiler %cmp_opt test.%middle %f -o %output %enableO2
// (CJNATIVE) EXEC: %run %run_opt %output 2>&1 | compare %f
// (CJNATIVE) ASSERT: scan call iterator

package test

import test.a.MyList

main() {
    var a = MyList<String>()
    println(a)
}