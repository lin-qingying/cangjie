// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

class A {
    public var a: Int64
    init(a_: Int64) {
        a = a_
    }
    operator func [](arg: A, value!: A) {
        this.a = arg.a
    }
}

func test01() {
    var a = A(1)
    (a.x, a.y) = (1, 2, 8)
}

func test02() {
    var class_a = A(1)
    var class_b = A(1)
    var class_c = A(1)
    (class_a[class_b], class_b[class_a]) = (1, 2)
    (class_a[class_b], class_b[class_a]) = 1
    (class_a[class_b], class_b[class_a]) = (1, 2, 3)
}

main() {
}

/* SCAN
error: 'x' is not a member of class 'A'
  ==> case05.cj:21:8:
   | 
21 |     (a.x, a.y) = (1, 2, 8)
   |        ^ 
   | 

error: 'y' is not a member of class 'A'
  ==> case05.cj:21:13:
   | 
21 |     (a.x, a.y) = (1, 2, 8)
   |             ^ 
   | 

error: mismatched types
  ==> case05.cj:21:18:
   | 
21 |     (a.x, a.y) = (1, 2, 8)
   |     ~~~~~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |     |
   |     the tuple has 2-elements, found one with 3-elements
   | 

error: cannot assign to this subscript expression
  ==> case05.cj:28:6:
   | 
28 |     (class_a[class_b], class_b[class_a]) = (1, 2)
   |      ^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Class-A, value!: Int64)' for type 'Class-A'

error: cannot assign to this subscript expression
  ==> case05.cj:28:24:
   | 
28 |     (class_a[class_b], class_b[class_a]) = (1, 2)
   |                        ^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Class-A, value!: Int64)' for type 'Class-A'

error: mismatched types
  ==> case05.cj:28:44:
   | 
28 |     (class_a[class_b], class_b[class_a]) = (1, 2)
   |      ~~~~~~~~~~~~~~~~                      ^^^^^^ the expression has type 'Tuple<Int64, Int64>'
   |      |
   |      can not assign it with 'Int64'
   | 

error: mismatched types
  ==> case05.cj:29:44:
   | 
29 |     (class_a[class_b], class_b[class_a]) = 1
   |     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   ^ the expression has type 'Int64'
   |     |
   |     can not assign it with 'Int64'
   | 

error: cannot assign to this subscript expression
  ==> case05.cj:30:6:
   | 
30 |     (class_a[class_b], class_b[class_a]) = (1, 2, 3)
   |      ^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Class-A, value!: Int64)' for type 'Class-A'

error: cannot assign to this subscript expression
  ==> case05.cj:30:24:
   | 
30 |     (class_a[class_b], class_b[class_a]) = (1, 2, 3)
   |                        ^^^^^^^^^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Class-A, value!: Int64)' for type 'Class-A'

error: mismatched types
  ==> case05.cj:30:44:
   | 
30 |     (class_a[class_b], class_b[class_a]) = (1, 2, 3)
   |     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |     |
   |     the tuple has 2-elements, found one with 3-elements
   | 

10 errors generated, 10 errors printed.
*/
