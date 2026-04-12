// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

// 操作符++通过class定义操作符函数的方式重载
main(): Int64 {
    return run()
}

open class Base{
    public var base_a:Int32 = 2
}

class COO <: Base{
    public var a: Int32 = 1
    public var b: Int32 = 0
    public operator func ++():Int32{   
        this.a = this.a + 1
        return this.a
    }
}

func run(): Int64 {
    var coo: COO = COO()
    var res = coo++
    if (res != 2) { return 1}
    return 0
}

/* SCAN
error: cannot overload operator '++' 
  ==> incordec10.cj:21:26:
   | 
21 |     public operator func ++():Int32{   
   |                          ^^ cannot overload this operator
   | 
   # note: only operator '+', '-', ... can be overloaded

1 error generated, 1 error printed.
*/
