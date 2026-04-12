// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class Day<T> where T <: Day<T> {
    public var day: T
    public init(x: T) {
        day = x
    }
    public static func foo<V>(x: V): V where V <: String {
        return x
    }
    public static func boo<V>(x: V): V where V <: Day<V> {
        return x
    }
    public static func coo<V>(x: V): V where V <: Day<T> {
        return x
    }
}

class Monday <: Day<Monday> {
    public var d: Int32
    public init() {
        this(0)
    }
    public init(x: Int32) {
        super(Monday())
        d = x
    }
}

func run(): Int64 {
    var v0: Int32 = 1
    var v1: Monday = Monday(1)
    var v2: Day<Monday> = Day<Monday>(v1)
    var v3: String = Day<Monday>.foo<String>("a")
    var v4: Monday = Day<Monday>.boo<Monday>(v1)
    var v5: Monday = Day<Monday>.coo<Monday>(v1)
    if (v2.day.d != 1 || v3 != "a" || v4.d != 1 || v5.d != 1) {
        return 1
    }
    return 0
}

main(): Int64 {
    return run()
}

/* SCAN
error: the upper bound 'Struct-String' of generic parameter 'Generics-V' must be class or interface
  ==> call22.cj:14:28:
   | 
14 |     public static func foo<V>(x: V): V where V <: String {
   |                            ^ 
   | 

error: recursive constructor calling detected
  ==> call22.cj:27:12:
   | 
27 |     public init() {
   |            ^^^^ 
   | 
note: depends on
  ==> call22.cj:28:9:
   | 
28 |         this(0)
   |         ^^^^ 
   | 
note: depends on
  ==> call22.cj:30:12:
   | 
30 |     public init(x: Int32) {
   |            ^^^^ 
   | 
note: depends on
  ==> call22.cj:31:15:
   | 
31 |         super(Monday())
   |               ^^^^^^ 
   | 
note: depends on
  ==> call22.cj:27:12:
   | 
27 |     public init() {
   |            ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
