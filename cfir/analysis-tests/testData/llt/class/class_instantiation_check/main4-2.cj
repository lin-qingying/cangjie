// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f


main(): Int64 {
    var a = B<Int32>()
    return 0
}

class B<T> {
    operatr func +(x:Int32){return 1} 
    operator func +(x:T){return 2}
}

/* SCAN
error: expected declaration, found 'operatr'
  ==> main4-2.cj:16:5:
   | 
16 |     operatr func +(x:Int32){return 1} 
   |     ^^^^^^^ expected declaration here
   | 

error: expected a func name after keyword 'func', found '+'
  ==> main4-2.cj:16:18:
   | 
16 |     operatr func +(x:Int32){return 1} 
   |             ~~~~ ^ expected a func name here
   |             |
   |             after keyword 'func'
   | 

2 errors generated, 2 errors printed.
*/
