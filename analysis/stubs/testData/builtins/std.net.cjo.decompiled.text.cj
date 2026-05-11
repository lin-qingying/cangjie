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

let AI_PASSIVE: Int32

let AI_ALL: Int32

let DOMAIN_MAX_LEN: Int64

let SUB_DOMAIN_LIST_LEN: Int64

let IPV4_ADDRESS_LEN: Int64

let IPV6_ADDRESS_LEN: Int64

let SOCK_ADDR_IPV4_LEN: Int64

let SOCK_ADDR_IPV6_LEN: Int64

let SOCK_ADDR_DOMAIN_LEN: Int64

let SOCK_READ_BUFFER_SIZE: Int32

let SOCK_WRITE_BUFFER_SIZE: Int32

let NULL_BYTE: std.core.String

let IPV4_ADDR_LEN: Int64

let IPV6_ADDR_LEN: Int64

let AF_INET: Int16

let AF_UNIX: Int16

let AF_INET6: Int16

let IPPROTO_IP: Int32

let IPPROTO_ICMP: Int32

let IPPROTO_IPV4: Int32

let IPPROTO_TCP: Int32

let IPPROTO_UDP: Int32

let IPPROTO_IPV6: Int32

let IPPROTO_RAW: Int32

let SOL_SOCKET: Int32

let SOCK_DEBUG: Int32

let SOCK_ACCEPTCONN: Int32

let SOCK_REUSEADDR: Int32

let SOCK_KEEPALIVE: Int32

let SOCK_DONTROUTE: Int32

let SOCK_BROADCAST: Int32

let SOCK_OOBINLINE: Int32

let SOCK_LINGER: Int32

let SOCK_SNDBUF: Int32

let SOCK_RCVBUF: Int32

let SOCK_SNDTIMEO: Int32

let SOCK_RCVTIMEO: Int32

let SOCK_ERROR: Int32

let SOCK_TYPE: Int32

let SOCK_REUSEPORT: Int32

let SOCK_BINDTODEVICE: Int32

let SOCK_TCP_NODELAY: Int32

let SOCK_TCP_KEEPIDLE: Int32

let SOCK_TCP_KEEPCNT: Int32

let SOCK_TCP_KEEPINTVL: Int32

let SOCK_TCP_QUICKACK: Int32

let SOCK_IP_HDRINCL: Int32

let SOCK_IP_TOS: Int32

let SOCK_IP_TTL: Int32

let SOCKET_DEFAULT_BACKLOG: Int32

let SOCK_STREAM: Int32

let SOCK_DGRAM: Int32

let SOCK_RDM: Int32

let SOCK_SEQPACKET: Int32

let ERRNO_SOCK_ARG_INVALID: Int32

let ERRNO_SOCK_NOT_SUPPORTED: Int32

let ERRNO_SOCK_NOT_REGISTERED: Int32

let ERRNO_SOCK_HANDLE_INVALID: Int32

let ERRNO_SOCK_FD_NUM_OVER_LIMIT: Int32

let ERRNO_SOCK_TIMEOUT: Int32

let ERRNO_SOCK_CREATE: Int32

let ERRNO_SOCK_EAGAIN: Int32

let ERRNO_SOCK_CLOSED: Int32

let ERRNO_EINVAL: Int32

let SOCK_LEN_MAX: Int64

let OS_WINDOWS: std.core.String

let OS_MACOS: std.core.String

let OS_OTHERS: std.core.String

let MAX_TIMEOUT_NS: Int64

let OS: std.core.String

struct AddressFamily <: std.core.ToString & std.core.Equatable<std.net.AddressFamily> {
    let UNSPEC: std.net.AddressFamily

    let UNIX: std.net.AddressFamily

    let INET: std.net.AddressFamily

    let INET6: std.net.AddressFamily

    let name: std.core.String

    let value: UInt16

    init(name: std.core.String, value: UInt16) { /* compiled code */ }

    operator func ==(rhs: std.net.AddressFamily): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.AddressFamily): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

interface StreamingSocket <: std.io.IOStream & std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    prop remoteAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }
}

interface ServerSocket <: std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    func bind(): Unit { /* compiled code */ }

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.StreamingSocket { /* compiled code */ }

    func accept(): std.net.StreamingSocket { /* compiled code */ }
}

struct SockAddr {
    let ptr: CPointer<Unit>

    let len: UInt32

    init(buffer: CPointer<Unit>, length: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    init(sa: std.net.SocketAddress) { /* compiled code */ }

    func free(): Unit { /* compiled code */ }

    prop family: Int16 {
        get() { /* compiled code */ }
    }

    func toSocketAddress(): std.net.SocketAddress { /* compiled code */ }

    func toRawAddress(): std.net.RawAddress { /* compiled code */ }
}

struct NonReentrantWriteLock <: std.sync.IReentrantMutex {
    init() { /* compiled code */ }

    override func lock(): Unit { /* compiled code */ }

    override func tryLock(): Bool { /* compiled code */ }

    override func unlock(): Unit { /* compiled code */ }

    let m: std.sync.Monitor

    let useCounter: std.sync.AtomicInt64

