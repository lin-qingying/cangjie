// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.net

import std.core.*
import std.binary.SwapEndianOrder
import std.collection.HashSet
import std.sort.sort
import std.binary.*
import std.convert.*
import std.overflow.CheckedOp
import std.overflow.WrappingOp
import std.convert.Parsable
import std.binary.BigEndianOrder
import std.binary.LittleEndianOrder
import std.sync.*
import std.overflow.*
import std.io.*
import std.time.*
import std.math.*

let AI_PASSIVE: Int32 = COMPILED_CODE /* compiled code */

let AI_ALL: Int32 = COMPILED_CODE /* compiled code */

let DOMAIN_MAX_LEN: Int64 = COMPILED_CODE /* compiled code */

let SUB_DOMAIN_LIST_LEN: Int64 = COMPILED_CODE /* compiled code */

let IPV4_ADDRESS_LEN: Int64 = COMPILED_CODE /* compiled code */

let IPV6_ADDRESS_LEN: Int64 = COMPILED_CODE /* compiled code */

let SOCK_ADDR_IPV4_LEN: Int64 = COMPILED_CODE /* compiled code */

let SOCK_ADDR_IPV6_LEN: Int64 = COMPILED_CODE /* compiled code */

let SOCK_ADDR_DOMAIN_LEN: Int64 = COMPILED_CODE /* compiled code */

let SOCK_READ_BUFFER_SIZE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_WRITE_BUFFER_SIZE: Int32 = COMPILED_CODE /* compiled code */

let NULL_BYTE: std.core.String = COMPILED_CODE /* compiled code */

let IPV4_ADDR_LEN: Int64 = COMPILED_CODE /* compiled code */

let IPV6_ADDR_LEN: Int64 = COMPILED_CODE /* compiled code */

let AF_INET: Int16 = COMPILED_CODE /* compiled code */

let AF_UNIX: Int16 = COMPILED_CODE /* compiled code */

let AF_INET6: Int16 = COMPILED_CODE /* compiled code */

let IPPROTO_IP: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_ICMP: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_IPV4: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_TCP: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_UDP: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_IPV6: Int32 = COMPILED_CODE /* compiled code */

let IPPROTO_RAW: Int32 = COMPILED_CODE /* compiled code */

let SOL_SOCKET: Int32 = COMPILED_CODE /* compiled code */

let SOCK_DEBUG: Int32 = COMPILED_CODE /* compiled code */

let SOCK_ACCEPTCONN: Int32 = COMPILED_CODE /* compiled code */

let SOCK_REUSEADDR: Int32 = COMPILED_CODE /* compiled code */

let SOCK_KEEPALIVE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_DONTROUTE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_BROADCAST: Int32 = COMPILED_CODE /* compiled code */

let SOCK_OOBINLINE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_LINGER: Int32 = COMPILED_CODE /* compiled code */

let SOCK_SNDBUF: Int32 = COMPILED_CODE /* compiled code */

let SOCK_RCVBUF: Int32 = COMPILED_CODE /* compiled code */

let SOCK_SNDTIMEO: Int32 = COMPILED_CODE /* compiled code */

let SOCK_RCVTIMEO: Int32 = COMPILED_CODE /* compiled code */

let SOCK_ERROR: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TYPE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_REUSEPORT: Int32 = COMPILED_CODE /* compiled code */

let SOCK_BINDTODEVICE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TCP_NODELAY: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TCP_KEEPIDLE: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TCP_KEEPCNT: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TCP_KEEPINTVL: Int32 = COMPILED_CODE /* compiled code */

let SOCK_TCP_QUICKACK: Int32 = COMPILED_CODE /* compiled code */

let SOCK_IP_HDRINCL: Int32 = COMPILED_CODE /* compiled code */

let SOCK_IP_TOS: Int32 = COMPILED_CODE /* compiled code */

let SOCK_IP_TTL: Int32 = COMPILED_CODE /* compiled code */

let SOCKET_DEFAULT_BACKLOG: Int32 = COMPILED_CODE /* compiled code */

let SOCK_STREAM: Int32 = COMPILED_CODE /* compiled code */

let SOCK_DGRAM: Int32 = COMPILED_CODE /* compiled code */

let SOCK_RDM: Int32 = COMPILED_CODE /* compiled code */

let SOCK_SEQPACKET: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_ARG_INVALID: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_NOT_SUPPORTED: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_NOT_REGISTERED: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_HANDLE_INVALID: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_FD_NUM_OVER_LIMIT: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_TIMEOUT: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_CREATE: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_EAGAIN: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_SOCK_CLOSED: Int32 = COMPILED_CODE /* compiled code */

let ERRNO_EINVAL: Int32 = COMPILED_CODE /* compiled code */

let SOCK_LEN_MAX: Int64 = COMPILED_CODE /* compiled code */

let OS_WINDOWS: std.core.String = COMPILED_CODE /* compiled code */

let OS_MACOS: std.core.String = COMPILED_CODE /* compiled code */

let OS_OTHERS: std.core.String = COMPILED_CODE /* compiled code */

let MAX_TIMEOUT_NS: Int64 = COMPILED_CODE /* compiled code */

let OS: std.core.String = COMPILED_CODE /* compiled code */

public struct AddressFamily <: std.core.ToString & std.core.Equatable<std.net.AddressFamily> {
    public static const UNSPEC: std.net.AddressFamily = COMPILED_CODE /* compiled code */

    public static const UNIX: std.net.AddressFamily = COMPILED_CODE /* compiled code */

    public static const INET: std.net.AddressFamily = COMPILED_CODE /* compiled code */

    public static const INET6: std.net.AddressFamily = COMPILED_CODE /* compiled code */

    public let name: std.core.String

    public let value: UInt16

    init(name: std.core.String, value: UInt16) { /* compiled code */ }

    public operator func ==(rhs: std.net.AddressFamily): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.AddressFamily): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public interface StreamingSocket <: std.io.IOStream & std.core.Resource & std.core.ToString {
    public open prop localAddress: std.net.SocketAddress

    public open prop remoteAddress: std.net.SocketAddress

    public open mut prop readTimeout: std.core.Option<std.core.Duration>

    public open mut prop writeTimeout: std.core.Option<std.core.Duration>
}

public interface ServerSocket <: std.core.Resource & std.core.ToString {
    public open prop localAddress: std.net.SocketAddress

    public open func bind(): Unit

    public open func accept(timeout!: std.core.Option<std.core.Duration>): std.net.StreamingSocket

    public open func accept(): std.net.StreamingSocket { /* compiled code */ }
}

internal struct SockAddr {
    internal let ptr: std.core.CPointer<Unit>

    internal let len: UInt32

