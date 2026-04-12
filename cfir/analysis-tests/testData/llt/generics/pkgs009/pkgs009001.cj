// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs009001a pkgs009002.cj pkgs009003.cj
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs009001a/pkgs009001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs009001a/pkgs009001a.cj -o pkgs009001a/pkgs009001a.%middle
// EXEC: %compiler %cmp_opt pkgs009001a/pkgs009001a.%middle %n.cj pkgs009002.cj pkgs009003.cj --import-path pkgs009001a -o %output
// EXEC: %run %run_opt %output %run_args

internal import pkgs009001a.NormalFunc
internal import pkgs009001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = NormalFunc(a)
    var c: Int32 = 4
    var d = Foo<Int32, Bool>(c, true)
    var e: Int32 = 5
    var f = Bar<Int32>(e)

    var h = one
    var i = one
    var j = two
    var k = two
    var l = three
    var m = three

    if (b == 3 && f == 5) {
        return 0
    } else {
        return 1
    }
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
