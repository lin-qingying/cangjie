// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

open class int16 {}

open class int32 <: int16 {}

open class int64 <: int32 {}

open class float16 {}

open class float32 <: float16 {}

open class float64 <: float32 {}

open class bool {}

interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<int32> & I<float64> {
    func foo(a: int32): int32
    func foo(a: float64): float64

    static func fs(a: int32): int32
    static func fs(a: float64): float64
}

interface III2 <: II<int32> {
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III3 <: II<int32> {
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III4<T> <: II<T> & II<float64> {
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface III5<T> <: II<T> & II<float64> {
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface III6 <: II<int64> & II<int64> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III7<T> <: II<T> & II<int64> & II<int32> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III18<T> <: II<T> & II<int64> & II<int32> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

main() {
    return 0
}

/* SCAN
error: interface 'III2' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:36:1:
   | 
36 | interface III2 <: II<int32> {
   | ^ 
   | 

error: interface 'III3' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:41:1:
   | 
41 | interface III3 <: II<int32> {
   | ^ 
   | 

error: interface 'III6' inherits or implements duplicate interface 'II'
  ==> interface_generic11.cj:56:1:
   | 
56 | interface III6 <: II<int64> & II<int64> {
   | ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:36:19:
   | 
36 | interface III2 <: II<int32> {
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:41:19:
   | 
41 | interface III3 <: II<int32> {
   |                   ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:46:30:
   | 
46 | interface III4<T> <: II<T> & II<float64> {
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:51:30:
   | 
51 | interface III5<T> <: II<T> & II<float64> {
   |                              ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:61:42:
   | 
61 | interface III7<T> <: II<T> & II<int64> & II<int32> {
   |                                          ^ 
   | 

error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic11.cj:68:43:
   | 
68 | interface III18<T> <: II<T> & II<int64> & II<int32> {
   |                                           ^ 
   | 

error: generic instantiation 'II<Class-int32>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:36:19:
   | 
36 | interface III2 <: II<int32> {
   |                   ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:29:10:
   | 
29 |     func foo(a: int32): int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Class-int32>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:41:19:
   | 
41 | interface III3 <: II<int32> {
   |                   ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:29:10:
   | 
29 |     func foo(a: int32): int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Class-float64>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:46:30:
   | 
46 | interface III4<T> <: II<T> & II<float64> {
   |                              ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:30:10:
   | 
30 |     func foo(a: float64): float64
   |          ^^^ 
   | 

error: generic instantiation 'II<Class-float64>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:51:30:
   | 
51 | interface III5<T> <: II<T> & II<float64> {
   |                              ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:30:10:
   | 
30 |     func foo(a: float64): float64
   |          ^^^ 
   | 

error: generic instantiation 'II<Class-int32>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:61:42:
   | 
61 | interface III7<T> <: II<T> & II<int64> & II<int32> {
   |                                          ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:29:10:
   | 
29 |     func foo(a: int32): int32
   |          ^^^ 
   | 

error: generic instantiation 'II<Class-int32>' causes ambiguous function 'foo'
  ==> interface_generic11.cj:68:43:
   | 
68 | interface III18<T> <: II<T> & II<int64> & II<int32> {
   |                                           ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:24:10:
   | 
24 |     func foo(a: T): T
   |          ^^^ 
   | 
note: found candidate
  ==> interface_generic11.cj:29:10:
   | 
29 |     func foo(a: int32): int32
   |          ^^^ 
   | 

15 errors generated, 15 errors printed.
*/
