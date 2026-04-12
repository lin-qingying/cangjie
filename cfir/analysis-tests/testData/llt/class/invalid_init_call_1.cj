// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

struct GenRec {
    public let b: Int32 = 1
    public var c: Int64 = 2
    public init(str: String) {
        if (str == "hi") {
            this.b = 2
            this()
            this.c = 4
        } else {
            this.b = b + 2
            this.c = this.c + 1
        }
    }
}

main(): Int64 {
    var rec = GenRec("hi")
    print(rec.b.toString())
    print(rec.c.toString())
    if (rec.c == 4 && rec.b == 2) {
        return 0
    }
    return 1
}

/* SCAN
error: call to 'this' must be first expression in constructor of struct 'GenRec'
  ==> invalid_init_call_1.cj:15:13:
   | 
15 |             this()
   |             ^ 
   | 

error: missing argument for parameter list '(Struct-String)' in call
  ==> invalid_init_call_1.cj:15:17:
   | 
15 |             this()
   |                 ^^ expected 1 argument, found 0
   | 
note: found candidate
  ==> invalid_init_call_1.cj:12:12:
   | 
12 |     public init(str: String) {
   |            ^^^^ 
   | 

error: cannot assign to immutable value
  ==> invalid_init_call_1.cj:14:13:
   | 
14 |             this.b = 2
   |             ^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> invalid_init_call_1.cj:10:16:
   | 
10 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> invalid_init_call_1.cj:18:13:
   | 
18 |             this.b = b + 2
   |             ^^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> invalid_init_call_1.cj:10:16:
   | 
10 |     public let b: Int32 = 1
   |                ^ 
   | 

4 errors generated, 4 errors printed.
*/
