// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt -Woff unused %f -o %output 2>&1 | compare %f

main() {
    // float literal exceeds the value range of target float type.
    let min16: Float16 = 5.960464477539063E-8
    let small16: Float16 = 5.9604E-8
    let max16: Float16 = 6.5504E4
    let large16: Float16 = 6.5540E4

    let min32: Float32 = 1.40129846E-45
    let small32: Float32 = 0.6012E-45
    let max32: Float32 = 3.40282347E38
    let large32: Float32 = 3.4029E38

    let min64: Float64 = 4.9406564584124654E-324
    let small64: Float64 = 2.4625E-324
    let max64: Float64 = 1.7976931348623157E308
    let large64: Float64 = 1.7977E308
    var lit01: Float64 = 2.10e-100000

    // float literal exceeds the supported parsable value range.
    let small = 1E-10000
    let large = 1E10000

    let _ = 1_e1000000000000000000000000000000000
    let _ = 0x1p-100000000000000000000000000000000000000000
}

/* SCAN
warning: magnitude of floating-point literal too small for type 'Float16', minimum is 5.960464477539063E-8
  ==> overflow_check_float.cj:12:28:
   | 
12 |     let small16: Float16 = 5.9604E-8
   |                            ^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too large for type 'Float16', maximum is 6.5504E4
  ==> overflow_check_float.cj:14:28:
   | 
14 |     let large16: Float16 = 6.5540E4
   |                            ^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too small for type 'Float32', minimum is 1.40129846E-45
  ==> overflow_check_float.cj:17:28:
   | 
17 |     let small32: Float32 = 0.6012E-45
   |                            ^^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too large for type 'Float32', maximum is 3.40282347E38
  ==> overflow_check_float.cj:19:28:
   | 
19 |     let large32: Float32 = 3.4029E38
   |                            ^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too small for type 'Float64', minimum is 4.9406564584124654E-324
  ==> overflow_check_float.cj:22:28:
   | 
22 |     let small64: Float64 = 2.4625E-324
   |                            ^^^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too large for type 'Float64', maximum is 1.7976931348623157E308
  ==> overflow_check_float.cj:24:28:
   | 
24 |     let large64: Float64 = 1.7977E308
   |                            ^^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too small for type 'Float64', minimum is 4.9406564584124654E-324
  ==> overflow_check_float.cj:25:26:
   | 
25 |     var lit01: Float64 = 2.10e-100000
   |                          ^^^^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too small for type 'Float', minimum is 4.9406564584124654E-324
  ==> overflow_check_float.cj:28:17:
   | 
28 |     let small = 1E-10000
   |                 ^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too large for type 'Float', maximum is 1.7976931348623157E308
  ==> overflow_check_float.cj:29:17:
   | 
29 |     let large = 1E10000
   |                 ^^^^^^^ 
   | 

warning: magnitude of floating-point literal too large for type 'Float', maximum is 1.7976931348623157E308
  ==> overflow_check_float.cj:31:13:
   | 
31 |     let _ = 1_e1000000000000000000000000000000000
   |             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

warning: magnitude of floating-point literal too small for type 'Float', minimum is 4.9406564584124654E-324
  ==> overflow_check_float.cj:32:13:
   | 
32 |     let _ = 0x1p-100000000000000000000000000000000000000000
   |             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

11 warnings generated, 11 warnings printed.
*/
