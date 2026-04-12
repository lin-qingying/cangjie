// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var a = Coo()
    if (a.b3 != 1) {
        return 1
    }
    return 0
}

open class Boo {}

class Coo <: Boo {
    public static var b5: Int64 = 1
    public var b3 = {=> b0}()
}

/* SCAN
error: undeclared identifier 'b0'
  ==> lambda_bnf_020201.cj:21:25:
   | 
21 |     public var b3 = {=> b0}()
   |                         ^ 
   | 

1 error generated, 1 error printed.
*/
