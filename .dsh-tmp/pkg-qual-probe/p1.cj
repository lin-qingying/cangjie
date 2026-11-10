package probe

import std.collection

func validImportedType(value: collection.ArrayList<Int64>): Unit {}

func invalidUnimportedType(value: std.collection.ArrayList<Int64>): Unit {}
