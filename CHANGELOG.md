# Changelog

All notable changes to this project will be documented in this file.

The format follows Keep a Changelog principles.

## [1.2.1] - 2026-10-07

### Fixed

- Harden incremental annotation processing with generated output provenance tracking.
- Distinguish EMG-owned generated views from user-defined and dependency types.
- Avoid unsafe silent reuse of stale generated outputs.
- Add diagnostics when generated schema becomes outdated.

### Added

- Add internal `@EmgGenerated` provenance marker.
- Add incremental javac/APT regression tests.

### Compatibility

- No breaking changes to public annotation APIs.
- Existing 1.2.0 generated outputs require one clean regeneration.