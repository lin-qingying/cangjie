// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class C1<T> {
    public func c1(a: Int32) {
        return 0
    }
    public func c1(a: T) {
        return 0
    }
}

open class C2<T> <: C1<T> {}

class C3 <: C2<Int32> {}

main() {
    var a = C1<Int32>()
    var b = C2<Int32>()
    return 0
}

/* SCAN
error: generic instantiation 'C2<Int32>' causes ambiguous function 'c1'
  ==> main1-6.cj:20:13:
   | 
20 | class C3 <: C2<Int32> {}
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main1-6.cj:10:17:
   | 
10 |     public func c1(a: Int32) {
   |                 ^^ 
   | 
note: found candidate
  ==> main1-6.cj:13:17:
   | 
13 |     public func c1(a: T) {
   |                 ^^ 
   | 

error: generic instantiation 'C1<Int32>' causes ambiguous function 'c1'
  ==> main1-6.cj:23:13:
   | 
23 |     var a = C1<Int32>()
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main1-6.cj:10:17:
   | 
10 |     public func c1(a: Int32) {
   |                 ^^ 
   | 
note: found candidate
  ==> main1-6.cj:13:17:
   | 
13 |     public func c1(a: T) {
   |                 ^^ 
   | 

error: generic instantiation 'C2<Int32>' causes ambiguous function 'c1'
  ==> main1-6.cj:24:13:
   | 
24 |     var b = C2<Int32>()
   |             ^^^^^^^^^ 
   | 
note: found candidate
  ==> main1-6.cj:10:17:
   | 
10 |     public func c1(a: Int32) {
   |                 ^^ 
   | 
note: found candidate
  ==> main1-6.cj:13:17:
   | 
13 |     public func c1(a: T) {
   |                 ^^ 
   | 

3 errors generated, 3 errors printed.
*/
