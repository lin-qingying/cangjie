// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func getTime<V>(x: V, z: V): V {
    return x
}

func getTime<V>(x: V): V {
    return x
}

enum Time<T> {
    Day(T)
}

func run(): Int64 {
    var time1 = Time.Day(getTime<Int32>)
    var time = Time.Day(getTime)
    var res: Int32 = match (time) {
        case Day(x) => x(3)
    }
    if (res == 3) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: ambiguous arguments type in call expression
  ==> enum11_3.cj:22:17:
   | 
22 |     var time1 = Time.Day(getTime<Int32>)
   |                 ^ 
   | 

error: generic type should be used with type argument
  ==> enum11_3.cj:23:25:
   | 
23 |     var time = Time.Day(getTime)
   |                         ^ 
   | 

2 errors generated, 2 errors printed.
*/
