// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

foreign func foo1(p: CPointer<Int64>): Unit

struct S {
    public var arr: VArray<Int32, $3> = [1, 2, 3]
}

unsafe main() {
    var a = VArray<Int64, $3>(repeat: 0)
    foo1(inout a.size)
    return 0
}

/* SCAN
error: 'inout' can only qualify variable defined with 'var'
  ==> varray_size02.cj:17:16:
   | 
17 |     foo1(inout a.size)
   |                ^^^^^^ is a immutable variable
   | 

1 error generated, 1 error printed.
*/
