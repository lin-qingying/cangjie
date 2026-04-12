// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend Int64 {
    public operator func +(right: Int64) {
        return this
    }
    public operator func ==(right: Int64) {
        return this
    }
    public operator func <<(right: Int64) {
        return this
    }
    public operator func -(right: Int64) {
        return this
    }
    public operator func -() {
        return this
    }
    public operator func [](right: Int64) { // OK
        return this
    }
    public operator func +(right: Int32) { // OK
        return this + Int64(right)
    }
}

extend Bool {
    public operator func ==(right: Bool) {
        return true
    }
    public operator func +(right: Bool) { // OK
        return true
    }
}

/* SCAN
error: operator func +(Int64) of type Int64 is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:10:5:
   | 
10 |     public operator func +(right: Int64) {
   |     ^ 
   | 

error: operator func ==(Int64) of type Int64 is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:13:5:
   | 
13 |     public operator func ==(right: Int64) {
   |     ^ 
   | 

error: operator func <<(Int64) of type Int64 is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:16:5:
   | 
16 |     public operator func <<(right: Int64) {
   |     ^ 
   | 

error: operator func -(Int64) of type Int64 is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:19:5:
   | 
19 |     public operator func -(right: Int64) {
   |     ^ 
   | 

error: operator func -() of type Int64 is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:22:5:
   | 
22 |     public operator func -() {
   |     ^ 
   | 

error: operator func ==(Bool) of type Bool is a built-in function and cannot be overridden
  ==> extend_overload_builtin_op1.cj:34:5:
   | 
34 |     public operator func ==(right: Bool) {
   |     ^ 
   | 

error: return type of '==' is not identical or not a subtype of the overridden/redefined/implement function
  ==> extend_overload_builtin_op1.cj:13:26:
   | 
13 |     public operator func ==(right: Int64) {
   |                          ^^ 
   | 
note: 'Int64' is not a subtype of 'Bool'
   ==> (package std.core)comparable.cj:331:26:

error: extend member '==' is not allowed to shadow members of 'extend Bool'
  ==> extend_overload_builtin_op1.cj:34:26:
   | 
34 |     public operator func ==(right: Bool) {
   |                          ^^ 
   | 

error: 'main' is missing
 ==> extend_overload_builtin_op1.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

9 errors generated, 9 errors printed.
*/
