// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum MyList<T> {
    | Cons(T, MyList<T>)
    | Nil
}

main() {
    let x = MyList.Cons(0, Cons(1, Cons(2, Nil)))
    match (x) {
        case Nil => 0
        case MyList<Bool>.Cons(_, Nil) => 1
        case _ => 2
    }
}

/* SCAN
error: enum pattern is not matched
  ==> usertype_04.cj:18:14:
   | 
18 |         case MyList<Bool>.Cons(_, Nil) => 1
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
