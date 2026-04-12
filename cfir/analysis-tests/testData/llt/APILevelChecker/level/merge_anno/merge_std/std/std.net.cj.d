// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.net

import std.binary.{
    BigEndianOrder,
    LittleEndianOrder,
    SwapEndianOrder,
}
import std.collection.HashSet
import std.sort.stableSort
import std.binary.*
import std.convert.*
import std.overflow.{
    CheckedOp,
    WrappingOp,
}
import std.convert.Parsable
import std.binary.{
    SwapEndianOrder,
    BigEndianOrder,
    LittleEndianOrder,
}
import std.overflow.*
import std.binary.BigEndianOrder
import std.collection.*
internal import std.time.*
internal import std.io.*
internal import std.sync.*
internal import std.math.*

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct AddressFamily <: ToString & Equatable<AddressFamily> {
    // UNSPEC
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const UNSPEC = AddressFamily("UNSPEC", 0)
    
    // UNIX
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const UNIX = AddressFamily("UNIX", 1)
    
    // INET
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const INET = AddressFamily("INET", 2)
    
    // INET6
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const INET6 = AddressFamily("INET6", 10)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const INET6 = AddressFamily("INET6", 23)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const INET6 = AddressFamily("INET6", 30)
    
    // NETLINK
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const NETLINK = AddressFamily("NETLINK", 16)
    
    // name
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public let name: String
    
    // value
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public let value: UInt16
    
    // init
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public const init(name: String, value: UInt16)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: AddressFamily): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: AddressFamily): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
abstract sealed class IPAddress <: ToString & Equatable<IPAddress> & Hashable & BigEndianOrder<IPAddress> {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop size: Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop hostName: ?String
    
    // parse a address string into IPAddress
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func parse(s: String): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func tryParse(s: String): ?IPAddress
    
    // resolve domain
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func resolve(family: AddressFamily, domain: String): Array<IPAddress>
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func resolve(domain: String): Array<IPAddress>
    
    // get addr bytes
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getAddressBytes(): Array<Byte>
    
    // is ipv4 addr
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isIPv4(): Bool
    
    // is ipv6 addr
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isIPv6(): Bool
    
    // either the IPv4 address "0.0.0.0" or the IPv6 address "::".
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isUnspecified(): Bool
    
    // Requirements for Internet Hosts -- Communication Layers (3.2.1.3 Addressing)// https://www.rfc-editor.org/rfc/rfc1122.html#section-3.2.1.3
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isLoopback(): Bool
    
    // Host Extensions for IP Multicasting (4. HOST GROUP ADDRESSES)// https://www.rfc-editor.org/rfc/rfc1112.html#section-4
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isMulticast(): Bool
    
    // RFC 1918 allocates 10.0.0.0/8, 172.16.0.0/12, and 192.168.0.0/16 as// private IPv4 address subnets.
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isPrivate(): Bool
    
    // Dynamic Configuration of IPv4 Link-Local Addresses// https://www.rfc-editor.org/rfc/rfc3927.html#section-2.1
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isLinkLocal(): Bool
    
    // Whether the address is a global unicast IPv6 address or a public IPv4 address
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func isGlobalUnicast(): Bool
    
    // Obtains the IP prefix object based on the prefix length.
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func getPrefix(prefixLen: UInt8): IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func readBigEndian(buffer: Array<Byte>): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: IPAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: IPAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
abstract sealed class IPPrefix <: Equatable<IPPrefix> & Hashable & ToString {
    // IPAddress
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop address: IPAddress
    
    // prefix length
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop prefixLength: UInt8
    
