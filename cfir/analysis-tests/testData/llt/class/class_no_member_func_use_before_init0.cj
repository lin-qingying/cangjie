// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct Date {
    private let Date: Int64 = 5
    private let Date1: Int64 = 6

    public func get_pub_Date(): Int64 {
        return Date
    }
    public let pub_modifier: () -> Int64 = get_pub_Date
    private let pri_modifier: () -> Int64 = get_pub_Date

    private func get_pri_Date1(): Int64 {
        return Date1
    }
    public let pub_modifier1: () -> Int64 = get_pri_Date1
    private let pri_modifier1: () -> Int64 = get_pri_Date1
}

main(): Int64 {
    var newRec1: Date = Date()
    if (newRec1.pub_modifier() != 5 || newRec1.pub_modifier1() != 6) {
        return 1
    }
    return 0
}

/* SCAN
error: 'get_pub_Date' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init0.cj:16:44:
   | 
16 |     public let pub_modifier: () -> Int64 = get_pub_Date
   |                                            ^ 
   | 

error: 'get_pub_Date' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init0.cj:17:45:
   | 
17 |     private let pri_modifier: () -> Int64 = get_pub_Date
   |                                             ^ 
   | 

error: 'get_pri_Date1' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init0.cj:22:45:
   | 
22 |     public let pub_modifier1: () -> Int64 = get_pri_Date1
   |                                             ^ 
   | 

error: 'get_pri_Date1' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init0.cj:23:46:
   | 
23 |     private let pri_modifier1: () -> Int64 = get_pri_Date1
   |                                              ^ 
   | 

4 errors generated, 4 errors printed.
*/
