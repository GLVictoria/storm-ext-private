# Storm Extensions

A collection of Cloudstream3 providers for streaming anime and movies in Spanish.

## Providers

### Anime
- **AnimeOnlineNinja** - https://ww3.animeonline.ninja
- **AnimeJl** - Anime streaming provider
- **AnimeflvIO** - Animeflv.io provider
- **Animeflv** - Animeflv.net provider
- **Animension** - Animension provider
- **Aniwatch** - Aniwatch provider
- **Aniwave** - Aniwave provider
- **JKAnime** - JKAnime provider
- **LatAnime** - LatinAnime provider
- **Monoschinos** - Monoschinos provider
- **MundoDonghua** - Mundo Donghua provider
- **SoloLatino** - SoloLatino provider
- **TioAnime** - TioAnime provider
- **LACartoons** - Latin American Cartoons provider

### Movies & Series
- **Bflix** - Bflix provider
- **Cinecalidad** - Cinecalidad provider
- **Cuevana** - Cuevana provider
- **DoramasFlix** - DoramasFlix provider
- **DoramasYT** - DoramasYT provider
- **Entrepeliculasyseries** - Entre Películas y Series provider
- **EstrenosDoramas** - Estrenos Doramas provider
- **PeliculasFlix** - PeliculasFlix provider
- **Pelispedia** - Pelispedia provider
- **Pelisplus4K** - Pelisplus 4K provider
- **PelisplusHD** - PelisplusHD provider
- **PelisplusSO** - PelisplusSO provider
- **Playhub** - Playhub provider
- **SeriesMetro** - SeriesMetro provider
- **Seriesflix** - Seriesflix provider

### TV & Live
- **CablevisionHD** - CablevisionHD provider

### Ramen Content
- **ComamosRamen** - Comamos Ramen provider

## Building

To build all providers:
```bash
./gradlew build
```

To build a specific provider:
```bash
./gradlew :ProviderName:build
```

## Installation

1. Download the APK files from the releases
2. Install them in Cloudstream3 via Settings > Extensions > Install from file

## Attribution

This template as well as the gradle plugin and the whole plugin system is **heavily** based on [Aliucord](https://github.com/Aliucord).
*Go use it, it's a great mobile discord client mod!*
