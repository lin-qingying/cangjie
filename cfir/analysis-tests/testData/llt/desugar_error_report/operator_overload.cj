// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A {
    public operator func -(input: A): Int64 {
        return 0
    }
}

main(): Int64 {
    var a: A = A()
    a -= a
    return 0
}

/* SCAN
error: type incompatible in this compound assignment expression
  ==> operator_overload.cj:17:5:
   | 
17 |     a -= a
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
