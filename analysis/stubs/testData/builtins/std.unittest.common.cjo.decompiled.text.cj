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

    func `get`(key: std.unittest.common.KeyFor<T>): std.core.Option<T>

    func getByName(name: std.core.String): std.core.Option<T>

    func `set`(key: std.unittest.common.KeyFor<T>, value: T): Unit

    func setByName(name: std.core.String, value: T): Unit

    func remove(key: std.unittest.common.KeyFor<T>): std.core.Option<T>

    func removeByName(key: std.core.String): std.core.Option<T>

    func clone: std.unittest.common.Configuration

    func toString: std.core.String

    func merge(parent: std.unittest.common.Configuration, child: std.unittest.common.Configuration): std.unittest.common.Configuration

    let storage: std.collection.HashMap<std.unittest.common.ConfigurationKey, std.core.Any>
}

interface DataShrinker {
    func shrink(value: T): std.core.Iterable<T>
}

class NoShrinker <: std.core.Object & std.unittest.common.DataShrinker<T> {
    func shrink(_: T): std.core.Iterable<T>

    func instance: std.unittest.common.DataShrinker<T>

    init() { /* compiled code */ }
}

interface KeyFor {
    prop name: std.core.String
}

struct KeyTags <: std.unittest.common.KeyFor<std.core.Array<std.core.String>> {
    prop tags: std.unittest.common.KeyTags

    prop name: std.core.String

    init() { /* compiled code */ }
}

class ConfigurationKey <: std.core.Object & std.core.Equatable<std.unittest.common.ConfigurationKey> & std.core.Hashable {
    func hashCode: Int64

    func equals(that: std.unittest.common.ConfigurationKey): Bool

    operator func ==(that: std.unittest.common.ConfigurationKey): Bool

    operator func !=(that: std.unittest.common.ConfigurationKey): Bool

    let name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

extend ConfigurationKey {
    func create(name: std.core.String): std.unittest.common.ConfigurationKey
}

interface DataProvider {
    func provide: std.core.Iterable<T>
}

interface DataStrategy {
    func provider(configuration: std.unittest.common.Configuration): std.unittest.common.DataProvider<T>

    func shrinker(configuration: std.unittest.common.Configuration): std.unittest.common.DataShrinker<T>

    prop isInfinite: Bool
}

extend Array <: DataStrategy & DataProvider {
    func provide: std.core.Iterable<T>

    prop isInfinite: Bool

    func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T>
}

extend Range <: DataStrategy & DataProvider {
    func provide: std.core.Iterable<T>

    prop isInfinite: Bool

    func provider(_: std.unittest.common.Configuration): std.unittest.common.DataProvider<T>
}

class JsonException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class JsonNull <: std.unittest.common.JsonValue {
    func kind: std.unittest.common.JsonKind

    func toString: std.core.String

    func toJsonString: std.core.String

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
    func toString: std.core.String

    func toJsonString: std.core.String

    func fromStr(s: std.core.String): std.unittest.common.JsonValue

    func kind: std.unittest.common.JsonKind

    func asNull: std.unittest.common.JsonNull

    func asBool: std.unittest.common.JsonBool

    func asInt: std.unittest.common.JsonInt

    func asFloat: std.unittest.common.JsonFloat

    func asString: std.unittest.common.JsonString

    func asArray: std.unittest.common.JsonArray

    func asObject: std.unittest.common.JsonObject

    init() { /* compiled code */ }
}

class JsonBool <: std.unittest.common.JsonValue {
    var value: Bool

    init(bv: Bool) { /* compiled code */ }

    func getValue: Bool

    func kind: std.unittest.common.JsonKind

    func toString: std.core.String

    func toJsonString: std.core.String
}

class JsonInt <: std.unittest.common.JsonValue {
    var value: Int64

    init(iv: Int64) { /* compiled code */ }

    func getValue: Int64

    func kind: std.unittest.common.JsonKind

    func toString: std.core.String

    func toJsonString: std.core.String
}

class JsonFloat <: std.unittest.common.JsonValue {
    var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    func getValue: Float64

    func kind: std.unittest.common.JsonKind

    func toString: std.core.String

    func toJsonString: std.core.String
}

class JsonString <: std.unittest.common.JsonValue {
    var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    func getValue: std.core.String

