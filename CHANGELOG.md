# Changelog

## [1.1.0] - 2026-03-23

### Added

- New plugin logo

### Removed

- Support for `__invoke` syntax (replaced with offset syntax)

## [1.0.0] - 2026-02-13

### Added

- Autocomplete for Map view methods — suggests methods from `values`, `keys`, and `entries` directly on Map instances with automatic view property insertion (e.g. `$map->first()` → `$map->values->first()`)
- Closure parameter type inference — resolves closure parameter types in collection callbacks (e.g. `$list->filter(fn ($v) => $v->...)` correctly types `$v` as the element type)
- Read-only collection mutation detection — reports errors when calling mutating methods on immutable/read-only collection types
