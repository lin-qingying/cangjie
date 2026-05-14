// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: A.cj B.cj C.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt A.cj -o A.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt B.cj -o B.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt C.cj -o C.%middle
// EXEC-PIPE-1: %compiler %cmp_opt A.%middle B.%middle C.%middle %f 2>&1 | compare %f

import A.Foo
import B.I1
import C.I2

main(): Int64 {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 1
}
