### 0.11.0

_Released 2026 Aug 04_

### Improvements

- Default publications are now only created if a bundling task is found in
  addition to the respective development plugin:
  - For Fabric Loom either `remapJar` or `jar` (in that order)
  - For all others: `jar`
