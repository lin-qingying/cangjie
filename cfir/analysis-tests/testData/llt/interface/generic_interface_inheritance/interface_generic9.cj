// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<Int32> & I<Float64> {
    func foo(a: Int32): Int32
    func foo(a: Float64): Float64

    static func fs(a: Int32): Int32
    static func fs(a: Float64): Float64
}

interface III2 <: II<Int32> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III3 <: II<Int32> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III4<T> <: II<T> & II<Float64> {
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface III5<T> <: II<T> & II<Float64> {
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface III6 <: II<Int64> & II<Int64> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III7<T> <: II<T> & II<Int64> & II<Int32> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III18<T> <: II<T> & II<Int64> & II<Int32> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

main() {
    return 0
}

/* SCAN
error: interface 'III2' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:22:1:
   | 
22 | interface III2 <: II<Int32> {
   | ^ 
   | 

error: interface 'III3' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:27:1:
   | 
27 | interface III3 <: II<Int32> {
   | ^ 
   | 

error: interface 'III6' inherits or implements duplicate interface 'II'
  ==> interface_generic9.cj:42:1:
   | 
42 | interface III6 <: II<Int64> & II<Int64> {
   | ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:22:19:
   | 
22 | interface III2 <: II<Int32> {
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:27:19:
   | 
27 | interface III3 <: II<Int32> {
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:32:30:
   | 
32 | interface III4<T> <: II<T> & II<Float64> {
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:37:30:
   | 
37 | interface III5<T> <: II<T> & II<Float64> {
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:47:42:
   | 
47 | interface III7<T> <: II<T> & II<Int64> & II<Int32> {
   |                                          ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic9.cj:54:43:
   | 
54 | interface III18<T> <: II<T> & II<Int64> & II<Int32> {
   |                                           ^ 
   | 

error: generic instantiation 'II<Int32>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:22:19:
   | 
22 | interface III2 <: II<Int32> {
   |                   ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:15:10:
   | 
15 |     func foo(a: Int32): Int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Int32>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:27:19:
   | 
27 | interface III3 <: II<Int32> {
   |                   ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:15:10:
   | 
15 |     func foo(a: Int32): Int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Float64>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:32:30:
   | 
32 | interface III4<T> <: II<T> & II<Float64> {
   |                              ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:16:10:
   | 
16 |     func foo(a: Float64): Float64
   |          ^^^ 
   | 

error: generic instantiation 'II<Float64>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:37:30:
   | 
37 | interface III5<T> <: II<T> & II<Float64> {
   |                              ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:16:10:
   | 
16 |     func foo(a: Float64): Float64
   |          ^^^ 
   | 

error: generic instantiation 'II<Int32>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:47:42:
   | 
47 | interface III7<T> <: II<T> & II<Int64> & II<Int32> {
   |                                          ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:15:10:
   | 
15 |     func foo(a: Int32): Int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Int32>' causes ambiguous function 'foo'
  ==> interface_generic9.cj:54:43:
   | 
54 | interface III18<T> <: II<T> & II<Int64> & II<Int32> {
   |                                           ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:10:10:
   | 
10 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic9.cj:15:10:
   | 
15 |     func foo(a: Int32): Int32
   |          ^^^ 
   | 

15 errors generated, 15 errors printed.
*/