    func kind: std.unittest.common.JsonKind

    func toString: std.core.String

    func toJsonString: std.core.String
}

class JsonArray <: std.unittest.common.JsonValue {
    var items: std.collection.ArrayList<std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.JsonValue>) { /* compiled code */ }

    init(list: std.core.Array<std.unittest.common.JsonValue>) { /* compiled code */ }

    func kind: std.unittest.common.JsonKind

    func toJsonString: std.core.String

    func toJsonString(depth: Int64, bracketInNewLine: Bool, indent: std.core.String): std.core.String

    func toString: std.core.String

    func size: Int64

    func add(jv: std.unittest.common.JsonValue): std.unittest.common.JsonArray

    func `get`(index: Int64): std.core.Option<std.unittest.common.JsonValue>

    operator func [](index: Int64): std.unittest.common.JsonValue

    func getItems: std.collection.ArrayList<std.unittest.common.JsonValue>
}

class JsonObject <: std.unittest.common.JsonValue {
    var fields: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>

    init() { /* compiled code */ }

    init(map: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>) { /* compiled code */ }

    func kind: std.unittest.common.JsonKind

    func toJsonString: std.core.String

    func toJsonString(depth: Int64, bracketInNewLine: Bool, indent: std.core.String): std.core.String

    func toString: std.core.String

    func size: Int64

    func containsKey(key: std.core.String): Bool

    func put(key: std.core.String, v: std.unittest.common.JsonValue): Unit

    func `get`(key: std.core.String): std.core.Option<std.unittest.common.JsonValue>

    operator func [](key: std.core.String): std.unittest.common.JsonValue

    func getFields: std.collection.HashMap<std.core.String, std.unittest.common.JsonValue>
}

interface ToJson {
    func toJson: std.unittest.common.JsonValue

    func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel
}

class DataModel {
    init() { /* compiled code */ }
}

extend DataModel <: ToJson {
    func fromJson(jv: std.unittest.common.JsonValue): std.unittest.common.DataModel

    func toJson: std.unittest.common.JsonValue
}

class UnittestOptionValidationException <: std.core.Exception {
    func getClassName: std.core.String
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

func registerOptionValidator(name: std.core.String, validator: (std.core.Any) -> std.unittest.common.OptionValidity): Unit

func setOptionInfo(name: std.core.String, types: std.core.Array<std.core.String>, description: std.core.Option<std.core.String>): Unit

func setOrUpdateOptionInfo(name: std.core.String, description: std.core.Option<std.core.String>, ty: std.core.String, typeDescription: std.core.String): Unit

enum Color <: std.core.Equatable<std.unittest.common.Color> {
    | RED

    | GREEN

    | YELLOW

    | BLUE

    | CYAN

    | MAGENTA

    | GRAY

    | DEFAULT_COLOR

    operator func ==(that: std.unittest.common.Color): Bool

    operator func !=(that: std.unittest.common.Color): Bool
}

class PrettyPrinter {
    func put(s: std.core.String): Unit

    func putNewLine: Unit

    func setColor(color: std.unittest.common.Color): Unit

    var currentIndent: std.core.String

    var needIndent: Bool

    var currentColor: std.unittest.common.Color

    prop isTopLevel: Bool

    func indent(body: () -> Unit): std.unittest.common.PrettyPrinter

