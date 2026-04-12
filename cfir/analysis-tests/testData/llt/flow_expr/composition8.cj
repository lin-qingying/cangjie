// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

struct A{
    public static var a = (input) => {input}
    static var b = (input) => {input}
    static var c = (input) => {input(1)}
}


main(): Int64 {
    1 |> A.a
    A.a~>A.b
    1 |> (A.a~><A.b) 
    return (A.a ~> A.b |> A.c) - 1
}

/* SCAN
error: expected declaration, found '=>'
  ==> composition8.cj:10:35:
   | 
10 |     public static var a = (input) => {input}
   |                                   ^^ expected declaration here
   | 

error: expected declaration, found '=>'
  ==> composition8.cj:11:28:
   | 
11 |     static var b = (input) => {input}
   |                            ^^ expected declaration here
   | 

error: expected declaration, found '=>'
  ==> composition8.cj:12:28:
   | 
12 |     static var c = (input) => {input(1)}
   |                            ^^ expected declaration here
   | 

error: expected expression after '~>', found '<'
  ==> composition8.cj:19:16:
   | 
19 |     1 |> (A.a~><A.b) 
   |                ^ expected expression here
   | 

4 errors generated, 4 errors printed.
*/
