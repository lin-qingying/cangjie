// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f



class A{}
extend<Int32> Array<Int32> {  // 'Int32' here is treated as generic parameter which is invalid.
    func plus(a: Int32): Int32 { 0 }
}

extend<A> Array<A> {
    func plus(a: Int32): Int32 { 0 }
}

main(): Int32 {
    return 0
}

/* SCAN
error: expected a generic type name after '<' in generic, found keyword 'Int32'
  ==> array_extend_01.cj:12:8:
   | 
12 | extend<Int32> Array<Int32> {  // 'Int32' here is treated as generic parameter which is invalid.
   |       ~^^^^^ expected a generic type name here
   |       |
   |       after '<' in generic
   | 
help: you could escape keyword as a generic type name using '`':
   | 
12 | extend<`Int32`> Array<Int32> {  // 'Int32' here is treated as generic parameter which is invalid.
   |        ~~~~~~~ 
   | 

1 error generated, 1 error printed.
*/
