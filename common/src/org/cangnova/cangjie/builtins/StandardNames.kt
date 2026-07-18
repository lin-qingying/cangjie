/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.builtins

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.newHashMapWithExpectedSize
import org.cangnova.cangjie.utils.newHashSetWithExpectedSize

/**
 * 编译器内置的仓颉标准名称索引。
 *
 * 该对象集中维护标准库包名、基础类型名、常用约定函数名以及它们派生出的 [FqName] 和 [ClassId]，
 * 解析、类型系统、诊断和序列化层都应从这里读取同一套稳定名称。
 */
@Suppress("Reformat")
object StandardNames {
    /**
     * 默认值参数使用的约定名称 `value`。
     */
    @JvmField
    val DEFAULT_VALUE_PARAMETER = Name.identifier("value")

    /**
     * 通用 `name` 属性或参数使用的约定名称。
     */
    @JvmField
    val NAME = Name.identifier("name")

    /**
     * `rangeOf` 标准构造函数名称。
     */
    val rangeOfName = Name.identifier("rangeOf")
    /**
     * `arrayOf` 标准构造函数名称。
     */
    val arrayOfName = Name.identifier("arrayOf")
    /**
     * `returnOf` 标准控制流辅助函数名称。
     */
    val returnOfName = Name.identifier("returnOf")
    /**
     * `spawn` 并发启动函数名称。
     */
    val spawnName = Name.identifier("spawn")
    /**
     * `unsafe` 标准命名空间或成员名称。
     */
    val unsafeName = Name.identifier("unsafe")

    /**
     * 根据函数参数个数生成 `FunctionN` 形式的标准函数类型名称。
     */
    @JvmStatic
    fun getFunctionName(parameterCount: Int): String {
        return "Function$parameterCount"
    }

    /**
     * 标准库根包 `std` 的名称片段。
     */
    val STD_PACKAGE_NAME = Name.identifier("std")
    /**
     * 标准核心包 `core` 的名称片段。
     */
    val CORE_PACKAGE_NAME = Name.identifier("core")
    /**
     * 标准 AST 包 `ast` 的名称片段。
     */
    val AST_PACKAGE_NAME = Name.identifier("ast")

    /**
     * 标准同步包 `sync` 的名称片段。
     */
    val SYNC_PACKAGE_NAME = Name.identifier("sync")
    /**
     * 标准命令行参数包 `argopt` 的名称片段。
     */
    val ARGOPT_PACKAGE_NAME = Name.identifier("argopt")
    /**
     * 标准二进制处理包 `binary` 的名称片段。
     */
    val BINARY_PACKAGE_NAME = Name.identifier("binary")
    /**
     * 标准集合包 `collection` 的名称片段。
     */
    val COLLECTION_PACKAGE_NAME = Name.identifier("collection")
    /**
     * 标准并发集合包 `concurrent` 的名称片段。
     */
    val CONCURRENT_PACKAGE_NAME = Name.identifier("concurrent")
    /**
     * 标准控制台包 `console` 的名称片段。
     */
    val CONSOLE_PACKAGE_NAME = Name.identifier("console")
    /**
     * 标准转换包 `convert` 的名称片段。
     */
    val CONVERT_PACKAGE_NAME = Name.identifier("convert")
    /**
     * 标准数据库包 `database` 的名称片段。
     */
    val DATABASE_PACKAGE_NAME = Name.identifier("database")
    /**
     * 标准 SQL 包 `sql` 的名称片段。
     */
    val SQL_PACKAGE_NAME = Name.identifier("sql")
    /**
     * 标准派生包 `deriving` 的名称片段。
     */
    val DERIVING_PACKAGE_NAME = Name.identifier("deriving")
    /**
     * 标准 API 子包 `api` 的名称片段。
     */
    val API_PACKAGE_NAME = Name.identifier("api")
    /**
     * 标准内置派生子包 `builtins` 的名称片段。
     */
    val BUILTINS_PACKAGE_NAME = Name.identifier("builtins")
    /**
     * 标准实现细节子包 `impl` 的名称片段。
     */
    val IMPL_PACKAGE_NAME = Name.identifier("impl")
    /**
     * 标准解析子包 `resolve` 的名称片段。
     */
    val RESOLVE_PACKAGE_NAME = Name.identifier("resolve")
    /**
     * 标准环境包 `env` 的名称片段。
     */
    val ENV_PACKAGE_NAME = Name.identifier("env")
    /**
     * 标准文件系统包 `fs` 的名称片段。
     */
    val FS_PACKAGE_NAME = Name.identifier("fs")
    /**
     * 标准 IO 包 `io` 的名称片段。
     */
    val IO_PACKAGE_NAME = Name.identifier("io")
    /**
     * 标准数学包 `math` 的名称片段。
     */
    val MATH_PACKAGE_NAME = Name.identifier("math")
    /**
     * 标准数值包 `numeric` 的名称片段。
     */
    val NUMERIC_PACKAGE_NAME = Name.identifier("numeric")
    /**
     * 标准对象池包 `objectpool` 的名称片段。
     */
    val OBJECTPOOL_PACKAGE_NAME = Name.identifier("objectpool")
    /**
     * 标准溢出处理包 `overflow` 的名称片段。
     */
    val OVERFLOW_PACKAGE_NAME = Name.identifier("overflow")
    /**
     * 标准 POSIX 包 `posix` 的名称片段。
     */
    val POSIX_PACKAGE_NAME = Name.identifier("posix")
    /**
     * 标准进程包 `process` 的名称片段。
     */
    val PROCESS_PACKAGE_NAME = Name.identifier("process")
    /**
     * 标准随机数包 `random` 的名称片段。
     */
    val RANDOM_PACKAGE_NAME = Name.identifier("random")
    /**
     * 标准引用包 `ref` 的名称片段。
     */
    val REF_PACKAGE_NAME = Name.identifier("ref")
    /**
     * 标准反射包 `reflect` 的名称片段。
     */
    val REFLECT_PACKAGE_NAME = Name.identifier("reflect")
    /**
     * 标准正则表达式包 `regex` 的名称片段。
     */
    val REGEX_PACKAGE_NAME = Name.identifier("regex")
    /**
     * 标准运行时包 `runtime` 的名称片段。
     */
    val RUNTIME_PACKAGE_NAME = Name.identifier("runtime")
    /**
     * 标准排序包 `sort` 的名称片段。
     */
    val SORT_PACKAGE_NAME = Name.identifier("sort")
    /**
     * 标准时间包 `time` 的名称片段。
     */
    val TIME_PACKAGE_NAME = Name.identifier("time")
    /**
     * 标准 Unicode 包 `unicode` 的名称片段。
     */
    val UNICODE_PACKAGE_NAME = Name.identifier("unicode")
    /**
     * 标准单元测试包 `unittest` 的名称片段。
     */
    val UNITTEST_PACKAGE_NAME = Name.identifier("unittest")
    /**
     * 标准公共测试子包 `common` 的名称片段。
     */
    val COMMON_PACKAGE_NAME = Name.identifier("common")
    /**
     * 标准测试差异子包 `diff` 的名称片段。
     */
    val DIFF_PACKAGE_NAME = Name.identifier("diff")
    /**
     * 标准 mock 测试子包 `mock` 的名称片段。
     */
    val MOCK_PACKAGE_NAME = Name.identifier("mock")
    /**
     * 标准内部实现子包 `internal` 的名称片段。
     */
    val INTERNAL_PACKAGE_NAME = Name.identifier("internal")
    /**
     * 标准 mock 宏子包 `mockmacro` 的名称片段。
     */
    val MOCKMACRO_PACKAGE_NAME = Name.identifier("mockmacro")
    /**
     * 标准性质测试子包 `prop_test` 的名称片段。
     */
    val PROP_TEST_PACKAGE_NAME = Name.identifier("prop_test")
    /**
     * 标准测试宏子包 `testmacro` 的名称片段。
     */
    val TESTMACRO_PACKAGE_NAME = Name.identifier("testmacro")
    /**
     * 标准密码算法子包 `cipher` 的名称片段。
     */
    val CIPHER_PACKAGE_NAME = Name.identifier("cipher")
    /**
     * 标准摘要算法子包 `digest` 的名称片段。
     */
    val DIGEST_PACKAGE_NAME = Name.identifier("digest")

