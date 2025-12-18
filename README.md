# Event Library Helper Plugin

IntelliJ IDEA inspections and quick-fixes for [Event Library](https://github.com/SmushyTaco/Event-Library)

This plugin provides **compile-time validation**, **clear diagnostics**,
and **safe quick fixes** for Event Library handlers across **Kotlin,
Java, and Scala 3**.

It helps you catch mistakes *before runtime*, without changing the
behavior or design of the Event Library itself.

------------------------------------------------------------------------

## ✨ Features

### ✅ `@EventHandler` Validation

The plugin verifies that every `@EventHandler` method:

-   Has **exactly one parameter**
-   That parameter is **assignable to `Event`**
-   Returns **`Unit` / `void` / `scala.Unit`**

Invalid handlers are highlighted with precise error locations.

------------------------------------------------------------------------

### ✅ `@ExceptionHandler` Validation

The plugin enforces all supported `@ExceptionHandler` shapes.

Supported signatures:

``` kotlin
@ExceptionHandler
fun onFailure(event: MyEvent, t: Throwable)

@ExceptionHandler
fun onFailure(event: MyEvent)

@ExceptionHandler
fun onFailure(t: Throwable)
```

What the plugin checks:

-   Parameter count must be **1 or 2**
-   Parameters must be assignable to `Event` and/or `Throwable`
-   Two-parameter handlers must be ordered as `(Event, Throwable)`

Smart quick fix:

If parameters are reversed, the plugin offers a **safe swap quick fix**.

------------------------------------------------------------------------

### ✏️ Return Type Inspection + Quick Fix

Handlers must not return values.

If a handler returns a value, the plugin reports an error and offers a
one-click fix:

| Language | Fix                                       |
|----------|-------------------------------------------|
| Java     | Change return type to `void`              |
| Kotlin   | Remove return type or replace with `Unit` |
| Scala 3  | Insert or replace return type with `Unit` |

------------------------------------------------------------------------

### 🌍 Multi-Language Support

Works across:

-   Kotlin
-   Java
-   Scala 3

Inspections and quick fixes are language-aware and highlight the most
relevant PSI element.

------------------------------------------------------------------------

## 🧪 Example

Invalid handler:

``` kotlin
@EventHandler
fun onEvent(event: MyEvent): Int {
    return 42
}
```

Error: \> Event Library handler methods must return void / Unit

Quick fix: \> Change return type to void / Unit

------------------------------------------------------------------------

## 📦 Installation

From JetBrains Marketplace:

1.  Open Settings / Preferences
2.  Go to Plugins
3.  Search for **Event Library Helper**
4.  Click Install
5.  Restart IDE

------------------------------------------------------------------------

## 📜 License

Apache 2.0 — see the [LICENSE](LICENSE) file for details.
