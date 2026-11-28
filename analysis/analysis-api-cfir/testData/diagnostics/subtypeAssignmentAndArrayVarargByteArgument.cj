import std.io.ByteBuffer
import std.net.StreamingSocket

// StreamingSocket 的接口成员未在此实现，子类型声明必须是抽象类。
abstract class TlsSocket <: StreamingSocket {}

type HandshakeByte = Byte
type HandshakeArray<T> = Array<T>
type HandshakeBytes = HandshakeArray<HandshakeByte>

class MariaConnection {
    var socket: StreamingSocket

    init(socket: StreamingSocket) {
        this.socket = socket
    }
}

// 完整 Array 按普通调用匹配；零个和多个元素使用同一个 SDK 成员的变长调用规则。
func writeNullTerminated(buffer: ByteBuffer, value: Array<Byte>): Unit {
    buffer.write(value)
    buffer.write()
    buffer.write(0x00)
    buffer.write(0x00, 0xFF)
}

func writeSocketBytes(connection: MariaConnection, value: Array<Byte>): Unit {
    connection.socket.write(value)
    connection.socket.write()
    connection.socket.write(0x00)
    connection.socket.write(0x00, 0xFF)
}

func writeAliasedBytes(connection: MariaConnection, buffer: ByteBuffer, value: HandshakeBytes, byte: HandshakeByte): Unit {
    connection.socket.write(value)
    connection.socket.write(byte)
    connection.socket.write(byte, 0xFF)
    buffer.write(value)
    buffer.write(byte)
    buffer.write(byte, 0xFF)
}

// Array 形参自身使用别名，验证泛型别名与嵌套别名展开后的变长调用。
func takesAliasedArray(value: HandshakeArray<HandshakeByte>): Unit {}
func takesAliasedBytes(value: HandshakeBytes): Unit {}

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

func writeSingleByte(value: Byte): Unit {}
func takesAliasedByte(value: HandshakeByte): Unit {}

func fixedByteControl(): Unit {
    writeSingleByte(0x00)
    writeSingleByte(0xFF)
    takesAliasedByte(0x00)
    takesAliasedByte(0xFF)
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

func probe(connection: MariaConnection, tls: TlsSocket, buffer: ByteBuffer, value: Array<Byte>): Unit {
    connection.socket = tls
    writeNullTerminated(buffer, value)
    writeSocketBytes(connection, value)
    writeAliasedBytes(connection, buffer, value, 0x00)
    aliasArrayControl(value)
    writeSingleByte(0x00)
    fixedByteControl()
    scalarOverloadControl()
    arrayOverloadControl()
    optionalByteSuffixControl()
}
