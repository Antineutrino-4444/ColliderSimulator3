# LHC Simulator

A realistic particle-physics management game built with LibGDX.

## Building

```bash
./gradlew desktop:run          # Run the game
./gradlew desktop:shadowJar    # Build a fat JAR
./gradlew :core:test           # Run unit tests
```

## Project Structure

- `core/` — Game logic, physics engine, data, rendering
- `desktop/` — LWJGL3 desktop launcher

## License

MIT — see [LICENSE](LICENSE).
