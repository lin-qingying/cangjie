// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo1() {
        let f = {=> this}

        func goo() {
            this
        }
        return 0
    }

    public mut func foo2() {
        let f = {=> this.i = 2}

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
error: 'this' cannot be captured in the mutable function 'foo1'
  ==> record_extend_mut_invalid_3.cj:15:21:
   | 
15 |         let f = {=> this}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo1'
  ==> record_extend_mut_invalid_3.cj:18:13:
   | 
18 |             this
   |             ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo2'
  ==> record_extend_mut_invalid_3.cj:24:21:
   | 
24 |         let f = {=> this.i = 2}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo2'
  ==> record_extend_mut_invalid_3.cj:27:13:
   | 
27 |             this.i = 2
   |             ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo3'
  ==> record_extend_mut_invalid_3.cj:33:21:
   | 
33 |         let f = {=> this.i}
   |                     ^ 
   | 

error: 'this' cannot be captured in the mutable function 'foo3'
  ==> record_extend_mut_invalid_3.cj:36:13:
   | 
36 |             this.i
   |             ^ 
   | 

error: 'this' cannot be used as an expression in the mutable function 'foo5'
  ==> record_extend_mut_invalid_3.cj:51:16:
   | 
51 |         return this
   |                ^ 
   | 

error: 'this' cannot be used as an expression in the mutable function 'foo6'
  ==> record_extend_mut_invalid_3.cj:55:17:
   | 
55 |         var a = this
   |                 ^ 
   | 

error: 'i' cannot be captured in the mutable function 'foo4'
  ==> record_extend_mut_invalid_3.cj:42:21:
   | 
42 |         let f = {=> i}
   |                     ^ 
   | 

error: 'i' cannot be captured in the mutable function 'foo4'
  ==> record_extend_mut_invalid_3.cj:45:13:
   | 
45 |             i
   |             ^ 
   | 

10 errors generated, 10 errors printed.
*/