    /**
     * 顶层压缩库包 `compress` 的名称片段。
     */
    val COMPRESS_PACKAGE_NAME = Name.identifier("compress")

    /**
     * 顶层密码库包 `crypto` 的名称片段。
     */
    val CRYPTO_PACKAGE_NAME = Name.identifier("crypto")

    /**
     * 顶层编码库包 `encoding` 的名称片段。
     */
    val ENCODING_PACKAGE_NAME = Name.identifier("encoding")

    /**
     * 顶层模糊测试库包 `fuzz` 的名称片段。
     */
    val FUZZ_PACKAGE_NAME = Name.identifier("fuzz")

    /**
     * 顶层网络库包 `net` 的名称片段。
     */
    val NET_PACKAGE_NAME = Name.identifier("net")

    /**
     * 顶层序列化库包 `serialization` 的名称片段。
     */
    val SERIALIZATION_PACKAGE_NAME = Name.identifier("serialization")

    /**
     * 程序入口函数 `main` 的标准名称。
     */
    @JvmField
    val MAIN = Name.identifier("main")

    /**
     * 标准库根包 `std` 的完整包名。
     */
    @JvmField
    val STD_PACKAGE_FQ_NAME = FqName.topLevel(STD_PACKAGE_NAME)

    /**
     * 顶层压缩库包 `compress` 的完整包名。
     */
    @JvmField
    val COMPRESS_PACKAGE_FQ_NAME = FqName.topLevel(COMPRESS_PACKAGE_NAME)

    /**
     * 顶层密码库包 `crypto` 的完整包名。
     */
    @JvmField
    val CRYPTO_PACKAGE_FQ_NAME = FqName.topLevel(CRYPTO_PACKAGE_NAME)

    /**
     * 顶层编码库包 `encoding` 的完整包名。
     */
    @JvmField
    val ENCODING_PACKAGE_FQ_NAME = FqName.topLevel(ENCODING_PACKAGE_NAME)

    /**
     * 顶层模糊测试库包 `fuzz` 的完整包名。
     */
    @JvmField
    val FUZZ_PACKAGE_FQ_NAME = FqName.topLevel(FUZZ_PACKAGE_NAME)

    /**
     * 顶层网络库包 `net` 的完整包名。
     */
    @JvmField
    val NET_PACKAGE_FQ_NAME = FqName.topLevel(NET_PACKAGE_NAME)

    /**
     * 顶层序列化库包 `serialization` 的完整包名。
     */
    @JvmField
    val SERIALIZATION_PACKAGE_FQ_NAME = FqName.topLevel(SERIALIZATION_PACKAGE_NAME)

    // std.* 子包 FqNames
    /**
     * 标准库 `std.argopt` 包的完整包名。
     */
    @JvmField
    val STD_ARGOPT_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(ARGOPT_PACKAGE_NAME)

    /**
     * 标准库 `std.ast` 包的完整包名。
     */
    @JvmField
    val STD_AST_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(AST_PACKAGE_NAME)

    /**
     * 标准库 `std.binary` 包的完整包名。
     */
    @JvmField
    val STD_BINARY_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(BINARY_PACKAGE_NAME)

    /**
     * 标准库 `std.collection` 包的完整包名。
     */
    @JvmField
    val STD_COLLECTION_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(COLLECTION_PACKAGE_NAME)

    /**
     * 标准库 `std.collection.concurrent` 包的完整包名。
     */
    @JvmField
    val STD_COLLECTION_CONCURRENT_PACKAGE_FQ_NAME = STD_COLLECTION_PACKAGE_FQ_NAME.child(CONCURRENT_PACKAGE_NAME)

