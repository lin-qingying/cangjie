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

    operator func ==(rhs: std.net.AddressFamily): Bool

    operator func !=(rhs: std.net.AddressFamily): Bool

    func toString: std.core.String
}

interface StreamingSocket <: std.io.IOStream & std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress

    prop remoteAddress: std.net.SocketAddress

    prop readTimeout: std.core.Option<std.core.Duration>

    prop writeTimeout: std.core.Option<std.core.Duration>
}

interface ServerSocket <: std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress

    func bind: Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.StreamingSocket

    func accept: std.net.StreamingSocket
}

struct SockAddr {
    let ptr: CPointer<Unit>

    let len: UInt32

    init(buffer: CPointer<Unit>, length: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    init(sa: std.net.SocketAddress) { /* compiled code */ }

    func free: Unit

    prop family: Int16

    func toSocketAddress: std.net.SocketAddress

    func toRawAddress: std.net.RawAddress
}

struct NonReentrantWriteLock <: std.sync.IReentrantMutex {
    init() { /* compiled code */ }

    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    let m: std.sync.Monitor

    let useCounter: std.sync.AtomicInt64

    init(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

struct SocketKeepAliveConfig <: std.core.ToString & std.core.Equatable<std.net.SocketKeepAliveConfig> {
    let idle: std.core.Duration

    let interval: std.core.Duration

    let count: UInt32

    init(idle: std.core.Duration, interval: std.core.Duration, count: UInt32) { /* compiled code */ }

    func toString: std.core.String

    operator func ==(other: std.net.SocketKeepAliveConfig): Bool

    operator func !=(other: std.net.SocketKeepAliveConfig): Bool
}

struct NonReentrantReadWriteMutex {
    let readMutex: std.net.NonReentrantReadLock

    let writeMutex: std.net.NonReentrantWriteLock

    init() { /* compiled code */ }
}

struct ExternallyLockedLazy {
    var value: std.core.Option<T>

    func compute(lock: std.net.NonReentrantReadWriteMutex, arg: Arg, provider: (Arg) -> T): T

    prop directValue: std.core.Option<T>

    func clearWithoutLock: Unit

    func putValue(lock: std.net.NonReentrantReadWriteMutex, value: T): Unit

    init() { /* compiled code */ }
}

class SocketAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.SocketAddress> & std.core.Hashable {
    prop size: Int64

    prop family: std.net.AddressFamily

    func getAddressBytes: std.core.Array<UInt8>

    operator func ==(rhs: std.net.SocketAddress): Bool

    operator func !=(rhs: std.net.SocketAddress): Bool

    init() { /* compiled code */ }
}

class SocketHolder <: std.core.Object & std.core.Resource {
    let socketCreationLock: std.net.NonReentrantWriteLock

    let useCounter: std.sync.AtomicUInt32

    var state_: std.net.InternalHolderState<NS>

    init(acceptedSocket: NS) { /* compiled code */ }

    prop state: std.net.SocketState

    func markConnected: Unit

    func unmarkConnected: Unit

    func markBound: Unit

    func markFailed: Unit

    func isClosed: Bool

    func close: Unit

    func use(block: (NS, std.net.SocketState) -> R): std.core.Option<R>

    func read(block: (NS, std.net.SocketState) -> R): std.core.Option<R>

    func write(block: (NS, std.net.SocketState) -> R): std.core.Option<R>

    init(create: () -> NS) { /* compiled code */ }
}

interface PlatformSocket {
    func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit

    func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64>

    func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64

    func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)>

    func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit

    func disconnect: Unit

    func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound: Bool): Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<Self>

    func shutdown: Unit

    func detectLocalAddress: std.core.Option<std.net.SocketAddress>

    func detectRemoteAddress: std.core.Option<std.net.SocketAddress>

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool

    func dispose: Unit

    func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Self
}

struct UInt128 {
    prop Max: std.net.UInt128

    prop Min: std.net.UInt128

    let _hi: UInt64

    let _lo: UInt64

    init(hi: UInt64, lo: UInt64) { /* compiled code */ }

    func checkedShl(shiftcount: UInt64): std.core.Option<std.net.UInt128>

    func checkedShr(shiftcount: UInt64): std.core.Option<std.net.UInt128>

    operator func &(rhs: std.net.UInt128): std.net.UInt128

    operator func ^(rhs: std.net.UInt128): std.net.UInt128

    operator func |(rhs: std.net.UInt128): std.net.UInt128

    operator func !: std.net.UInt128

    func toU16: std.core.Array<UInt16>

    func fromU16(src: std.core.Array<UInt16>): std.net.UInt128

    func readBigEndian(buf: std.core.Array<UInt8>): std.net.UInt128

    func writeBigEndian(buf: std.core.Array<UInt8>): Int64

    func readLittleEndian(buf: std.core.Array<UInt8>): std.net.UInt128

    func writeLittleEndian(buf: std.core.Array<UInt8>): Int64

