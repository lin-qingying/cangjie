// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

type A<T, Y, U> = Int64

main() {
    var a: A<Bool, Rune, Int64> = 10;
    var b: Int32 = 20;
    var c = a + b;
    return 0;
}

/* SCAN
warning: type arg(s) Generics-T,Generics-Y,Generics-U are not used
 ==> typealias4.cj:9:1:
  | 
9 | type A<T, Y, U> = Int64
  | ^ 
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: invalid binary operator '+' on type 'Int64' and 'Int32'
  ==> typealias4.cj:14:15:
   | 
14 |     var c = a + b;
   |               ^ 
   | 
   # note: you may want to implement 'operator func +(right: Int32)' for type 'Int64'

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
