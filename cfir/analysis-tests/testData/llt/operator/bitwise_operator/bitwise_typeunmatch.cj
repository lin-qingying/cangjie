// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a: Int8 = 1
    var b: Int16 = 1
    var c: Int32 = 1
    var d: Int64 = 1
    var e: Int64 = 1
    var r1 = a & b
    var r2 = a ^ c
    var r3 = a | e
    var r4 = a & b ^ c << d | e
    var r5 = a | e << c ^ d | b
    return 0
}

/* SCAN
error: invalid binary operator '&' on type 'Int8' and 'Int16'
  ==> bitwise_typeunmatch.cj:15:16:
   | 
15 |     var r1 = a & b
   |                ^ 
   | 
   # note: you may want to implement 'operator func &(right: Int16)' for type 'Int8'

error: invalid binary operator '^' on type 'Int8' and 'Int32'
  ==> bitwise_typeunmatch.cj:16:16:
   | 
16 |     var r2 = a ^ c
   |                ^ 
   | 
   # note: you may want to implement 'operator func ^(right: Int32)' for type 'Int8'

error: invalid binary operator '|' on type 'Int8' and 'Int64'
  ==> bitwise_typeunmatch.cj:17:16:
   | 
17 |     var r3 = a | e
   |                ^ 
   | 
   # note: you may want to implement 'operator func |(right: Int64)' for type 'Int8'

error: invalid binary operator '&' on type 'Int8' and 'Int16'
  ==> bitwise_typeunmatch.cj:18:16:
   | 
18 |     var r4 = a & b ^ c << d | e
   |                ^ 
   | 
   # note: you may want to implement 'operator func &(right: Int16)' for type 'Int8'

error: invalid binary operator '|' on type 'Int8' and 'Int64'
  ==> bitwise_typeunmatch.cj:19:16:
   | 
19 |     var r5 = a | e << c ^ d | b
   |                ^ 
   | 
   # note: you may want to implement 'operator func |(right: Int64)' for type 'Int8'

5 errors generated, 5 errors printed.
*/