    func toBigEndian: std.core.Array<UInt8>

    func toLittleEndian: std.core.Array<UInt8>

    func swapBytes: std.net.UInt128

    operator func ==(rhs: std.net.UInt128): Bool

    operator func !=(rhs: std.net.UInt128): Bool

    func toString: std.core.String
}

func isDomainValid(domainStr: std.core.String): Bool

func resolveDomain(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress>

class IPAddress <: std.core.Object & std.core.ToString & std.core.Equatable<std.net.IPAddress> & std.core.Hashable & std.binary.BigEndianOrder<std.net.IPAddress> {
    prop size: Int64

    prop hostName: std.core.Option<std.core.String>

    func parse(s: std.core.String): std.net.IPAddress

    func tryParse(s: std.core.String): std.core.Option<std.net.IPAddress>

    func resolve(family: std.net.AddressFamily, domain: std.core.String): std.core.Array<std.net.IPAddress>

    func resolve(domain: std.core.String): std.core.Array<std.net.IPAddress>

    func getAddressBytes: std.core.Array<UInt8>

    func isIPv4: Bool

    func isIPv6: Bool

    func isUnspecified: Bool

    func isLoopback: Bool

    func isMulticast: Bool

    func isPrivate: Bool

    func isLinkLocal: Bool

    func isGlobalUnicast: Bool

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPAddress

    operator func ==(rhs: std.net.IPAddress): Bool

    operator func !=(rhs: std.net.IPAddress): Bool

    func hashCode: Int64

    init() { /* compiled code */ }
}

func sortIPAddr(lhs: std.net.IPAddress, rhs: std.net.IPAddress): std.core.Ordering

class IPPrefix <: std.core.Object & std.core.Equatable<std.net.IPPrefix> & std.core.Hashable & std.core.ToString {
    prop address: std.net.IPAddress

    prop prefixLength: UInt8

    func parse(s: std.core.String): std.net.IPPrefix

    func tryParse(s: std.core.String): std.core.Option<std.net.IPPrefix>

    func contains(rhs: std.net.IPAddress): Bool

    func contains(rhs: std.net.IPPrefix): Bool

    func overlaps(rhs: std.net.IPPrefix): Bool

    func netmask: std.net.IPAddress

    func hostmask: std.net.IPAddress

    func broadcast: std.net.IPAddress

    func network: std.net.IPAddress

    func masked: std.net.IPPrefix

    operator func ==(rhs: std.net.IPPrefix): Bool

    operator func !=(rhs: std.net.IPPrefix): Bool

    func toString: std.core.String

    init() { /* compiled code */ }
}

func parseIPv4(address: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv4Address>

enum Result {
    | Ok(T)

    | Err(std.core.Exception)

    func isOk: Bool

    func isErr: Bool

    func unwrap: T

    func unwrapErr: std.core.Exception

    func toOption: std.core.Option<T>
}

class IPv4Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv4Address> & std.core.LessOrEqual<std.net.IPv4Address> {
    let localhost: std.net.IPv4Address

    let unspecified: std.net.IPv4Address

    let broadcast: std.net.IPv4Address

    var _octets: std.core.Array<UInt8>

    init(bits: UInt32) { /* compiled code */ }

    init(a: UInt8, b: UInt8, c: UInt8, d: UInt8) { /* compiled code */ }

    func toBits: UInt32

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv4Address

    func isBroadcast: Bool

    func isGlobalUnicast: Bool

    func isLinkLocal: Bool

    func isLoopback: Bool

    func isMulticast: Bool

    func isPrivate: Bool

    func isUnspecified: Bool

    func toIPv6Compatible: std.net.IPv6Address

    func toIPv6Mapped: std.net.IPv6Address

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix

    operator func <=(rhs: std.net.IPv4Address): Bool

    operator func ==(rhs: std.net.IPv4Address): Bool

    operator func !=(rhs: std.net.IPv4Address): Bool

    func toString: std.core.String
}

func parseNumber(part: std.core.String, radix: UInt32, minDigits: UInt8, maxDigits: UInt8, allowZeroPrefix: Bool, origin: std.core.String): std.net.Result<(UInt32, Int64)>

func parseIPv6(src: std.core.String, origin: std.core.String): std.net.Result<std.net.IPv6Address>

class IPv6Address <: std.net.IPAddress & std.core.ToString & std.core.Equatable<std.net.IPv6Address> & std.core.LessOrEqual<std.net.IPv6Address> {
    let localhost: std.net.IPv6Address

    let unspecified: std.net.IPv6Address

    var _octets: std.core.Array<UInt8>

    let _scopeId: std.core.Option<UInt32>

    init(bits: std.net.UInt128, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    init(octets: std.core.Array<UInt8>, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    init(a: UInt16, b: UInt16, c: UInt16, d: UInt16, e: UInt16, f: UInt16, g: UInt16, h: UInt16, scopeId: std.core.Option<UInt32>) { /* compiled code */ }

    prop scopeId: std.core.Option<UInt32>

    func toBits: std.net.UInt128

    func segments: std.core.Array<UInt16>

    func getPrefix(prefixLen: UInt8): std.net.IPPrefix

    func toIPv4: std.core.Option<std.net.IPv4Address>

    func toIPv4Mapped: std.core.Option<std.net.IPv4Address>

    func scope(scopeId: std.core.Option<UInt32>): std.net.IPv6Address

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): std.net.IPv6Address

    func isGlobalUnicast: Bool

    func isIPv4Mapped: Bool

    func isLinkLocal: Bool

    func isLoopback: Bool

    func isMulticast: Bool

    func isPrivate: Bool

    func isTeredo: Bool

    func isUnspecified: Bool

    operator func <=(rhs: std.net.IPv6Address): Bool

    operator func ==(rhs: std.net.IPv6Address): Bool

    operator func !=(rhs: std.net.IPv6Address): Bool

    func toString: std.core.String

    func countZeros(segments: std.core.Array<UInt16>): (Int64, Int64)

    func u16ArrToHexString(sb: std.core.StringBuilder, chunk: std.core.Array<UInt16>): Unit
}

func innerParseIPv6(src: std.core.String, origin: std.core.String, e: std.core.Option<Int64>): std.net.Result<(Bool, Int64, Int64, std.core.Option<Int64>, std.core.Array<UInt8>)>

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

func getaddrinfo(node: CString, service: CPointer<UInt8>, hints: CPointer<std.net.AddrInfo>, res: CPointer<CPointer<std.net.AddrInfo>>): Int32

func freeaddrinfo(res: CPointer<std.net.AddrInfo>): Unit

func CJ_SOCKET_GetAddrFromAddrinfo(info: CPointer<std.net.AddrInfo>): CPointer<UInt8>

struct SockAddrUn {
    var family: UInt16

    var sun_path: std.core.Array<UInt8>

    init() { /* compiled code */ }
}

class IPv4Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv4Prefix> & std.core.Hashable & std.core.ToString {
    let _ip: std.net.IPv4Address

    let _prefixLength: UInt8

    init(ip: std.net.IPv4Address, prefixLength: UInt8) { /* compiled code */ }

    prop address: std.net.IPAddress

    prop prefixLength: UInt8

    func contains(rhs: std.net.IPv4Address): Bool

    func contains(rhs: std.net.IPv4Prefix): Bool

    func overlaps(rhs: std.net.IPv4Prefix): Bool

    func hashCode: Int64

    func hostmaskBits: UInt32

    func netmaskBits: UInt32

    func netmask: std.net.IPv4Address

    func hostmask: std.net.IPv4Address

    func broadcast: std.net.IPv4Address

    func network: std.net.IPv4Address

    func masked: std.net.IPv4Prefix

    func supernet: std.net.IPv4Prefix

    operator func ==(rhs: std.net.IPv4Prefix): Bool

    operator func !=(rhs: std.net.IPv4Prefix): Bool

    func toString: std.core.String
}

class IPv6Prefix <: std.net.IPPrefix & std.core.Equatable<std.net.IPv6Prefix> & std.core.Hashable & std.core.ToString {
    let _ip: std.net.IPv6Address

    let _prefixLength: UInt8

    init(ip: std.net.IPv6Address, prefixLength: UInt8) { /* compiled code */ }

    prop address: std.net.IPAddress

    prop prefixLength: UInt8

    func contains(rhs: std.net.IPv6Address): Bool

    func contains(rhs: std.net.IPv6Prefix): Bool

    func overlaps(rhs: std.net.IPv6Prefix): Bool

    func hashCode: Int64

    func hostmaskBits: std.net.UInt128

    func netmaskBits: std.net.UInt128

    func netmask: std.net.IPv6Address

    func hostmask: std.net.IPv6Address

    func broadcast: std.net.IPv6Address

    func network: std.net.IPv6Address

    func masked: std.net.IPv6Prefix

    func supernet: std.net.IPv6Prefix

    operator func ==(rhs: std.net.IPv6Prefix): Bool

    operator func !=(rhs: std.net.IPv6Prefix): Bool

    func toString: std.core.String
}

class IPSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.IPSocketAddress> {
    let _addr: std.net.IPAddress

    let _port: UInt16

    init(address: std.core.Array<UInt8>, port: UInt16) { /* compiled code */ }

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.IPAddress, port: UInt16) { /* compiled code */ }

    prop size: Int64

    prop family: std.net.AddressFamily

    func parse(s: std.core.String): std.net.IPSocketAddress

    func tryParse(s: std.core.String): std.core.Option<std.net.IPSocketAddress>

    prop address: std.net.IPAddress

    prop port: UInt16

    func getAddressBytes: std.core.Array<UInt8>

    func isIPv4: Bool

    func isIPv6: Bool

    operator func ==(rhs: std.net.IPSocketAddress): Bool

    operator func !=(rhs: std.net.IPSocketAddress): Bool

    func hashCode: Int64

    func toString: std.core.String
}

func innerParse(s: std.core.String): std.net.Result<(std.core.String, std.core.String, Bool)>

struct Linger {
    var enabled: UInt16

    var seconds: UInt16

    init(onOff: Bool, seconds: Int64) { /* compiled code */ }

    let size: UIntNative

    func use(block: (CPointer<std.net.Linger>) -> R): R
}

extend SocketCommon {
    func getLinger: std.core.Option<std.core.Duration>

    func setLinger(newLinger: std.core.Option<std.core.Duration>): Unit
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

    prop localAddress: std.core.Option<std.net.SocketAddress>

    func lazyLocalAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress>

    prop remoteAddress: std.core.Option<std.net.SocketAddress>

    func lazyRemoteAddress(holder: std.net.SocketHolder<NS>): std.core.Option<std.net.SocketAddress>

    prop bindToDevice: std.core.Option<std.core.String>

    prop isBound: Bool

    prop isConnected: Bool

    func checkNotClosed: Unit

    func checkNotBound: Unit

    func checkNotConnected: Unit

    prop readTimeout: std.core.Option<std.core.Duration>

    prop writeTimeout: std.core.Option<std.core.Duration>

    prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig>

    func write(payload: std.core.Array<UInt8>): Unit

    func read(buffer: std.core.Array<UInt8>): Int64

    func send(payload: std.core.Array<UInt8>, destination: std.net.SocketAddress): Int64

    func receiveFrom(buffer: std.core.Array<UInt8>): std.core.Option<(std.net.SocketAddress, Int64)>

    func connect(timeout: std.core.Option<std.core.Duration>, shouldBeBound: Bool, mayBeAlreadyConnected: Bool): Unit

    func disconnect: Unit

    func bind(backlogSize: std.core.Option<Int32>): Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.SocketCommon<NS>>

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func isClosed: Bool

    func close: Unit

    func toString: std.core.String

    let holder: std.net.SocketHolder<NS>

    let kind: std.net.AddressFamily
}

struct NonReentrantReadLock <: std.sync.IReentrantMutex {
    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    let m: std.sync.Monitor

    let useCounter: std.sync.AtomicInt64

    init(m: std.sync.Monitor, useCounter: std.sync.AtomicInt64) { /* compiled code */ }
}

extend Result <: ToString {
    func toString: std.core.String
}

interface DatagramSocket <: std.core.Resource & std.core.ToString {
    prop localAddress: std.net.SocketAddress

    prop remoteAddress: std.core.Option<std.net.SocketAddress>

    prop receiveTimeout: std.core.Option<std.core.Duration>

    prop sendTimeout: std.core.Option<std.core.Duration>

    func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64)

    func sendTo(address: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit
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

func CJ_SOCKET_BufferInit(`handle`: Int64, rBufSize: Int32, wBufSize: Int32): CPointer<std.net.SocketBuffer>

func CJ_SOCKET_BufferRead(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, flags: Int32): Int32

func CJ_SOCKET_BufferRecvFrom(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, readSize: Int32, timeout: Int64, source: CPointer<std.net.SockAddr>, flags: Int32): Int32

func CJ_SOCKET_BufferWrite(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, flags: Int32): Int32

func CJ_SOCKET_BufferSendto(sockBuf: CPointer<std.net.SocketBuffer>, bufOff: UIntNative, writeSize: Int32, timeout: Int64, dest: CPointer<std.net.SockAddr>, flags: Int32): Int32

func CJ_SOCKET_BufferRCopy(sockBuf: CPointer<std.net.SocketBuffer>, arrBuf: CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

func CJ_SOCKET_BufferWCopy(sockBuf: CPointer<std.net.SocketBuffer>, arrBuf: CPointer<UInt8>, bufLen: Int64, copyLen: Int32): Int32

func CJ_SOCKET_BufferClose(sockBuf: CPointer<std.net.SocketBuffer>, `handle`: Int64): Int32

enum SocketNet <: std.core.ToString & std.core.Equatable<std.net.SocketNet> {
    | TCP

    | UDP

    | UNIX

    func toString: std.core.String

    operator func ==(that: std.net.SocketNet): Bool

    operator func !=(that: std.net.SocketNet): Bool
}

enum SocketMode <: std.core.Equal<std.net.SocketMode> {
    | StreamingMode

    | DatagramMode

    | SequentialMode

    operator func ==(other: std.net.SocketMode): Bool
}

class SocketException <: std.io.IOException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String

    func throwClosedException: Nothing

    func notYetConnected: Nothing

    func notYetBound: Nothing

    func notYetBoundNeedBind: Nothing

    func alreadyBound: Nothing

    func alreadyConnected: Nothing
}

class SocketTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
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

func CJ_SockErrnoGet: Int32

func CJ_SockKeepAliveSet(sock: Int64, cfg: CPointer<std.net.SockKeepAliveCfgS>): Int32

func CJ_SockOptionSet(sock: Int64, level: Int32, optname: Int32, optval: CPointer<Unit>, oplen: Int32): Int32

func CJ_SockOptionGet(sock: Int64, level: Int32, optname: Int32, optval: CPointer<Unit>, oplen: CPointer<Int32>): Int32

func CJ_SOCKET_GetErrMessage(errno: Int32): CString

func CJ_GetOptionInt(sock: Int64, level: Int32, optname: Int32): Int32

func CJ_SetOptionInt(sock: Int64, level: Int32, optname: Int32, optval: Int32): Int32

func CJ_RawAddressCreate(arr: CPointer<UInt8>, size: Int32): CPointer<std.net.SockAddr>

func CJ_RawAddressLenGet(addrPtr: CPointer<std.net.SockAddr>): UIntNative

func CJ_RawAddressDestroy(addrPtr: CPointer<std.net.SockAddr>): Unit

func getAddrPointer(addr: std.core.Array<UInt8>): (std.core.CPointerHandle<UInt8>, CPointer<std.net.SockAddr>)

func releaseAddrPointer(`handle`: std.core.CPointerHandle<UInt8>, addrPtr: CPointer<std.net.SockAddr>): Unit

func CJ_MRT_TcpsockInit: Int32

func CJ_MRT_UdpsockInit: Int32

func CJ_MRT_RawsockInit: Int32

let CJ_MRT_TCP_SOCK_INIT: Int32

let CJ_MRT_UDP_SOCK_INIT: Int32

let CJ_MRT_RAW_SOCK_INIT: Int32

func localAddrGet(`handle`: Int64, addr: CPointer<std.net.SockAddr>): Int32

func peerAddrGet(`handle`: Int64, addr: CPointer<std.net.SockAddr>): Int32

func getAddress(`handle`: Int64, getAddressFunc: (Int64, CPointer<std.net.SockAddr>) -> Int32, msg: std.core.String): std.net.SockAddr

func getLocalAddress(`handle`: Int64): std.net.SocketAddress

func getRemoteAddress(`handle`: Int64): std.net.SocketAddress

func CJ_MRT_SockCreate(domain: Int32, sock_type: Int32, protocol: Int32, net: CString): Int64

func CJ_MRT_SockBind(sock: Int64, addr: CPointer<std.net.SockAddr>, backlog: Int32): Int64

func CJ_MRT_SockAccept(sock: Int64, addr: CPointer<std.net.SockAddr>): Int64

func CJ_MRT_SockAcceptTimeout(sock: Int64, addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int64

func CJ_MRT_SockConnect(sock: Int64, local_addr: CPointer<std.net.SockAddr>, peer_addr: CPointer<std.net.SockAddr>): Int64

func CJ_MRT_SockConnectTimeout(sock: Int64, local_addr: CPointer<std.net.SockAddr>, peer_addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int64

func CJ_MRT_SockDisconnect(sock: Int64, isIpv6: Bool): Int32

func CJ_MRT_SockClose(sock: Int64): Int32

func CJ_MRT_SockShutdown(sock: Int64): Int32

func CJ_MRT_SockSend(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32): Int32

func CJ_MRT_SockSendTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

func CJ_MRT_SockSendNonBlock(`handle`: Int64, buf: CPointer<UInt8>, len: Int32, flags: Int32): Int32

func CJ_MRT_SockRecv(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32): Int32

func CJ_MRT_SockRecvTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, timeout: UInt64): Int32

func CJ_MRT_SockRecvNonBlock(`handle`: Int64, buf: CPointer<UInt8>, len: Int32, flags: Int32): Int32

func CJ_MRT_SockRecvfromTimeout(sock: Int64, buf: CPointer<UInt8>, length: UInt32, flags: Int32, addr: CPointer<std.net.SockAddr>, timeout: UInt64): Int32

func CJ_MRT_SockWaitRecv(`handle`: Int64): Int32

func CJ_MRT_SockWaitRecvTimeout(`handle`: Int64, timeout: UInt64): Int32

func CJ_MRT_SockWaitSend(`handle`: Int64): Int32

func CJ_MRT_SockWaitSendTimeout(`handle`: Int64, timeout: UInt64): Int32

func CJ_MRT_SockListen(sockfd: Int64, backlog: Int32): Int64

func CJ_MRT_SockSendtoNonBlock(sockfd: Int64, buf: CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: CPointer<std.net.SockAddr>): Int32

func CJ_MRT_SockRecvfromNonBlock(sockfd: Int64, buf: CPointer<UInt8>, size: Int32, flags: Int32, sockaddr: CPointer<std.net.SockAddr>): Int32

func CJ_MRT_SockLocalAddrGet(sock: Int64, addr: CPointer<std.net.SockAddr>): Int32

func CJ_MRT_SockPeerAddrGet(sock: Int64, addr: CPointer<std.net.SockAddr>): Int32

enum ErrnoLabel <: std.core.ToString {
    | Accept

    | Connect

    | Read

    | Write

    | CreateSock

    | Bind

    | Listen

    func toString: std.core.String
}

func getSocketError: Int32

func socketProcessErrno(label: std.net.ErrnoLabel): Nothing

func sockError(errno: Int32): std.core.String

func isSocketErrorInvalidArgument(errno: Int32): Bool

type ActualPlatformSocket = std.net.DopraOtherSocketImpl

class DopraOtherSocketImpl <: std.net.DopraSocketImpl<std.net.DopraOtherSocketImpl> {
    let socketBufferPtr: CPointer<std.net.SocketBuffer>

    func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit

    func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64>

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraOtherSocketImpl>

    func receiveFrom(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)>

    func send(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>, destination: std.net.SocketAddress): Int64

    func shutdown: Unit

    func dispose: Unit

    func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraOtherSocketImpl

    func onHandleReplaced(newHandle: Int64): Unit
}

type ActualTcpPlatformSocket = std.net.DopraOtherSocketImpl

class DopraSocketImpl <: std.core.Object & std.net.PlatformSocket<Self> {
    var address: std.core.Option<std.net.SockAddr>

    var isIPv6: Bool

    prop `handle`: Int64

    func connect(localAddress: std.core.Option<std.net.SocketAddress>, address: std.net.SocketAddress, timeout: std.core.Option<std.core.Duration>): Unit

    func onHandleReplaced(newHandle: Int64): Unit

    func disconnect: Unit

    func bindListen(address: std.net.SocketAddress, backlogSize: std.core.Option<Int32>, ignoreAlreadyBound: Bool): Unit

    func acceptImpl(timeout: std.core.Option<std.core.Duration>): (Int64, std.net.SocketNet)

    func shutdown: Unit

    func detectLocalAddress: std.core.Option<std.net.SocketAddress>

    func detectRemoteAddress: std.core.Option<std.net.SocketAddress>

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func setKeepAlive(config: std.core.Option<std.net.SocketKeepAliveConfig>): Bool

    func create(_: std.net.SocketNet, _: std.net.AddressFamily, _: std.net.SocketMode): Self

    func createSocket(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): Int64

    func toDopraTimeout(timeout: std.core.Duration): UInt64

    var _handle: std.sync.AtomicInt64

    init(_handle: std.sync.AtomicInt64) { /* compiled code */ }
}

class DopraTcpSocketImpl <: std.net.DopraSocketImpl<std.net.DopraTcpSocketImpl> {
    func write(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): Unit

    func read(buffer: std.core.Array<UInt8>, timeout: std.core.Option<std.core.Duration>): std.core.Option<Int64>

    func accept(timeout: std.core.Option<std.core.Duration>): std.core.Option<std.net.DopraTcpSocketImpl>

    func send(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>, _: std.net.SocketAddress): Int64

    func receiveFrom(_: std.core.Array<UInt8>, _: std.core.Option<std.core.Duration>): std.core.Option<(std.net.SocketAddress, Int64)>

    func dispose: Unit

    func onHandleReplaced(_: Int64): Unit

    func create(net: std.net.SocketNet, kind: std.net.AddressFamily, mode: std.net.SocketMode): std.net.DopraTcpSocketImpl
}

enum DopraAsyncResult {
    | BytesTransferred(UInt32)

    | RetryAgain

    | EOF
}

func setFamily(ptr: CPointer<Unit>, v: Int16): Unit

func readFamily(ptr: CPointer<Unit>): Int16

struct RawAddress {
    let addr_: std.core.Array<UInt8>

    prop addr: std.core.Array<UInt8>

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

    operator func ==(other: std.net.HolderState): Bool

    operator func !=(other: std.net.HolderState): Bool
}

struct SocketState {
    let Initial: std.net.SocketState

    let Terminal: std.net.SocketState

    let JustAccepted: std.net.SocketState

    prop useCounter: UInt32

    prop disposedOrShuttingDown: Bool

    prop nativeState: std.net.HolderState

    prop connected: Bool

    prop bound: Bool

    prop hasReaders: Bool

    prop hasWriters: Bool

    prop failed: Bool

    func ensureNotClosed: Unit

    func ensureConnected: Unit

    func ensureNotConnected: Unit

    func ensureBound: Unit

    func ensureBindInvoked: Unit

    func ensureNotBound: Unit

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

    prop sockFd: Int64

    prop localAddr: std.net.RawAddress

    prop localAddress: std.net.RawAddress

    prop remoteAddr: std.net.RawAddress

    prop remoteAddress: std.net.RawAddress

    prop readTimeout: std.core.Option<std.core.Duration>

    prop writeTimeout: std.core.Option<std.core.Duration>

    init(domain: std.net.SocketDomain, `type`: std.net.SocketType, protocol: std.net.ProtocolType) { /* compiled code */ }

    func bind(addr: std.net.RawAddress): Unit

    func listen(backlog: Int32): Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.RawSocket

    func connect(addr: std.net.RawAddress, timeout: std.core.Option<std.core.Duration>): Unit

    func close: Unit

    func sendTo(addr: std.net.RawAddress, buffer: std.core.Array<UInt8>, flags: Int32): Unit

    func receiveFrom(buffer: std.core.Array<UInt8>, flags: Int32): (std.net.RawAddress, Int64)

    func send(buffer: std.core.Array<UInt8>, flags: Int32): Unit

    func receive(buffer: std.core.Array<UInt8>, flags: Int32): Int64

    func setSocketOption(level: Int32, option: Int32, value: CPointer<UInt8>, len: Int32): Unit

    func getSocketOption(level: Int32, option: Int32, value: CPointer<UInt8>, len: CPointer<Int32>): Unit
}

class RawSocketImpl {
    var sockFd: Int64

    var isBound: Bool

    var socketBufferPtr: CPointer<std.net.SocketBuffer>

    var bufferInitialized: std.sync.AtomicBool

    init(sockFd: Int64) { /* compiled code */ }

    func bufferInit: Unit

    func bind(addr: std.net.RawAddress): Unit

    func connect(addr: std.net.RawAddress, timeout: Int64): Unit

    func sendTo(addr: std.core.Array<UInt8>, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit

    func send(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Unit

    func receiveFrom(size: Int64, buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): (std.net.RawAddress, Int64)

    func receive(buffer: std.core.Array<UInt8>, flags: Int32, timeout: Int64): Int64

    func isConnectionOriented(sockType: Int32): Bool

    func close: Unit
}

struct SocketDomain <: std.core.Equatable<std.net.SocketDomain> & std.core.ToString & std.core.Hashable {
    let val: Int32

    let IPV4: std.net.SocketDomain

    let IPV6: std.net.SocketDomain

    let UNIX: std.net.SocketDomain

    init(domain: Int32) { /* compiled code */ }

    operator func ==(r: std.net.SocketDomain): Bool

    operator func !=(r: std.net.SocketDomain): Bool

    func toString: std.core.String

    func hashCode: Int64
}

struct SocketType <: std.core.Equatable<std.net.SocketType> & std.core.ToString & std.core.Hashable {
    let val: Int32

    let STREAM: std.net.SocketType

    let DATAGRAM: std.net.SocketType

    let RAW: std.net.SocketType

    let SEQPACKET: std.net.SocketType

    init(`type`: Int32) { /* compiled code */ }

    operator func ==(r: std.net.SocketType): Bool

    operator func !=(r: std.net.SocketType): Bool

    func toString: std.core.String

    func hashCode: Int64
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

    operator func ==(r: std.net.ProtocolType): Bool

    operator func !=(r: std.net.ProtocolType): Bool

    func toString: std.core.String

    func hashCode: Int64
}

func getSocketOptionInt32(sockFd: Int64, level: Int32, option: Int32): Int32

func getOnceSendSize(size: Int64): Int64

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

func getTimeout(timeout: std.core.Option<std.core.Duration>): Int64

let MAX_TIMEOUT_DURATION: std.core.Duration

func unwrapOption(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T>

func getOrFail(e: () -> std.core.Exception): (std.core.Option<T>) -> T

func formatError(errnum: Int32): std.core.String

func throwIfIPv4ZeroOnWindows(socketAddress: std.net.SocketAddress): Unit

func zeroMemory(buffer: CPointer<UInt8>, size: Int64): Unit

func resolveHelper(address: std.core.String): std.net.IPAddress

class TcpSocket <: std.core.Object & std.net.StreamingSocket & std.core.Equatable<std.net.TcpSocket> & std.core.Hashable {
    let id: Int64

    init(address: std.core.String, port: UInt16) { /* compiled code */ }

    init(address: std.net.SocketAddress) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress: std.core.Option<std.net.SocketAddress>) { /* compiled code */ }

    prop remoteAddress: std.net.SocketAddress

    prop localAddress: std.net.SocketAddress

    prop readTimeout: std.core.Option<std.core.Duration>

    prop writeTimeout: std.core.Option<std.core.Duration>

    prop bindToDevice: std.core.Option<std.core.String>

    prop keepAlive: std.core.Option<std.net.SocketKeepAliveConfig>

    prop noDelay: Bool

    prop quickAcknowledge: Bool

    prop linger: std.core.Option<std.core.Duration>

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    func read(buffer: std.core.Array<UInt8>): Int64

    func write(payload: std.core.Array<UInt8>): Unit

    func connect(timeout: std.core.Option<std.core.Duration>): Unit

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func close: Unit

    func isClosed: Bool

    operator func ==(other: std.net.TcpSocket): Bool

    operator func !=(other: std.net.TcpSocket): Bool

    func hashCode: Int64

    func toString: std.core.String

    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }
}

class TcpServerSocket <: std.core.Object & std.net.ServerSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    var backlogSize_: Int32

    prop localAddress: std.net.SocketAddress

    init(bindAt: UInt16) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    prop reuseAddress: Bool

    prop reusePort: Bool

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    prop bindToDevice: std.core.Option<std.core.String>

    prop backlogSize: Int64

    func bind: Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.TcpSocket

    func accept: std.net.TcpSocket

    func close: Unit

    func isClosed: Bool

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func toString: std.core.String
}

class UdpSocket <: std.core.Object & std.net.DatagramSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt: UInt16) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    prop remoteAddress: std.core.Option<std.net.SocketAddress>

    prop localAddress: std.net.SocketAddress

    prop receiveTimeout: std.core.Option<std.core.Duration>

    prop sendTimeout: std.core.Option<std.core.Duration>

    func close: Unit

    func isClosed: Bool

    func bind: Unit

    func connect(remote: std.net.SocketAddress): Unit

    func disconnect: Unit

    func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64)

    func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit

    func send(payload: std.core.Array<UInt8>): Unit

    func receive(buffer: std.core.Array<UInt8>): Int64

    prop reusePort: Bool

    prop reuseAddress: Bool

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func toString: std.core.String
}

func checkUnixSocketAddress(address: std.net.SocketAddress, name: std.core.String): std.net.SocketAddress

class UnixSocket <: std.core.Object & std.net.StreamingSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(path: std.core.String, localPath: std.core.Option<std.core.String>) { /* compiled code */ }

    init(address: std.net.SocketAddress, localAddress: std.core.Option<std.net.SocketAddress>) { /* compiled code */ }

    init(impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>) { /* compiled code */ }

    prop remoteAddress: std.net.SocketAddress

    prop localAddress: std.net.SocketAddress

    prop readTimeout: std.core.Option<std.core.Duration>

    prop writeTimeout: std.core.Option<std.core.Duration>

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    func read(buffer: std.core.Array<UInt8>): Int64

    func write(buffer: std.core.Array<UInt8>): Unit

    func connect(timeout: std.core.Option<std.core.Duration>): Unit

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func close: Unit

    func isClosed: Bool

    func toString: std.core.String
}

class UnixServerSocket <: std.core.Object & std.net.ServerSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    var backlogSize_: Int32