    init(buffer: std.core.CPointer<Unit>, length: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    init(sa: std.net.SocketAddress) { /* compiled code */ }

    public func free(): Unit { /* compiled code */ }

    internal prop family: Int16 {
        get() { /* compiled code */ }
    }

    internal func toSocketAddress(): std.net.SocketAddress { /* compiled code */ }

    internal func toRawAddress(): std.net.RawAddress { /* compiled code */ }
}

internal struct NonReentrantWriteLock <: std.sync.IReentrantMutex {
    init() { /* compiled code */ }

    public override func lock(): Unit { /* compiled code */ }

    public override func tryLock(): Bool { /* compiled code */ }

    public override func unlock(): Unit { /* compiled code */ }

    private let m: std.sync.Monitor

    private let useCounter: std.sync.AtomicInt64

    NonReentrantWriteLock(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

public struct SocketKeepAliveConfig <: std.core.ToString & std.core.Equatable<std.net.SocketKeepAliveConfig> {
    public let idle: std.core.Duration

    public let interval: std.core.Duration

    public let count: UInt32

    init(idle!: std.core.Duration = COMPILED_CODE /* compiled code */, interval!: std.core.Duration = COMPILED_CODE /* compiled code */, count!: UInt32 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    public override operator func ==(other: std.net.SocketKeepAliveConfig): Bool { /* compiled code */ }

    public override operator func !=(other: std.net.SocketKeepAliveConfig): Bool { /* compiled code */ }
}

internal struct NonReentrantReadWriteMutex {
    internal let readMutex: std.net.NonReentrantReadLock

    internal let writeMutex: std.net.NonReentrantWriteLock

    init() { /* compiled code */ }
}

internal struct ExternallyLockedLazy<T, Arg> {
    private var value: std.core.Option<T> = COMPILED_CODE /* compiled code */

    internal mut func compute(lock: std.net.NonReentrantReadWriteMutex, arg: Arg, provider: (Arg) -> T): T { /* compiled code */ }

    internal mut prop directValue: std.core.Option<T> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut func clearWithoutLock(): Unit { /* compiled code */ }

    internal mut func putValue(lock: std.net.NonReentrantReadWriteMutex, value: T): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

public abstract open class SocketAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.SocketAddress> & std.core.Hashable {
    public prop size: Int64

    public prop family: std.net.AddressFamily

    public func getAddressBytes(): std.core.Array<UInt8>

    public operator func ==(rhs: std.net.SocketAddress): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.SocketAddress): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class SocketHolder<NS> <: std.core.Object & std.core.Resource {
    private let socketCreationLock: std.net.NonReentrantWriteLock = COMPILED_CODE /* compiled code */

    private let useCounter: std.sync.AtomicUInt32 = COMPILED_CODE /* compiled code */

    private var state_: std.net.InternalHolderState<NS>

    init(acceptedSocket!: NS) { /* compiled code */ }

    internal prop state: std.net.SocketState {
        get() { /* compiled code */ }
    }

    internal func markConnected(): Unit { /* compiled code */ }

    internal func unmarkConnected(): Unit { /* compiled code */ }

    internal func markBound(): Unit { /* compiled code */ }

    internal func markFailed(): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public override func close(): Unit { /* compiled code */ }

    internal func use<R>(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    internal func read<R>(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    internal func write<R>(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    SocketHolder(create: () -> NS) { /* compiled code */ }
}

internal interface PlatformSocket<Self> {
    public open func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit

    public open func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64>

    public open func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64

    public open func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)>

    public open func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit

    public open func disconnect(): Unit

    public open func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound!: Bool): Unit

    public open func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<Self>

    public open func shutdown(): Unit

    public open func detectLocalAddress(): std.core.Option<std.net.SocketAddress>

    public open func detectRemoteAddress(): std.core.Option<std.net.SocketAddress>

    public open func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit

    public open func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit

    public open func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    public open func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    public open func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool

    public open func dispose(): Unit

    public static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Self
}

internal struct UInt128 {
    internal static prop Max: std.net.UInt128 {
        get() { /* compiled code */ }
    }

    internal static prop Min: std.net.UInt128 {
        get() { /* compiled code */ }
    }

    private let _hi: UInt64

    private let _lo: UInt64

    init(hi: UInt64, lo: UInt64) { /* compiled code */ }

    public func checkedShl(shiftcount: UInt64): std.core.Option<std.net.UInt128> { /* compiled code */ }

    public func checkedShr(shiftcount: UInt64): std.core.Option<std.net.UInt128> { /* compiled code */ }

    public operator func &(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    public operator func ^(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    public operator func |(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    public operator func !(): std.net.UInt128 { /* compiled code */ }

    public func toU16(): std.core.Array<UInt16> { /* compiled code */ }

    public static func fromU16(src: std.core.Array<UInt16>): std.net.UInt128 { /* compiled code */ }

    public static func readBigEndian(buf: std.core.Array<UInt8>): std.net.UInt128 { /* compiled code */ }

    public func writeBigEndian(buf: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readLittleEndian(buf: std.core.Array<UInt8>): std.net.UInt128 { /* compiled code */ }

    public func writeLittleEndian(buf: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func toBigEndian(): std.core.Array<UInt8> { /* compiled code */ }

    public func toLittleEndian(): std.core.Array<UInt8> { /* compiled code */ }

    public func swapBytes(): std.net.UInt128 { /* compiled code */ }

    public operator func ==(rhs: std.net.UInt128): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.UInt128): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal func isDomainValid(domainStr: std.core.String): Bool { /* compiled code */ }

internal func resolveDomain(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

public abstract open class IPAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.IPAddress> & std.core.Hashable & std.binary.BigEndianOrder<std.net.IPAddress> {
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop hostName: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
    }

    public static func parse(s: std.core.String): std.net.IPAddress { /* compiled code */ }

    public static func tryParse(s: std.core.String): std.core.Option<std.net.IPAddress> { /* compiled code */ }

    public static func resolve(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

    public static func resolve(domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

    public func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    public func isIPv4(): Bool { /* compiled code */ }

    public func isIPv6(): Bool { /* compiled code */ }

    public open func isUnspecified(): Bool { /* compiled code */ }

    public open func isLoopback(): Bool { /* compiled code */ }

    public open func isMulticast(): Bool

    public open func isPrivate(): Bool

    public open func isLinkLocal(): Bool

    public open func isGlobalUnicast(): Bool

    public open func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    public open func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPAddress { /* compiled code */ }

    public operator func ==(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func sortIPAddr(lhs: std.net.IPAddress, rhs: std.net.IPAddress): std.core.Ordering { /* compiled code */ }

public abstract open class IPPrefix <: std.core.Object & std.core.Equatable<std.net.IPPrefix> & std.core.Hashable & std.core.ToString {
    public prop address: std.net.IPAddress

    public prop prefixLength: UInt8

    public static func parse(s: std.core.String): std.net.IPPrefix { /* compiled code */ }

    public static func tryParse(s: std.core.String): std.core.Option<std.net.IPPrefix> { /* compiled code */ }

    public func contains(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    public func contains(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    public func overlaps(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    public open func netmask(): std.net.IPAddress { /* compiled code */ }

    public open func hostmask(): std.net.IPAddress { /* compiled code */ }

    public open func broadcast(): std.net.IPAddress { /* compiled code */ }

    public open func network(): std.net.IPAddress { /* compiled code */ }

    public open func masked(): std.net.IPPrefix { /* compiled code */ }

    public operator func ==(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    public func toString(): std.core.String

    init() { /* compiled code */ }
}

internal func parseIPv4(address: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv4Address> { /* compiled code */ }

internal enum Result<T> {
    | Ok(T)

    | Err(std.core.Exception)

    internal func isOk(): Bool { /* compiled code */ }

    internal func isErr(): Bool { /* compiled code */ }

    internal func unwrap(): T { /* compiled code */ }

    internal func unwrapErr(): std.core.Exception { /* compiled code */ }

    internal func toOption(): std.core.Option<T> { /* compiled code */ }
}

public class IPv4Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv4Address> & std.core.LessOrEqual<std.net.IPv4Address> {
    public static let localhost: std.net.IPv4Address

    public static let unspecified: std.net.IPv4Address

    public static let broadcast: std.net.IPv4Address

    internal var _octets: std.core.Array<UInt8>

    init(bits: UInt32) { /* compiled code */ }

    init(a: UInt8, b: UInt8, c: UInt8, d: UInt8) { /* compiled code */ }

    public func toBits(): UInt32 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv4Address { /* compiled code */ }

    public func isBroadcast(): Bool { /* compiled code */ }

    public func isGlobalUnicast(): Bool { /* compiled code */ }

    public func isLinkLocal(): Bool { /* compiled code */ }

    public func isLoopback(): Bool { /* compiled code */ }

    public func isMulticast(): Bool { /* compiled code */ }

    public func isPrivate(): Bool { /* compiled code */ }

    public func isUnspecified(): Bool { /* compiled code */ }

    public func toIPv6Compatible(): std.net.IPv6Address { /* compiled code */ }

    public func toIPv6Mapped(): std.net.IPv6Address { /* compiled code */ }

    public func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    public operator func <=(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    public operator func ==(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal func parseNumber(part: std.core.String, radix: UInt32, minDigits: UInt8, maxDigits: UInt8, allowZeroPrefix: Bool, origin: std.core.String): std.net.Result<(UInt32, Int64)> { /* compiled code */ }

internal func parseIPv6(src: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv6Address> { /* compiled code */ }

public class IPv6Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv6Address> & std.core.LessOrEqual<std.net.IPv6Address> {
    public static let localhost: std.net.IPv6Address

    public static let unspecified: std.net.IPv6Address

    internal var _octets: std.core.Array<UInt8>

    internal let _scopeId: std.core.Option<UInt32>

    init(bits: std.net.UInt128, scopeId!: std.core.Option<UInt32> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(octets: std.core.Array<UInt8>, scopeId!: std.core.Option<UInt32> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(a: UInt16, b: UInt16, c: UInt16, d: UInt16, e: UInt16, f: UInt16, g: UInt16, h: UInt16, scopeId!: std.core.Option<UInt32> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public prop scopeId: std.core.Option<UInt32> {
        get() { /* compiled code */ }
    }

    internal func toBits(): std.net.UInt128 { /* compiled code */ }

    internal func segments(): std.core.Array<UInt16> { /* compiled code */ }

    public func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    public func toIPv4(): std.core.Option<std.net.IPv4Address> { /* compiled code */ }

    public func toIPv4Mapped(): std.core.Option<std.net.IPv4Address> { /* compiled code */ }

    public func scope(scopeId: std.core.Option<UInt32>): std.net.IPv6Address { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv6Address { /* compiled code */ }

    public func isGlobalUnicast(): Bool { /* compiled code */ }

    public func isIPv4Mapped(): Bool { /* compiled code */ }

    public func isLinkLocal(): Bool { /* compiled code */ }

    public func isLoopback(): Bool { /* compiled code */ }

    public func isMulticast(): Bool { /* compiled code */ }

    public func isPrivate(): Bool { /* compiled code */ }

    public func isTeredo(): Bool { /* compiled code */ }

    public func isUnspecified(): Bool { /* compiled code */ }

    public operator func <=(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    public operator func ==(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func countZeros(segments: std.core.Array<UInt16>): (Int64, Int64) { /* compiled code */ }

    internal func u16ArrToHexString(sb: std.core.StringBuilder, chunk: std.core.Array<UInt16>): Unit { /* compiled code */ }
}

internal func innerParseIPv6(src: std.core.String, origin: std.core.String, e: std.core.Option<Int64>): std.net.Result<(Bool, Int64, Int64, std.core.Option<Int64>, std.core.Array<UInt8>)> { /* compiled code */ }

internal type sa_family_t = UInt16

internal type in_port_t = UInt16

internal type socklen_t = UInt32

internal type SockAddrByte = UInt8

internal struct AddrInfo {
    internal var ai_flags: Int32

    internal var ai_family: Int32

    internal var ai_socktype: Int32

    internal var ai_protocol: Int32

    internal var ai_addrlen: UInt32

    internal var ptr1: std.core.CPointer<Unit>

    internal var ptr2: std.core.CPointer<Unit>

    internal var ai_next: std.core.CPointer<std.net.AddrInfo>

    init() { /* compiled code */ }
}

internal struct SockAddrIn {
    internal var family: UInt16

    internal var port: UInt16

    internal var addr: UInt32

    internal let pad: Int64

    init() { /* compiled code */ }
}

internal struct SockAddrIn6 {
    internal var family: UInt16

    internal var port: UInt16

    internal var flowinfo: UInt32

    internal var addr: UInt32

    internal var pad1: UInt32

    internal var pad2: UInt32

    internal var pad3: UInt32

    internal var scope_id: UInt32

    init() { /* compiled code */ }
}

internal type PAddrinfo = std.core.CPointer<std.net.AddrInfo>

internal foreign func getaddrinfo(node: CString, service: std.core.CPointer<UInt8>, hints: std.core.CPointer<std.net.AddrInfo>, res: std.core.CPointer<std.core.CPointer<std.net.AddrInfo>>): Int32

internal foreign func freeaddrinfo(res: std.core.CPointer<std.net.AddrInfo>): Unit

internal foreign func CJ_SOCKET_GetAddrFromAddrinfo(info: std.core.CPointer<std.net.AddrInfo>): std.core.CPointer<UInt8>

internal struct SockAddrUn {
    internal var family: UInt16

    internal var sun_path: std.core.Array<UInt8>

    init() { /* compiled code */ }
}

internal class IPv4Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv4Prefix> & std.core.Hashable & std.core.ToString {
    internal let _ip: std.net.IPv4Address

    internal let _prefixLength: UInt8

    init(ip: std.net.IPv4Address, prefixLength: UInt8) { /* compiled code */ }

    public prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    public prop prefixLength: UInt8 {
        get() { /* compiled code */ }
    }

    public func contains(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    public func contains(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    public func overlaps(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    internal func hostmaskBits(): UInt32 { /* compiled code */ }

    internal func netmaskBits(): UInt32 { /* compiled code */ }

    public func netmask(): std.net.IPv4Address { /* compiled code */ }

    public func hostmask(): std.net.IPv4Address { /* compiled code */ }

    public func broadcast(): std.net.IPv4Address { /* compiled code */ }

    public func network(): std.net.IPv4Address { /* compiled code */ }

    public func masked(): std.net.IPv4Prefix { /* compiled code */ }

    internal func supernet(): std.net.IPv4Prefix { /* compiled code */ }

    public operator func ==(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal class IPv6Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv6Prefix> & std.core.Hashable & std.core.ToString {
    internal let _ip: std.net.IPv6Address

    internal let _prefixLength: UInt8

    init(ip: std.net.IPv6Address, prefixLength: UInt8) { /* compiled code */ }

    public prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    public prop prefixLength: UInt8 {
        get() { /* compiled code */ }
    }

    public func contains(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    public func contains(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    internal func overlaps(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    internal func hostmaskBits(): std.net.UInt128 { /* compiled code */ }

    internal func netmaskBits(): std.net.UInt128 { /* compiled code */ }

    public func netmask(): std.net.IPv6Address { /* compiled code */ }

    public func hostmask(): std.net.IPv6Address { /* compiled code */ }

    public func broadcast(): std.net.IPv6Address { /* compiled code */ }

    public func network(): std.net.IPv6Address { /* compiled code */ }

    public func masked(): std.net.IPv6Prefix { /* compiled code */ }

    internal func supernet(): std.net.IPv6Prefix { /* compiled code */ }

    public operator func ==(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class IPSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.IPSocketAddress> {
    internal let _addr: std.net.IPAddress

    internal let _port: UInt16

    init(address: std.core.Array<UInt8>, port: UInt16) { /* compiled code */ }

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.IPAddress, port: UInt16) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop family: std.net.AddressFamily {
        get() { /* compiled code */ }
    }

    public static func parse(s: std.core.String): std.net.IPSocketAddress { /* compiled code */ }

    public static func tryParse(s: std.core.String): std.core.Option<std.net.IPSocketAddress> { /* compiled code */ }

    public prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    public prop port: UInt16 {
        get() { /* compiled code */ }
    }

    public func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    public func isIPv4(): Bool { /* compiled code */ }

    public func isIPv6(): Bool { /* compiled code */ }

    public operator func ==(rhs: std.net.IPSocketAddress): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.IPSocketAddress): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal func innerParse(s: std.core.String): std.net.Result<(std.core.String, std.core.String, Bool)> { /* compiled code */ }

internal struct Linger {
    internal var enabled: UInt16

    internal var seconds: UInt16

    init(onOff: Bool, seconds: Int64) { /* compiled code */ }

    internal static const size: UIntNative = COMPILED_CODE /* compiled code */

    internal static func use<R>(block: (std.core.CPointer<std.net.Linger>) -> R): R { /* compiled code */ }
}

extend std.net.SocketCommon<ActualPlatformSocket> {
    internal func getLinger(): std.core.Option<std.core.Duration> { /* compiled code */ }

    internal func setLinger(newLinger: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }
}

internal class SocketCommon<NS> <: std.core.Object & std.core.Resource & std.core.ToString {
    private var readTimeout_: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */

    private var writeTimeout_: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */

    private var bindToDevice_: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */

    private var keepAlive_: std.core.Option<std.net.SocketKeepAliveConfig> = COMPILED_CODE /* compiled code */

    private let addressLock: std.net.NonReentrantReadWriteMutex = COMPILED_CODE /* compiled code */

    private var remoteAddress_: std.net.ExternallyLockedLazy<std.core.Option<std.net.SocketAddress>, std.net.SocketHolder<NS>> = COMPILED_CODE /* compiled code */

    private var localAddress_: std.net.ExternallyLockedLazy<std.core.Option<std.net.SocketAddress>, std.net.SocketHolder<NS>> = COMPILED_CODE /* compiled code */

    init(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode) { /* compiled code */ }

    internal mut prop localAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    private static func lazyLocalAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    internal mut prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    private static func lazyRemoteAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    internal mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal prop isBound: Bool {
        get() { /* compiled code */ }
    }

    internal prop isConnected: Bool {
        get() { /* compiled code */ }
    }

    internal func checkNotClosed(): Unit { /* compiled code */ }

    internal func checkNotBound(): Unit { /* compiled code */ }

    internal func checkNotConnected(): Unit { /* compiled code */ }

    internal mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal func write(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    internal func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    internal func send(payload: std.core.Array<UInt8>, destination: std.net.SocketAddress): Int64 { /* compiled code */ }

    internal func receiveFrom(buffer: std.core.Array<UInt8>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    internal func connect(timeout: std.core.Option<std.core.Duration>, shouldBeBound!: Bool = COMPILED_CODE /* compiled code */, mayBeAlreadyConnected!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func disconnect(): Unit { /* compiled code */ }

    internal func bind(backlogSize: std.core.Option<Int32>): Unit { /* compiled code */ }

    internal func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.SocketCommon<NS>> { /* compiled code */ }

    internal func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    internal func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    internal func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    internal func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    internal func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    internal func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public override func close(): Unit { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    private let holder: std.net.SocketHolder<NS>

    internal let kind: std.net.AddressFamily
}

internal struct NonReentrantReadLock <: std.sync.IReentrantMutex {
    public override func lock(): Unit { /* compiled code */ }

    public override func tryLock(): Bool { /* compiled code */ }

    public override func unlock(): Unit { /* compiled code */ }

    private let m: std.sync.Monitor

    private let useCounter: std.sync.AtomicInt64

    NonReentrantReadLock(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

extend std.net.Result<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

public interface DatagramSocket <: std.core.Resource & std.core.ToString {
    public open prop localAddress: std.net.SocketAddress

    public open prop remoteAddress: std.core.Option<std.net.SocketAddress>

    public open mut prop receiveTimeout: std.core.Option<std.core.Duration>

    public open mut prop sendTimeout: std.core.Option<std.core.Duration>

    public open func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64)

    public open func sendTo(address: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit
}

internal struct SocketBuffer {
    internal let rBufSize: Int32

    internal let wBufSize: Int32

    internal let rBuf: std.core.CPointer<UInt8>

    internal let wBuf: std.core.CPointer<UInt8>

    internal var handle: Int64

    internal let count: Int32

    init() { /* compiled code */ }
}

internal foreign func CJ_SOCKET_BufferInit(handle: Int64, rBufSize: Int32, wBufSize: Int32): std.core.CPointer<std.net.SocketBuffer>

internal foreign func CJ_SOCKET_BufferRead(sockBuf: std.core.CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, flags: Int32): Int32

internal foreign func CJ_SOCKET_BufferRecvFrom(sockBuf: std.core.CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, source: std.core.CPointer<std.net.SockAddr>, flags: Int32): Int32

internal foreign func CJ_SOCKET_BufferWrite(sockBuf: std.core.CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, flags: Int32): Int32

internal foreign func CJ_SOCKET_BufferSendto(sockBuf: std.core.CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, dest: std.core.CPointer<std.net.SockAddr>, flags: Int32): Int32

internal foreign func CJ_SOCKET_BufferRCopy(sockBuf: std.core.CPointer<std.net.SocketBuffer>, arrBuf: std.core.CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

internal foreign func CJ_SOCKET_BufferWCopy(sockBuf: std.core.CPointer<std.net.SocketBuffer>, arrBuf: std.core.CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

internal foreign func CJ_SOCKET_BufferClose(sockBuf: std.core.CPointer<std.net.SocketBuffer>, handle: Int64): Int32

public enum SocketNet <: std.core.ToString & std.core.Equatable<std.net.SocketNet> {
    | TCP

    | UDP

    | UNIX

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(that: std.net.SocketNet): Bool { /* compiled code */ }

    public operator func !=(that: std.net.SocketNet): Bool { /* compiled code */ }
}

internal enum SocketMode <: std.core.Equal<std.net.SocketMode> {
    | StreamingMode

    | DatagramMode

    | SequentialMode

    public override operator func ==(other: std.net.SocketMode): Bool { /* compiled code */ }
}

public class SocketException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }

    internal static func throwClosedException(): Nothing { /* compiled code */ }

    internal static func notYetConnected(): Nothing { /* compiled code */ }

    internal static func notYetBound(): Nothing { /* compiled code */ }

    internal static func notYetBoundNeedBind(): Nothing { /* compiled code */ }

    internal static func alreadyBound(): Nothing { /* compiled code */ }

    internal static func alreadyConnected(): Nothing { /* compiled code */ }
}

public class SocketTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

let SOCKET_STREAM: Int32

let SOCKET_DGRAM: Int32

let SOCKET_RAW: Int32

let SOCKET_SEQPACKET: Int32

internal struct SockKeepAliveCfgS {
    internal var keep_alive: UInt32

    internal var idle: UInt32

    internal var interval: UInt32

    internal var count: UInt32

    init(keep_alive!: UInt32, idle!: UInt32, interval!: UInt32, count!: UInt32) { /* compiled code */ }
}

internal foreign func CJ_SockErrnoGet(): Int32

internal foreign func CJ_SockKeepAliveSet(sock: Int64, cfg: std.core.CPointer<std.net.SockKeepAliveCfgS>): Int32

internal foreign func CJ_SockOptionSet(sock: Int64, level: Int32, optname: Int32, optval: std.core.CPointer<Unit>, oplen: Int32): Int32

internal foreign func CJ_SockOptionGet(sock: Int64, level: Int32, optname: Int32, optval: std.core.CPointer<Unit>, oplen: std.core.CPointer<Int32>): Int32

internal foreign func CJ_SOCKET_GetErrMessage(errno: Int32): CString

internal foreign func CJ_GetOptionInt(sock: Int64, level: Int32, optname: Int32): Int32

internal foreign func CJ_SetOptionInt(sock: Int64, level: Int32, optname: Int32, optval: Int32): Int32

internal foreign func CJ_RawAddressCreate(arr: std.core.CPointer<UInt8>, size: Int32): std.core.CPointer<std.net.SockAddr>

internal foreign func CJ_RawAddressLenGet(addrPtr: std.core.CPointer<std.net.SockAddr>): UIntNative

internal foreign func CJ_RawAddressDestroy(addrPtr: std.core.CPointer<std.net.SockAddr>): Unit

internal func getAddrPointer(addr: std.core.Array<UInt8>): (std.core.CPointerHandle<UInt8>, std.core.CPointer<std.net.SockAddr>) { /* compiled code */ }

internal func releaseAddrPointer(handle: std.core.CPointerHandle<UInt8>, addrPtr: std.core.CPointer<std.net.SockAddr>): Unit { /* compiled code */ }

internal foreign func CJ_MRT_TcpsockInit(): Int32

internal foreign func CJ_MRT_UdpsockInit(): Int32

internal foreign func CJ_MRT_RawsockInit(): Int32

let CJ_MRT_TCP_SOCK_INIT: Int32

let CJ_MRT_UDP_SOCK_INIT: Int32

let CJ_MRT_RAW_SOCK_INIT: Int32

internal func localAddrGet(handle: Int64, addr: std.core.CPointer<std.net.SockAddr>): Int32 { /* compiled code */ }

internal func peerAddrGet(handle: Int64, addr: std.core.CPointer<std.net.SockAddr>): Int32 { /* compiled code */ }

internal func getAddress(handle: Int64, getAddressFunc: (Int64, std.core.CPointer<std.net.SockAddr>) -> Int32, msg: std.core.String): std.net.SockAddr { /* compiled code */ }

internal func getLocalAddress(handle: Int64): std.net.SocketAddress { /* compiled code */ }

internal func getRemoteAddress(handle: Int64): std.net.SocketAddress { /* compiled code */ }

internal foreign func CJ_MRT_SockCreate(domain: Int32, sock_type: Int32, protocol: Int32, net: CString): Int64

internal foreign func CJ_MRT_SockBind(sock: Int64, addr: std.core.CPointer<std.net.SockAddr>, backlog: Int32): Int64

internal foreign func CJ_MRT_SockAccept(sock: Int64, addr: std.core.CPointer<std.net.SockAddr>): Int64

internal foreign func CJ_MRT_SockAcceptTimeout(sock: Int64, addr: std.core.CPointer<std.net.SockAddr>, timeout: UInt64): Int64

internal foreign func CJ_MRT_SockConnect(sock: Int64, local_addr: std.core.CPointer<std.net.SockAddr>, peer_addr: std.core.CPointer<std.net.SockAddr>): Int64

internal foreign func CJ_MRT_SockConnectTimeout(sock: Int64, local_addr: std.core.CPointer<std.net.SockAddr>, peer_addr: std.core.CPointer<std.net.SockAddr>, timeout: UInt64): Int64

internal foreign func CJ_MRT_SockDisconnect(sock: Int64, isIpv6: Bool): Int32

internal foreign func CJ_MRT_SockClose(sock: Int64): Int32

internal foreign func CJ_MRT_SockShutdown(sock: Int64): Int32

internal foreign func CJ_MRT_SockSend(sock: Int64, buf: std.core.CPointer<UInt8>, length: UInt32, flags: Int32): Int32

internal foreign func CJ_MRT_SockSendTimeout(sock: Int64, buf: std.core.CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

internal foreign func CJ_MRT_SockSendNonBlock(handle: Int64, buf: std.core.CPointer<UInt8>, len: Int32, flags: Int32): Int32

internal foreign func CJ_MRT_SockRecv(sock: Int64, buf: std.core.CPointer<UInt8>, length: UInt32, flags: Int32): Int32

internal foreign func CJ_MRT_SockRecvTimeout(sock: Int64, buf: std.core.CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

internal foreign func CJ_MRT_SockRecvNonBlock(handle: Int64, buf: std.core.CPointer<UInt8>, len: Int32, flags: Int32): Int32

internal foreign func CJ_MRT_SockRecvfromTimeout(sock: Int64, buf: std.core.CPointer<UInt8>, length: UInt32, flags: Int32, addr: std.core.CPointer<std.net.SockAddr>, timeout: UInt64): Int32

internal foreign func CJ_MRT_SockWaitRecv(handle: Int64): Int32

internal foreign func CJ_MRT_SockWaitRecvTimeout(handle: Int64, timeout: UInt64): Int32

internal foreign func CJ_MRT_SockWaitSend(handle: Int64): Int32

internal foreign func CJ_MRT_SockWaitSendTimeout(handle: Int64, timeout: UInt64): Int32

internal foreign func CJ_MRT_SockListen(sockfd: Int64, backlog: Int32): Int64

internal foreign func CJ_MRT_SockSendtoNonBlock(sockfd: Int64, buf: std.core.CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: std.core.CPointer<std.net.SockAddr>): Int32

internal foreign func CJ_MRT_SockRecvfromNonBlock(sockfd: Int64, buf: std.core.CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: std.core.CPointer<std.net.SockAddr>): Int32

internal foreign func CJ_MRT_SockLocalAddrGet(sock: Int64, addr: std.core.CPointer<std.net.SockAddr>): Int32

internal foreign func CJ_MRT_SockPeerAddrGet(sock: Int64, addr: std.core.CPointer<std.net.SockAddr>): Int32

internal enum ErrnoLabel <: std.core.ToString {
    | Accept

    | Connect

    | Read

    | Write

    | CreateSock

    | Bind

    | Listen

    public override func toString(): std.core.String { /* compiled code */ }
}

internal func getSocketError(): Int32 { /* compiled code */ }

internal func socketProcessErrno(label: std.net.ErrnoLabel): Nothing { /* compiled code */ }

internal func sockError(errno: Int32): std.core.String { /* compiled code */ }

internal func isSocketErrorInvalidArgument(errno: Int32): Bool { /* compiled code */ }

internal type ActualPlatformSocket = std.net.DopraOtherSocketImpl

internal class DopraOtherSocketImpl <: std.net.DopraSocketImpl<std.net.DopraOtherSocketImpl> {
    private let socketBufferPtr: std.core.CPointer<std.net.SocketBuffer>

    public override func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    public override func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64> { /* compiled code */ }

    public override func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraOtherSocketImpl> { /* compiled code */ }

    public override func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    public override func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64 { /* compiled code */ }

    public override func shutdown(): Unit { /* compiled code */ }

    public override func dispose(): Unit { /* compiled code */ }

    public redef static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraOtherSocketImpl { /* compiled code */ }

    protected override func onHandleReplaced(newHandle: Int64): Unit { /* compiled code */ }
}

internal type ActualTcpPlatformSocket = std.net.DopraOtherSocketImpl

internal abstract class DopraSocketImpl<Self> <: std.core.Object & std.net.PlatformSocket<Self> {
    protected var address: std.core.Option<std.net.SockAddr> = COMPILED_CODE /* compiled code */

    private var isIPv6: Bool = COMPILED_CODE /* compiled code */

    protected mut prop handle: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    protected func onHandleReplaced(newHandle: Int64): Unit

    public override func disconnect(): Unit { /* compiled code */ }

    public override func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound!: Bool): Unit { /* compiled code */ }

    protected func acceptImpl(timeout: std.core.Option<std.core.Duration>): (Int64, std.net.SocketNet) { /* compiled code */ }

    public open override func shutdown(): Unit { /* compiled code */ }

    public override func detectLocalAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    public override func detectRemoteAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    public override func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public override func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public override func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public override func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public override func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool { /* compiled code */ }

    public redef static func create(_: std.net.SocketNet, _: std.net.AddressFamily, _: std.net.SocketMode): Self { /* compiled code */ }

    protected static func createSocket(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Int64 { /* compiled code */ }

    internal static func toDopraTimeout(timeout: std.core.Duration): UInt64 { /* compiled code */ }

    protected var _handle: std.sync.AtomicInt64

    DopraSocketImpl(_handle: std.sync.AtomicInt64) { /* compiled code */ }
}

internal class DopraTcpSocketImpl <: std.net.DopraSocketImpl<std.net.DopraTcpSocketImpl> {
    public override func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    public override func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64> { /* compiled code */ }

    public override func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraTcpSocketImpl> { /* compiled code */ }

    public override func send(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>, _: std.net.SocketAddress): Int64 { /* compiled code */ }

    public override func receiveFrom(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    public override func dispose(): Unit { /* compiled code */ }

    protected override func onHandleReplaced(_: Int64): Unit { /* compiled code */ }

    public redef static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraTcpSocketImpl { /* compiled code */ }
}

internal enum DopraAsyncResult {
    | BytesTransferred(UInt32)

    | RetryAgain

    | EOF
}

internal func setFamily(ptr: std.core.CPointer<Unit>, v: Int16): Unit { /* compiled code */ }

internal func readFamily(ptr: std.core.CPointer<Unit>): Int16 { /* compiled code */ }

public struct RawAddress {
    internal let addr_: std.core.Array<UInt8>

    public prop addr: std.core.Array<UInt8> {
        get() { /* compiled code */ }
    }

    init(addr: std.core.Array<UInt8>) { /* compiled code */ }
}

internal enum InternalHolderState<NS> {
    | NotCreated(() -> NS)

    | Ready(NS)

    | Disposed
}

internal enum HolderState <: std.core.Equatable<std.net.HolderState> {
    | SocketNotCreated

    | SocketPrepared

    | SocketClosing

    | SocketDisposed

    public override operator func ==(other: std.net.HolderState): Bool { /* compiled code */ }

    public override operator func !=(other: std.net.HolderState): Bool { /* compiled code */ }
}

internal struct SocketState {
    internal static let Initial: std.net.SocketState

    internal static let Terminal: std.net.SocketState

    internal static let JustAccepted: std.net.SocketState

    internal mut prop useCounter: UInt32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal prop disposedOrShuttingDown: Bool {
        get() { /* compiled code */ }
    }

    internal mut prop nativeState: std.net.HolderState {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop connected: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop bound: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop hasReaders: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop hasWriters: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal mut prop failed: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal func ensureNotClosed(): Unit { /* compiled code */ }

    internal func ensureConnected(): Unit { /* compiled code */ }

    internal func ensureNotConnected(): Unit { /* compiled code */ }

    internal func ensureBound(): Unit { /* compiled code */ }

    internal func ensureBindInvoked(): Unit { /* compiled code */ }

    internal func ensureNotBound(): Unit { /* compiled code */ }

    internal var value: UInt32

    SocketState(value: UInt32) { /* compiled code */ }
}

public struct SocketOptions {
    @Deprecated[message: "Use `public static const SOCKET: Int32` in `public struct OptionLevel` instead."]
    public static const SOL_SOCKET: Int32 = COMPILED_CODE /* compiled code */

    @Deprecated[message: "Use `public static const TCP: Int32` in `public struct OptionLevel` instead."]
    public static const IPPROTO_TCP: Int32 = COMPILED_CODE /* compiled code */

    @Deprecated[message: "Use `public static const UDP: Int32` in `public struct OptionLevel` instead."]
    public static const IPPROTO_UDP: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_KEEPALIVE: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_NODELAY: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_QUICKACK: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_LINGER: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_SNDBUF: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_RCVBUF: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_REUSEADDR: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_REUSEPORT: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_BINDTODEVICE: Int32 = COMPILED_CODE /* compiled code */
}

public class RawSocket {
    internal let sockImpl: std.net.RawSocketImpl

    private var isClosed: Bool

    private var readTimeout_: std.core.Option<std.core.Duration>

    private var writeTimeout_: std.core.Option<std.core.Duration>

    internal var localAddr_: std.core.Option<std.net.RawAddress>

    internal var remoteAddr_: std.core.Option<std.net.RawAddress>

    internal prop sockFd: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop localAddress: RawAddress` instead."]
    public prop localAddr: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    public prop localAddress: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use `public prop remoteAddress: RawAddress` instead."]
    public prop remoteAddr: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    public prop remoteAddress: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    public mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    init(domain: std.net.SocketDomain, `type`: std.net.SocketType, protocol: std.net.ProtocolType) { /* compiled code */ }

    public func bind(addr: std.net.RawAddress): Unit { /* compiled code */ }

    public func listen(backlog: Int32): Unit { /* compiled code */ }

    public func accept(timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): std.net.RawSocket { /* compiled code */ }

    public func connect(addr: std.net.RawAddress, timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public func sendTo(addr: std.net.RawAddress, buffer: std.core.Array<UInt8>, flags: Int32): Unit { /* compiled code */ }

    public func receiveFrom(buffer: std.core.Array<UInt8>, flags: Int32): (std.net.RawAddress, Int64) { /* compiled code */ }

    public func send(buffer: std.core.Array<UInt8>, flags: Int32): Unit { /* compiled code */ }

    public func receive(buffer: std.core.Array<UInt8>, flags: Int32): Int64 { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<UInt8>, len: Int32): Unit { /* compiled code */ }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<UInt8>, len: std.core.CPointer<Int32>): Unit { /* compiled code */ }
}

internal class RawSocketImpl {
    internal var sockFd: Int64

    internal var isBound: Bool

    protected var socketBufferPtr: std.core.CPointer<std.net.SocketBuffer>

    internal var bufferInitialized: std.sync.AtomicBool

    init(sockFd: Int64) { /* compiled code */ }

    internal func bufferInit(): Unit { /* compiled code */ }

    internal func bind(addr: std.net.RawAddress): Unit { /* compiled code */ }

    internal func connect(addr: std.net.RawAddress, timeout: Int64): Unit { /* compiled code */ }

    internal func sendTo(addr: std.core.Array<UInt8>, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit { /* compiled code */ }

    internal func send(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit { /* compiled code */ }

    internal func receiveFrom(size: Int64, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): (std.net.RawAddress, Int64) { /* compiled code */ }

    internal func receive(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Int64 { /* compiled code */ }

    internal func isConnectionOriented(sockType: Int32): Bool { /* compiled code */ }

    internal func close(): Unit { /* compiled code */ }
}

public struct SocketDomain <: std.core.Equatable<std.net.SocketDomain> & std.core.ToString & std.core.Hashable {
    internal let val: Int32

    public static let IPV4: std.net.SocketDomain

    public static let IPV6: std.net.SocketDomain

    public static let UNIX: std.net.SocketDomain

    init(domain: Int32) { /* compiled code */ }

    public operator func ==(r: std.net.SocketDomain): Bool { /* compiled code */ }

    public operator func !=(r: std.net.SocketDomain): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

public struct SocketType <: std.core.Equatable<std.net.SocketType> & std.core.ToString & std.core.Hashable {
    internal let val: Int32

    public static let STREAM: std.net.SocketType

    public static let DATAGRAM: std.net.SocketType

    public static let RAW: std.net.SocketType

    public static let SEQPACKET: std.net.SocketType

    init(`type`: Int32) { /* compiled code */ }

    public operator func ==(r: std.net.SocketType): Bool { /* compiled code */ }

    public operator func !=(r: std.net.SocketType): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

public struct ProtocolType <: std.core.Equatable<std.net.ProtocolType> & std.core.ToString & std.core.Hashable {
    internal let val: Int32

    public static let Unspecified: std.net.ProtocolType

    public static let IPV4: std.net.ProtocolType

    public static let IPV6: std.net.ProtocolType

    public static let ICMP: std.net.ProtocolType

    public static let TCP: std.net.ProtocolType

    public static let UDP: std.net.ProtocolType

    public static let RAW: std.net.ProtocolType

    init(protocol: Int32) { /* compiled code */ }

    public operator func ==(r: std.net.ProtocolType): Bool { /* compiled code */ }

    public operator func !=(r: std.net.ProtocolType): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }
}

internal func getSocketOptionInt32(sockFd: Int64, level: Int32, option: Int32): Int32 { /* compiled code */ }

internal func getOnceSendSize(size: Int64): Int64 { /* compiled code */ }

public struct OptionLevel {
    public static const IP: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP: Int32 = COMPILED_CODE /* compiled code */

    public static const UDP: Int32 = COMPILED_CODE /* compiled code */

    public static const ICMP: Int32 = COMPILED_CODE /* compiled code */

    public static const RAW: Int32 = COMPILED_CODE /* compiled code */

    public static const SOCKET: Int32 = COMPILED_CODE /* compiled code */

    init() { /* compiled code */ }
}

public struct OptionName {
    public static const IP_HDRINCL: Int32 = COMPILED_CODE /* compiled code */

    public static const IP_TOS: Int32 = COMPILED_CODE /* compiled code */

    public static const IP_TTL: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_KEEPCNT: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_KEEPIDLE: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_KEEPINTVL: Int32 = COMPILED_CODE /* compiled code */

    public static const TCP_NODELAY: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_DEBUG: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_ACCEPTCONN: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_REUSEADDR: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_KEEPALIVE: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_DONTROUTE: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_BROADCAST: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_LINGER: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_OOBINLINE: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_SNDBUF: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_RCVBUF: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_SNDTIMEO: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_RCVTIMEO: Int32 = COMPILED_CODE /* compiled code */

    public static const SO_ERROR: Int32 = COMPILED_CODE /* compiled code */

    init() { /* compiled code */ }
}

internal func getTimeout(timeout: std.core.Option<std.core.Duration>): Int64 { /* compiled code */ }

let MAX_TIMEOUT_DURATION: std.core.Duration

internal func unwrapOption<T>(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T> { /* compiled code */ }

internal func getOrFail<T>(e: () -> std.core.Exception): T { /* compiled code */ }

internal func formatError(errnum: Int32): std.core.String { /* compiled code */ }

internal func throwIfIPv4ZeroOnWindows(socketAddress: std.net.SocketAddress): Unit { /* compiled code */ }

internal func zeroMemory(buffer: std.core.CPointer<UInt8>, size: Int64): Unit { /* compiled code */ }

internal func resolveHelper(address: std.core.String): std.net.IPAddress { /* compiled code */ }

public class TcpSocket <: std.core.Object & std.net.StreamingSocket & std.core.Equatable<std.net.TcpSocket> & std.core.Hashable {
    private let id: Int64

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.SocketAddress) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress!: std.core.Option<std.net.SocketAddress>) { /* compiled code */ }

    public override prop remoteAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop noDelay: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop quickAcknowledge: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop linger: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public override func write(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func connect(timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public override operator func ==(other: std.net.TcpSocket): Bool { /* compiled code */ }

    public override operator func !=(other: std.net.TcpSocket): Bool { /* compiled code */ }

    public override func hashCode(): Int64 { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    TcpSocket(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }
}

public class TcpServerSocket <: std.core.Object & std.net.ServerSocket {
    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    private var backlogSize_: Int32

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    init(bindAt!: UInt16) { /* compiled code */ }

    init(bindAt!: std.net.SocketAddress) { /* compiled code */ }

    public mut prop reuseAddress: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop reusePort: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop backlogSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func bind(): Unit { /* compiled code */ }

    public override func accept(timeout!: std.core.Option<std.core.Duration>): std.net.TcpSocket { /* compiled code */ }

    public override func accept(): std.net.TcpSocket { /* compiled code */ }

    public override func close(): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

public class UdpSocket <: std.core.Object & std.net.DatagramSocket {
    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt!: UInt16) { /* compiled code */ }

    init(bindAt!: std.net.SocketAddress) { /* compiled code */ }

    public override prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
    }

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override mut prop receiveTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override mut prop sendTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func close(): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public func bind(): Unit { /* compiled code */ }

    public func connect(remote: std.net.SocketAddress): Unit { /* compiled code */ }

    public func disconnect(): Unit { /* compiled code */ }

    public override func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64) { /* compiled code */ }

    public override func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func send(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func receive(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public mut prop reusePort: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop reuseAddress: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

internal func checkUnixSocketAddress(address: std.net.SocketAddress, name: std.core.String): std.net.SocketAddress { /* compiled code */ }

public class UnixSocket <: std.core.Object & std.net.StreamingSocket {
    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(path: std.core.String, localPath!: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress!: std.core.Option<std.net.SocketAddress> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }

    public override prop remoteAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public override func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func connect(timeout!: std.core.Option<std.core.Duration> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

public class UnixServerSocket <: std.core.Object & std.net.ServerSocket {
    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    private var backlogSize_: Int32

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    init(bindAt!: std.core.String) { /* compiled code */ }

    init(bindAt!: std.net.SocketAddress) { /* compiled code */ }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop backlogSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func bind(): Unit { /* compiled code */ }

    public override func accept(timeout!: std.core.Option<std.core.Duration>): std.net.UnixSocket { /* compiled code */ }

    public override func accept(): std.net.UnixSocket { /* compiled code */ }

    public override func close(): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

public class UnixDatagramSocket <: std.core.Object & std.net.DatagramSocket {
    private let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt!: std.core.String) { /* compiled code */ }

    init(bindAt!: std.net.SocketAddress) { /* compiled code */ }

    public override prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
    }

    public override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    public override mut prop receiveTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override mut prop sendTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public override func close(): Unit { /* compiled code */ }

    public override func isClosed(): Bool { /* compiled code */ }

    public func bind(): Unit { /* compiled code */ }

    public func connect(remotePath: std.core.String): Unit { /* compiled code */ }

    public func connect(remote: std.net.SocketAddress): Unit { /* compiled code */ }

    public func disconnect(): Unit { /* compiled code */ }

    public override func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64) { /* compiled code */ }

    public override func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func send(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func receive(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func getSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: std.core.CPointer<UIntNative>): Unit { /* compiled code */ }

    public func setSocketOption(level: Int32, option: Int32, value: std.core.CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    public func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    public func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    public func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    public func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

internal func findLastNoneZeroIdx(path: std.core.Array<UInt8>): Int64 { /* compiled code */ }

public class UnixSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.UnixSocketAddress> {
    internal let _path: std.core.String

    internal let _len: Int64

    init(path: std.core.Array<UInt8>) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop family: std.net.AddressFamily {
        get() { /* compiled code */ }
    }

    public operator func ==(rhs: std.net.UnixSocketAddress): Bool { /* compiled code */ }

    public operator func !=(rhs: std.net.UnixSocketAddress): Bool { /* compiled code */ }

    public func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}