    // Parse the IP address prefix from the character string. If the IP address prefix is invalid, IllegalFormatException is thrown.
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func parse(s: String): IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func tryParse(s: String): ?IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func contains(rhs: IPAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func contains(rhs: IPPrefix): Bool
    
    // Overlap with the Specified IP Network
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func overlaps(rhs: IPPrefix): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func netmask(): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func hostmask(): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func broadcast(): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func network(): IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public open func masked(): IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: IPPrefix): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: IPPrefix): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class IPSocketAddress <: SocketAddress & Equatable<IPSocketAddress> {
    // init with byte array and port
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: Array<Byte>, port: UInt16)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: String, port: UInt16)
    
    // init by IPAddress and port
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: IPAddress, port: UInt16)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop size: Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop family: AddressFamily
    
    /**
    * Parses the socket address in text format, for example, 192.168.0.0:80 or [fc00::1]:8080.
    * If the parsing fails, an exception is thrown.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func parse(s: String): IPSocketAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func tryParse(s: String): ?IPSocketAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop address: IPAddress
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop port: UInt16
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getAddressBytes(): Array<Byte>
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isIPv4(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isIPv6(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: IPSocketAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: IPSocketAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

// ref https://www.rfc-editor.org/rfc/rfc791.html
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class IPv4Address <: IPAddress & ToString & Equatable<IPv4Address> & LessOrEqual<IPv4Address> {
    // An IPv4 address with the address pointing to localhost: `127.0.0.1`
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let localhost = IPv4Address(0x7F, 0, 0, 0x01)
    
    // An IPv4 address representing an unspecified address: `0.0.0.0`
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let unspecified = IPv4Address(0, 0, 0, 0)
    
    // An IPv4 address representing the broadcast address: `255.255.255.255`
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let broadcast = IPv4Address(0xFF, 0xFF, 0xFF, 0xFF)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bits: UInt32)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(a: Byte, b: Byte, c: Byte, d: Byte)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toBits(): UInt32
    
    // into in_addr
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func readBigEndian(buffer: Array<Byte>): IPv4Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isBroadcast(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isGlobalUnicast(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isLinkLocal(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isLoopback(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isMulticast(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isPrivate(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isUnspecified(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toIPv6Compatible(): IPv6Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toIPv6Mapped(): IPv6Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getPrefix(prefixLen: UInt8): IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func <=(rhs: IPv4Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: IPv4Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: IPv4Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

extend<T> Array<T> where T <: LessOrEqual<T> {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func <=(rhs: Array<T>): Bool
}

// ref https://www.rfc-editor.org/rfc/rfc4291.html
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class IPv6Address <: IPAddress & ToString & Equatable<IPv6Address> & LessOrEqual<IPv6Address> {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let localhost = IPv6Address(0u16, 0, 0, 0, 0, 0, 0, 1)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let unspecified = IPv6Address(0u16, 0, 0, 0, 0, 0, 0, 0)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(octets: Array<Byte>, scopeId!: ?UInt32 = None)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(a: UInt16, b: UInt16, c: UInt16, d: UInt16, e: UInt16, f: UInt16, g: UInt16, h: UInt16, scopeId!: ?UInt32 = None)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop scopeId: ?UInt32
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getPrefix(prefixLen: UInt8): IPPrefix
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toIPv4(): ?IPv4Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toIPv4Mapped(): ?IPv4Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func scope(scopeId: ?UInt32): IPv6Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static func readBigEndian(buffer: Array<Byte>): IPv6Address
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isGlobalUnicast(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isIPv4Mapped(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isLinkLocal(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isLoopback(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isMulticast(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isPrivate(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isTeredo(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isUnspecified(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func <=(rhs: IPv6Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: IPv6Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: IPv6Address): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

extend<T> Result<T> <: ToString where T <: ToString {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

extend Rune {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toDigit(radix: UInt32): ?UInt32
}

extend Byte {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toDigit(radix: UInt32): ?UInt32
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
abstract sealed class SocketAddress <: ToString & Equatable<SocketAddress> & Hashable {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop size: Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop family: AddressFamily
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getAddressBytes(): Array<Byte>
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: SocketAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: SocketAddress): Bool
}

/**
* Represents a socket that works in streaming mode. It is a duplex stream having both read and write operations.
*
* A streaming socket could be bound and connected. The remote and local address could be determined via the corresponding properties.
* Read and write operations can optionally have completion time limit.
*
* A streaming socket does send bytes sequentially (possibly batching by packets), the order or incoming and outgoing bytes is always considered
* and kept. The actuall streaming socket implementation usually has buffers for reading and writing.
*
* The read function copies some incoming bytes to the specified buffer, the actual amount of bytes per invocation is generally unpredictable
* and depends on many factors like the buffer size, the underlying buffer size, timing and the operating system and it's implementation.
* Every time read() function is invoked, a batch of bytes (at least one byte) is copied and the bytes are remembered as read.
* So the next read invocation can only receive remaining bytes. If there are no unprocessed incoming bytes yet, the read operation blocks until
* any bytes arrives (or timeout limit exceeded).
* The write operation works in a similar way: writing buffer does copy and schedule bytes for sending. If the underlying buffer capacity is not enough
* or we are writing bytes faster then they are transferred, then the write operation may block until some buffer space get freed or the timeout limit exceeded.
*
* The order of bytes and batches/packets is always preserved in both read and write direction.
* However, the timing and the number of batches is not guaranteed to remain the same during transferring, only the total number of bytes and the content.
* For example, a peer sends 10 bytes and then 15 more bytes. The remote peer could receive these bytes in two pieces (10 then 15 bytes) or
* it may receive all bytes at once (25 bytes) depending on timing, network strucuture and many other reasons.
*
* A streaming socket is usually (but not necessarily) connected to a stream of unknown/unpredictable size
* so the length property inherited from InputStream returns -1 for such sockets.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public interface StreamingSocket <: IOStream & Resource & ToString {
}

/**
* Represents a socket that handles datagrams in both input and output directions.
*
* A datagram is a bytes message of a finite size (possibly empty).
* Depending on the actual datagram socket kind, there is usually a maxmimal datagram size. For example, a UDP socket can handle up to 64KiB at once.
*
* A datagram is transferred at once while the order or datagrams is not guaranteed, only the order and content of bytes inside.
* Unlike streaming socket, datagrams may arrive in different order or not arrive at all.
* This is because datagrams are routed independently and possibly via different actual routes.
* Also, datagram size is always preserved (if possible, see receive/receiveFrom). For example, a peer sends datagrams of 10 and 15 bytes. Then,
* the remote peer receives bytes in batches of the same size, 10 bytes and 15 bytes.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public interface DatagramSocket <: Resource & ToString {
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public interface ServerSocket <: Resource & ToString {
}

/**
* Transport Layer Protocol Kind
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public enum SocketNet <: ToString & Equatable<SocketNet> {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    TCP |
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    UDP |
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    UNIX
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(that: SocketNet): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(that: SocketNet): Bool
}

/**
* The SocketException class is used to handle exceptions related to socket connection errors.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class SocketException <: IOException {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(message: String)
}

/**
* The SocketTimeoutException class is used to handle exceptions related to socket connection timeout.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class SocketTimeoutException <: Exception {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(message: String)
}

/**
* TCP KeepAlive Options
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct SocketKeepAliveConfig <: ToString & Equatable<SocketKeepAliveConfig> {
    /**
    * If no data is exchanged within the `idle` period, the probe is performed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public let idle: Duration
    
    /**
    * The interval for sending probe packets is `interval`.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public let interval: Duration
    
    /**
    * Number of probe packets are sent before the connection is considered invalid.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public let count: UInt32
    
    /**
    * Creates a socket keep alive config instance.
    * Please note that the actual time granularity could be different depending on the operating system
    * so could be bumped or rounded if needed correspondingly.
    *
    * @throws IllegalArgumentException if the specified idle or interval duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(
        idle!: Duration = Duration.second * 45,
        interval!: Duration = Duration.second * 5,
        count!: UInt32 = 5
    )
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override operator func ==(other: SocketKeepAliveConfig): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override operator func !=(other: SocketKeepAliveConfig): Bool
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct SocketOptions {
    @Deprecated[message: "Use `public static const SOCKET: Int32` in `public struct OptionLevel` instead."]
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SOL_SOCKET: Int32 = SOL_SOCKET
    
    @Deprecated[message: "Use `public static const TCP: Int32` in `public struct OptionLevel` instead."]
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IPPROTO_TCP: Int32 = IPPROTO_TCP
    
    @Deprecated[message: "Use `public static const UDP: Int32` in `public struct OptionLevel` instead."]
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IPPROTO_UDP: Int32 = IPPROTO_UDP
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_KEEPALIVE: Int32 = SOCK_KEEPALIVE
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_NODELAY: Int32 = SOCK_TCP_NODELAY
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_QUICKACK: Int32 = SOCK_TCP_QUICKACK
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_LINGER: Int32 = SOCK_LINGER
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_SNDBUF: Int32 = SOCK_SNDBUF
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_RCVBUF: Int32 = SOCK_RCVBUF
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_REUSEADDR: Int32 = SOCK_REUSEADDR
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_REUSEPORT: Int32 = SOCK_REUSEPORT
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_BINDTODEVICE: Int32 = SOCK_BINDTODEVICE
}

/**
* Socket domain values.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct SocketDomain <: Equatable<SocketDomain> & ToString & Hashable {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV4: SocketDomain = SocketDomain(AF_INET)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV6: SocketDomain = SocketDomain(AF_INET6)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let UNIX: SocketDomain = SocketDomain(AF_UNIX)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let NETLINK: SocketDomain = SocketDomain(AF_NETLINK)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let PACKET: SocketDomain = SocketDomain(AF_PACKET)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(domain: Int32)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(r: SocketDomain): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(r: SocketDomain): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct SocketDomain <: Equatable<SocketDomain> & ToString & Hashable {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV4: SocketDomain = SocketDomain(AF_INET)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV6: SocketDomain = SocketDomain(AF_INET6)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let UNIX: SocketDomain = SocketDomain(AF_UNIX)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(domain: Int32)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(r: SocketDomain): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(r: SocketDomain): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
}

/**
* Socket type values.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct SocketType <: Equatable<SocketType> & ToString & Hashable {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let STREAM: SocketType = SocketType(SOCKET_STREAM)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let DATAGRAM: SocketType = SocketType(SOCKET_DGRAM)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let RAW: SocketType = SocketType(SOCKET_RAW)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let SEQPACKET: SocketType = SocketType(SOCKET_SEQPACKET)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(`type`: Int32)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(r: SocketType): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(r: SocketType): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
}

/**
* Socket protocol values.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct ProtocolType <: Equatable<ProtocolType> & ToString & Hashable {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let Unspecified: ProtocolType = ProtocolType(0)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV4: ProtocolType = ProtocolType(IPPROTO_IPV4)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let IPV6: ProtocolType = ProtocolType(IPPROTO_IPV6)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let ICMP: ProtocolType = ProtocolType(IPPROTO_ICMP)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let TCP: ProtocolType = ProtocolType(IPPROTO_TCP)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let UDP: ProtocolType = ProtocolType(IPPROTO_UDP)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static let RAW: ProtocolType = ProtocolType(IPPROTO_RAW)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(protocol: Int32)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(r: ProtocolType): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(r: ProtocolType): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
}

/**
* Socket option level values.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct OptionLevel {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IP: Int32 = IPPROTO_IP
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP: Int32 = IPPROTO_TCP
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const UDP: Int32 = IPPROTO_UDP
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const ICMP: Int32 = IPPROTO_ICMP
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const RAW: Int32 = IPPROTO_RAW
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SOCKET: Int32 = SOL_SOCKET
}

/**
* Socket optoin name values.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct OptionName {
    // IPOption
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IP_HDRINCL: Int32 = SOCK_IP_HDRINCL
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IP_TOS: Int32 = SOCK_IP_TOS
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const IP_TTL: Int32 = SOCK_IP_TTL
    
    // TCPOption
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_KEEPCNT: Int32 = SOCK_TCP_KEEPCNT
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_KEEPIDLE: Int32 = SOCK_TCP_KEEPIDLE
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_KEEPINTVL: Int32 = SOCK_TCP_KEEPINTVL
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const TCP_NODELAY: Int32 = SOCK_TCP_NODELAY
    
    // SOCKETOption
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_DEBUG: Int32 = SOCK_DEBUG
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_ACCEPTCONN: Int32 = SOCK_ACCEPTCONN
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_REUSEADDR: Int32 = SOCK_REUSEADDR
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_KEEPALIVE: Int32 = SOCK_KEEPALIVE
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_DONTROUTE: Int32 = SOCK_DONTROUTE
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_BROADCAST: Int32 = SOCK_BROADCAST
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_LINGER: Int32 = SOCK_LINGER
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_OOBINLINE: Int32 = SOCK_OOBINLINE
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_SNDBUF: Int32 = SOCK_SNDBUF
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_RCVBUF: Int32 = SOCK_RCVBUF
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_SNDTIMEO: Int32 = SOCK_SNDTIMEO
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_RCVTIMEO: Int32 = SOCK_RCVTIMEO
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public static const SO_ERROR: Int32 = SOCK_ERROR
}

/**
* This class defines the address for RawSocket.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public struct RawAddress {
    /* Get the array of RawAddress */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop addr: Array<Byte>
    
    /**
    * Initialize an address with a byte array.
    * The byte array needs to be memory aligned with the C struct, such as sockaddr_in, sockaddr_un.
    *
    * @param addr The byte sequence corresponding to socket address.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(addr: Array<Byte>)
}

/*
* This API supplies users with the basic functionality to create and use sockets of different domains and types.
* For example, based on this API user should be able to create sockets in following domains: AF_PACKET, AF_NETLINK,
* and in following types: SOCK_RAW, SOCK_SEQPACKET.
* This is a low-level API, not recommended for general and direct usage. Users are encouraged to construct their own
* higher level APIs based on this one to access a socket of specific domain and/or type.
* For general usage of TCP, UDP, UDS in stream or datagram mode, it's encouraged to use existing corresponding
* Cangjie APIs rather than this one.
* Implementation note:
* This API is an thin interface between Cangjie code and system calls related to socket.
* To enable users to access these system calls, the implementation makes sure that calling these wrapped cangjie API
* will not block underlying system thread.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class RawSocket {
    /* Get local socket address */
    @Deprecated[message: "Use `public prop localAddress: RawAddress` instead."]
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop localAddr: RawAddress
    
    /* Get local socket address */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop localAddress: RawAddress
    
    /* Get remote socket address */
    @Deprecated[message: "Use `public prop remoteAddress: RawAddress` instead."]
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop remoteAddr: RawAddress
    
    /* Get remote socket address */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop remoteAddress: RawAddress
    
    /* Get or set timeout for socket read */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop readTimeout: ?Duration
    
    /* Get or set timeout for socket write */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop writeTimeout: ?Duration
    
    /**
    * Create a socket with specific domain, type and protocol.
    *
    * @param domain The socket communication domain defined in struct SocketDomain.
    * @param sockType The socket type defined in struct SocketType.
    * @param protocol The socket protocol defined in struct ProtocolType.
    *
    * @throws SocketException if fail to create socket.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(domain: SocketDomain, `type`: SocketType, protocol: ProtocolType)
    
    /**
    * Assign an address to the socket.
    *
    * @param addr The socket address stored in byte array.
    *
    * @throws SocketException if fail to bind address.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func bind(addr: RawAddress): Unit
    
    /**
    * Mark the socket as a passive socket to accept incoming connection.
    *
    * @param backlog The maximum length of the pending connections queue.
    *
    * @throws SocketException if fail to listen.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func listen(backlog: Int32): Unit
    
    /**
    * Accept a connecting socket, waiting for one if there are no pending connection requests within the specified timeout.
    * If the timeout is `None`, then acception attempts will continue without time limit.
    * If the timeout less than or equal to Duration.Zero, it will timeout immediately.
    *
    * @param timeout The maxium waiting duration for getting a connection.
    * @return RawSocket for communication.
    *
    * @throws IllegalArgumentException if timeout is negative.
    * @throws SocketTimeoutException if the specified timeout ellapsed before any connection request were made.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func accept(timeout!: ?Duration = None): RawSocket
    
    /**
    * Connect the socket to the address specified by addr within the specified timeout.
    * If the timeout is `None`, then acception attempts will continue without time limit.
    *
    * @param addr The peer socket address.
    * @param timeout The maxium duration for connection.
    *
    * @throws IllegalArgumentException if timeout is negative.
    * @throws SocketTimeoutException if the specified timeout ellapsed before acception was made.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(addr: RawAddress, timeout!: ?Duration = None): Unit
    
    /* Close the socket */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func close(): Unit
    
    /**
    * Transmit a message specified by buffer to another socket.
    *
    * @param addr The peer address.
    * @param buffer The array data to be send.
    * @param flag The flag of how to invoke this func.
    *
    * @throws SocketTimeoutException when timeout.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func sendTo(addr: RawAddress, buffer: Array<Byte>, flags: Int32): Unit
    
    /**
    * Receive a message and store it in buffer.
    *
    * @param buffer The buffer for message to be stored in.
    * @param flag The flag of how to invoke this func.
    * @return tuple of peer address and the number of bytes sent.
    *
    * @throws SocketTimeoutException when timeout.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func receiveFrom(buffer: Array<Byte>, flags: Int32): (RawAddress, Int64)
    
    /**
    * Transmit a message specified by buffer to another socket. This api is only used for connected socket.
    *
    * @param buffer The array data to be send.
    * @param flag The flag of how to invoke this func.
    *
    * @throws SocketTimeoutException when timeout.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func send(buffer: Array<Byte>, flags: Int32): Unit
    
    /**
    * Receive a message and store it in buffer. This api is only used for connected socket.
    *
    * @param buffer The buffer for message to be stored in.
    * @param flag The flag of how to invoke this func.
    * @return the number of bytes sent.
    *
    * @throws SocketTimeoutException when timeout.
    * @throws SocketException when other error occurs.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func receive(buffer: Array<Byte>, flags: Int32): Int64
    
    /**
    * Set socket option with value specified by @p value in byte array form.
    *
    * @param level Socket option level defined in struct OptionLevel or Int32 value.
    * @param option Socket option name defined in struct OptionName or Int32 value.
    * @param value The value of option name in CPointer form.
    * @param len The length of option name struct.
    
    * @throws SocketException if fail to set socket option.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public unsafe func setSocketOption(level: Int32, option: Int32, value: CPointer<Byte>, len: Int32): Unit
    
    /**
    * Get socket option value.
    *
    * @param level Socket option level defined in struct OptionLevel or Int32 value.
    * @param option Socket option name defined in struct OptionName or Int32 value.
    * @param value The value of option name to be stored in.
    * @param len The length of value to be stored in.
    *
    * @throws SocketException if fail to get socket option.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public unsafe func getSocketOption(level: Int32, option: Int32, value: CPointer<Byte>, len: CPointer<Int32>): Unit
}

/*
* Represents a TCP streaming socket.
*
* Once an instance is created, it is not yet connected so should be connected explicitly via connect().
*
* Instances of this type should be explicitly closed even when the connect() hasn't been invoked.
*
* @see StreamingSocket for more details on how do streaming sockets work.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class TcpSocket <: StreamingSocket & Equatable<TcpSocket> & Hashable {
    /**
    * Create an unconnected TCP socket ready to connect to the specified address and port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: String, port: UInt16)
    
    /**
    * Create an unconnected TCP socket ready to connect to the specified address and port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: SocketAddress)
    
    /**
    * Create an unconnected TCP socket ready to connect to the specified address and port
    * and optionally binding client socket to the particular localAddress (None to automatically find local address to bind)
    *
    * Specifying localAddress usually makes sense to give a hint, which network interface to use for connection.
    * If in doubt or don't know, specify None or use the constructor without localAddress.
    * Specifying localAddress does also configure SO_REUSEADDR by default for convenience ortherwise spurious "address already in use" may occur.
    * Use setSocketOptionBool(SocketOptions.SOL_SOCKET, SocketOptions.SO_REUSEADDR, false) to eliminate this option if needed.
    * Also note that local and remote address should always have the same address family: for example, both IPv4.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: SocketAddress, localAddress!: ?SocketAddress)
    
    /**
    * Remote address the socket will be or is currently connected to.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop remoteAddress: SocketAddress
    
    /**
    * Local address the socket will be or currently is bound at.
    *
    * @throws SocketException is the socket is already closed
    * or no local address is available (local address was not provided during creation and the socket is not connected).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Read operation time limit or `None` for infinite read attempts.
    * The value specified here is actually the minimal amount of time before a read operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop readTimeout: ?Duration
    
    /**
    * Write operation time limit or `None` for infinite read attempts.
    *
    * The value specified here is actually the minimal amount of time before a write operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop writeTimeout: ?Duration
    
    /**
    * Network interface name to bind at.
    * Despite it's a client TCP socket, we still do bind before
    * connect to occupy a local port and it is sometimes important
    * to bind at some particular network interface to try to
    * enforce the particular route.
    *
    * This option is a hint for the operating system that may decide to ignore the value
    * or reject an attempt to configure it, especially when it's not
    * allowed, unsupported or we specify a wrong name.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop bindToDevice: ?String
    
    /**
    * TCP Keep-Alive options or `None` if disabled.
    * If not configured, the operating system may decide to use some default keep-alive configuration.
    * Changing this option may have delayed effect or may be silently ignored or reinterpreted by the operating system
    * due to some reasons such as system configurations and/or missing support of particular features in the underlying TCP stack implementation.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop keepAlive: ?SocketKeepAliveConfig
    
    /**
    * `TCP_NODELAY`, true by default
    *
    * This option disables the Nagel's algorithm so any bytes chunk
    * written to the socket is scheduled for sending immediately without delay.
    * When this option is disabled, then Nagel's implementation does introduce time-based
    * delay before actually sending bytes. This is done to group outgoing byte chunks to bigger
    * TCP packets so there will be less quantity of them and the overhead decreases. In other words,
    * this is a time-based debouncing algorithm.
    * Despite that it looks reasonable, most applications already have proper buffering usually
    * on multiple application layers so this Nagel algorithm will simply introduce latency without
    * any benefits. So in this case TCP_NODELAY option is used to disable the debouncing.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop noDelay: Bool
    
    /**
    * TCP_QUICKACK, false by default
    *
    * This is similar to TCP_NODELAY but affects only TCP ACK and first response bytes chunk.
    * Usually (without TCP_QUICKACK), the TCP stack implementation does defer sending TCP ACK packet
    * until the first bytes will be sent (but with some time limit).
    * The idea is to group ACK and data bytes into a single batch, and reduce overhead.
    * Because of this, the remote peer doesn't get connection acknowledgement immediately but after some delay.
    * In some latency-sensitive or interactive protocols it is not acceptable.
    * So here TCP_QUCKACK option comes and provides a way to force sending TCP ACK immediately to reduce
    * the connection latency. However, generally this is not always good as we sacrify throughput a little bit
    * and increase the number of network packets, enlarging load on network hardware, switches.
    * Increasing the number of packets also leads to loss probability growth reducing robustness.
    * This is why this option could be good or bad depending on the usage scenario and environment.
    *
    * Not supported on windows and macOS
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop quickAcknowledge: Bool
    
    /**
    * SO_LINGER duration, the default is system-dependant. `None` if linger is disabled.
    *
    * When a socket is closed, if there are pending outgoing bytes, we are waiting
    * for the linger time before aborting connection. If the time is out but bytes were not sent yet,
    * then usually the connection get aborted (via reset / TCP reset).
    *
    * When the linger is disabled (`None`), then the connection will be aborted immediately:
    * depending on the presence of pending outgoing bytes, it will be either terminated successully (FIN-ACK)
    * or reset (RST).
    *
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop linger: ?Duration
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    /**
    * Read at least one byte to the specified buffer waiting for incoming data if necessary.
    *
    * Returns number of bytes written to the buffer or 0 when the remote peer closed the stream
    * or also 0 when the socket is closed.
    *
    * @throws IllegalArgumentException if the specified buffer is empty
    * @throws SocketTimeoutException if the waiting time has expired.
    * @throws SocketException when the connection is broken
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func read(buffer: Array<Byte>): Int64
    
    /**
    * Write the payload bytes to the socket waiting for the output buffer space if necessary.
    *
    * The provided bytes are copied and transmitted asynchronously so returning from this function
    * doesn't guarantee actual data delivery. When the link is poor or the remote peer is unable
    * to handle data fast enough, the send buffer may overflow and in this case an attempt
    * to write more bytes may block coroutine here until enough bytes will be transmitted to
    * get enough send buffer space to write the payload. If the payload is too big to fit the send
    * buffer, then the payload will be fragmented and sent chunk by chunk.
    *
    * Also note that due to the nature of IP and TCP having packet fragmentation,
    * the underlying network may split the payload into smaller parts of any size
    * so it is not guaranteed that the the whole payload will be delivered to the remote peer at once.
    *
    * Despite TCP provides delivery acknowledge, the delivery here means that bytes were transmitted
    * to the remote peer and it doesn't gurantee that the remote application actually received
    * and processed bytes. Also, this function only schedule the payload for sending.
    * Therefore, successfully returning write() invocation doesn't mean that bytes were
    * deliverd. To get guaranteed delivery, use application-level acknowledges instead.
    *
    * @throws IllegalArgumentException if the specified buffer is empty
    * @throws SocketTimeoutException if the waiting time has expired.
    * @throws SocketException when the socket is closed or the connection is broken
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func write(payload: Array<Byte>): Unit
    
    /**
    * Connects to the remote peer within the specified timeout.
    * If the timeout is `None`, then connection attempts will continue without time limit.
    * Please note that this function doesn't do retry so if the server peer does reject connection, we get error despite the timeout duration.
    *
    * This function also does bind first before doing connect so there is no need to invoke bind
    *
    * @throws SocketException if the connection cannot be established.
    * @throws SocketTimeoutException if the waiting time has expired.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(timeout!: ?Duration = None): Unit
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isClosed(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override operator func ==(other: TcpSocket): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override operator func !=(other: TcpSocket): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func hashCode(): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

/**
* TCP server socket providing a way to listen for TCP incoming connections.
*
* Once created, could be configured via corresponding properties (e.g. reusePort) or setSocketOptionXX functions.
*
* To start listening, use bind() function that does bind socket on a local port and start listening for connections.
*
* Receiving an incoming connection is provided via accept() function that does wait for the next connection or returns immediately
* if there is already pending connection.
*
* Instances of this type should be explicitly closed even when the bind() hasn't been invoked.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class TcpServerSocket <: ServerSocket {
    /**
    * Local address the socket will be or is currently bound at.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Creates a TCP server socket that is not yet bound so client can't connects until we do bind()
    * bindAt specifies the local port to bind at or zero to bind at a random free local port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: UInt16)
    
    /**
    * Creates a TCP server socket that is not yet bound so clients can't connect until we do bind()
    * bindAt specifies the local address to bind at, possibly with zero port to bind at a random free local port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: SocketAddress)
    
    /**
    * When binding socket, try to reuse the address even if it's already used and bound. This property configures SO_REUSEADDR.
    * This is usually makes sense when we are binding and there are still opened connections remaining from the previous socket.
    * Such old sockets are usually hanging in TIME_WAIT state and may prevent us from binding a new server socket.
    * Enabled by default
    * Behaviour of this option is system-dependant. Please consult with SO_REUSEADDR/SOCK_REUSEADDR documentation before using.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop reuseAddress: Bool
    
    /**
    * When binding socket to a local port, try to reuse it even if it's already used and bound.
    *
    * Please note that there are limitations on when ports could be reused. Behaviour of this option
    * is system-dependant (e.g. this option is unavailable on Windows).
    * Please consult with SO_REUSEPORT documentation before using.
    *
    * This option could be only modified before binding and will fail
    * after a successful bind() invocation.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop reusePort: Bool
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    /**
    * Network interface name to bind to.
    * This option is a hint for the operating system that may decide to ignore the value
    * or reject an attempt to configure it.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop bindToDevice: ?String
    
    /**
    * Configure incoming connections backlog size. This only works before binding socket.
    * Changing this value is not guaranteed to be actually applied since the operating system may decide to
    * change or bump it, or simply ignore.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop backlogSize: Int64
    
    /**
    * Bind TCP socket on a local port. Depending on [reusePort] and [reuseAddress] flag, it may fail if the port is already occupied
    * or when there are connections remaining from the previously bound socket.
    * This function also does listen just after binding creating an incoming connections queue that could be accessed via "accept()" function.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func bind(): Unit
    
    /**
    * Accept a client TCP socket, waiting for one if there are no pending connection requests.
    *
    * The OS implementation usually provides an incoming connection requests queue (called backlog),
    * so calling accept() does takes a candidate from the queue
    * or wait until we get some request if the queue is empty.
    *
    * Usually, a TCP client is waiting for connection until we get it from server.accept() function. If the server don't
    * get it in time then the client may fail to connect with error (e.g. connection timeout error). If the backlog queue is full
    * due to the missing accept() invocations then the operating system usually start rejecting new incoming connections that don't fit
    * the backlog queue capacity.
    * This fact could be used for backpressure control so if a server detects that no requests could be processed for some reason
    * then it may stop doing accept() to keep client in the queue and limit workload.
    *
    * The specified timeout is applied to accept operation
    * @throws SocketTimeoutException when the specified timeout is over
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func accept(timeout!: ?Duration): TcpSocket
    
    /**
    * Accept a client TCP socket, waiting for one if there are no pending connection requests.
    *
    * The OS implementation usually provides an incoming connection requests queue (called backlog),
    * so calling accept() does takes a candidate from the queue
    * or wait until we get some request if the queue is empty.
    *
    * Usually, a TCP client is waiting for connection until we get it from server.accept() function. If the server don't
    * get it in time then the client may fail to connect with error (e.g. connection timeout error). If the backlog queue is full
    * due to the missing accept() invocations then the operating system usually start rejecting new incoming connections that don't fit
    * the backlog queue capacity.
    * This fact could be used for backpressure control so if a server detects that no requests could be processed for some reason
    * then it may stop doing accept() to keep client in the queue and limit workload.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func accept(): TcpSocket
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func isClosed(): Bool
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

/*
* Represents a UDP datagram socket.
*
* Once an instance is created, it is not yet bound so should be bound explicitly via bind().
* Unlike TCP, a UDP socket may remain unconnected and work without pairing to any remote address handling
* multiple peers at the same time.
* However, a UDP socket could be optionally paired via connect() that generally doesn't involve any negotiation so "connecting" to
* a non-existing address could compelte successfully. A created pairing (after connect invocation) could be terminated via disconnect().
*
* UDP protocol does only allow sending and receiving datagrams at most of 64Kib long.
*
* Instances of this type should be explicitly closed even when the bind() hasn't been invoked.
*
* @see DatagramSocket for more details on how do datagram sockets work.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class UdpSocket <: DatagramSocket {
    /**
    * Creates an unbound UDP socket ready to bind at the specified port
    *
    * The default address is 0.0.0.0 what means every IP address of current machine,
    * this is more suitable for a server that can be connected from the local network
    * using localhost address (127.0.0.1) or from outside using corresponding address.
    * Therefore, this address (0.0.0.0) shall not be used as the destination address,
    * so it is necessary to specify an address in a particular network when calling
    * `connect` or `sendTo`. In other words, needs to select the network
    * through which to communicate. Linux, however, able to resolve network on its own,
    * in case of acting with 0.0.0.0 real address resolves in network of other side
    * (source address copies to destination address or vice versa), however,
    * do not rely on it in case of a portable app because windows does not support it.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: UInt16)
    
    /**
    * Creates an unbound UDP socket ready to bind at the specified interface/port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: SocketAddress)
    
    /**
    * Remote address the socket is connected to or `None` if the socket is unconnected.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop remoteAddress: ?SocketAddress
    
    /**
    * Local address the socket will be or is currently bound at.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Receive/ReceiveFrom operation time limit or `None` for infinite read attempts.
    * The value specified here is actually the minimal amount of time before a read operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop receiveTimeout: ?Duration
    
    /**
    * Send/SendTo operation time limit or `None` for infinite read attempts.
    *
    * The value specified here is actually the minimal amount of time before a write operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop sendTimeout: ?Duration
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func isClosed(): Bool
    
    /**
    * Bind UDP socket at local port
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func bind(): Unit
    
    /**
    * Configure the socket to only work with the specified remote peer address. To undo this action use disconnect()
    * Note that the remote address should always have the same address family as local at which the socket is bound: for example, both IPv4.
    *
    * After invoking this function with a particular address, function send will use the preset address. Functions send/sendTo may throw exceptions if
    * ICMP abnormal responses are recieved. We will also never receive any messages from other peers (they will be just filtered out).
    * Invoking disconnect() reverts the filter to the initial state so it will be possible to send message to any recipients again.
    *
    * This function should be only invoked after bind()
    *
    * @throws IllegalArgumentException if remote address has wrong kind.
    * @throws SocketException if the socket is not bound.
    * @throws SocketException if the connection cannot be established.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(remote: SocketAddress): Unit
    
    /**
    * Reverts the effect of connect() function so we can send and receive to/from any address again.
    * This function makes no effect if invoked multiple times or if we invoke disconnect without connect invocation.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func disconnect(): Unit
    
    /**
    * Receive the next datagram into the specified buffer waiting for data if needed.
    *
    * Returns a pair of the datagram sender address and the actual size of received datagram, possibly zero
    * or a value greater than the passed buffer size.
    *
    * Unlike read in streams, this function requires a buffer of proper size (big enough),
    * otherwise a datagram that is bigger than the provided buffer will be
    * truncated and the returned datagram size will be greater that the buffer size.
    *
    * @throws SocketException if buffer is empty or if it is not possible to read the data.
    * @throws SocketException if not bound or already closed
    * @throws SocketTimeoutException if reading time has expired.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func receiveFrom(buffer: Array<Byte>): (SocketAddress, Int64)
    
    /**
    * Sends datagram of the payload to the specified recipient address.
    *
    * It also may block in this function invocation if there is not enough
    * output buffer space available for some reason. Depending on the underlying
    * implementation, it may also silently discard a datagram in this case.
    *
    * @throws SocketException if payload size is larger than allowed by platform.
    * @throws SocketException if connect was preliminary called and abnormal ICMP was received.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func sendTo(recipient: SocketAddress, payload: Array<Byte>): Unit
    
    /**
    * Send a message with the specified payload to the peer with preconfigured address.
    * This only works if address has been specified using `connect() ` otherwise will fail immediately.
    *
    * In other aspects, it works the same as regular `sendTo(recipient,payload).
    *
    * @throws SocketException if not connected, not bound or already closed
    * @throws SocketException if payload size is larger than allowed by platform.
    * @throws SocketException if connect was preliminary called and abnormal ICMP was received.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func send(payload: Array<Byte>): Unit 
    
    /**
    * Receive a datagram message from the preconfigured peer address.
    * This only works if the address has been specified via connect() otherwise will fail.
    * In other aspects, it works the same as regular `receiveFrom(buffer).
    *
    * @throws SocketException if buffer is empty or if it is not possible to read the data.
    * @throws SocketException if not connected, not bound or already closed
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func receive(buffer: Array<Byte>): Int64
    
    /**
    * When binding socket to a local port, try to reuse it even if it's already used and bound.
    *
    * Please note that there are limitations on when ports could be reused. Behaviour of this option
    * is system-dependant (e.g. this option is unavailable on Windows).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop reusePort: Bool
    
    /**
    * When binding socket, try to reuse the address even if it's already used and bound. This property configures SO_REUSEADDR.
    * This is especially useful when doing multicasting. Behaviour of this option is system-dependant.
    * Please consult with SO_REUSEADDR/SOCK_REUSEADDR documentation before using.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop reuseAddress: Bool
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

/*
* Represents a Unix domain streaming socket.
*
* Once an instance is created, it is not yet connected so should be connected explicitly via connect().
*
* Instances of this type should be explicitly closed even when the connect() hasn't been invoked.
*
* @see StreamingSocket for more details on how do streaming sockets work.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class UnixSocket <: StreamingSocket {
    /**
    * Create an unconnected Unix domain socket ready to connect to the specified socket path
    *
    * @param path to connect to
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(path: String, localPath!: ?String = None)
    
    /**
    * Create an unconnected Unix domain socket ready to connect to the specified socket path
    * @param address to connect to
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(address: SocketAddress, localAddress!: ?SocketAddress = None)
    
    /**
    * Remote address the socket will be or is currently connected to.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop remoteAddress: SocketAddress
    
    /**
    * Local address the socket will be or currently is bound at.
    *
    * @throws SocketException is the socket is already closed
    * or no local address is available (local address was not provided during creation and the socket is not connected).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Read operation time limit or `None` for infinite read attempts.
    * The value specified here is actually the minimal amount of time before a read operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop readTimeout: ?Duration
    
    /**
    * Write operation time limit or `None` for infinite read attempts.
    *
    * The value specified here is actually the minimal amount of time before a write operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop writeTimeout: ?Duration
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func read(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func write(buffer: Array<Byte>): Unit
    
    /**
    * Connects to the remote peer within the specified timeout.
    * If the timeout is `None`, then connection attempts will continue without time limit.
    * Please note that this function doesn't do retry so if the server peer does reject connection, we get error despite the timeout duration.
    *
    * This function also does bind first before doing connect so there is no need to invoke bind
    *
    * @throws SocketException if the connection cannot be established.
    * @throws SocketTimeoutException if the waiting time has expired.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(timeout!: ?Duration = None): Unit
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func isClosed(): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

/**
* Unix Domain server socket providing a way to listen for incoming connections.
*
* Once created, could be configured via corresponding properties (e.g. reusePort) or setSocketOptionXX functions.
*
* To start listening, use bind() function that does bind socket on a local path and start listening for connections.
* The local path to bind at should be a non-existing path otherwise bind() will fail.
*
* Receiving an incoming connection is provided via accept() function that does wait for the next connection or returns immediately
* if there is already pending connection.
*
* Instances of this type should be explicitly closed even when the bind() hasn't been invoked.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class UnixServerSocket <: ServerSocket {
    /**
    * Local address the socket will be or is currently bound at.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Creates an anbound Unix server streaming socket configured to bind at the specified path
    * @param bindAt path for the unix server socket
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: String)
    
    /**
    * Creates an anbound Unix server streaming socket configured to bind at the specified path
    * @param bindAt path for the unix server socket
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: SocketAddress)
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    /**
    * Configure incoming connections backlog size. This only works before binding socket.
    * Changing this value is not guaranteed to be actually applied since the operating system may decide to
    * change or bump it, or simply ignore.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop backlogSize: Int64
    
    /**
    * Bind a streaming UNIX domain socket.
    *
    * This function also does listen just after binding creating an incoming connections queue that could be accessed via "accept()" function.
    *
    * This operation does atomically create a socket file at the local path.
    * If the path is already existing then bind() fails with SocketException.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func bind(): Unit
    
    /**
    * Accept a client socket, waiting for one if there are no pending connection requests.
    *
    * The OS implementation usually provides an incoming connection requests queue,
    * so calling accept() does takes a candidate from the queue
    * or wait until we get some request if the queue is empty.
    *
    * @throws SocketTimeoutException if the spcified timeout allapsed before got pending connection request
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func accept(timeout!: ?Duration): UnixSocket
    
    /**
    * Accept a client socket, waiting for one if there are no pending connection requests.
    *
    * The OS implementation usually provides an incoming connection requests queue,
    * so calling accept() does takes a candidate from the queue
    * or wait until we get some request if the queue is empty.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func accept(): UnixSocket
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func isClosed(): Bool
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

/*
* Represents a Unix Domain datagram socket.
*
* Once an instance is created, it is not yet bound so should be bound explicitly via bind().
* Unlike UnixSocket, a UnixDatagramSocket may remain unconnected and work without pairing to any remote address handling
* multiple peers at the same time.
* However, a UnixDatagramSocket socket could be optionally paired via connect() to an existing bound socket file.
* A created pairing (after connect invocation) could be terminated via disconnect().
*
* Unlike UDP, Unix Domain socket don't limit datagram size to 64Kib, however due to the underlying implementation and operation system
* limitations, the maximum datagram size could be still limited to some amount.
*
* Instances of this type should be explicitly closed even when the bind() hasn't been invoked.
*
* @see DatagramSocket for more details on how do datagram sockets work.
*/
@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class UnixDatagramSocket <: DatagramSocket {
    /**
    * Creates an unbound Unix datagram socket ready to bind at the specified path
    *
    * @param bindAt local path (should be a non-existing path that will be created during bind)
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: String)
    
    /**
    * Creates an unbound Unix datagram socket ready to bind at the specified address
    *
    * @param bindAt local path (should be a non-existing path that will be created during bind)
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(bindAt!: SocketAddress)
    
    /**
    * Remote address the socket is connected to or `None` if the socket is unconnected.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop remoteAddress: ?SocketAddress
    
    /**
    * Local address the socket will be or is currently bound at.
    *
    * @throws SocketException is the socket is already closed.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override prop localAddress: SocketAddress
    
    /**
    * Receive/ReceiveFrom operation time limit or `None` for infinite read attempts.
    * The value specified here is actually the minimal amount of time before a read operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop receiveTimeout: ?Duration
    
    /**
    * Send/SendTo operation time limit or `None` for infinite read attempts.
    *
    * The value specified here is actually the minimal amount of time before a write operation cancelled.
    * The actual time is not guaranteed but it will be never cancelled earlier than the specified timeout value.
    * If the duration is too big than it can be bumped to the infinite. When it's too small then if will be bumped to the minimal clock granularity.
    *
    * The default value is None.
    * @throws IllegalArgumentException if the specified timeout duration is negative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override mut prop sendTimeout: ?Duration
    
    /**
    * Close the socket releasing all resources. All operations except for close() and isClose() are no longer available.
    * This function is reentrant.
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func close(): Unit
    
    /**
    * Checks whether this socket has been explicitly closed via close()
    **/
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func isClosed(): Bool
    
    /**
    * Bind Unix datagram socket at local path.
    *
    * This operation does atomically create a socket file at the local path.
    *
    * @throws SocketException if the socket is already bound or path already exists.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func bind(): Unit
    
    /**
    * Configure the socket to only work with the specified remote peer address. To undo this action use disconnect()
    *
    * After invoking this function with a particular address, we will not be able to send message to peers other than this. We will also never receive any messages from other peers (they will be just filtered out). Invoking disconnect() reverts the filter to the initial state so it will be possible to send message to any recipients again.
    *
    * This function should be only invoked after bind()
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(remotePath: String): Unit
    
    /**
    * Configure the socket to only work with the specified remote peer address. To undo this action use disconnect()
    *
    * After invoking this function with a particular address, we will not be able to send message to peers other than this. We will also never receive any messages from other peers (they will be just filtered out). Invoking disconnect() reverts the filter to the initial state so it will be possible to send message to any recipients again.
    *
    * This function should be only invoked after bind()
    *
    * @throws SocketException if the socket is not bound.
    * @throws SocketException if the connection cannot be established.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func connect(remote: SocketAddress): Unit
    
    /**
    * Reverts the effect of connect() function so we can send and receive to/from any address again.
    * This function makes no effect if invoked multiple times or if we invoke disconnect without connect invocation.
    *
    * @throws SocketException if the socket is not bound.
    * @throws SocketException if the connection cannot be established.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func disconnect(): Unit
    
    /**
    * Receive the next datagram into the specified buffer waiting for data if needed.
    *
    * Returns a pair of the datagram sender address and the actual size of received datagram, possibly zero
    * or a value greater than the passed buffer size.
    *
    * Unlike read in streams, this function requires a buffer of proper size (big enough),
    * otherwise a datagram that is bigger than the provided buffer will be
    * truncated and the returned datagram size will be greater that the buffer size.
    *
    * @throws SocketException if buffer is empty or if it is not possible to read the data.
    * @throws SocketTimeoutException if reading time has expired.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func receiveFrom(buffer: Array<Byte>): (SocketAddress, Int64)
    
    /**
    * Sends datagram of the payload to the specified recipient address.
    *
    * It also may block in this function invocation if there is not enough
    * output buffer space available for some reason. Depending on the underlying
    * implementation, it may also silently discard a datagram in this case.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func sendTo(recipient: SocketAddress, payload: Array<Byte>): Unit
    
    /**
    * Send a message with the specified payload to the peer with preconfigured address.
    * This only works if address has been specified using `connect() ` otherwise will fail immediately.
    *
    * In other aspects, it works the same as regular `sendTo(recipient,payload).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func send(payload: Array<Byte>): Unit 
    
    /**
    * Receive a datagram message from the preconfigured peer address.
    * This only works if the address has been specified via connect() otherwise will fail.
    * In other aspects, it works the same as regular `receiveFrom(buffer).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func receive(buffer: Array<Byte>): Int64
    
    /**
    * SO_SNDBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired outgoing buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop sendBufferSize: Int64
    
    /**
    * SO_RCVBUF option, providing a way to specify hint for the underlying
    * native socket implementation about the desired receive buffer size.
    *
    * Changing this option is not guaranteed to have any effect since it's
    * completely up to the operating system.
    *
    * Reading this property could also provide non-realistic values on some systems in
    * some cases so no logic should strictly rely on this value.
    *
    * @throws IllegalArgumentException if the specified buffer size is negative or 0.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public mut prop receiveBufferSize: Int64
    
    /**
    * Read the specified socket option writing the result to value buffer
    * of the specified valueLength (in bytes).
    * Before invoking this function valueLength should be initialized with the buffer size
    * After invoking this function valueLength will contain the actual result
    * size in bytes.
    *
    * Throws an exception if failed (when getsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: CPointer<UIntNative>
    ): Unit
    
    /**
    * Write the specified socket option from value buffer having valueLength
    * size in bytes.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1).
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOption(
        level: Int32,
        option: Int32,
        value: CPointer<Unit>,
        valueLength: UIntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as IntNative result.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionIntNative(
        level: Int32,
        option: Int32
    ): IntNative
    
    /**
    * Write a numeric IntNative value to the specified socket option.
    *
    * See SocketOptions for popular option constants.
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionIntNative(
        level: Int32,
        option: Int32,
        value: IntNative
    ): Unit
    
    /**
    * Read the specified socket option returning it's value as a boolean value
    * converting it from an IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as 0 => false, other values => true.
    *
    * Throws an exception if failed (when getsockopt returns -1) or if the result
    * has size different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getSocketOptionBool(
        level: Int32,
        option: Int32
    ): Bool
    
    /**
    * Write a boolean value to the specified socket option converting it to IntNative.
    *
    * See SocketOptions for popular option constants.
    *
    * The conversion is defined as false => 0, true => 1
    *
    * Throws an exception if failed (when setsockopt returns -1), for example
    * when the option size is different from IntNative.
    */
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func setSocketOptionBool(
        level: Int32,
        option: Int32,
        value: Bool
    ): Unit
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public override func toString(): String
}

@!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
public class UnixSocketAddress <: SocketAddress & Equatable<UnixSocketAddress> {
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(path: Array<Byte>)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public init(path: String)
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop size: Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public prop family: AddressFamily
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func ==(rhs: UnixSocketAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public operator func !=(rhs: UnixSocketAddress): Bool
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func getAddressBytes(): Array<Byte>
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[since: "12", permission : "ohos.permission.GET_NETWORK_INFO" & "ohos.permission.INTERNET", atomicservice : true]
    public func toString(): String
}

