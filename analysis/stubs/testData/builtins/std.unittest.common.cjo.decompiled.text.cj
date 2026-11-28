// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.common

import std.collection.*
import std.core.*
import std.convert.Parsable
import std.sync.ReentrantMutex
import std.collection.ArrayList
import std.collection.fold
import std.collection.enumerate
import std.convert.Formattable

let NOT_PRINTABLE_PLACEHOLDER: std.core.String = COMPILED_CODE /* compiled code */

public class Configuration <: std.core.Object & std.core.ToString {
    init() { /* compiled code */ }

    public func get<T>(key: std.unittest.common.KeyFor<T>): std.core.Option<T> { /* compiled code */ }

    public func getByName<T>(name: std.core.String): std.core.Option<T> { /* compiled code */ }

    public func set<T>(key: std.unittest.common.KeyFor<T>, value: T): Unit { /* compiled code */ }

    public func setByName<T>(name: std.core.String, value: T): Unit { /* compiled code */ }

    public func remove<T>(key: std.unittest.common.KeyFor<T>): std.core.Option<T> { /* compiled code */ }

    public func removeByName<T>(key: std.core.String): std.core.Option<T> { /* compiled code */ }

    public func clone(): std.unittest.common.Configuration { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public static func merge(parent: std.unittest.common.Configuration, child: std.unittest.common.Configuration): std.unittest.common.Configuration { /* compiled code */ }

    private let storage: std.collection.HashMap<std.unittest.common.ConfigurationKey, std.core.Any>
}

public interface DataShrinker<T> {
    public open func shrink(value: T): std.core.Iterable<T>
}

internal class NoShrinker<T> <: std.core.Object & std.unittest.common.DataShrinker<T> {
    public override func shrink(_: T): std.core.Iterable<T> { /* compiled code */ }

