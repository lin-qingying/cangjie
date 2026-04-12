// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package std.crypto.digest
import std.io.{
    InputStream,
    OutputStream
}


/**
* The Digest interface
*
*
*/
@!APILevel[12, atomicservice : true]
public interface Digest {
}

/**
* The function is to calculate the digest of data
*
* @param algorithm - digest type
* @param data - data to be digested
*
* @return message-digested data
*/
@Deprecated[message: "Use global function `public func digest<T>(algorithm: T, input: InputStream): Array<Byte> where T <: Digest` instead."]
@!APILevel[12, atomicservice : true]
public func digest<T>(algorithm: T, data: String): Array<Byte> where T <: Digest

@!APILevel[12, atomicservice : true]
public func digest<T>(algorithm: T, input: InputStream): Array<Byte> where T <: Digest

@!APILevel[12, atomicservice : true]
public func digest<T>(algorithm: T, data: Array<Byte>): Array<Byte> where T <: Digest

