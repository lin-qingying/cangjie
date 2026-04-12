// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs007001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs007001a/pkgs007001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs007001a/pkgs007001a.cj -o pkgs007001a/pkgs007001a.%middle
// EXEC: %compiler %cmp_opt pkgs007001a/pkgs007001a.%middle %n.cj --import-path pkgs007001a -o %n.%suffix 2>&1 | compare %f
// Test condition: package with several generic functions along with normal items. But not all items are imported.

internal import pkgs007001a.NormalFunc
internal import pkgs007001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = NormalFunc(a)
    var c: Int32 = 4
    var d = Foo<Int32, Bool>(c, true)
    var e: Int32 = 5
    var f = Bar<Int32>(e)

    var h = one
    var i = two
    var j = three

    if (b == 3 && f == 5) {
        return 0
    } else {
        return 1
    }
}

/* SCAN
error: undeclared identifier 'Foo'
  ==> pkgs007001.cj:20:13:
   | 
20 |     var d = Foo<Int32, Bool>(c, true)
   |             ^ 
   | 

error: undeclared identifier 'one'
  ==> pkgs007001.cj:24:13:
   | 
24 |     var h = one
   |             ^ 
   | 

error: undeclared identifier 'two'
  ==> pkgs007001.cj:25:13:
   | 
25 |     var i = two
   |             ^ 
   | 

error: undeclared identifier 'three'
  ==> pkgs007001.cj:26:13:
   | 
26 |     var j = three
   |             ^ 
   | 

4 errors generated, 4 errors printed.
*/
