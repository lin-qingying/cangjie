// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

var a = 0

open class C1 {
    public open func f(): Bool {
        a = 3
        return true
    }
}

class C2 <: C1 {
    public override func f(): Bool {
        super.f()
    }
}

main(): Int64 {
    if (C2().f()) {
        return (a - 3)
    } else {
        return 2
    }
        % n
}

/* SCAN
warning: possibly confusing line terminator
  ==> binary_nothing.cj:29:6:
   | 
29 |       }
   |       ~^ 
   |  _____|
30 | |         % n
   | |_________~ possibly confusing line terminator between '}' and '%'
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff parser`

error: undeclared identifier 'n'
  ==> binary_nothing.cj:30:11:
   | 
30 |         % n
   |           ^ 
   | 

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
