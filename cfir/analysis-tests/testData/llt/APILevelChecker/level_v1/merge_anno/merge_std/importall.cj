// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj ./std
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// ERRCHECK: %compiler %cmp_opt --import-path ./std/ %f apilevel.%middle --cfg=APILevel_level=11 -Woff all

import std.argopt.*
import std.ast.*
import std.binary.*
import std.collection.*
import std.collection.concurrent.*
import std.console.*
import std.convert.*
import std.core.*
import std.crypto.cipher.*
import std.crypto.digest.*
import std.database.sql.*
import std.deriving.*
import std.env.*
import std.fs.*
import std.io.*
import std.math.*
import std.math.numeric.*
import std.net.*
import std.objectpool.*
import std.overflow.*
import std.posix.*
import std.process.*
import std.random.*
import std.ref.*
import std.reflect.*
import std.regex.*
import std.runtime.*
import std.sort.*
import std.sync.*
import std.time.*
import std.unicode.*
import std.unittest.*
import std.unittest.common.*
import std.unittest.diff.*
import std.unittest.mock.*
import std.unittest.mock.mockmacro.*
import std.unittest.prop_test.*
import std.unittest.testmacro.*

main() {
    ArgOpt()
    0
}

/* SCAN
error: cannot reference 'ArgOpt'(level: 12) which higher than level of the current scope(level: 11)
  ==> importall.cj:51:5:
   | 
51 |     ArgOpt()
   |     ^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