    prop localAddress: std.net.SocketAddress

    init(bindAt: std.core.String) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    prop backlogSize: Int64

    func bind: Unit

    func accept(timeout: std.core.Option<std.core.Duration>): std.net.UnixSocket

    func accept: std.net.UnixSocket

    func close: Unit

    func isClosed: Bool

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func toString: std.core.String
}

class UnixDatagramSocket <: std.core.Object & std.net.DatagramSocket {
    let impl: std.net.SocketCommon<std.net.DopraOtherSocketImpl>

    init(bindAt: std.core.String) { /* compiled code */ }

    init(bindAt: std.net.SocketAddress) { /* compiled code */ }

    prop remoteAddress: std.core.Option<std.net.SocketAddress>

    prop localAddress: std.net.SocketAddress

    prop receiveTimeout: std.core.Option<std.core.Duration>

    prop sendTimeout: std.core.Option<std.core.Duration>

    func close: Unit

    func isClosed: Bool

    func bind: Unit

    func connect(remotePath: std.core.String): Unit

    func connect(remote: std.net.SocketAddress): Unit

    func disconnect: Unit

    func receiveFrom(buffer: std.core.Array<UInt8>): (std.net.SocketAddress, Int64)

    func sendTo(recipient: std.net.SocketAddress, payload: std.core.Array<UInt8>): Unit

