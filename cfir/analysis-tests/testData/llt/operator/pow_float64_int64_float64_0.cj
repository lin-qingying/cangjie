// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let a1 = 1.0 ** Int32(-1.0)
    let a2: UInt64 = 1.0 ** -1.0
    let c = Float16(0.0) ** 0.1
    let d = -1.0 ** r'c'
    // no need to test more primitive types
    let e1 = A() ** 0.0
    let e2 = 0.0 ** A()
    0
}

class A {}

/* SCAN
error: invalid binary operator '**' on type 'Float64' and 'Int32'
  ==> pow_float64_int64_float64_0.cj:10:18:
   | 
10 |     let a1 = 1.0 ** Int32(-1.0)
   |                  ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Int32)' for type 'Float64', or to provide a right operand of type 'Int64' or 'Float64'

error: mismatched types
  ==> pow_float64_int64_float64_0.cj:11:22:
   | 
11 |     let a2: UInt64 = 1.0 ** -1.0
   |                      ^^^^^^^^^^^ expected 'UInt64', found 'Float64'
   | 

error: invalid binary operator '**' on type 'Float16' and 'Float64'
  ==> pow_float64_int64_float64_0.cj:12:26:
   | 
12 |     let c = Float16(0.0) ** 0.1
   |                          ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Float64)' for type 'Float16', or to provide a left operand of type 'Float64'

error: invalid binary operator '**' on type 'Float64' and 'Rune'
  ==> pow_float64_int64_float64_0.cj:13:18:
   | 
13 |     let d = -1.0 ** r'c'
   |                  ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Rune)' for type 'Float64', or to provide a right operand of type 'Int64' or 'Float64'

error: invalid binary operator '**' on type 'Class-A' and 'Float64'
  ==> pow_float64_int64_float64_0.cj:15:18:
   | 
15 |     let e1 = A() ** 0.0
   |                  ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Float64)' for type 'Class-A', or to provide a left operand of type 'Float64'

error: invalid binary operator '**' on type 'Float64' and 'Class-A'
  ==> pow_float64_int64_float64_0.cj:16:18:
   | 
16 |     let e2 = 0.0 ** A()
   |                  ^^ 
   | 
   # note: you may want to implement 'operator func **(right: Class-A)' for type 'Float64', or to provide a right operand of type 'Int64' or 'Float64'

6 errors generated, 6 errors printed.
*/
