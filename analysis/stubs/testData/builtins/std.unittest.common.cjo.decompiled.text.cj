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

let NOT_PRINTABLE_PLACEHOLDER: std.core.String

class Configuration <: std.core.Object & std.core.ToString {
    init() { /* compiled code */ }

    func `get`(key: std.unittest.common.KeyFor<T>): std.core.Option<T> { /* compiled code */ }

    func getByName(name: std.core.String): std.core.Option<T> { /* compiled code */ }

    func `set`(key: std.unittest.common.KeyFor<T>, value: T): Unit { /* compiled code */ }

    func setByName(name: std.core.String, value: T): Unit { /* compiled code */ }

    func remove(key: std.unittest.common.KeyFor<T>): std.core.Option<T> { /* compiled code */ }

    func removeByName(key: std.core.String): std.core.Option<T> { /* compiled code */ }

    func clone(): std.unittest.common.Configuration { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    static func merge(parent: std.unittest.common.Configuration, child: std.unittest.common.Configuration): std.unittest.common.Configuration { /* compiled code */ }

    let storage: std.collection.HashMap<std.unittest.common.ConfigurationKey, std.core.Any>
}

interface DataShrinker {
    func shrink(value: T): std.core.Iterable<T> { /* compiled code */ }
}

class NoShrinker <: std.core.Object & std.unittest.common.DataShrinker<T> {
    override func shrink(_: T): std.core.Iterable<T> { /* compiled code */ }

