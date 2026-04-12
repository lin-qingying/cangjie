// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1<T> {
    public var i: T

    public mut func foo1() {
        let f = {=> this}

        func goo() {
            this
        }
        return 0
    }

    public mut func foo2(x: T) {
        let f = {=> this.i = x}

        func goo() {
            this.i = 2
        }
        return 0
    }

    public mut func foo3() {
        let f = {=> this.i}

        func goo() {
            this.i
        }
        return 0
    }

    public mut func foo4() {
        let f = {=> i}

        func goo() {
            i
        }
        return 0
    }

    public mut func foo5() {
        return this
    }

    public mut func foo6() {
        var a = this
        return a
    }
}

main() {
    return 1
}

/* SCAN
error: cannot convert an integer literal to type 'Generics-T'
  ==> record_mut_invalid_5.cj:25:22:
   | 
25 |             this.i = 2
   |                      ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo1'
  ==> record_mut_invalid_5.cj:13:21:
   | 
13 |         let f = {=> this}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo1'
  ==> record_mut_invalid_5.cj:16:13:
   | 
16 |             this
   |             ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo2'
  ==> record_mut_invalid_5.cj:22:21:
   | 
22 |         let f = {=> this.i = x}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo2'
  ==> record_mut_invalid_5.cj:25:13:
   | 
25 |             this.i = 2
   |             ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo3'
  ==> record_mut_invalid_5.cj:31:21:
   | 
31 |         let f = {=> this.i}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo3'
  ==> record_mut_invalid_5.cj:34:13:
   | 
34 |             this.i
   |             ^ 
   | 

error: 'this' cannot be used as an expression in the mutable function 'foo5'
  ==> record_mut_invalid_5.cj:49:16:
   | 
49 |         return this
   |                ^ 
   | 

error: 'this' cannot be used as an expression in the mutable function 'foo6'
  ==> record_mut_invalid_5.cj:53:17:
   | 
53 |         var a = this
   |                 ^ 
   | 

error: the uninitialized member variable 'i' is not initialized in the constructor of class or struct
  ==> record_mut_invalid_5.cj:10:5:
   | 
10 |     public var i: T
   |     ^ 
   | 

error: 'i' cannot be captured in the mutable function 'foo4'
  ==> record_mut_invalid_5.cj:40:21:
   | 
40 |         let f = {=> i}
   |                     ^ 
   | 

error: 'i' cannot be captured in the mutable function 'foo4'
  ==> record_mut_invalid_5.cj:43:13:
   | 
43 |             i
   |             ^ 
   | 

12 errors generated, 12 errors printed.
*/
