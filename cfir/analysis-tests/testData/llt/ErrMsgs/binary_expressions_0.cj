// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var a = 1 + true
    1 + true + a

    1 + 2 + a
    a - 2 - 3
    a ** 1
    a * true
    a / true
    a % true
    a + true
    a - true
    a << true
    a >> true
    a && true
    a || true
    a & true
    a ^ true
    a | true

    0
}

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> binary_expressions_0.cj:10:15:
   | 
10 |     var a = 1 + true
   |               ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> binary_expressions_0.cj:11:7:
   | 
11 |     1 + true + a
   |       ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

2 errors generated, 2 errors printed.
*/
