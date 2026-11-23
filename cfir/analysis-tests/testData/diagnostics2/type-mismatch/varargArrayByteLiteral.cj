// RUN_PIPELINE_TILL: FRONTEND
import std.net.StreamingSocket

// 使用 SDK 的 StreamingSocket API 验证真实的 write(Array<Byte>) 声明和调用规则。
// 类型别名使用仓颉的 type 语法；泛型 Array 别名和 Byte 别名应展开为 Array<UInt8>。
type HandshakeByte = Byte
type HandshakeArray<T> = Array<T>
type HandshakeBytes = HandshakeArray<HandshakeByte>

class MariaConnection {
    var socket: StreamingSocket

    init(socket: StreamingSocket) {
        this.socket = socket
    }
}

abstract class TlsSocket <: StreamingSocket {}
class WrongSocket {}

func sslHandshake(connection: MariaConnection, tls: TlsSocket): Unit {
    connection.socket = tls
}

func rejectUnrelatedSocket(connection: MariaConnection, wrong: WrongSocket): Unit {
    connection.socket = <!TYPE_MISMATCH!>wrong<!>
}

// 完整 Array 优先按普通调用匹配；同一 SDK 成员还允许零个、一个和多个 Byte 实参。
func writeNullTerminated(connection: MariaConnection, value: Array<Byte>): Unit {
    connection.socket.write(value)
    connection.socket.write()
    connection.socket.write(0x00)
    connection.socket.write(0x00, 0xFF)
    connection.socket.write(<!CANNOT_CONVERT_LITERAL!>2<!>56)
}

func writeAliasedBytes(connection: MariaConnection, value: HandshakeBytes, byte: HandshakeByte): Unit {
    connection.socket.write(value)
    connection.socket.write(byte)
    connection.socket.write(byte, 0xFF)
}

// 形参声明本身经过别名展开后，仍满足最后一个非命名 Array 参数的变长调用规则。
func takesAliasedArray(value: HandshakeArray<HandshakeByte>): Unit {}
func takesAliasedBytes(value: HandshakeBytes): Unit {}
func takesAliasedByte(value: HandshakeByte): Unit {}

func aliasArrayControl(value: HandshakeBytes): Unit {
    takesAliasedArray(value)
    takesAliasedArray()
    takesAliasedArray(0x00)
    takesAliasedArray(0x00, 0xFF)
    takesAliasedBytes(value)
    takesAliasedBytes()
    takesAliasedBytes(0x00)
    takesAliasedBytes(0x00, 0xFF)
}

func aliasArrayRangeControl(): Unit {
    takesAliasedArray(<!CANNOT_CONVERT_LITERAL!>2<!>56)
    takesAliasedBytes(<!CANNOT_CONVERT_LITERAL!>2<!>56)
}

func takesByte(value: Byte): Unit {}

func fixedByteControl(): Unit {
    takesByte(0x00)
    takesByte(0xFF)
    takesAliasedByte(0x00)
    takesAliasedByte(0xFF)
}

// 固定 Byte 目标的越界与 Array 调用失败是两类诊断，不能混用期望。
func fixedByteRangeControl(): Unit {
    takesByte(<!LITERAL_NUMERIC_OVERFLOW!>256<!>)
    takesAliasedByte(<!LITERAL_NUMERIC_OVERFLOW!>256<!>)
}

// CJC 对该失败调用的主诊断范围只覆盖整数字面量首字符。
func rejectIncompatibleByteSuffix(connection: MariaConnection): Unit {
    connection.socket.write(<!CANNOT_CONVERT_LITERAL!>0<!>u16)
}

// 256 必须按各候选的期望类型检查范围；显式后缀必须保留其指定类型。
func chooseScalar(value: UInt8): Int64 {
    return 8
}

func chooseScalar(value: UInt16): Int64 {
    return 16
}

func scalarOverloadControl(): Int64 {
    let wide = chooseScalar(256)
    let narrow = chooseScalar(0u8)
    let explicitWide = chooseScalar(256u16)
    return wide + narrow + explicitWide
}

// Array 变参候选也必须逐元素按期望类型检查范围和显式后缀。
func chooseArray(value: Array<UInt8>): Int64 {
    return 8
}

func chooseArray(value: Array<UInt16>): Int64 {
    return 16
}

func arrayOverloadControl(): Int64 {
    let wide = chooseArray(256)
    let narrow = chooseArray(0u8)
    let explicitWide = chooseArray(256u16)
    return wide + narrow + explicitWide
}

// Option 数值字面量装箱按内部 Byte 定型，0u16 仍可合法装箱为 ?Byte。
func takesOptionalByte(value: ?Byte): Unit {}

func optionalByteSuffixControl(): Unit {
    takesOptionalByte(0u16)
}

// 装箱后的范围检查使用内部 UInt8，256u16 必须报告完整字面量越界。
func optionalByteSuffixRangeControl(): Unit {
    takesOptionalByte(<!LITERAL_NUMERIC_OVERFLOW!>256u16<!>)
}

main(): Unit {}
