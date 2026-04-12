// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var aa = 0

main(): Int64 {
    if (C().b is Data && C.data.a == 8 && aa == 1) {
        return 0
    }
    return 1
}

class Data {
    public Data(var a!: Int64 = 6) {
        aa++
    }

    public func f2() {
        this
    }

    public func f1(i: Int64): This {
        f2()
    }
}

class C {
    public static var data = Data(a: 8)
    public var b = {
        => data.f2(3)
    }()
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> invoke_nothis_12.cj:35:19:
   | 
35 |         => data.f2(3)
   |                   ^^^ expected 0 arguments, found 1
   | 
note: found candidate
  ==> invoke_nothis_12.cj:23:17:
   | 
23 |     public func f2() {
   |                 ^^ 
   | 

1 error generated, 1 error printed.
*/