    static func instance(): std.unittest.common.DataShrinker<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

interface KeyFor {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }
}

struct KeyTags <: std.unittest.common.KeyFor<std.core.Array<std.core.String>> {
    static prop tags: std.unittest.common.KeyTags {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

class ConfigurationKey <: std.core.Object & std.core.Equatable<std.unittest.common.ConfigurationKey> & std.core.Hashable {
    override func hashCode(): Int64 { /* compiled code */ }

    func equals(that: std.unittest.common.ConfigurationKey): Bool { /* compiled code */ }

    override operator func ==(that: std.unittest.common.ConfigurationKey): Bool { /* compiled code */ }

    override operator func !=(that: std.unittest.common.ConfigurationKey): Bool { /* compiled code */ }

    let name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

extend ConfigurationKey {
    static func create(name: std.core.String): std.unittest.common.ConfigurationKey { /* compiled code */ }
}

interface DataProvider {
    func provide(): std.core.Iterable<T> { /* compiled code */ }
}

interface DataStrategy {
    func provider(configuration: std.unittest.common.Configuration): std.unittest.common.DataProvider<T> { /* compiled code */ }

    func shrinker(configuration: std.unittest.common.Configuration): std.unittest.common.DataShrinker<T> { /* compiled code */ }

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }
}

extend Array <: DataStrategy & DataProvider {
    func provide(): std.core.Iterable<T> { /* compiled code */ }

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T> { /* compiled code */ }
}

extend Range <: DataStrategy & DataProvider {
    func provide(): std.core.Iterable<T> { /* compiled code */ }

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T> { /* compiled code */ }
}

class JsonException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class JsonNull <: std.unittest.common.JsonValue {
    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

enum JsonKind {
    | JsNull

    | JsBool

    | JsInt

    | JsFloat

    | JsString

    | JsArray

    | JsObject
}

class JsonValue <: std.core.Object & std.core.ToString {
    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }

    static func fromStr(s: std.core.String): std.unittest.common.JsonValue { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func asNull(): std.unittest.common.JsonNull { /* compiled code */ }

    func asBool(): std.unittest.common.JsonBool { /* compiled code */ }

    func asInt(): std.unittest.common.JsonInt { /* compiled code */ }

    func asFloat(): std.unittest.common.JsonFloat { /* compiled code */ }

    func asString(): std.unittest.common.JsonString { /* compiled code */ }

    func asArray(): std.unittest.common.JsonArray { /* compiled code */ }

    func asObject(): std.unittest.common.JsonObject { /* compiled code */ }

    init() { /* compiled code */ }
}

class JsonBool <: std.unittest.common.JsonValue {
    var value: Bool

    init(bv: Bool) { /* compiled code */ }

    func getValue(): Bool { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }
}

class JsonInt <: std.unittest.common.JsonValue {
    var value: Int64

    init(iv: Int64) { /* compiled code */ }

    func getValue(): Int64 { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }
}

class JsonFloat <: std.unittest.common.JsonValue {
    var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    func getValue(): Float64 { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }
}

class JsonString <: std.unittest.common.JsonValue {
    var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    func getValue(): std.core.String { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }
}

class JsonArray <: std.unittest.common.JsonValue {
    var items: std.collection.ArrayList<std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.JsonValue>) { /* compiled code */ }

    init(list: std.core.Array<std.unittest.common.JsonValue>) { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }

    func toJsonString(depth: Int64, bracketInNewLine: Bool, indent: std.core.String): std.core.String { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func size(): Int64 { /* compiled code */ }

    func add(jv: std.unittest.common.JsonValue): std.unittest.common.JsonArray { /* compiled code */ }

    func `get`(index: Int64): std.core.Option<std.unittest.common.JsonValue> { /* compiled code */ }

    operator func [](index: Int64): std.unittest.common.JsonValue { /* compiled code */ }

    func getItems(): std.collection.ArrayList<std.unittest.common.JsonValue> { /* compiled code */ }
}

class JsonObject <: std.unittest.common.JsonValue {
    var fields: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(map: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>) { /* compiled code */ }

    func kind(): std.unittest.common.JsonKind { /* compiled code */ }

    func toJsonString(): std.core.String { /* compiled code */ }

    func toJsonString(depth: Int64, bracketInNewLine: Bool, indent: std.core.String): std.core.String { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func size(): Int64 { /* compiled code */ }

    func containsKey(key: std.core.String): Bool { /* compiled code */ }

    func put(key: std.core.String, v: std.unittest.common.JsonValue): Unit { /* compiled code */ }

    func `get`(key: std.core.String): std.core.Option<std.unittest.common.JsonValue> { /* compiled code */ }

    operator func [](key: std.core.String): std.unittest.common.JsonValue { /* compiled code */ }

    func getFields(): std.collection.HashMap<std.core.String, std.unittest.common.JsonValue> { /* compiled code */ }
}

interface ToJson {
    func toJson(): std.unittest.common.JsonValue { /* compiled code */ }

    static func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel { /* compiled code */ }
}

class DataModel {
    init() { /* compiled code */ }
}

extend DataModel <: ToJson {
    static func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel { /* compiled code */ }

    func toJson(): std.unittest.common.JsonValue { /* compiled code */ }
}

class UnittestOptionValidationException <: std.core.Exception {
    override func getClassName(): std.core.String { /* compiled code */ }
}

enum OptionValidity {
    | UnknownOptionType

    | InvalidOption(std.core.String)

    | ValidOption(std.unittest.common.ConfigurationKey)
}

var unittestOptionsRegistryClosed: Bool

struct OptionInfo {
    let name: std.core.String

    let userDefined: Bool

    let description: std.core.Option<std.core.String>

    let types: std.collection.HashMap<std.core.String, std.core.Option<std.core.String>>
}

let optionsInfo: std.collection.HashMap<std.core.String, std.unittest.common.OptionInfo>

func registerOptionValidator(name: std.core.String, validator: (std.core.Any) -> std.unittest.common.OptionValidity): Unit { /* compiled code */ }

func setOptionInfo(name: std.core.String, types: std.core.Array<std.core.String>, description: std.core.Option<std.core.String>): Unit { /* compiled code */ }

func setOrUpdateOptionInfo(name: std.core.String, description: std.core.Option<std.core.String>, ty: std.core.String, typeDescription: std.core.String): Unit { /* compiled code */ }

enum Color <: std.core.Equatable<std.unittest.common.Color> {
    | RED

    | GREEN

    | YELLOW

    | BLUE

    | CYAN

    | MAGENTA

    | GRAY

    | DEFAULT_COLOR

    operator func ==(that: std.unittest.common.Color): Bool { /* compiled code */ }

    operator func !=(that: std.unittest.common.Color): Bool { /* compiled code */ }
}

class PrettyPrinter {
    func put(s: std.core.String): Unit { /* compiled code */ }

    func putNewLine(): Unit { /* compiled code */ }

    func setColor(color: std.unittest.common.Color): Unit { /* compiled code */ }

    var currentIndent: std.core.String

    var needIndent: Bool

    var currentColor: std.unittest.common.Color

    prop isTopLevel: Bool {
        get() { /* compiled code */ }
    }

    func indent(body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func indent(indents: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func customOffset(symbols: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func colored(color: std.unittest.common.Color, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func colored(color: std.unittest.common.Color, text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func append(text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendCentered(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendLeftAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendRightAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func append(value: PP): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func newLine(): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendLine(text: std.core.String): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendLine(value: PP): std.unittest.common.PrettyPrinter { /* compiled code */ }

    let indentationSize: UInt64

    let startingIndent: UInt64

    init(indentationSize: UInt64, startingIndent: UInt64) { /* compiled code */ }
}

interface PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

enum PrettyPrintableTextChunk {
    | PPTextPiece(std.core.String)

    | PPColorSwitch(std.unittest.common.Color)

    | PPNewLine
}

class PrettyText <: std.unittest.common.PrettyPrinter & std.unittest.common.PrettyPrintable & std.core.ToString {
    init() { /* compiled code */ }

    init(string: std.core.String) { /* compiled code */ }

    static func of(pp: PP): std.unittest.common.PrettyText { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    override func put(s: std.core.String): Unit { /* compiled code */ }

    override func setColor(color: std.unittest.common.Color): Unit { /* compiled code */ }

    override func putNewLine(): Unit { /* compiled code */ }

    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let data: std.collection.ArrayList<std.unittest.common.PrettyPrintableTextChunk>
}

extend Array <: PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

extend ArrayList <: PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

func toStringOrPlaceholder(value: T): std.core.String { /* compiled code */ }

func toStringQuotedOrPlaceholder(value: T): std.core.String { /* compiled code */ }

func quoteString(value: T): std.core.String { /* compiled code */ }

extend Color <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.Color { /* compiled code */ }
}

interface Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): T { /* compiled code */ }
}

extend PrettyText <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.PrettyText { /* compiled code */ }
}

class DataModelNull <: std.unittest.common.DataModel {
    init() { /* compiled code */ }
}

class DataModelBool <: std.unittest.common.DataModel {
    var value: Bool

    init(bv: Bool) { /* compiled code */ }

    func getValue(): Bool { /* compiled code */ }
}

class DataModelInt <: std.unittest.common.DataModel {
    var value: Int64

    init(iv: Int64) { /* compiled code */ }

    func getValue(): Int64 { /* compiled code */ }
}

class DataModelFloat <: std.unittest.common.DataModel {
    var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    func getValue(): Float64 { /* compiled code */ }
}

class DataModelString <: std.unittest.common.DataModel {
    var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    func getValue(): std.core.String { /* compiled code */ }
}

class DataModelSeq <: std.unittest.common.DataModel {
    var items: std.collection.ArrayList<std.unittest.common.DataModel>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.DataModel>) { /* compiled code */ }

    func getItems(): std.collection.ArrayList<std.unittest.common.DataModel> { /* compiled code */ }

    func add(dm: std.unittest.common.DataModel): Unit { /* compiled code */ }
}

class DataModelStruct <: std.unittest.common.DataModel {
    var fields: std.collection.ArrayList<std.unittest.common.Field>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.Field>) { /* compiled code */ }

    func getFields(): std.collection.ArrayList<std.unittest.common.Field> { /* compiled code */ }

    func add(fie: std.unittest.common.Field): std.unittest.common.DataModelStruct { /* compiled code */ }

    func `get`(key: std.core.String): std.unittest.common.DataModel { /* compiled code */ }
}

class Field {
    var name: std.core.String

    var data: std.unittest.common.DataModel

    init(name: std.core.String, data: std.unittest.common.DataModel) { /* compiled code */ }

    func getName(): std.core.String { /* compiled code */ }

    func getData(): std.unittest.common.DataModel { /* compiled code */ }
}

func field(name: std.core.String, data: T): std.unittest.common.Field { /* compiled code */ }

class DataModelException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Int64 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Int32 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Int16 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Int8 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): UInt64 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): UInt32 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): UInt16 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): UInt8 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Float64 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Float32 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Float16 { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Bool { /* compiled code */ }
}

extend String <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.core.String { /* compiled code */ }
}

extend Extend <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): Rune { /* compiled code */ }
}

extend Option <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.core.Option<T> { /* compiled code */ }
}

extend Array <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.core.Array<T> { /* compiled code */ }
}

extend ArrayList <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.collection.ArrayList<T> { /* compiled code */ }
}

extend HashSet <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.collection.HashSet<T> { /* compiled code */ }
}

extend HashMap <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.collection.HashMap<K, V> { /* compiled code */ }
}

interface TextWidth {
    func width(): Int64 { /* compiled code */ }
}

extend Extend <: TextWidth {
    func width(): Int64 { /* compiled code */ }
}

extend String <: TextWidth {
    func width(): Int64 { /* compiled code */ }
}

func shieldEscapeSeq(s: std.core.String, shieldSingleQuote: Bool, shieldDoubleQuote: Bool, shieldBackslash: Bool, shieldInterpolation: Bool): std.core.String { /* compiled code */ }

extend PrettyPrinter {
    func space(): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func appendLine(): std.unittest.common.PrettyPrinter { /* compiled code */ }
}
