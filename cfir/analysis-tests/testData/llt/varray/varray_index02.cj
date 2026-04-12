// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    // Get and Set of Multidimensional Arrays.
    var arr8: VArray<VArray<Int64, $2>, $2> = [[1, 2], [3, 4]]
    let element1 = arr8[1][0] // ok
    arr8[1][1] = 5 // error
    let element1_1 = arr8[2][1] // error
    arr8[1][2] = 6 // error
}

/* SCAN
error: cannot assign to immutable value
  ==> varray_index02.cj:13:5:
   | 
13 |     arr8[1][1] = 5 // error
   |     ^^^^^^^^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray_index02.cj:15:5:
   | 
15 |     arr8[1][2] = 6 // error
   |     ^^^^^^^^^^^^^^ 
   | 

error: VArray index must be in bounds
  ==> varray_index02.cj:14:22:
   | 
14 |     let element1_1 = arr8[2][1] // error
   |                      ^    ~ 'VArray' index 2 is past the end of 'VArray' (which contains 2 elements)
   | 

error: VArray index must be in bounds
  ==> varray_index02.cj:15:5:
   | 
15 |     arr8[1][2] = 6 // error
   |     ^       ~ 'VArray' index 2 is past the end of 'VArray' (which contains 2 elements)
   | 

4 errors generated, 4 errors printed.
*/
