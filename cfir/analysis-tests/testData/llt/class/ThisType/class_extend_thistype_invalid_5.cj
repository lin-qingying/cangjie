// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class R{

}

extend R {
    public static func a2() : This{ 

    }

	func foo(): This {
		return this
	}
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'This' type is not allowed
  ==> class_extend_thistype_invalid_5.cj:14:31:
   | 
14 |     public static func a2() : This{ 
   |                               ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

error: 'This' type is not allowed
  ==> class_extend_thistype_invalid_5.cj:18:14:
   | 
18 |     func foo(): This {
   |                 ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

2 errors generated, 2 errors printed.
*/
