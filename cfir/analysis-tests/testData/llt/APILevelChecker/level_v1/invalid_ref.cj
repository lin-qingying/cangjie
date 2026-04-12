// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// ERRCHECK: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix --cfg=APILevel_level=11

import ohos.labels.*



@!APILevel[10]
public func foo(): Unit {
    goo // error
    hoo // error
    koo // ok
}

@!APILevel[11]
public func goo(): Unit {
    foo // ok
    hoo // error
    koo // ok
}

@!APILevel[12]
public func hoo(): Unit {
    goo() // ok
    foo() // ok
    koo() // ok
}


public func koo(): Unit {
    foo() // ok
    goo() // ok
    hoo() // error
}

main() {}

/* SCAN
error: cannot reference 'goo'(level: 11) which higher than level of the current scope(level: 10)
  ==> invalid_ref.cj:17:5:
   | 
17 |     goo // error
   |     ^^^ 
   | 

error: cannot reference 'hoo'(level: 12) which higher than level of the current scope(level: 10)
  ==> invalid_ref.cj:18:5:
   | 
18 |     hoo // error
   |     ^^^ 
   | 

error: cannot reference 'hoo'(level: 12) which higher than level of the current scope(level: 11)
  ==> invalid_ref.cj:25:5:
   | 
25 |     hoo // error
   |     ^^^ 
   | 

error: cannot reference 'hoo'(level: 12) which higher than level of the current scope(level: 11)
  ==> invalid_ref.cj:40:5:
   | 
40 |     hoo() // error
   |     ^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
