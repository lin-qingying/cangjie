// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    var a = B<Int32>()
    return 0
}

class B<T> {
    public operator func +(x: Int32) {
        return;
    }
    public operator func +(x: T) {
        return 2
    }
}

/* SCAN
error: generic instantiation 'B<Int32>' causes ambiguous function '+'
  ==> main4-1.cj:10:13:
   | 
10 |     var a = B<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> main4-1.cj:15:26:
   | 
15 |     public operator func +(x: Int32) {
   |                          ^ 
   | 
note: found candidate
  ==> main4-1.cj:18:26:
   | 
18 |     public operator func +(x: T) {
   |                          ^ 
   | 

1 error generated, 1 error printed.
*/
