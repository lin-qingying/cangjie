// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %enableO2 %cmp_opt %n.cj -o %output 2>&1 |compare %f

main(): Int64 {
    var b = Boo()
    b.m = 1
    return b.m
}

class Boo {
    public mut prop m: Int64 {
        get() {
            return m
        }
        set(input) {
            var a = 0;
            m /= a
        }
    }
}

/* SCAN
warning: unused variable:'input'
  ==> binary_error_report_05.cj:20:13:
   | 
20 |         set(input) {
   |             ^^^^^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: divide by zero
  ==> binary_error_report_05.cj:22:13:
   | 
22 |             m /= a
   |             ^^^^^^ attempt to divide by zero
   | 

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