    /**
     * 标准库 `std.console` 包的完整包名。
     */
    @JvmField
    val STD_CONSOLE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(CONSOLE_PACKAGE_NAME)

    /**
     * 标准库 `std.convert` 包的完整包名。
     */
    @JvmField
    val STD_CONVERT_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(CONVERT_PACKAGE_NAME)

    /**
     * 标准库 `std.core` 包的完整包名。
     */
    @JvmField
    val STD_CORE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(CORE_PACKAGE_NAME)

    /**
     * 标准库 `std.crypto` 包的完整包名。
     */
    @JvmField
    val STD_CRYPTO_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(CRYPTO_PACKAGE_NAME)

    /**
     * 标准库 `std.crypto.cipher` 包的完整包名。
     */
    @JvmField
    val STD_CRYPTO_CIPHER_PACKAGE_FQ_NAME = STD_CRYPTO_PACKAGE_FQ_NAME.child(CIPHER_PACKAGE_NAME)

    /**
     * 标准库 `std.crypto.digest` 包的完整包名。
     */
    @JvmField
    val STD_CRYPTO_DIGEST_PACKAGE_FQ_NAME = STD_CRYPTO_PACKAGE_FQ_NAME.child(DIGEST_PACKAGE_NAME)

    /**
     * 标准库 `std.database` 包的完整包名。
     */
    @JvmField
    val STD_DATABASE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(DATABASE_PACKAGE_NAME)

    /**
     * 标准库 `std.database.sql` 包的完整包名。
     */
    @JvmField
    val STD_DATABASE_SQL_PACKAGE_FQ_NAME = STD_DATABASE_PACKAGE_FQ_NAME.child(SQL_PACKAGE_NAME)

    /**
     * 标准库 `std.deriving` 包的完整包名。
     */
    @JvmField
    val STD_DERIVING_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(DERIVING_PACKAGE_NAME)

    /**
     * 标准库 `std.deriving.api` 包的完整包名。
     */
    @JvmField
    val STD_DERIVING_API_PACKAGE_FQ_NAME = STD_DERIVING_PACKAGE_FQ_NAME.child(API_PACKAGE_NAME)

    /**
     * 标准库 `std.deriving.builtins` 包的完整包名。
     */
    @JvmField
    val STD_DERIVING_BUILTINS_PACKAGE_FQ_NAME = STD_DERIVING_PACKAGE_FQ_NAME.child(BUILTINS_PACKAGE_NAME)

    /**
     * 标准库 `std.deriving.impl` 包的完整包名。
     */
    @JvmField
    val STD_DERIVING_IMPL_PACKAGE_FQ_NAME = STD_DERIVING_PACKAGE_FQ_NAME.child(IMPL_PACKAGE_NAME)

    /**
     * 标准库 `std.deriving.resolve` 包的完整包名。
     */
    @JvmField
    val STD_DERIVING_RESOLVE_PACKAGE_FQ_NAME = STD_DERIVING_PACKAGE_FQ_NAME.child(RESOLVE_PACKAGE_NAME)

    /**
     * 标准库 `std.env` 包的完整包名。
     */
    @JvmField
    val STD_ENV_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(ENV_PACKAGE_NAME)

    /**
     * 标准库 `std.fs` 包的完整包名。
     */
    @JvmField
    val STD_FS_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(FS_PACKAGE_NAME)

    /**
     * 标准库 `std.io` 包的完整包名。
     */
    @JvmField
    val STD_IO_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(IO_PACKAGE_NAME)

    /**
     * 标准库 `std.math` 包的完整包名。
     */
    @JvmField
    val STD_MATH_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(MATH_PACKAGE_NAME)

    /**
     * 标准库 `std.math.numeric` 包的完整包名。
     */
    @JvmField
    val STD_MATH_NUMERIC_PACKAGE_FQ_NAME = STD_MATH_PACKAGE_FQ_NAME.child(NUMERIC_PACKAGE_NAME)

    /**
     * 标准库 `std.net` 包的完整包名。
     */
    @JvmField
    val STD_NET_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(NET_PACKAGE_NAME)

    /**
     * 标准库 `std.objectpool` 包的完整包名。
     */
    @JvmField
    val STD_OBJECTPOOL_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(OBJECTPOOL_PACKAGE_NAME)

    /**
     * 标准库 `std.overflow` 包的完整包名。
     */
    @JvmField
    val STD_OVERFLOW_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(OVERFLOW_PACKAGE_NAME)

    /**
     * 标准库 `std.posix` 包的完整包名。
     */
    @JvmField
    val STD_POSIX_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(POSIX_PACKAGE_NAME)

    /**
     * 标准库 `std.process` 包的完整包名。
     */
    @JvmField
    val STD_PROCESS_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(PROCESS_PACKAGE_NAME)

    /**
     * 标准库 `std.random` 包的完整包名。
     */
    @JvmField
    val STD_RANDOM_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(RANDOM_PACKAGE_NAME)

    /**
     * 标准库 `std.ref` 包的完整包名。
     */
    @JvmField
    val STD_REF_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(REF_PACKAGE_NAME)

    /**
     * 标准库 `std.reflect` 包的完整包名。
     */
    @JvmField
    val STD_REFLECT_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(REFLECT_PACKAGE_NAME)

    /**
     * 标准库 `std.regex` 包的完整包名。
     */
    @JvmField
    val STD_REGEX_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(REGEX_PACKAGE_NAME)

    /**
     * 标准库 `std.runtime` 包的完整包名。
     */
    @JvmField
    val STD_RUNTIME_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(RUNTIME_PACKAGE_NAME)

    /**
     * 标准库 `std.sort` 包的完整包名。
     */
    @JvmField
    val STD_SORT_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(SORT_PACKAGE_NAME)

    /**
     * 标准库 `std.sync` 包的完整包名。
     */
    @JvmField
    val STD_SYNC_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(SYNC_PACKAGE_NAME)

