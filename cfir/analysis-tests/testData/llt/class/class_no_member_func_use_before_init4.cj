// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public let b: Int32 = 0
    public let c = {
        => foo()
    }
    public let d = {
        => let e = {
            => let f = {
                => let g = {
                    => foo()
                }
            }
        }
    }
    public init(str: String) {
        this.b = foo()
    }

    init() {
        func ff() {
            func fff() {
                func ffff() {
                    foo()
                }
            }
        }
    }

    init(n: Int64) {
        var a = {
            => func ff() {
                var b = {
                    => func fff() {
                        this.foo()
                    }
                }
            }
        }
    }
    public func foo(): Int32 {
        0
    }
}

main(): Int64 {
    0
}

/* SCAN
error: 'foo' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init4.cj:12:12:
   | 
12 |         => foo()
   |            ^ 
   | 

error: 'foo' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init4.cj:18:24:
   | 
18 |                     => foo()
   |                        ^ 
   | 

error: cannot assign to immutable value
  ==> class_no_member_func_use_before_init4.cj:24:9:
   | 
24 |         this.b = foo()
   |         ^^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> class_no_member_func_use_before_init4.cj:10:16:
   | 
10 |     public let b: Int32 = 0
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