    func send(payload: std.core.Array<UInt8>): Unit

    func receive(buffer: std.core.Array<UInt8>): Int64

    prop sendBufferSize: Int64

    prop receiveBufferSize: Int64

    func getSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: CPointer<UIntNative>): Unit

    func setSocketOption(level: Int32, option: Int32, value: CPointer<Unit>, valueLength: UIntNative): Unit

    func getSocketOptionIntNative(level: Int32, option: Int32): IntNative

    func setSocketOptionIntNative(level: Int32, option: Int32, value: IntNative): Unit

    func getSocketOptionBool(level: Int32, option: Int32): Bool

    func setSocketOptionBool(level: Int32, option: Int32, value: Bool): Unit

    func toString: std.core.String
}

func findLastNoneZeroIdx(path: std.core.Array<UInt8>): Int64

class UnixSocketAddress <: std.net.SocketAddress & std.core.Equatable<std.net.UnixSocketAddress> {
    let _path: std.core.String

    let _len: Int64

    init(path: std.core.Array<UInt8>) { /* compiled code */ }

    init(path: std.core.String) { /* compiled code */ }

    prop size: Int64

    prop family: std.net.AddressFamily

    operator func ==(rhs: std.net.UnixSocketAddress): Bool

    operator func !=(rhs: std.net.UnixSocketAddress): Bool

    func getAddressBytes: std.core.Array<UInt8>

    func hashCode: Int64

    func toString: std.core.String
}
