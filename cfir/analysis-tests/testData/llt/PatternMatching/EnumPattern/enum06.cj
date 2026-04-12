// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum TimeUnit {
      Year(Int32)
    | Month(Int32, Int32)
    | Day(Int32, Int32, Int32)
    | Hour(Int32, Int32, Int32, Int32)
}
main():Int64 {
    var time = Month(1, 2)
    var d = match (time) {
        case Year(a) => a
        case Month(a, b) => a+b
        case Year(a)|Day(a, b, c) => { 
            a+b+c
        }
        case _ => -1
    }
    if (d == 6) {
        return 0
    }
    return 1
}

/* SCAN
error: expected '=>' in lambda expression, found 'a'
  ==> enum06.cj:21:13:
   | 
21 |             a+b+c
   |             ^ expected '=>' here
   | 

1 error generated, 1 error printed.
*/
