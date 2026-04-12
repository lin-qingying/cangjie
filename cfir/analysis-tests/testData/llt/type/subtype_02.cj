// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

main() {
    let t1: Option<ToString> = Option<ToString>.Some(Test1)
    let t2: Option<Test> = Option<Test>.None
    doSomething(t1)
    doSomething(t2)
}

func doSomething(a: Option<ToString>) {}

enum Test <: ToString {
    Test1

    public func toString() {
        "test"
    }
}

/* SCAN
error: mismatched types
  ==> subtype_02.cj:13:17:
   | 
13 |     doSomething(t2)
   |                 ^^ expected 'Enum-Option<Interface-ToString>', found 'Enum-Option<Enum-Test>'
   | 

1 error generated, 1 error printed.
*/
