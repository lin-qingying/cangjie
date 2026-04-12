// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var err = 1 + false

    !err
    -err
        + err
    err++
    err--

    0
}

/* SCAN
warning: possibly confusing line terminator
  ==> unary_0.cj:13:9:
   | 
13 |       -err
   |        ~  ^ 
   |  ______|
14 | |         + err
   | |_________~ possibly confusing line terminator between 'err' and '+'
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff parser`

error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> unary_0.cj:10:17:
   | 
10 |     var err = 1 + false
   |                 ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
