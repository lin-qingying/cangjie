// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class C1 {
    public mut func goo() { 

    }
}

struct R1 {
    public mut func goo() {

    }

    public mut static func foo() { 

    }

    public mut operator func +(x: R1, y: R1) { 
        return x
    }
}

main() {
    return 1
}

/* SCAN
error: unexpected modifier 'mut' on function declaration in class body
  ==> record_mut_invalid_1.cj:10:12:
   | 
9  | class C1 {
   | ~~~~~ in class
10 |     public mut func goo() { 
   |            ^^^ ~~~~ on function declaration
   |            |
   |            unexpected modifier
   | 

error: 'static' and 'mut' modifiers conflict on function declaration
  ==> record_mut_invalid_1.cj:20:16:
   | 
20 |     public mut static func foo() { 
   |            ~~~ ^^^^^^ ~~~~ on function declaration
   |            |   |
   |            |   unexpected modifier
   |            |
   |            because it is conflicted with this
   | 

2 errors generated, 2 errors printed.
*/