    init(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

struct SocketKeepAliveConfig <: std.core.ToString & std.core.Equatable<std.net.SocketKeepAliveConfig> {
    let idle: std.core.Duration

    let interval: std.core.Duration

    let count: UInt32

    init(idle: std.core.Duration, interval: std.core.Duration, count: UInt32) { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    override operator func ==(other: std.net.SocketKeepAliveConfig): Bool { /* compiled code */ }

    override operator func !=(other: std.net.SocketKeepAliveConfig): Bool { /* compiled code */ }
}

struct NonReentrantReadWriteMutex {
    let readMutex: std.net.NonReentrantReadLock

    let writeMutex: std.net.NonReentrantWriteLock

    init() { /* compiled code */ }
}

struct ExternallyLockedLazy {
    var value: std.core.Option<T>

    mut func compute(lock: std.net.NonReentrantReadWriteMutex, arg: Arg, provider: (Arg) -> T): T { /* compiled code */ }

    mut prop directValue: std.core.Option<T> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut func clearWithoutLock(): Unit { /* compiled code */ }

    mut func putValue(lock: std.net.NonReentrantReadWriteMutex, value: T): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class SocketAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.SocketAddress> & std.core.Hashable {
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop family: std.net.AddressFamily {
        get() { /* compiled code */ }
    }

    func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    operator func ==(rhs: std.net.SocketAddress): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.SocketAddress): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

class SocketHolder <: std.core.Object & std.core.Resource {
    let socketCreationLock: std.net.NonReentrantWriteLock

    let useCounter: std.sync.AtomicUInt32

    var state_: std.net.InternalHolderState<NS>

    init(acceptedSocket: NS) { /* compiled code */ }

    prop state: std.net.SocketState {
        get() { /* compiled code */ }
    }

    func markConnected(): Unit { /* compiled code */ }

    func unmarkConnected(): Unit { /* compiled code */ }

    func markBound(): Unit { /* compiled code */ }

    func markFailed(): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    override func close(): Unit { /* compiled code */ }

    func use(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    func read(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    func write(block: (NS, std.net.SocketState) -> R): std.core.Option<R> { /* compiled code */ }

    init(create: () -> NS) { /* compiled code */ }
}

interface PlatformSocket {
    func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64> { /* compiled code */ }

    func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64 { /* compiled code */ }

    func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func disconnect(): Unit { /* compiled code */ }

    func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound: Bool): Unit { /* compiled code */ }

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<Self> { /* compiled code */ }

    func shutdown(): Unit { /* compiled code */ }

    func detectLocalAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    func detectRemoteAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool { /* compiled code */ }

    func dispose(): Unit { /* compiled code */ }

    static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Self { /* compiled code */ }
}

struct UInt128 {
    static prop Max: std.net.UInt128 {
        get() { /* compiled code */ }
    }

    static prop Min: std.net.UInt128 {
        get() { /* compiled code */ }
    }

    let _hi: UInt64

    let _lo: UInt64

    init(hi: UInt64, lo: UInt64) { /* compiled code */ }

    func checkedShl(shiftcount: UInt64): std.core.Option<std.net.UInt128> { /* compiled code */ }

    func checkedShr(shiftcount: UInt64): std.core.Option<std.net.UInt128> { /* compiled code */ }

    operator func &(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    operator func ^(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    operator func |(rhs: std.net.UInt128): std.net.UInt128 { /* compiled code */ }

    operator func !(): std.net.UInt128 { /* compiled code */ }

    func toU16(): std.core.Array<UInt16> { /* compiled code */ }

    static func fromU16(src: std.core.Array<UInt16>): std.net.UInt128 { /* compiled code */ }

    static func readBigEndian(buf: std.core.Array<UInt8>): std.net.UInt128 { /* compiled code */ }

    func writeBigEndian(buf: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readLittleEndian(buf: std.core.Array<UInt8>): std.net.UInt128 { /* compiled code */ }

    func writeLittleEndian(buf: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func toBigEndian(): std.core.Array<UInt8> { /* compiled code */ }

    func toLittleEndian(): std.core.Array<UInt8> { /* compiled code */ }

    func swapBytes(): std.net.UInt128 { /* compiled code */ }

    operator func ==(rhs: std.net.UInt128): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.UInt128): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

func isDomainValid(domainStr: std.core.String): Bool { /* compiled code */ }

func resolveDomain(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

class IPAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.IPAddress> & std.core.Hashable & std.binary.BigEndianOrder<std.net.IPAddress> {
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop hostName: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
    }

    static func parse(s: std.core.String): std.net.IPAddress { /* compiled code */ }

    static func tryParse(s: std.core.String): std.core.Option<std.net.IPAddress> { /* compiled code */ }

    static func resolve(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

    static func resolve(domain: std.core.String): std.core.Array<std.net.IPAddress> { /* compiled code */ }

    func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func isIPv4(): Bool { /* compiled code */ }

    func isIPv6(): Bool { /* compiled code */ }

    func isUnspecified(): Bool { /* compiled code */ }

    func isLoopback(): Bool { /* compiled code */ }

    func isMulticast(): Bool { /* compiled code */ }

    func isPrivate(): Bool { /* compiled code */ }

    func isLinkLocal(): Bool { /* compiled code */ }

    func isGlobalUnicast(): Bool { /* compiled code */ }

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPAddress { /* compiled code */ }

    operator func ==(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

func sortIPAddr(lhs: std.net.IPAddress, rhs: std.net.IPAddress): std.core.Ordering { /* compiled code */ }

class IPPrefix <: std.core.Object & std.core.Equatable<std.net.IPPrefix> & std.core.Hashable & std.core.ToString {
    prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    prop prefixLength: UInt8 {
        get() { /* compiled code */ }
    }

    static func parse(s: std.core.String): std.net.IPPrefix { /* compiled code */ }

    static func tryParse(s: std.core.String): std.core.Option<std.net.IPPrefix> { /* compiled code */ }

    func contains(rhs: std.net.IPAddress): Bool { /* compiled code */ }

    func contains(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    func overlaps(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    func netmask(): std.net.IPAddress { /* compiled code */ }

    func hostmask(): std.net.IPAddress { /* compiled code */ }

    func broadcast(): std.net.IPAddress { /* compiled code */ }

    func network(): std.net.IPAddress { /* compiled code */ }

    func masked(): std.net.IPPrefix { /* compiled code */ }

    operator func ==(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPPrefix): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

func parseIPv4(address: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv4Address> { /* compiled code */ }

enum Result {
    | Ok(T)

    | Err(std.core.Exception)

    func isOk(): Bool { /* compiled code */ }

    func isErr(): Bool { /* compiled code */ }

    func unwrap(): T { /* compiled code */ }

    func unwrapErr(): std.core.Exception { /* compiled code */ }

    func toOption(): std.core.Option<T> { /* compiled code */ }
}

class IPv4Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv4Address> & std.core.LessOrEqual<std.net.IPv4Address> {
    let localhost: std.net.IPv4Address

    let unspecified: std.net.IPv4Address

    let broadcast: std.net.IPv4Address

    var _octets: std.core.Array<UInt8>

    init(bits: UInt32) { /* compiled code */ }

    init(a: UInt8, b: UInt8, c: UInt8, d: UInt8) { /* compiled code */ }

    func toBits(): UInt32 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv4Address { /* compiled code */ }

    func isBroadcast(): Bool { /* compiled code */ }

    func isGlobalUnicast(): Bool { /* compiled code */ }

    func isLinkLocal(): Bool { /* compiled code */ }

    func isLoopback(): Bool { /* compiled code */ }

    func isMulticast(): Bool { /* compiled code */ }

    func isPrivate(): Bool { /* compiled code */ }

    func isUnspecified(): Bool { /* compiled code */ }

    func toIPv6Compatible(): std.net.IPv6Address { /* compiled code */ }

    func toIPv6Mapped(): std.net.IPv6Address { /* compiled code */ }

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    operator func <=(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    operator func ==(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

func parseNumber(part: std.core.String, radix: UInt32, minDigits: UInt8, maxDigits: UInt8, allowZeroPrefix: Bool, origin: std.core.String): std.net.Result<(UInt32, Int64)> { /* compiled code */ }

func parseIPv6(src: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv6Address> { /* compiled code */ }

class IPv6Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv6Address> & std.core.LessOrEqual<std.net.IPv6Address> {
    let localhost: std.net.IPv6Address

    let unspecified: std.net.IPv6Address

    var _octets: std.core.Array<UInt8>

    let _scopeId: std.core.Option<UInt32>

    init(bits: std.net.UInt128, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    init(octets: std.core.Array<UInt8>, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    init(a: UInt16, b: UInt16, c: UInt16, d: UInt16, e: UInt16, f: UInt16, g: UInt16, h: UInt16, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    prop scopeId: std.core.Option<UInt32> {
        get() { /* compiled code */ }
    }

    func toBits(): std.net.UInt128 { /* compiled code */ }

    func segments(): std.core.Array<UInt16> { /* compiled code */ }

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix { /* compiled code */ }

    func toIPv4(): std.core.Option<std.net.IPv4Address> { /* compiled code */ }

    func toIPv4Mapped(): std.core.Option<std.net.IPv4Address> { /* compiled code */ }

    func scope(scopeId: std.core.Option<UInt32>): std.net.IPv6Address { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv6Address { /* compiled code */ }

    func isGlobalUnicast(): Bool { /* compiled code */ }

    func isIPv4Mapped(): Bool { /* compiled code */ }

    func isLinkLocal(): Bool { /* compiled code */ }

    func isLoopback(): Bool { /* compiled code */ }

    func isMulticast(): Bool { /* compiled code */ }

    func isPrivate(): Bool { /* compiled code */ }

    func isTeredo(): Bool { /* compiled code */ }

    func isUnspecified(): Bool { /* compiled code */ }

    operator func <=(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    operator func ==(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func countZeros(segments: std.core.Array<UInt16>): (Int64, Int64) { /* compiled code */ }

    func u16ArrToHexString(sb: std.core.StringBuilder, chunk: std.core.Array<UInt16>): Unit { /* compiled code */ }
}

func innerParseIPv6(src: std.core.String, origin: std.core.String, e: std.core.Option<Int64>): std.net.Result<(Bool, Int64, Int64, std.core.Option<Int64>, std.core.Array<UInt8>)> { /* compiled code */ }

type sa_family_t = UInt16

type in_port_t = UInt16

type socklen_t = UInt32

type SockAddrByte = UInt8

struct AddrInfo {
    var ai_flags: Int32

    var ai_family: Int32

    var ai_socktype: Int32

    var ai_protocol: Int32

    var ai_addrlen: UInt32

    var ptr1: CPointer<Unit>

    var ptr2: CPointer<Unit>

    var ai_next: CPointer<std.net.AddrInfo>

    init() { /* compiled code */ }
}

struct SockAddrIn {
    var family: UInt16

    var port: UInt16

    var addr: UInt32

    let pad: Int64

    init() { /* compiled code */ }
}

struct SockAddrIn6 {
    var family: UInt16

    var port: UInt16

    var flowinfo: UInt32

    var addr: UInt32

    var pad1: UInt32

    var pad2: UInt32

    var pad3: UInt32

    var scope_id: UInt32

    init() { /* compiled code */ }
}

type PAddrinfo = CPointer<std.net.AddrInfo>

foreign func getaddrinfo(node: CString, service: CPointer<UInt8>, hints: CPointer<std.net.AddrInfo>, res: CPointer<CPointer<std.net.AddrInfo>>): Int32

foreign func freeaddrinfo(res: CPointer<std.net.AddrInfo>): Unit

foreign func CJ_SOCKET_GetAddrFromAddrinfo(info: CPointer<std.net.AddrInfo>): CPointer<UInt8>

struct SockAddrUn {
    var family: UInt16

    var sun_path: std.core.Array<UInt8>

    init() { /* compiled code */ }
}

class IPv4Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv4Prefix> & std.core.Hashable & std.core.ToString {
    let _ip: std.net.IPv4Address

    let _prefixLength: UInt8

    init(ip: std.net.IPv4Address, prefixLength: UInt8) { /* compiled code */ }

    prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    prop prefixLength: UInt8 {
        get() { /* compiled code */ }
    }

    func contains(rhs: std.net.IPv4Address): Bool { /* compiled code */ }

    func contains(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    func overlaps(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func hostmaskBits(): UInt32 { /* compiled code */ }

    func netmaskBits(): UInt32 { /* compiled code */ }

    func netmask(): std.net.IPv4Address { /* compiled code */ }

    func hostmask(): std.net.IPv4Address { /* compiled code */ }

    func broadcast(): std.net.IPv4Address { /* compiled code */ }

    func network(): std.net.IPv4Address { /* compiled code */ }

    func masked(): std.net.IPv4Prefix { /* compiled code */ }

    func supernet(): std.net.IPv4Prefix { /* compiled code */ }

    operator func ==(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPv4Prefix): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class IPv6Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv6Prefix> & std.core.Hashable & std.core.ToString {
    let _ip: std.net.IPv6Address

    let _prefixLength: UInt8

    init(ip: std.net.IPv6Address, prefixLength: UInt8) { /* compiled code */ }

    prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    prop prefixLength: UInt8 {
        get() { /* compiled code */ }
    }

    func contains(rhs: std.net.IPv6Address): Bool { /* compiled code */ }

    func contains(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    func overlaps(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func hostmaskBits(): std.net.UInt128 { /* compiled code */ }

    func netmaskBits(): std.net.UInt128 { /* compiled code */ }

    func netmask(): std.net.IPv6Address { /* compiled code */ }

    func hostmask(): std.net.IPv6Address { /* compiled code */ }

    func broadcast(): std.net.IPv6Address { /* compiled code */ }

    func network(): std.net.IPv6Address { /* compiled code */ }

    func masked(): std.net.IPv6Prefix { /* compiled code */ }

    func supernet(): std.net.IPv6Prefix { /* compiled code */ }

    operator func ==(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPv6Prefix): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class IPSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.IPSocketAddress> {
    let _addr: std.net.IPAddress

    let _port: UInt16

    init(address: std.core.Array<UInt8>, port: UInt16) { /* compiled code */ }

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.IPAddress, port: UInt16) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop family: std.net.AddressFamily {
        get() { /* compiled code */ }
    }

    static func parse(s: std.core.String): std.net.IPSocketAddress { /* compiled code */ }

    static func tryParse(s: std.core.String): std.core.Option<std.net.IPSocketAddress> { /* compiled code */ }

    prop address: std.net.IPAddress {
        get() { /* compiled code */ }
    }

    prop port: UInt16 {
        get() { /* compiled code */ }
    }

    func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func isIPv4(): Bool { /* compiled code */ }

    func isIPv6(): Bool { /* compiled code */ }

    operator func ==(rhs: std.net.IPSocketAddress): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.IPSocketAddress): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

func innerParse(s: std.core.String): std.net.Result<(std.core.String, std.core.String, Bool)> { /* compiled code */ }

struct Linger {
    var enabled: UInt16

    var seconds: UInt16

    init(onOff: Bool, seconds: Int64) { /* compiled code */ }

    let size: UIntNative

    static func use(block: (CPointer<std.net.Linger>) -> R): R { /* compiled code */ }
}

extend SocketCommon {
    func getLinger(): std.core.Option<std.core.Duration> { /* compiled code */ }

    func setLinger(newLinger: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }
}

class SocketCommon <: std.core.Object & std.core.Resource & std.core.ToString {
    var readTimeout_: std.core.Option<std.core.Duration>

    var writeTimeout_: std.core.Option<std.core.Duration>

    var bindToDevice_: std.core.Option<std.core.String>

    var keepAlive_: std.core.Option<std.net.SocketKeepAliveConfig>

    let addressLock: std.net.NonReentrantReadWriteMutex

    var remoteAddress_: std.net.ExternallyLockedLazy<std.core.Option<std.net.SocketAddress>, std.net.SocketHolder<NS>>

    var localAddress_: std.net.ExternallyLockedLazy<std.core.Option<std.net.SocketAddress>, std.net.SocketHolder<NS>>

    init(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode) { /* compiled code */ }

    mut prop localAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    static func lazyLocalAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    mut prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    static func lazyRemoteAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop isBound: Bool {
        get() { /* compiled code */ }
    }

    prop isConnected: Bool {
        get() { /* compiled code */ }
    }

    func checkNotClosed(): Unit { /* compiled code */ }

    func checkNotBound(): Unit { /* compiled code */ }

    func checkNotConnected(): Unit { /* compiled code */ }

    mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func write(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func send(payload: std.core.Array<UInt8>, destination: std.net.SocketAddress): Int64 { /* compiled code */ }

    func receiveFrom(buffer: std.core.Array<UInt8>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    func connect(timeout: std.core.Option<std.core.Duration>, shouldBeBound: Bool, mayBeAlreadyConnected: Bool): Unit { /* compiled code */ }

    func disconnect(): Unit { /* compiled code */ }

    func bind(backlogSize: std.core.Option<Int32>): Unit { /* compiled code */ }

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.SocketCommon<NS>> { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    override func close(): Unit { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    let holder: std.net.SocketHolder<NS>

    let kind: std.net.AddressFamily
}

struct NonReentrantReadLock <: std.sync.IReentrantMutex {
    override func lock(): Unit { /* compiled code */ }

    override func tryLock(): Bool { /* compiled code */ }

    override func unlock(): Unit { /* compiled code */ }

    let m: std.sync.Monitor

    let useCounter: std.sync.AtomicInt64

    init(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

extend Result <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

interface DatagramSocket <: std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
    }

    mut prop receiveTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop sendTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64) { /* compiled code */ }

    func sendTo(address: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit { /* compiled code */ }
}

struct SocketBuffer {
    let rBufSize: Int32

    let wBufSize: Int32

    let rBuf: CPointer<UInt8>

    let wBuf: CPointer<UInt8>

    var `handle`: Int64

    let count: Int32

    init() { /* compiled code */ }
}

foreign func CJ_SOCKET_BufferInit(`handle`: Int64, rBufSize: Int32, wBufSize: Int32): CPointer<std.net.SocketBuffer>

foreign func CJ_SOCKET_BufferRead(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, flags: Int32): Int32

foreign func CJ_SOCKET_BufferRecvFrom(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, source: CPointer<std.net.SockAddr>, flags: Int32): Int32

foreign func CJ_SOCKET_BufferWrite(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, flags: Int32): Int32

foreign func CJ_SOCKET_BufferSendto(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, dest: CPointer<std.net.SockAddr>, flags: Int32): Int32

foreign func CJ_SOCKET_BufferRCopy(sockBuf: CPointer<std.net.SocketBuffer>, arrBuf: CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

foreign func CJ_SOCKET_BufferWCopy(sockBuf: CPointer<std.net.SocketBuffer>, arrBuf: CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

foreign func CJ_SOCKET_BufferClose(sockBuf: CPointer<std.net.SocketBuffer>, `handle`: Int64): Int32

enum SocketNet <: std.core.ToString & std.core.Equatable<std.net.SocketNet> {
    | TCP

    | UDP

    | UNIX

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(that: std.net.SocketNet): Bool { /* compiled code */ }

    operator func !=(that: std.net.SocketNet): Bool { /* compiled code */ }
}

enum SocketMode <: std.core.Equal<std.net.SocketMode> {
    | StreamingMode

    | DatagramMode

    | SequentialMode

    override operator func ==(other: std.net.SocketMode): Bool { /* compiled code */ }
}

class SocketException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }

    static func throwClosedException(): Nothing { /* compiled code */ }

    static func notYetConnected(): Nothing { /* compiled code */ }

    static func notYetBound(): Nothing { /* compiled code */ }

    static func notYetBoundNeedBind(): Nothing { /* compiled code */ }

    static func alreadyBound(): Nothing { /* compiled code */ }

    static func alreadyConnected(): Nothing { /* compiled code */ }
}

class SocketTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

let SOCKET_STREAM: Int32

let SOCKET_DGRAM: Int32

let SOCKET_RAW: Int32

let SOCKET_SEQPACKET: Int32

struct SockKeepAliveCfgS {
    var keep_alive: UInt32

    var idle: UInt32

    var interval: UInt32

    var count: UInt32

    init(keep_alive: UInt32, idle: UInt32, interval: UInt32, count: UInt32) { /* compiled code */ }
}

foreign func CJ_SockErrnoGet(): Int32

foreign func CJ_SockKeepAliveSet(sock: Int64, cfg: CPointer<std.net.SockKeepAliveCfgS>): Int32

foreign func CJ_SockOptionSet(sock: Int64, level: Int32, optname: Int32, optval: CPointer<Unit>, oplen: Int32): Int32

foreign func CJ_SockOptionGet(sock: Int64, level: Int32, optname: Int32, optval: CPointer<Unit>, oplen: CPointer<Int32>): Int32

foreign func CJ_SOCKET_GetErrMessage(errno: Int32): CString

foreign func CJ_GetOptionInt(sock: Int64, level: Int32, optname: Int32): Int32

foreign func CJ_SetOptionInt(sock: Int64, level: Int32, optname: Int32, optval: Int32): Int32

foreign func CJ_RawAddressCreate(arr: CPointer<UInt8>, size: Int32): CPointer<std.net.SockAddr>

foreign func CJ_RawAddressLenGet(addrPtr: CPointer<std.net.SockAddr>): UIntNative

foreign func CJ_RawAddressDestroy(addrPtr: CPointer<std.net.SockAddr>): Unit

func getAddrPointer(addr: std.core.Array<UInt8>): (std.core.CPointerHandle<UInt8>, CPointer<std.net.SockAddr>) { /* compiled code */ }

func releaseAddrPointer(`handle`: std.core.CPointerHandle<UInt8>, addrPtr: CPointer<std.net.SockAddr>): Unit { /* compiled code */ }

foreign func CJ_MRT_TcpsockInit(): Int32

foreign func CJ_MRT_UdpsockInit(): Int32

foreign func CJ_MRT_RawsockInit(): Int32

let CJ_MRT_TCP_SOCK_INIT: Int32

let CJ_MRT_UDP_SOCK_INIT: Int32

let CJ_MRT_RAW_SOCK_INIT: Int32

func localAddrGet(`handle`: Int64, addr: CPointer<std.net.SockAddr>): Int32 { /* compiled code */ }

func peerAddrGet(`handle`: Int64, addr: CPointer<std.net.SockAddr>): Int32 { /* compiled code */ }

func getAddress(`handle`: Int64, getAddressFunc: (Int64, CPointer<std.net.SockAddr>) -> Int32, msg: std.core.String): std.net.SockAddr { /* compiled code */ }

func getLocalAddress(`handle`: Int64): std.net.SocketAddress { /* compiled code */ }

func getRemoteAddress(`handle`: Int64): std.net.SocketAddress { /* compiled code */ }

foreign func CJ_MRT_SockCreate(domain: Int32, sock_type: Int32, protocol: Int32, net: CString): Int64

foreign func CJ_MRT_SockBind(sock: Int64, addr: CPointer<std.net.SockAddr>, backlog: Int32): Int64

foreign func CJ_MRT_SockAccept(sock: Int64, addr: CPointer<std.net.SockAddr>): Int64

foreign func CJ_MRT_SockAcceptTimeout(sock: Int64, addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int64

foreign func CJ_MRT_SockConnect(sock: Int64, local_addr: CPointer<std.net.SockAddr>, peer_addr: CPointer<std.net.SockAddr>): Int64

foreign func CJ_MRT_SockConnectTimeout(sock: Int64, local_addr: CPointer<std.net.SockAddr>, peer_addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int64

foreign func CJ_MRT_SockDisconnect(sock: Int64, isIpv6: Bool): Int32

foreign func CJ_MRT_SockClose(sock: Int64): Int32

foreign func CJ_MRT_SockShutdown(sock: Int64): Int32

foreign func CJ_MRT_SockSend(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32): Int32

foreign func CJ_MRT_SockSendTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

foreign func CJ_MRT_SockSendNonBlock(`handle`: Int64, buf: CPointer<UInt8>, len: Int32, flags: Int32): Int32

foreign func CJ_MRT_SockRecv(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32): Int32

foreign func CJ_MRT_SockRecvTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

foreign func CJ_MRT_SockRecvNonBlock(`handle`: Int64, buf: CPointer<UInt8>, len: Int32, flags: Int32): Int32

foreign func CJ_MRT_SockRecvfromTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int32

foreign func CJ_MRT_SockWaitRecv(`handle`: Int64): Int32

foreign func CJ_MRT_SockWaitRecvTimeout(`handle`: Int64, timeout: UInt64): Int32

foreign func CJ_MRT_SockWaitSend(`handle`: Int64): Int32

foreign func CJ_MRT_SockWaitSendTimeout(`handle`: Int64, timeout: UInt64): Int32

foreign func CJ_MRT_SockListen(sockfd: Int64, backlog: Int32): Int64

foreign func CJ_MRT_SockSendtoNonBlock(sockfd: Int64, buf: CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: CPointer<std.net.SockAddr>): Int32

foreign func CJ_MRT_SockRecvfromNonBlock(sockfd: Int64, buf: CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: CPointer<std.net.SockAddr>): Int32

foreign func CJ_MRT_SockLocalAddrGet(sock: Int64, addr: CPointer<std.net.SockAddr>): Int32

foreign func CJ_MRT_SockPeerAddrGet(sock: Int64, addr: CPointer<std.net.SockAddr>): Int32

enum ErrnoLabel <: std.core.ToString {
    | Accept

    | Connect

    | Read

    | Write

    | CreateSock

    | Bind

    | Listen

    override func toString(): std.core.String { /* compiled code */ }
}

func getSocketError(): Int32 { /* compiled code */ }

func socketProcessErrno(label: std.net.ErrnoLabel): Nothing { /* compiled code */ }

func sockError(errno: Int32): std.core.String { /* compiled code */ }

func isSocketErrorInvalidArgument(errno: Int32): Bool { /* compiled code */ }

type ActualPlatformSocket = std.net.DopraOtherSocketImpl

class DopraOtherSocketImpl <: std.net.DopraSocketImpl<std.net.DopraOtherSocketImpl> {
    let socketBufferPtr: CPointer<std.net.SocketBuffer>

    override func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    override func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64> { /* compiled code */ }

    override func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraOtherSocketImpl> { /* compiled code */ }

    override func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    override func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64 { /* compiled code */ }

    override func shutdown(): Unit { /* compiled code */ }

    override func dispose(): Unit { /* compiled code */ }

    redef static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraOtherSocketImpl { /* compiled code */ }

    override func onHandleReplaced(newHandle: Int64): Unit { /* compiled code */ }
}

type ActualTcpPlatformSocket = std.net.DopraOtherSocketImpl

class DopraSocketImpl <: std.core.Object & std.net.PlatformSocket<Self> {
    var address: std.core.Option<std.net.SockAddr>

    var isIPv6: Bool

    mut prop `handle`: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func onHandleReplaced(newHandle: Int64): Unit { /* compiled code */ }

    override func disconnect(): Unit { /* compiled code */ }

    override func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound: Bool): Unit { /* compiled code */ }

    func acceptImpl(timeout: std.core.Option<std.core.Duration>): (Int64, std.net.SocketNet) { /* compiled code */ }

    override func shutdown(): Unit { /* compiled code */ }

    override func detectLocalAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    override func detectRemoteAddress(): std.core.Option<std.net.SocketAddress> { /* compiled code */ }

    override func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    override func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    override func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    override func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    override func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool { /* compiled code */ }

    redef static func create(_: std.net.SocketNet, _: std.net.AddressFamily, _: std.net.SocketMode): Self { /* compiled code */ }

    static func createSocket(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Int64 { /* compiled code */ }

    static func toDopraTimeout(timeout: std.core.Duration): UInt64 { /* compiled code */ }

    var _handle: std.sync.AtomicInt64

    init(_handle: std.sync.AtomicInt64) { /* compiled code */ }
}

class DopraTcpSocketImpl <: std.net.DopraSocketImpl<std.net.DopraTcpSocketImpl> {
    override func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    override func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64> { /* compiled code */ }

    override func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraTcpSocketImpl> { /* compiled code */ }

    override func send(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>, _: std.net.SocketAddress): Int64 { /* compiled code */ }

    override func receiveFrom(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)> { /* compiled code */ }

    override func dispose(): Unit { /* compiled code */ }

    override func onHandleReplaced(_: Int64): Unit { /* compiled code */ }

    redef static func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraTcpSocketImpl { /* compiled code */ }
}

enum DopraAsyncResult {
    | BytesTransferred(UInt32)

    | RetryAgain

    | EOF
}

func setFamily(ptr: CPointer<Unit>, v: Int16): Unit { /* compiled code */ }

func readFamily(ptr: CPointer<Unit>): Int16 { /* compiled code */ }

struct RawAddress {
    let addr_: std.core.Array<UInt8>

    prop addr: std.core.Array<UInt8> {
        get() { /* compiled code */ }
    }

    init(addr: std.core.Array<UInt8>) { /* compiled code */ }
}

enum InternalHolderState {
    | NotCreated(() -> NS)

    | Ready(NS)

    | Disposed
}

enum HolderState <: std.core.Equatable<std.net.HolderState> {
    | SocketNotCreated

    | SocketPrepared

    | SocketClosing

    | SocketDisposed

    override operator func ==(other: std.net.HolderState): Bool { /* compiled code */ }

    override operator func !=(other: std.net.HolderState): Bool { /* compiled code */ }
}

struct SocketState {
    let Initial: std.net.SocketState

    let Terminal: std.net.SocketState

    let JustAccepted: std.net.SocketState

    mut prop useCounter: UInt32 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop disposedOrShuttingDown: Bool {
        get() { /* compiled code */ }
    }

    mut prop nativeState: std.net.HolderState {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop connected: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop bound: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop hasReaders: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop hasWriters: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop failed: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func ensureNotClosed(): Unit { /* compiled code */ }

    func ensureConnected(): Unit { /* compiled code */ }

    func ensureNotConnected(): Unit { /* compiled code */ }

    func ensureBound(): Unit { /* compiled code */ }

    func ensureBindInvoked(): Unit { /* compiled code */ }

    func ensureNotBound(): Unit { /* compiled code */ }

    var value: UInt32

    init(value: UInt32) { /* compiled code */ }
}

struct SocketOptions {
    let SOL_SOCKET: Int32

    let IPPROTO_TCP: Int32

    let IPPROTO_UDP: Int32

    let SO_KEEPALIVE: Int32

    let TCP_NODELAY: Int32

    let TCP_QUICKACK: Int32

    let SO_LINGER: Int32

    let SO_SNDBUF: Int32

    let SO_RCVBUF: Int32

    let SO_REUSEADDR: Int32

    let SO_REUSEPORT: Int32

    let SO_BINDTODEVICE: Int32
}

class RawSocket {
    let sockImpl: std.net.RawSocketImpl

    var isClosed: Bool

    var readTimeout_: std.core.Option<std.core.Duration>

    var writeTimeout_: std.core.Option<std.core.Duration>

    var localAddr_: std.core.Option<std.net.RawAddress>

    var remoteAddr_: std.core.Option<std.net.RawAddress>

    prop sockFd: Int64 {
        get() { /* compiled code */ }
    }

    prop localAddr: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    prop localAddress: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    prop remoteAddr: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    prop remoteAddress: std.net.RawAddress {
        get() { /* compiled code */ }
    }

    mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    init(domain: std.net.SocketDomain, `type`: std.net.SocketType, protocol: std.net.ProtocolType) { /* compiled code */ }

    func bind(addr: std.net.RawAddress): Unit { /* compiled code */ }

    func listen(backlog: Int32): Unit { /* compiled code */ }

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.RawSocket { /* compiled code */ }

    func connect(addr: std.net.RawAddress, timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func sendTo(addr: std.net.RawAddress, buffer: std.core.Array<UInt8>, flags: Int32): Unit { /* compiled code */ }

    func receiveFrom(buffer: std.core.Array<UInt8>, flags: Int32): (std.net.RawAddress, Int64) { /* compiled code */ }

    func send(buffer: std.core.Array<UInt8>, flags: Int32): Unit { /* compiled code */ }

    func receive(buffer: std.core.Array<UInt8>, flags: Int32): Int64 { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<UInt8>, len: Int32): Unit { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<UInt8>, len: CPointer<Int32>): Unit { /* compiled code */ }
}

class RawSocketImpl {
    var sockFd: Int64

    var isBound: Bool

    var socketBufferPtr: CPointer<std.net.SocketBuffer>

    var bufferInitialized: std.sync.AtomicBool

    init(sockFd: Int64) { /* compiled code */ }

    func bufferInit(): Unit { /* compiled code */ }

    func bind(addr: std.net.RawAddress): Unit { /* compiled code */ }

    func connect(addr: std.net.RawAddress, timeout: Int64): Unit { /* compiled code */ }

    func sendTo(addr: std.core.Array<UInt8>, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit { /* compiled code */ }

    func send(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit { /* compiled code */ }

    func receiveFrom(size: Int64, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): (std.net.RawAddress, Int64) { /* compiled code */ }

    func receive(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Int64 { /* compiled code */ }

    func isConnectionOriented(sockType: Int32): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }
}

struct SocketDomain <: std.core.Equatable<std.net.SocketDomain> & std.core.ToString & std.core.Hashable {
    let val: Int32

    let IPV4: std.net.SocketDomain

    let IPV6: std.net.SocketDomain

    let UNIX: std.net.SocketDomain

    init(domain: Int32) { /* compiled code */ }

    operator func ==(r: std.net.SocketDomain): Bool { /* compiled code */ }

    operator func !=(r: std.net.SocketDomain): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

struct SocketType <: std.core.Equatable<std.net.SocketType> & std.core.ToString & std.core.Hashable {
    let val: Int32

    let STREAM: std.net.SocketType

    let DATAGRAM: std.net.SocketType

    let RAW: std.net.SocketType

    let SEQPACKET: std.net.SocketType

    init(`type`: Int32) { /* compiled code */ }

    operator func ==(r: std.net.SocketType): Bool { /* compiled code */ }

    operator func !=(r: std.net.SocketType): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

struct ProtocolType <: std.core.Equatable<std.net.ProtocolType> & std.core.ToString & std.core.Hashable {
    let val: Int32

    let Unspecified: std.net.ProtocolType

    let IPV4: std.net.ProtocolType

    let IPV6: std.net.ProtocolType

    let ICMP: std.net.ProtocolType

    let TCP: std.net.ProtocolType

    let UDP: std.net.ProtocolType

    let RAW: std.net.ProtocolType

    init(protocol: Int32) { /* compiled code */ }

    operator func ==(r: std.net.ProtocolType): Bool { /* compiled code */ }

    operator func !=(r: std.net.ProtocolType): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }
}

func getSocketOptionInt32(sockFd: Int64, level: Int32, option: Int32): Int32 { /* compiled code */ }

func getOnceSendSize(size: Int64): Int64 { /* compiled code */ }

struct OptionLevel {
    let IP: Int32

    let TCP: Int32

    let UDP: Int32

    let ICMP: Int32

    let RAW: Int32

    let SOCKET: Int32

    init() { /* compiled code */ }
}

struct OptionName {
    let IP_HDRINCL: Int32

    let IP_TOS: Int32

    let IP_TTL: Int32

    let TCP_KEEPCNT: Int32

    let TCP_KEEPIDLE: Int32

    let TCP_KEEPINTVL: Int32

    let TCP_NODELAY: Int32

    let SO_DEBUG: Int32

    let SO_ACCEPTCONN: Int32

    let SO_REUSEADDR: Int32

    let SO_KEEPALIVE: Int32

    let SO_DONTROUTE: Int32

    let SO_BROADCAST: Int32

    let SO_LINGER: Int32

    let SO_OOBINLINE: Int32

    let SO_SNDBUF: Int32

    let SO_RCVBUF: Int32

    let SO_SNDTIMEO: Int32

    let SO_RCVTIMEO: Int32

    let SO_ERROR: Int32

    init() { /* compiled code */ }
}

func getTimeout(timeout: std.core.Option<std.core.Duration>): Int64 { /* compiled code */ }

let MAX_TIMEOUT_DURATION: std.core.Duration

func unwrapOption(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T> { /* compiled code */ }

func getOrFail(e: () -> std.core.Exception): (std.core.Option<T>) -> T { /* compiled code */ }

func formatError(errnum: Int32): std.core.String { /* compiled code */ }

func throwIfIPv4ZeroOnWindows(socketAddress: std.net.SocketAddress): Unit { /* compiled code */ }

func zeroMemory(buffer: CPointer<UInt8>, size: Int64): Unit { /* compiled code */ }

func resolveHelper(address: std.core.String): std.net.IPAddress { /* compiled code */ }

class TcpSocket <: std.core.Object & std.net.StreamingSocket & std.core.Equatable<std.net.TcpSocket> & std.core.Hashable {
    let id: Int64

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.SocketAddress) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress: std.core.Option<std.net.SocketAddress>) { /* compiled code */ }

    override prop remoteAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop noDelay: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop quickAcknowledge: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop linger: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    override func write(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func connect(timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    override operator func ==(other: std.net.TcpSocket): Bool { /* compiled code */ }

    override operator func !=(other: std.net.TcpSocket): Bool { /* compiled code */ }

    override func hashCode(): Int64 { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }
}

class TcpServerSocket <: std.core.Object & std.net.ServerSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    var backlogSize_: Int32

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    init(bindAt: UInt16) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    mut prop reuseAddress: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop reusePort: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop bindToDevice: std.core.Option<std.core.String> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop backlogSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func bind(): Unit { /* compiled code */ }

    override func accept(timeout: std.core.Option<std.core.Duration>): std.net.TcpSocket { /* compiled code */ }

    override func accept(): std.net.TcpSocket { /* compiled code */ }

    override func close(): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

class UdpSocket <: std.core.Object & std.net.DatagramSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt: UInt16) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    override prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
    }

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override mut prop receiveTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override mut prop sendTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func close(): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    func bind(): Unit { /* compiled code */ }

    func connect(remote: std.net.SocketAddress): Unit { /* compiled code */ }

    func disconnect(): Unit { /* compiled code */ }

    override func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64) { /* compiled code */ }

    override func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func send(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func receive(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    mut prop reusePort: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop reuseAddress: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

func checkUnixSocketAddress(address: std.net.SocketAddress, name: std.core.String): std.net.SocketAddress { /* compiled code */ }

class UnixSocket <: std.core.Object & std.net.StreamingSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(path: std.core.String, localPath: std.core.Option<std.core.String>) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress: std.core.Option<std.net.SocketAddress>) { /* compiled code */ }

    init(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }

    override prop remoteAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override mut prop readTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override mut prop writeTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    override func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func connect(timeout: std.core.Option<std.core.Duration>): Unit { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

class UnixServerSocket <: std.core.Object & std.net.ServerSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    var backlogSize_: Int32

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    init(bindAt: std.core.String) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop backlogSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func bind(): Unit { /* compiled code */ }

    override func accept(timeout: std.core.Option<std.core.Duration>): std.net.UnixSocket { /* compiled code */ }

    override func accept(): std.net.UnixSocket { /* compiled code */ }

    override func close(): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

class UnixDatagramSocket <: std.core.Object & std.net.DatagramSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt: std.core.String) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    override prop remoteAddress: std.core.Option<std.net.SocketAddress> {
        get() { /* compiled code */ }
    }

    override prop localAddress: std.net.SocketAddress {
        get() { /* compiled code */ }
    }

    override mut prop receiveTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override mut prop sendTimeout: std.core.Option<std.core.Duration> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    override func close(): Unit { /* compiled code */ }

    override func isClosed(): Bool { /* compiled code */ }

    func bind(): Unit { /* compiled code */ }

    func connect(remotePath: std.core.String): Unit { /* compiled code */ }

    func connect(remote: std.net.SocketAddress): Unit { /* compiled code */ }

    func disconnect(): Unit { /* compiled code */ }

    override func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64) { /* compiled code */ }

    override func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func send(payload: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func receive(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    mut prop sendBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop receiveBufferSize: Int64 {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit { /* compiled code */ }

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit { /* compiled code */ }

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative { /* compiled code */ }

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit { /* compiled code */ }

    func getSocketOptionBool(level: Int32, option: Int32): Bool { /* compiled code */ }

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

func findLastNoneZeroIdx(path: std.core.Array<UInt8>): Int64 { /* compiled code */ }

class UnixSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.UnixSocketAddress> {
    let _path: std.core.String

    let _len: Int64

    init(path: std.core.Array<UInt8>) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop family: std.net.AddressFamily {
        get() { /* compiled code */ }
    }

    operator func ==(rhs: std.net.UnixSocketAddress): Bool { /* compiled code */ }

    operator func !=(rhs: std.net.UnixSocketAddress): Bool { /* compiled code */ }

    func getAddressBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}