    func indent(indents: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter

    func customOffset(symbols: UInt64, body: () -> Unit): std.unittest.common.PrettyPrinter

    func colored(color: std.unittest.common.Color, body: () -> Unit): std.unittest.common.PrettyPrinter

    func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter

    func colored(color: std.unittest.common.Color, text: std.core.String): std.unittest.common.PrettyPrinter

    func append(text: std.core.String): std.unittest.common.PrettyPrinter

    func appendCentered(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter

    func appendLeftAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter

    func appendRightAligned(text: std.core.String, space: UInt64): std.unittest.common.PrettyPrinter

    func append(value: PP): std.unittest.common.PrettyPrinter

    func newLine: std.unittest.common.PrettyPrinter

    func appendLine(text: std.core.String): std.unittest.common.PrettyPrinter

    func appendLine(value: PP): std.unittest.common.PrettyPrinter

    let indentationSize: UInt64

    let startingIndent: UInt64

    init(indentationSize: UInt64, startingIndent: UInt64) { /* compiled code */ }
}

interface PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

enum PrettyPrintableTextChunk {
    | PPTextPiece(std.core.String)

    | PPColorSwitch(std.unittest.common.Color)

    | PPNewLine
}

class PrettyText <: std.unittest.common.PrettyPrinter & std.unittest.common.PrettyPrintable & std.core.ToString {
    init() { /* compiled code */ }

    init(string: std.core.String) { /* compiled code */ }

    func of(pp: PP): std.unittest.common.PrettyText

    func isEmpty: Bool

    func put(s: std.core.String): Unit

    func setColor(color: std.unittest.common.Color): Unit

    func putNewLine: Unit

    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

    func toString: std.core.String

    let data: std.collection.ArrayList<std.unittest.common.PrettyPrintableTextChunk>
}

extend Array <: PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

extend ArrayList <: PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

func toStringOrPlaceholder(value: T): std.core.String

func toStringQuotedOrPlaceholder(value: T): std.core.String

func quoteString(value: T): std.core.String

extend Color <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.Color
}

interface Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): T
}

extend PrettyText <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.common.PrettyText
}

class DataModelNull <: std.unittest.common.DataModel {
    init() { /* compiled code */ }
}

class DataModelBool <: std.unittest.common.DataModel {
    var value: Bool

    init(bv: Bool) { /* compiled code */ }

    func getValue: Bool
}

class DataModelInt <: std.unittest.common.DataModel {
    var value: Int64

    init(iv: Int64) { /* compiled code */ }

    func getValue: Int64
}

class DataModelFloat <: std.unittest.common.DataModel {
    var value: Float64

    init(fv: Float64) { /* compiled code */ }

    init(v: Int64) { /* compiled code */ }

    func getValue: Float64
}

class DataModelString <: std.unittest.common.DataModel {
    var value: std.core.String

    init(sv: std.core.String) { /* compiled code */ }

    func getValue: std.core.String
}

class DataModelSeq <: std.unittest.common.DataModel {
    var items: std.collection.ArrayList<std.unittest.common.DataModel>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.DataModel>) { /* compiled code */ }

    func getItems: std.collection.ArrayList<std.unittest.common.DataModel>

    func add(dm: std.unittest.common.DataModel): Unit
}

class DataModelStruct <: std.unittest.common.DataModel {
    var fields: std.collection.ArrayList<std.unittest.common.Field>

    init() { /* compiled code */ }

    init(list: std.collection.ArrayList<std.unittest.common.Field>) { /* compiled code */ }

    func getFields: std.collection.ArrayList<std.unittest.common.Field>

    func add(fie: std.unittest.common.Field): std.unittest.common.DataModelStruct

    func `get`(key: std.core.String): std.unittest.common.DataModel
}

class Field {
    var name: std.core.String

    var data: std.unittest.common.DataModel

    init(name: std.core.String, data: std.unittest.common.DataModel) { /* compiled code */ }

    func getName: std.core.String

    func getData: std.unittest.common.DataModel
}

func field(name: std.core.String, data: T): std.unittest.common.Field

class DataModelException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Int64
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Int32
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Int16
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Int8
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): UInt64
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): UInt32
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): UInt16
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): UInt8
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Float64
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Float32
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Float16
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Bool
}

extend String <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.core.String
}

extend Extend <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): Rune
}

extend Option <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.core.Option<T>
}

extend Array <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.core.Array<T>
}

extend ArrayList <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.collection.ArrayList<T>
}

extend HashSet <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.collection.HashSet<T>
}

extend HashMap <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.collection.HashMap<K, V>
}

interface TextWidth {
    func width: Int64
}

extend Extend <: TextWidth {
    func width: Int64
}

extend String <: TextWidth {
    func width: Int64
}

func shieldEscapeSeq(s: std.core.String, shieldSingleQuote: Bool, shieldDoubleQuote: Bool, shieldBackslash: Bool, shieldInterpolation: Bool): std.core.String

extend PrettyPrinter {
    func space: std.unittest.common.PrettyPrinter

    func appendLine: std.unittest.common.PrettyPrinter
}
