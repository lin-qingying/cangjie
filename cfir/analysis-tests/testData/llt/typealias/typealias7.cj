// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

type D<T> = F<Int32>

type F<U> = D<Rune>

main() {
    return 0
}

/* SCAN
error: type cycle detected: 'D->F->D'
 ==> typealias7.cj:9:1:
  | 
9 | type D<T> = F<Int32>
  | ^ 
  | 

error: type cycle detected: 'F->D->F'
  ==> typealias7.cj:11:1:
   | 
11 | type F<U> = D<Rune>
   | ^ 
   | 

warning: type arg(s) Generics-T are not used
 ==> typealias7.cj:9:1:
  | 
9 | type D<T> = F<Int32>
  | ^ 
  | 
  # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: type arg(s) Generics-U are not used
  ==> typealias7.cj:11:1:
   | 
11 | type F<U> = D<Rune>
   | ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 errors generated, 2 errors printed.
2 warnings generated, 2 warnings printed.
*/
