// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var i16 = Array<Int64>(10, repeat: 5)

public enum Test<T> {
    T1

    func a16(x: Array<Int64>) {
        return x
    }
}

main(): Int64 {
    if (T1.a16(i16) != i16) {
        return 1
    }
    return 0
}

/* SCAN
error: unable to infer generic argument of this function
  ==> infer_member_access_with_enum_ctor_fail.cj:20:9:
   | 
20 |     if (T1.a16(i16) != i16) {
   |         ^^^^^^ 
   | 
note: no information known for type variable 'Generics-T'
  ==> infer_member_access_with_enum_ctor_fail.cj:11:18:
   | 
11 | public enum Test<T> {
   |                  ^ 
   | 

error: generic type should be used with type argument
  ==> infer_member_access_with_enum_ctor_fail.cj:20:9:
   | 
20 |     if (T1.a16(i16) != i16) {
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
