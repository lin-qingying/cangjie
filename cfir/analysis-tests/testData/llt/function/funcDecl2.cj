// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func test() {}

func test1(a!: Int32 = 2, b!: Int32 = 2) {}

func help() {
    return 0
}

func help2(): Unit {
    return 0
}

func help3(): Unit {
    return
}

func exit() {
    if (true) {
        return 0
    } else {
        return ""
    };
    return
}
//func exit2() {if (true {return 0} else) {}}
func exit3() {
    if (true) {
        return
    } else {
        return
    };
    return
}

main() {
    test1(a: 1, b: 1)
    test1(1, b: 1)
    test1(b: 1, a: 1)
    test1()
    test1(b: 2)
    test1(a: 1)
    test1(a: 1, 1)
    test1(1, a: 1)
    test1("", "")
    test1("", "", "")
    return 0
}

func fun(a: Int32): Int32 {
    return a
}

func callFunc(myfunc: (Int32, Int32) -> Int32): Int32 {
    return myfunc(0)
}

func foo(): Int32 {
    return callFunc(fun)
}

func bar() {
    return true;
    1
}

func baz() {
    while (true) {
        return false
    };
    1
}

/* SCAN
error: cannot convert an integer literal to type 'Unit'
  ==> funcDecl2.cj:18:12:
   | 
18 |     return 0
   |            ^ 
   | 

error: the return type of this function cannot be calculated from the function body and all the return expressions
  ==> funcDecl2.cj:31:5:
   | 
31 |     return
   |     ^ 
   | 
   # note: Traces:
The types 'Int64', 'Nothing', 'Struct-String' and 'Unit' do not have the smallest common supertype


error: missing argument prefix 'a:' for named parameter
  ==> funcDecl2.cj:45:11:
   | 
45 |     test1(1, b: 1)
   |           ^ 
   | 

error: positional argument cannot appear after named argument
  ==> funcDecl2.cj:50:17:
   | 
50 |     test1(a: 1, 1)
   |                 ^ 
   | 

error: missing argument prefix 'b:' for named parameter
  ==> funcDecl2.cj:51:11:
   | 
51 |     test1(1, a: 1)
   |           ^ 
   | 

error: missing argument prefix 'a:' or 'b:' for named parameter
  ==> funcDecl2.cj:52:11:
   | 
52 |     test1("", "")
   |           ^^ 
   | 

error: extra argument given for parameter list '(Int32, Int32)' in call
  ==> funcDecl2.cj:53:10:
   | 
53 |     test1("", "", "")
   |          ^^^^^^^^^^^^ expected 2 arguments, found 3
   | 
note: found candidate
  ==> funcDecl2.cj:11:6:
   | 
11 | func test1(a!: Int32 = 2, b!: Int32 = 2) {}
   |      ^^^^^ 
   | 

error: missing argument for parameter list '(Int32, Int32)' in call
  ==> funcDecl2.cj:62:18:
   | 
62 |     return myfunc(0)
   |                  ^^^ expected 2 arguments, found 1
   | 
note: found candidate
  ==> funcDecl2.cj:61:15:
   | 
61 | func callFunc(myfunc: (Int32, Int32) -> Int32): Int32 {
   |               ^^^^^^ 
   | 

error: no matching function declaration for function reference 'fun'
  ==> funcDecl2.cj:66:21:
   | 
66 |     return callFunc(fun)
   |                     ^ 
   | 

error: the return type of this function cannot be calculated from the function body and all the return expressions
  ==> funcDecl2.cj:71:5:
   | 
71 |     1
   |     ^ 
   | 
   # note: Traces:
The types 'Bool' and 'Int64' do not have the smallest common supertype


error: the return type of this function cannot be calculated from the function body and all the return expressions
  ==> funcDecl2.cj:78:5:
   | 
78 |     1
   |     ^ 
   | 
   # note: Traces:
The types 'Bool' and 'Int64' do not have the smallest common supertype


11 errors generated, 11 errors printed.
*/
