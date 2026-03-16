Noctud Collection for PhpStorm
=========================================

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/30173-noctud.svg?label=marketplace)](https://plugins.jetbrains.com/plugin/30173-noctud)
[![Build](https://img.shields.io/github/actions/workflow/status/noctud/intellij-noctud/build.yaml?branch=main)](https://github.com/noctud/intellij-noctud/actions)
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
[![Discord](https://img.shields.io/badge/discord-join-5865F2?logo=discord&logoColor=white)](https://discord.noctud.dev)

<!-- Plugin description -->
PhpStorm plugin for [Noctud Collection](https://noctud.dev/collection/getting-started) — a Kotlin-style collection library for PHP 8.4+ with full generics support. Provides enhanced autocomplete and type inference that PhpStorm can't handle on its own due to limited generics support.

If you have any problems with the plugin, [create an issue](https://github.com/noctud/intellij-noctud/issues/new/choose) or join the [Noctud Discord](https://discord.gg/jS3fKe6vW9).
<!-- Plugin description end -->

Installation
------------
Settings → Plugins → Marketplace → Search "Noctud" → Install

Installation from .jar file
------------
Download `instrumented.jar` from the [latest release](https://github.com/noctud/intellij-noctud/releases) or the latest successful [GitHub Actions build](https://github.com/noctud/intellij-noctud/actions).

Features
------------------

* **Map view method forwarding** — autocomplete methods from `values`, `keys`, and `entries` directly on Map instances with automatic view property insertion (e.g. `$map->first()` → `$map->values->first()`)
* **Closure parameter type inference** — resolves closure parameter types in collection callbacks (e.g. `$list->filter(fn ($v) => $v->...)` correctly types `$v` as the element type)
* **Read-only collection mutation detection** — reports errors when calling mutating methods on immutable/read-only collection types

Building
------------

```sh
./gradlew buildPlugin
```

Testing in sandbox IDE
------------

```sh
./gradlew runIde
```