    /**
     * 标准库 `std.time` 包的完整包名。
     */
    @JvmField
    val STD_TIME_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(TIME_PACKAGE_NAME)

    /**
     * 标准库 `std.unicode` 包的完整包名。
     */
    @JvmField
    val STD_UNICODE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(UNICODE_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(UNITTEST_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.common` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_COMMON_PACKAGE_FQ_NAME = STD_UNITTEST_PACKAGE_FQ_NAME.child(COMMON_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.diff` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_DIFF_PACKAGE_FQ_NAME = STD_UNITTEST_PACKAGE_FQ_NAME.child(DIFF_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.mock` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_MOCK_PACKAGE_FQ_NAME = STD_UNITTEST_PACKAGE_FQ_NAME.child(MOCK_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.mock.internal` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_MOCK_INTERNAL_PACKAGE_FQ_NAME = STD_UNITTEST_MOCK_PACKAGE_FQ_NAME.child(INTERNAL_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.mock.mockmacro` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_MOCK_MOCKMACRO_PACKAGE_FQ_NAME = STD_UNITTEST_MOCK_PACKAGE_FQ_NAME.child(MOCKMACRO_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.prop_test` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_PROP_TEST_PACKAGE_FQ_NAME = STD_UNITTEST_PACKAGE_FQ_NAME.child(PROP_TEST_PACKAGE_NAME)

    /**
     * 标准库 `std.unittest.testmacro` 包的完整包名。
     */
    @JvmField
    val STD_UNITTEST_TESTMACRO_PACKAGE_FQ_NAME = STD_UNITTEST_PACKAGE_FQ_NAME.child(TESTMACRO_PACKAGE_NAME)

    /**
     * 底类型 `Nothing` 的标准短名称。
     */
    @JvmField
    val NOTHING = Name.identifier("Nothing")

    /**
     * 字符标量类型 `Rune` 的标准短名称。
     */
    @JvmField
    val RUNE = Name.identifier("Rune")

    /**
     * 空返回类型 `Unit` 的标准短名称。
     */
    @JvmField
    val UNIT = Name.identifier("Unit")

    /**
     * 8 位有符号整数类型 `Int8` 的标准短名称。
     */
    @JvmField
    val INT8 = Name.identifier("Int8")

    /**
     * 16 位有符号整数类型 `Int16` 的标准短名称。
     */
    @JvmField
    val INT16 = Name.identifier("Int16")

    /**
     * 32 位有符号整数类型 `Int32` 的标准短名称。
     */
    @JvmField
    val INT32 = Name.identifier("Int32")

    /**
     * 64 位有符号整数类型 `Int64` 的标准短名称。
     */
    @JvmField
    val INT64 = Name.identifier("Int64")

    /**
     * 平台原生有符号整数类型 `IntNative` 的标准短名称。
     */
    @JvmField
    val INT_NATIVE = Name.identifier("IntNative")

    /**
     * 8 位无符号整数类型 `UInt8` 的标准短名称。
     */
    @JvmField
    val UINT8 = Name.identifier("UInt8")

    /**
     * 16 位无符号整数类型 `UInt16` 的标准短名称。
     */
    @JvmField
    val UINT16 = Name.identifier("UInt16")

    /**
     * 32 位无符号整数类型 `UInt32` 的标准短名称。
     */
    @JvmField
    val UINT32 = Name.identifier("UInt32")

    /**
     * 64 位无符号整数类型 `UInt64` 的标准短名称。
     */
    @JvmField
    val UINT64 = Name.identifier("UInt64")

    /**
     * 平台原生无符号整数类型 `UIntNative` 的标准短名称。
     */
    @JvmField
    val UINT_NATIVE = Name.identifier("UIntNative")

    /**
     * 16 位浮点类型 `Float16` 的标准短名称。
     */
    @JvmField
    val FLOAT16 = Name.identifier("Float16")

    /**
     * 32 位浮点类型 `Float32` 的标准短名称。
     */
    @JvmField
    val FLOAT32 = Name.identifier("Float32")

    /**
     * 64 位浮点类型 `Float64` 的标准短名称。
     */
    @JvmField
    val FLOAT64 = Name.identifier("Float64")

    /**
     * 布尔类型 `Bool` 的标准短名称。
     */
    @JvmField
    val BOOL = Name.identifier("Bool")

    /**
     * 标准异常基类 `Exception` 的短名称。
     */
    @JvmField
    val EXCEPTION = Name.identifier("Exception")

    /**
     * 标准错误基类 `Error` 的短名称。
     */
    @JvmField
    val ERROR = Name.identifier("Error")

    /**
     * 标准资源接口 `Resource` 的短名称。
     */
    @JvmField
    val RESOURCE = Name.identifier("Resource")

    /**
     * effect 命令类型 `Command` 的短名称。
     */
    @JvmField
    val COMMAND = Name.identifier("Command")

    /**
     * effect 恢复类型 `Resumption` 的短名称。
     */
    @JvmField
    val RESUMPTION = Name.identifier("Resumption")

    /**
     * AST token 集合类型 `Tokens` 的短名称。
     */
    @JvmField
    val TOKENS = Name.identifier("Tokens")

    /**
     * 可重入互斥锁类型 `ReentrantMutex` 的短名称。
     */
    @JvmField
    val REENTRANT_MUTEX = Name.identifier("ReentrantMutex")

    /**
     * 可迭代协议类型 `Iterable` 的短名称。
     */
    @JvmField
    val ITERABLE = Name.identifier("Iterable")

    /**
     * 集合协议类型 `Collection` 的短名称。
     */
    @JvmField
    val COLLECTION = Name.identifier("Collection")

    /**
     * 标准对象类型 `Object` 的短名称。
     */
    @JvmField
    val OBJECT = Name.identifier("Object")

    /**
     * 顶层根类型 `Any` 的短名称。
     */
    @JvmField
    val ANY = Name.identifier("Any")

    /**
     * 标准数组类型 `Array` 的短名称。
     */
    @JvmField
    val ARRAY = Name.identifier("Array")

    /**
     * 标准区间类型 `Range` 的短名称。
     */
    @JvmField
    val RANGE = Name.identifier("Range")

    /**
     * 可计数协议类型 `Countable` 的短名称。
     */
    @JvmField
    val COUNTABLE = Name.identifier("Countable")

    /**
     * 可相等比较协议类型 `Equatable` 的短名称。
     */
    @JvmField
    val EQUATABLE = Name.identifier("Equatable")

    /**
     * 可排序比较协议类型 `Comparable` 的短名称。
     */
    @JvmField
    val COMPARABLE = Name.identifier("Comparable")

    /**
     * 异步结果类型 `Future` 的短名称。
     */
    @JvmField
    val FUTURE = Name.identifier("Future")

    /**
     * 调度上下文接口 `ThreadContext` 的短名称。
     */
    @JvmField
    val THREAD_CONTEXT = Name.identifier("ThreadContext")

    /**
     * 标准字符串类型 `String` 的短名称。
     */
    @JvmField
    val STRING = Name.identifier("String")

    /**
     * 标准可选类型 `Option` 的短名称。
     */
    @JvmField
    val OPTION = Name.identifier("Option")

    /**
     * C 互操作指针类型 `CPointer` 的短名称。
     */
    @JvmField
    val CPOINTER = Name.identifier("CPointer")

    /**
     * C 字符串类型 `CString` 的短名称。
     */
    @JvmField
    val CSTRING = Name.identifier("CString")

    /**
     * C 互操作类型标记 `CType` 的短名称。
     */
    @JvmField
    val CTYPE = Name.identifier("CType")

    /**
     * 字符串化协议 `ToString` 的短名称。
     */
    @JvmField
    val TOSTRING = Name.identifier("ToString")

    /**
     * 基础类型所在的空包 FqName。
     */
    @JvmField
    val BASIC_PACKAGE_FQ_NAME = FqName("")

    /**
     * 标准名称中常用完整限定名和反向索引的命名空间。
     */
    object FqNames {
        /**
         * 标准 `Deprecated` 注解的完整限定名。
         */
        @JvmField
        val deprecated: FqName = fqName("Deprecated")

        // Internal annotation used by the plugin to store parameter names in function types
        // CangJie syntax: (name: String, price: Int64) -> Unit
        // This is not a user-visible annotation, but an internal implementation detail
        /**
         * 函数类型参数名内部注解 `ParameterName` 的完整限定名。
         */
        @JvmField
        val parameterName: FqName = fqName("ParameterName")

        /**
         * 从基础类型完整限定名到 [PrimitiveType] 枚举的反向索引。
         */
        @JvmField
        val fqNameToPrimitiveType: Map<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.entries.size).apply {
                for (primitiveType in PrimitiveType.entries) {
                    this[fqNameUnsafe(primitiveType.typeName.asString())] = primitiveType
                }
            }


        /**
         * 所有 primitive 数组类型的短名称集合。
         */
        @JvmField
        val primitiveArrayTypeShortNames: Set<Name> =
            newHashSetWithExpectedSize<Name>(PrimitiveType.entries.size).apply {
                PrimitiveType.entries.mapTo(this) { it.arrayTypeName }
            }

        /**
         * 所有 primitive 类型的短名称集合。
         */
        @JvmField
        val primitiveTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.entries.size).apply {
            PrimitiveType.entries.mapTo(this) { it.typeName }
        }

        /**
         * 从 primitive 数组类型完整限定名到元素 [PrimitiveType] 的反向索引。
         */
        @JvmField
        val arrayClassFqNameToPrimitiveType: MutableMap<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.entries.size).apply {
                for (primitiveType in PrimitiveType.entries) {
                    this[fqNameUnsafe(primitiveType.arrayTypeName.asString())] = primitiveType
                }
            }

        /**
         * 标准核心包 `std.core` 的完整限定名。
         */
        @JvmField
        val core: FqName = FqName.topLevel(STD_PACKAGE_NAME).child(CORE_PACKAGE_NAME)

        /**
         * 标准 AST 包 `std.ast` 的完整限定名。
         */
        @JvmField
        val ast: FqName = FqName.topLevel(STD_PACKAGE_NAME).child(AST_PACKAGE_NAME)

        /**
         * 标准同步包 `std.sync` 的完整限定名。
         */
        @JvmField
        val sync: FqName = FqName.topLevel(STD_PACKAGE_NAME).child(SYNC_PACKAGE_NAME)

        /** 标准库类型 `std.core.Any` 的 FqName。 */
        @JvmField
        val anyFqName: FqName = core.child(ANY)

        /**
         * 标准库类型 `std.core.Any` 的 unsafe FqName 形式。
         */
        @JvmField
        val anyUFqName: FqNameUnsafe = anyFqName.toUnsafe()

        /**
         * 标准异常基类 `std.core.Exception` 的 FqName。
         */
        @JvmField
        val exceptionFqName: FqName = core.child(EXCEPTION)

        /**
         * 标准错误基类 `std.core.Error` 的 FqName。
         */
        @JvmField
        val errorFqName: FqName = core.child(ERROR)

        /**
         * 标准资源接口 `std.core.Resource` 的 FqName。
         */
        @JvmField
        val resourceFqName: FqName = core.child(RESOURCE)

        /**
         * 扩展 effect 包 `stdx.effect` 的 FqName。
         */
        @JvmField
        val effect: FqName = FqName.fromSegments(listOf("stdx", "effect"))

        /**
         * effect 命令类型 `stdx.effect.Command` 的 FqName。
         */
        @JvmField
        val commandFqName: FqName = effect.child(COMMAND)

        /**
         * effect 恢复类型 `stdx.effect.Resumption` 的 FqName。
         */
        @JvmField
        val resumptionFqName: FqName = effect.child(RESUMPTION)

        /** 标准库类型 `std.core.Object` 的 FqName。 */
        @JvmField
        val objectFqName: FqName = core.child(OBJECT)

        /**
         * 标准库类型 `std.core.Object` 的 unsafe FqName 形式。
         */
        @JvmField
        val objectUFqName: FqNameUnsafe = objectFqName.toUnsafe()

        /** 标准库类型 `std.core.Option` 的 FqName。 */
        @JvmField
        val optionFqName: FqName = core.child(OPTION)

        /**
         * 标准库类型 `std.core.Option` 的 unsafe FqName 形式。
         */
        @JvmField
        val optionUFqName: FqNameUnsafe = optionFqName.toUnsafe()

        /**
         * 标准库协议 `std.core.Countable` 的 FqName。
         */
        @JvmField
        val countableFqName: FqName = core.child(COUNTABLE)

        /**
         * 标准库协议 `std.core.Equatable` 的 FqName。
         */
        @JvmField
        val equatableFqName: FqName = core.child(EQUATABLE)

        /**
         * 标准库协议 `std.core.Iterable` 的 FqName。
         */
        @JvmField
        val iterableFqName: FqName = core.child(ITERABLE)

        /**
         * 标准库同步类型 `std.sync.ReentrantMutex` 的 FqName。
         */
        @JvmField
        val reentrantMutexFqName: FqName = sync.child(REENTRANT_MUTEX)

        /**
         * 标准库 AST token 类型 `std.ast.Tokens` 的 FqName。
         */
        @JvmField
        val tokensFqName: FqName = ast.child(TOKENS)

        /**
         * 标准库协议 `std.core.Comparable` 的 FqName。
         */
        @JvmField
        val comparableFqName: FqName = core.child(COMPARABLE)

        /**
         * 标准库异步类型 `std.core.Future` 的 FqName。
         */
        @JvmField
        val futureFqName: FqName = core.child(FUTURE)

        /**
         * 标准库调度上下文接口 `std.core.ThreadContext` 的 FqName。
         */
        @JvmField
        val threadContextFqName: FqName = core.child(THREAD_CONTEXT)

        /** 标准库类型 `std.core.Range` 的 FqName。 */
        @JvmField
        val rangeFqName: FqName = core.child(RANGE)

        /** 标准库类型 `std.core.String` 的 FqName。 */
        @JvmField
        val stringFqName: FqName = core.child(STRING)

        /**
         * 标准库类型 `std.core.String` 的 unsafe FqName 形式。
         */
        @JvmField
        val stringUFqName: FqNameUnsafe = stringFqName.toUnsafe()

        /** 标准库类型 `std.core.Array` 的 FqName。 */
        @JvmField
        val arrayFqName: FqName = core.child(ARRAY)

        /**
         * 标准库类型 `std.core.Array` 的 unsafe FqName 形式。
         */
        @JvmField
        val arrayUFqName: FqNameUnsafe = arrayFqName.toUnsafe()

        /** 基本类型 `Nothing` 的 FqName，位于空包。 */
        @JvmField
        val nothingFqName: FqName = fqName(NOTHING)

        /**
         * 基本类型 `Nothing` 的 unsafe FqName 形式。
         */
        @JvmField
        val nothingUFqName: FqNameUnsafe = nothingFqName.toUnsafe()

        /**
         * 字符串化协议 `ToString` 的 FqName，位于空包。
         */
        @JvmField
        val toStringFqName: FqName = fqName(TOSTRING)

        /**
         * 字符串化协议 `ToString` 的 unsafe FqName 形式。
         */
        @JvmField
        val toStringUFqName: FqNameUnsafe = nothingFqName.toUnsafe()

        /** 基本类型 `Rune` 的 FqName，位于空包。 */
        @JvmField
        val runeFqName: FqName = fqName(RUNE)

        /**
         * 基本类型 `Rune` 的 unsafe FqName 形式。
         */
        @JvmField
        val runeUFqName: FqNameUnsafe = runeFqName.toUnsafe()

        /** 基本类型 `Unit` 的 FqName，位于空包。 */
        @JvmField
        val unitFqName = fqName(UNIT)

        /**
         * 基本类型 `Unit` 的 unsafe FqName 形式。
         */
        @JvmField
        val unitUFqName = unitFqName.toUnsafe()

        /***************************Int***************************/
        /** 基本类型 `Int8` 的 FqName，位于空包。 */
        @JvmField
        val int8FqName: FqName = fqName(INT8)

        /**
         * 基本类型 `Int8` 的 unsafe FqName 形式。
         */
        @JvmField
        val int8UFqName: FqNameUnsafe = int8FqName.toUnsafe()

        /** 基本类型 `Int16` 的 FqName，位于空包。 */
        @JvmField
        val int16FqName: FqName = fqName(INT16)

        /**
         * 基本类型 `Int16` 的 unsafe FqName 形式。
         */
        @JvmField
        val int16UFqName: FqNameUnsafe = int16FqName.toUnsafe()

        /** 基本类型 `Int32` 的 FqName，位于空包。 */
        @JvmField
        val int32FqName: FqName = fqName(INT32)

        /**
         * 基本类型 `Int32` 的 unsafe FqName 形式。
         */
        @JvmField
        val int32UFqName: FqNameUnsafe = int32FqName.toUnsafe()

        /** 基本类型 `Int64` 的 FqName，位于空包。 */
        @JvmField
        val int64FqName: FqName = fqName(INT64)

        /**
         * 基本类型 `Int64` 的 unsafe FqName 形式。
         */
        @JvmField
        val int64UFqName: FqNameUnsafe = int64FqName.toUnsafe()

        /** 基本类型 `IntNative` 的 FqName，位于空包。 */
        @JvmField
        val int_nativeFqName: FqName = fqName(INT_NATIVE)

        /**
         * 基本类型 `IntNative` 的 unsafe FqName 形式。
         */
        @JvmField
        val int_nativeUFqName: FqNameUnsafe = int_nativeFqName.toUnsafe()

        /***************************UInt***************************/
        /** 基本类型 `UInt8` 的 FqName，位于空包。 */
        @JvmField
        val uint8FqName: FqName = fqName(UINT8)

        /**
         * 基本类型 `UInt8` 的 unsafe FqName 形式。
         */
        @JvmField
        val uint8UFqName: FqNameUnsafe = uint8FqName.toUnsafe()

        /** 基本类型 `UInt16` 的 FqName，位于空包。 */
        @JvmField
        val uint16FqName: FqName = fqName(UINT16)

        /**
         * 基本类型 `UInt16` 的 unsafe FqName 形式。
         */
        @JvmField
        val uint16UFqName: FqNameUnsafe = uint16FqName.toUnsafe()

        /** 基本类型 `UInt32` 的 FqName，位于空包。 */
        @JvmField
        val uint32FqName: FqName = fqName(UINT32)

        /**
         * 基本类型 `UInt32` 的 unsafe FqName 形式。
         */
        @JvmField
        val uint32UFqName: FqNameUnsafe = uint32FqName.toUnsafe()

        /** 基本类型 `UInt64` 的 FqName，位于空包。 */
        @JvmField
        val uint64FqName: FqName = fqName(UINT64)

        /**
         * 基本类型 `UInt64` 的 unsafe FqName 形式。
         */
        @JvmField
        val uint64UFqName: FqNameUnsafe = uint64FqName.toUnsafe()

        /** 基本类型 `UIntNative` 的 FqName，位于空包。 */
        @JvmField
        val uint_nativeFqName: FqName = fqName(UINT_NATIVE)

        /**
         * 基本类型 `UIntNative` 的 unsafe FqName 形式。
         */
        @JvmField
        val uint_nativeUFqName: FqNameUnsafe = uint_nativeFqName.toUnsafe()

        /***************************Bool***************************/
        /** 基本类型 `Bool` 的 FqName，位于空包。 */
        @JvmField
        val boolFqName: FqName = fqName(BOOL)

        /**
         * 基本类型 `Bool` 的 unsafe FqName 形式。
         */
        @JvmField
        val boolUFqName: FqNameUnsafe = boolFqName.toUnsafe()

        /***************************Float***************************/

        /** 基本类型 `Float16` 的 FqName，位于空包。 */
        @JvmField
        val float16FqName: FqName = fqName(FLOAT16)

        /**
         * 基本类型 `Float16` 的 unsafe FqName 形式。
         */
        @JvmField
        val float16UFqName: FqNameUnsafe = float16FqName.toUnsafe()

        /** 基本类型 `Float32` 的 FqName，位于空包。 */
        @JvmField
        val float32FqName: FqName = fqName(FLOAT32)

        /**
         * 基本类型 `Float32` 的 unsafe FqName 形式。
         */
        @JvmField
        val float32UFqName: FqNameUnsafe = float32FqName.toUnsafe()

        /** 基本类型 `Float64` 的 FqName，位于空包。 */
        @JvmField
        val float64FqName: FqName = fqName(FLOAT64)

        /**
         * 基本类型 `Float64` 的 unsafe FqName 形式。
         */
        @JvmField
        val float64UFqName: FqNameUnsafe = float64FqName.toUnsafe()

        /***************************内置类型***************************/
        /**
         * C 互操作指针类型 `std.core.CPointer` 的 FqName。
         */
        @JvmField
        val cpointerFqName = core.child(CPOINTER)

        /**
         * C 互操作指针类型 `std.core.CPointer` 的 unsafe FqName 形式。
         */
        @JvmField
        val cpointerUFqName: FqNameUnsafe = cpointerFqName.toUnsafe()

        /**
         * C 字符串类型 `std.core.CString` 的 FqName。
         */
        @JvmField
        val cstringFqName: FqName = core.child(CSTRING)

        /**
         * C 字符串类型 `std.core.CString` 的 unsafe FqName 形式。
         */
        @JvmField
        val cstringUFqName: FqNameUnsafe = cstringFqName.toUnsafe()

        /**
         * C 类型标记 `std.core.CType` 的 FqName。
         */
        @JvmField
        val ctypeFqName: FqName = core.child(CTYPE)

        /**
         * C 类型标记 `std.core.CType` 的 unsafe FqName 形式。
         */
        @JvmField
        val ctypeUFqName: FqNameUnsafe = ctypeFqName.toUnsafe()

        /**
         * 无符号 8 位整数类型的顶层 ClassId。
         */
        @JvmField
        val uInt8ClassId: ClassId = ClassId.topLevel(uint8FqName)

        /**
         * 无符号 16 位整数类型的顶层 ClassId。
         */
        @JvmField
        val uInt16ClassId: ClassId = ClassId.topLevel(uint16FqName)

        /**
         * 无符号 32 位整数类型的顶层 ClassId。
         */
        @JvmField
        val uInt32ClassId: ClassId = ClassId.topLevel(uint32FqName)

        /**
         * 无符号 64 位整数类型的顶层 ClassId。
         */
        @JvmField
        val uInt64ClassId: ClassId = ClassId.topLevel(uint64FqName)



        /**
         * 标准注解基类 `Annotation` 的 FqName。
         */
        @JvmField
        val annotation: FqName = fqName("Annotation")



        /**
         * 将空包基础类型短名称转换为 unsafe FqName。
         */
        private fun fqNameUnsafe(simpleName: String): FqNameUnsafe {
            return fqName(simpleName).toUnsafe()
        }

        /**
         * 在基础类型空包下构造指定名称的 FqName。
         */
        private fun fqName(name: Name): FqName {
            return BASIC_PACKAGE_FQ_NAME.child(name)
        }

        /**
         * 在基础类型空包下构造指定字符串名称的 FqName。
         */
        private fun fqName(simpleName: String): FqName {
            return fqName(Name.identifier(simpleName))
        }

        /**
         * 根据基础类型短名称返回对应的标准 FqName。
         *
         * 该函数只接受本对象已登记的基础类型和核心互操作类型名称。
         */
        fun fromByName(name: Name): FqName =
            when (name) {
                NOTHING -> nothingFqName

                UINT8 -> uint8FqName
                UINT16 -> uint16FqName
                UINT32 -> uint32FqName
                UINT64 -> uint64FqName
                UINT_NATIVE -> uint_nativeFqName

                INT8 -> int8FqName
                INT16 -> int16FqName
                INT32 -> int32FqName
                INT64 -> int64FqName
                INT_NATIVE -> int_nativeFqName

                FLOAT16 -> float16FqName
                FLOAT32 -> float32FqName
                FLOAT64 -> float64FqName

                BOOL -> boolFqName

                RUNE -> runeFqName

                ARRAY -> arrayFqName
                UNIT -> unitFqName

                CPOINTER -> cpointerFqName
                CSTRING -> cstringFqName

                else -> throw IllegalArgumentException("Unknown name: $name")
            }
    }

    /**
     * 编译器直接视为基础类型的短名称集合。
     */
    @JvmField
    val BASIC_TYPE_NAMES = setOf(
        UNIT,
    )

    /**
     * 当前编译器认定为标准库根的包名集合。
     */
    @JvmField
    val STDLIB_PACKAGE_FQ_NAMES = setOf(
        STD_PACKAGE_FQ_NAME,
        COMPRESS_PACKAGE_FQ_NAME,
        NET_PACKAGE_FQ_NAME,
        FUZZ_PACKAGE_FQ_NAME,
        ENCODING_PACKAGE_FQ_NAME,
        CRYPTO_PACKAGE_FQ_NAME,
        SERIALIZATION_PACKAGE_FQ_NAME,
    )

    /**
     * 根据参数个数生成标准函数类型的 ClassId。
     */
    fun getFunctionClassId(parameterCount: Int): ClassId {
        return ClassId(BASIC_PACKAGE_FQ_NAME, Name.identifier(
            StandardNames.getFunctionName(
                parameterCount
            )
        ))
    }


    /**
     * 汇总所有受编译器特殊识别的标准库包名。
     */
    private fun namesToSetOf(): Set<FqName> {
        val set = mutableSetOf<FqName>()
        set.add(BASIC_PACKAGE_FQ_NAME)

        // std 主包
        set.add(STD_PACKAGE_FQ_NAME)

        // std.* 一级子包
        set.add(STD_ARGOPT_PACKAGE_FQ_NAME)
        set.add(STD_AST_PACKAGE_FQ_NAME)
        set.add(STD_BINARY_PACKAGE_FQ_NAME)
        set.add(STD_COLLECTION_PACKAGE_FQ_NAME)
        set.add(STD_CONSOLE_PACKAGE_FQ_NAME)
        set.add(STD_CONVERT_PACKAGE_FQ_NAME)
        set.add(STD_CORE_PACKAGE_FQ_NAME)
        set.add(STD_CRYPTO_PACKAGE_FQ_NAME)
        set.add(STD_DATABASE_PACKAGE_FQ_NAME)
        set.add(STD_DERIVING_PACKAGE_FQ_NAME)
        set.add(STD_ENV_PACKAGE_FQ_NAME)
        set.add(STD_FS_PACKAGE_FQ_NAME)
        set.add(STD_IO_PACKAGE_FQ_NAME)
        set.add(STD_MATH_PACKAGE_FQ_NAME)
        set.add(STD_NET_PACKAGE_FQ_NAME)
        set.add(STD_OBJECTPOOL_PACKAGE_FQ_NAME)
        set.add(STD_OVERFLOW_PACKAGE_FQ_NAME)
        set.add(STD_POSIX_PACKAGE_FQ_NAME)
        set.add(STD_PROCESS_PACKAGE_FQ_NAME)
        set.add(STD_RANDOM_PACKAGE_FQ_NAME)
        set.add(STD_REF_PACKAGE_FQ_NAME)
        set.add(STD_REFLECT_PACKAGE_FQ_NAME)
        set.add(STD_REGEX_PACKAGE_FQ_NAME)
        set.add(STD_RUNTIME_PACKAGE_FQ_NAME)
        set.add(STD_SORT_PACKAGE_FQ_NAME)
        set.add(STD_SYNC_PACKAGE_FQ_NAME)
        set.add(STD_TIME_PACKAGE_FQ_NAME)
        set.add(STD_UNICODE_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_PACKAGE_FQ_NAME)

        // std.collection.* 子包
        set.add(STD_COLLECTION_CONCURRENT_PACKAGE_FQ_NAME)

        // std.crypto.* 子包
        set.add(STD_CRYPTO_CIPHER_PACKAGE_FQ_NAME)
        set.add(STD_CRYPTO_DIGEST_PACKAGE_FQ_NAME)

        // std.database.* 子包
        set.add(STD_DATABASE_SQL_PACKAGE_FQ_NAME)

        // std.deriving.* 子包
        set.add(STD_DERIVING_API_PACKAGE_FQ_NAME)
        set.add(STD_DERIVING_BUILTINS_PACKAGE_FQ_NAME)
        set.add(STD_DERIVING_IMPL_PACKAGE_FQ_NAME)
        set.add(STD_DERIVING_RESOLVE_PACKAGE_FQ_NAME)

        // std.math.* 子包
        set.add(STD_MATH_NUMERIC_PACKAGE_FQ_NAME)

        // std.unittest.* 子包
        set.add(STD_UNITTEST_COMMON_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_DIFF_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_MOCK_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_PROP_TEST_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_TESTMACRO_PACKAGE_FQ_NAME)

        // std.unittest.mock.* 子包
        set.add(STD_UNITTEST_MOCK_INTERNAL_PACKAGE_FQ_NAME)
        set.add(STD_UNITTEST_MOCK_MOCKMACRO_PACKAGE_FQ_NAME)

        return set
    }

    /**
     * 所有内置基础包和标准库包的完整集合。
     */
    @JvmField
    val ALL_NAMES = namesToSetOf()

}
