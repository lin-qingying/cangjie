// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

internal import pkga.*

class C <: I { // error
    public func f() {
        2
    }
}

interface J <: I {} // error

class D {}

extend D <: I { // error
    public func f() {
        3
    }
}

main() {}

/* SCAN
error: cannot implement imported 'sealed' interface 'I'
  ==> fail.cj:13:7:
   | 
13 | class C <: I { // error
   |       ^    ~ sealed interface declared in package 'pkga'
   | 

error: cannot inherit imported 'sealed' interface 'I'
  ==> fail.cj:19:11:
   | 
19 | interface J <: I {} // error
   |           ^    ~ sealed interface declared in package 'pkga'
   | 

error: cannot implement imported 'sealed' interface 'I'
  ==> fail.cj:21:7:
   | 
21 | class D {}
   |       ^ 
22 | 
23 | extend D <: I { // error
   |             ~ sealed interface declared in package 'pkga'
   | 

3 errors generated, 3 errors printed.
*/
