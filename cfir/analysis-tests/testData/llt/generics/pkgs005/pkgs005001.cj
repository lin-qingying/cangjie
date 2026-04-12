// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs005001a
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs005001a/pkgs005001a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs005001a/pkgs005001a.cj -o pkgs005001a/pkgs005001a.%middle
// EXEC: %frontendCompiler %typecheck %cmp_opt pkgs005001a/pkgs005001a.%middle %n.cj --import-path pkgs005001a
// EXEC: %compiler %cmp_opt pkgs005001a/pkgs005001a.%middle %n.cj --import-path pkgs005001a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// Test condition: package with a single generic function is imported by only importing packageName like "import pkgName"

internal import pkgs005001a.*

main(): Int64 {
    var a: Float64 = 1.0
    var resa = Foo<Float64>(a)
    var b: Int64 = 2
    var resb = Foo<Int64>(b)
    var c: Float64 = 3.0
    var resc = Foo<Float64>(c)
    var d: Int64 = 4
    var resd = Foo<Int64>(d)
    var e: Bool = true
    var rese = Foo<Bool>(e)
    var f: Int32 = 5
    var resf = Bar<Int32>(f)
    var g = Bar<Float16>;
    var resh = g(3.14)
    if (resa == 1.0 && resb == 2 && resc == 3.0 && resd == 4 && rese == true && resf == 5 && resh == 3.14) {
        return 0
    } else {
        return 1
    }
}
