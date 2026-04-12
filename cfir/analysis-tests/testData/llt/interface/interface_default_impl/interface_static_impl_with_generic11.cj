// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface Ser<T> {
    static func foo(): Unit
}

class AL<T> {}

extend<T> AL<T> <: Ser<AL<T>> where T <: Ser<T> {
    public static func foo(): Unit {}
}

abstract class DM <: Ser<DM> {}

main() {
    var a = AL<DM>()
}

/* SCAN
error: abstract class 'DM' cannot contain unimplemented static function 'foo'
  ==> interface_static_impl_with_generic11.cj:19:16:
   | 
19 | abstract class DM <: Ser<DM> {}
   |                ^^ 
   | 

1 error generated, 1 error printed.
*/