    internal static func instance(): std.unittest.common.DataShrinker<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

public interface KeyFor<T> {
    public open prop name: std.core.String
}

public struct KeyTags <: std.unittest.common.KeyFor<std.core.Array<std.core.String>> {
    public static prop tags: std.unittest.common.KeyTags {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public abstract open class ConfigurationKey <: std.core.Object & std.core.Equatable<std.unittest.common.ConfigurationKey> & std.core.Hashable {
    public override func hashCode(): Int64 { /* compiled code */ }

    protected func equals(that: std.unittest.common.ConfigurationKey): Bool

    public override operator func ==(that: std.unittest.common.ConfigurationKey): Bool { /* compiled code */ }

    public override operator func !=(that: std.unittest.common.ConfigurationKey): Bool { /* compiled code */ }

    public let name: std.core.String

    ConfigurationKey(name: std.core.String) { /* compiled code */ }
}

extend std.unittest.common.ConfigurationKey {
    public static func create<T>(name: std.core.String): std.unittest.common.ConfigurationKey { /* compiled code */ }
}

public interface DataProvider<T> {
    public open func provide(): std.core.Iterable<T>
}

public interface DataStrategy<T> {
    public open func provider(configuration: std.unittest.common.Configuration): std.unittest.common.DataProvider<T>

    public open func shrinker(configuration: std.unittest.common.Configuration): std.unittest.common.DataShrinker<T> { /* compiled code */ }

    public open prop isInfinite: Bool {
        get() { /* compiled code */ }
    }
}

extend std.core.Array<T> <: DataStrategy & DataProvider {
    public func provide(): std.core.Iterable<T> { /* compiled code */ }

    public prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    public func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T> { /* compiled code */ }
}

extend std.core.Range<T> <: DataStrategy & DataProvider {
    public func provide(): std.core.Iterable<T> { /* compiled code */ }

    public prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    public func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T> { /* compiled code */ }
}

protected class JsonException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

protected class JsonNull <: std.unittest.common.JsonValue {
    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

protected enum JsonKind {
    | JsNull

    | JsBool

    | JsInt

    | JsFloat

    | JsString

    | JsArray

    | JsObject
}

protected abstract class JsonValue <: std.core.Object & std.core.ToString {
    public func toString(): std.core.String

    public func toJsonString(): std.core.String

    public static func fromStr(s: std.core.String): std.unittest.common.JsonValue { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind

    public func asNull(): std.unittest.common.JsonNull { /* compiled code */ }

    public func asBool(): std.unittest.common.JsonBool { /* compiled code */ }

    public func asInt(): std.unittest.common.JsonInt { /* compiled code */ }

    public func asFloat(): std.unittest.common.JsonFloat { /* compiled code */ }

    public func asString(): std.unittest.common.JsonString { /* compiled code */ }

    public func asArray(): std.unittest.common.JsonArray { /* compiled code */ }

    public func asObject(): std.unittest.common.JsonObject { /* compiled code */ }

    init() { /* compiled code */ }
}

protected class JsonBool <: std.unittest.common.JsonValue {
    private var value: Bool

    init(bv: Bool) { /* compiled code */ }

    public func getValue(): Bool { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }
}

protected class JsonInt <: std.unittest.common.JsonValue {
    private var value: Int64

    init(iv: Int64) { /* compiled code */ }

    public func getValue(): Int64 { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }
}

protected class JsonFloat <: std.unittest.common.JsonValue {
    private var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    public func getValue(): Float64 { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }
}

protected class JsonString <: std.unittest.common.JsonValue {
    internal var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    public func getValue(): std.core.String { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }
}

protected class JsonArray <: std.unittest.common.JsonValue {
    private var items: std.collection.ArrayList<std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.JsonValue>) { /* compiled code */ }

    init(list: std.core.Array<std.unittest.common.JsonValue>) { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }

    public func toJsonString(depth: Int64, bracketInNewLine!: Bool = COMPILED_CODE /* compiled code */, indent!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func size(): Int64 { /* compiled code */ }

    public func add(jv: std.unittest.common.JsonValue): std.unittest.common.JsonArray { /* compiled code */ }

    public func get(index: Int64): std.core.Option<std.unittest.common.JsonValue> { /* compiled code */ }

    public operator func [](index: Int64): std.unittest.common.JsonValue { /* compiled code */ }

    public func getItems(): std.collection.ArrayList<std.unittest.common.JsonValue> { /* compiled code */ }
}

protected class JsonObject <: std.unittest.common.JsonValue {
    private var fields: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(map: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>) { /* compiled code */ }

    public func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    public func toJsonString(): std.core.String { /* compiled code */ }

    public func toJsonString(depth: Int64, bracketInNewLine!: Bool = COMPILED_CODE /* compiled code */, indent!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func size(): Int64 { /* compiled code */ }

    public func containsKey(key: std.core.String): Bool { /* compiled code */ }

    public func put(key: std.core.String, v: std.unittest.common.JsonValue): Unit { /* compiled code */ }

    public func get(key: std.core.String): std.core.Option<std.unittest.common.JsonValue> { /* compiled code */ }

    public operator func [](key: std.core.String): std.unittest.common.JsonValue { /* compiled code */ }

    public func getFields(): std.collection.HashMap<std.core.String, std.unittest.common.JsonValue> { /* compiled code */ }
}

protected interface ToJson {
    public open func toJson(): std.unittest.common.JsonValue

    public static func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel
}

protected abstract class DataModel {
    init() { /* compiled code */ }
}

extend std.unittest.common.DataModel <: ToJson {
    public static func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel { /* compiled code */ }

    public func toJson(): std.unittest.common.JsonValue { /* compiled code */ }
}

public class UnittestOptionValidationException <: std.core.Exception {
    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public enum OptionValidity {
    | UnknownOptionType

    | InvalidOption(std.core.String)

    | ValidOption(std.unittest.common.ConfigurationKey)
}

var unittestOptionsRegistryClosed: Bool

public struct OptionInfo {
    public let name: std.core.String

    public let userDefined: Bool

    public let description: std.core.Option<std.core.String>

    public let types: std.collection.HashMap<std.core.String, std.core.Option<std.core.String>>
}

let optionsInfo: std.collection.HashMap<std.core.String, std.unittest.common.OptionInfo>

public func registerOptionValidator(name: std.core.String, validator: (std.core.Any) -> std.unittest.common.OptionValidity): Unit { /* compiled code */ }

public func setOptionInfo(name: std.core.String, types: std.core.Array<std.core.String>, description!: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func setOrUpdateOptionInfo(name: std.core.String, description: std.core.Option<std.core.String>, ty: std.core.String, typeDescription: std.core.String): Unit { /* compiled code */ }

public enum Color <: std.core.Equatable<std.unittest.common.Color> {
    | RED

    | GREEN

    | YELLOW

    | BLUE

    | CYAN

    | MAGENTA

    | GRAY

    | DEFAULT_COLOR

    public operator func ==(that: std.unittest.common.Color): Bool { /* compiled code */ }

    public operator func !=(that: std.unittest.common.Color): Bool { /* compiled code */ }
}

public abstract class PrettyPrinter {
    protected func put(s: std.core.String): Unit

    protected open func putNewLine(): Unit { /* compiled code */ }

    protected func setColor(color: std.unittest.common.Color): Unit

    private var currentIndent: std.core.String

    private var needIndent: Bool

    private var currentColor: std.unittest.common.Color

    public prop isTopLevel: Bool {
        get() { /* compiled code */ }
    }

    public func indent(body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func indent(indents: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func customOffset(symbols: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func colored(color: std.unittest.common.Color, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public open func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func colored(color: std.unittest.common.Color, text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func append(text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func appendCentered(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func appendLeftAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func appendRightAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func append<PP>(value: PP): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func newLine(): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func appendLine(text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func appendLine<PP>(value: PP): std.unittest.common.PrettyPrinter { /* compiled code */ }

    internal let indentationSize: UInt64

    internal let startingIndent: UInt64

    PrettyPrinter(indentationSize!: UInt64 = COMPILED_CODE /* compiled code */, startingIndent!: UInt64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

public interface PrettyPrintable {
    public open func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

internal enum PrettyPrintableTextChunk {
    | PPTextPiece(std.core.String)

    | PPColorSwitch(std.unittest.common.Color)

    | PPNewLine
}

public class PrettyText <: std.unittest.common.PrettyPrinter & std.unittest.common.PrettyPrintable & std.core.ToString {
    init() { /* compiled code */ }

    init(string: std.core.String) { /* compiled code */ }

    public static func of<PP>(pp: PP): std.unittest.common.PrettyText { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    protected override func put(s: std.core.String): Unit { /* compiled code */ }

    protected override func setColor(color: std.unittest.common.Color): Unit { /* compiled code */ }

    protected override func putNewLine(): Unit { /* compiled code */ }

    public func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal let data: std.collection.ArrayList<std.unittest.common.PrettyPrintableTextChunk>
}

extend std.core.Array<T> <: PrettyPrintable {
    public func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: PrettyPrintable {
    public func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

protected func toStringOrPlaceholder<T>(value: T): std.core.String { /* compiled code */ }

protected func toStringQuotedOrPlaceholder<T>(value: T): std.core.String { /* compiled code */ }

protected func quoteString<T>(value: T): std.core.String { /* compiled code */ }

extend std.unittest.common.Color <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.Color { /* compiled code */ }
}

protected interface Serializable<T> {
    public open func serializeInternal(): std.unittest.common.DataModel

    public static func deserialize(dm: std.unittest.common.DataModel): T
}

extend std.unittest.common.PrettyText <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.PrettyText { /* compiled code */ }
}

protected class DataModelNull <: std.unittest.common.DataModel {
    init() { /* compiled code */ }
}

protected class DataModelBool <: std.unittest.common.DataModel {
    private var value: Bool

    init(bv: Bool) { /* compiled code */ }

    public func getValue(): Bool { /* compiled code */ }
}

protected class DataModelInt <: std.unittest.common.DataModel {
    private var value: Int64

    init(iv: Int64) { /* compiled code */ }

    public func getValue(): Int64 { /* compiled code */ }
}

protected class DataModelFloat <: std.unittest.common.DataModel {
    private var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    public func getValue(): Float64 { /* compiled code */ }
}

protected class DataModelString <: std.unittest.common.DataModel {
    private var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    public func getValue(): std.core.String { /* compiled code */ }
}

protected class DataModelSeq <: std.unittest.common.DataModel {
    private var items: std.collection.ArrayList<std.unittest.common.DataModel>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.DataModel>) { /* compiled code */ }

    public func getItems(): std.collection.ArrayList<std.unittest.common.DataModel> { /* compiled code */ }

    public func add(dm: std.unittest.common.DataModel): Unit { /* compiled code */ }
}

protected class DataModelStruct <: std.unittest.common.DataModel {
    private var fields: std.collection.ArrayList<std.unittest.common.Field>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.Field>) { /* compiled code */ }

    public func getFields(): std.collection.ArrayList<std.unittest.common.Field> { /* compiled code */ }

    public func add(fie: std.unittest.common.Field): std.unittest.common.DataModelStruct { /* compiled code */ }

    public func get(key: std.core.String): std.unittest.common.DataModel { /* compiled code */ }
}

protected class Field {
    private var name: std.core.String

    internal var data: std.unittest.common.DataModel

    init(name: std.core.String, data: std.unittest.common.DataModel) { /* compiled code */ }

    public func getName(): std.core.String { /* compiled code */ }

    public func getData(): std.unittest.common.DataModel { /* compiled code */ }
}

protected func field<T>(name: std.core.String, data: T): std.unittest.common.Field { /* compiled code */ }

protected class DataModelException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

extend Int64 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Int64 { /* compiled code */ }
}

extend Int32 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Int32 { /* compiled code */ }
}

extend Int16 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Int16 { /* compiled code */ }
}

extend Int8 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Int8 { /* compiled code */ }
}

extend UInt64 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): UInt64 { /* compiled code */ }
}

extend UInt32 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): UInt32 { /* compiled code */ }
}

extend UInt16 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): UInt16 { /* compiled code */ }
}

extend UInt8 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): UInt8 { /* compiled code */ }
}

extend Float64 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Float64 { /* compiled code */ }
}

extend Float32 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Float32 { /* compiled code */ }
}

extend Float16 <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Float16 { /* compiled code */ }
}

extend Bool <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Bool { /* compiled code */ }
}

extend std.core.String <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.core.String { /* compiled code */ }
}

extend Rune <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): Rune { /* compiled code */ }
}

extend std.core.Option<T> <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.core.Option<T> { /* compiled code */ }
}

extend std.core.Array<T> <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.core.Array<T> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.collection.ArrayList<T> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.collection.HashSet<T> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.collection.HashMap<K, V> { /* compiled code */ }
}

protected interface TextWidth {
    public open func width(): Int64
}

extend Rune <: TextWidth {
    public func width(): Int64 { /* compiled code */ }
}

extend std.core.String <: TextWidth {
    public func width(): Int64 { /* compiled code */ }
}

protected func shieldEscapeSeq(s: std.core.String, shieldSingleQuote!: Bool = COMPILED_CODE /* compiled code */, shieldDoubleQuote!: Bool = COMPILED_CODE /* compiled code */, shieldBackslash!: Bool = COMPILED_CODE /* compiled code */, shieldInterpolation!: Bool = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

extend std.unittest.common.PrettyPrinter {
    protected func space(): std.unittest.common.PrettyPrinter { /* compiled code */ }

    protected func appendLine(): std.unittest.common.PrettyPrinter { /* compiled code */ }
}
