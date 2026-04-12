// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

main(): Int64 {
    return run()
}

struct Data {
    public var width: Int32
    public var length: Int32

    public init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    public operator func -(): Data {
        var left: Int32 = width
        var right: Int32 = length
    }

    public static func g() {
        var x = width
        return x
    }
}

func run(): Int64 {
    var data: Data = Data(1, 2)
    var dd = -data
    return 0
}

/* SCAN
error: mismatched types
  ==> record_static_call_non_static1.cj:24:9:
   | 
24 |         var right: Int32 = length
   |         ^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Struct-Data', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

error: 'width' is non-static member, cannot be accessed by static function 'g'
  ==> record_static_call_non_static1.cj:28:17:
   | 
28 |         var x = width
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
