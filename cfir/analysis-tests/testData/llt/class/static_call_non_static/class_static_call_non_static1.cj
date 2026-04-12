// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

main(): Int64 {
    return run()
}

class Data {
    var width: Int32
    var length: Int32

    init(x: Int32, y: Int32) {
        width = x
        length = y
    }

    func getWidth() {
        return width
    }

    func getLength() {
        return length
    }

    operator func -(operand: Data): Data {
        var left: Int32 = getWidth() - 1
        var right: Int32 = getLength() - 1
    }

    static func g() {
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
  ==> class_static_call_non_static1.cj:32:9:
   | 
32 |         var right: Int32 = getLength() - 1
   |         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expected 'Class-Data', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

error: invalid unary operator '-' on type 'Class-Data'
  ==> class_static_call_non_static1.cj:43:14:
   | 
43 |     var dd = -data
   |              ^^^^^ 
   | 
   # note: you may want to implement 'operator func -()' for type 'Class-Data'

error: 'width' is non-static member, cannot be accessed by static function 'g'
  ==> class_static_call_non_static1.cj:36:17:
   | 
36 |         var x = width
   |                 ^ 
   | 

3 errors generated, 3 errors printed.
*/
