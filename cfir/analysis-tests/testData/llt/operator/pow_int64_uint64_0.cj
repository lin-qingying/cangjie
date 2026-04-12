// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let a1 = 1 ** -1
    let a2: UInt64 = 1 ** -1
    let b1 = -1 ** -1
    let b2: UInt64 = -1 ** -1
    let c = Int8(0) ** 0
    let d = -1 ** r'c'
    // no need to test more primitive types
    let e1 = A() ** 0
    let e2 = 0 ** A()
    0
}

class A {}

/* SCAN
error: invalid binary operator '**' on type 'Int64' and 'Int64'
  ==> pow_int64_uint64_0.cj:10:16:
   | 
10 |     let a1 = 1 ** -1
   |                ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Int64', or to provide a right operand of type 'UInt64'

error: invalid binary operator '**' on type 'Int64' and 'Int64'
  ==> pow_int64_uint64_0.cj:11:24:
   | 
11 |     let a2: UInt64 = 1 ** -1
   |                        ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Int64', or to provide a right operand of type 'UInt64'

error: invalid binary operator '**' on type 'Int64' and 'Int64'
  ==> pow_int64_uint64_0.cj:12:17:
   | 
12 |     let b1 = -1 ** -1
   |                 ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Int64', or to provide a right operand of type 'UInt64'

error: invalid binary operator '**' on type 'Int64' and 'Int64'
  ==> pow_int64_uint64_0.cj:13:25:
   | 
13 |     let b2: UInt64 = -1 ** -1
   |                         ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Int64', or to provide a right operand of type 'UInt64'

error: invalid binary operator '**' on type 'Int8' and 'Int64'
  ==> pow_int64_uint64_0.cj:14:21:
   | 
14 |     let c = Int8(0) ** 0
   |                     ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Int8', or to provide a left operand of type 'Float64'

error: invalid binary operator '**' on type 'Int64' and 'Rune'
  ==> pow_int64_uint64_0.cj:15:16:
   | 
15 |     let d = -1 ** r'c'
   |                ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Rune)' for type 'Int64', or to provide a right operand of type 'UInt64'

error: invalid binary operator '**' on type 'Class-A' and 'Int64'
  ==> pow_int64_uint64_0.cj:17:18:
   | 
17 |     let e1 = A() ** 0
   |                  ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int64)' for type 'Class-A', or to provide a left operand of type 'Float64'

error: invalid binary operator '**' on type 'Int64' and 'Class-A'
  ==> pow_int64_uint64_0.cj:18:16:
   | 
18 |     let e2 = 0 ** A()
   |                ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Class-A)' for type 'Int64', or to provide a right operand of type 'UInt64'

8 errors generated, 8 errors printed.
*/
