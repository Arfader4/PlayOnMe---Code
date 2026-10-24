# PlayOnMe – Code

Gra na Androida do nauki **czytania** kodu. Kotlin + Jetpack Compose + Room.
Format paczek zadań: [docs/pack-format.md](docs/pack-format.md) (wspólny ze StudyOnMe).

## Uruchomienie
1. Otwórz folder w Android Studio (Ladybug lub nowszy), poczekaj na sync Gradle.
2. Uruchom konfigurację `app` na emulatorze lub telefonie (minSdk 26 = Android 8.0).
3. Testy paczek: `./gradlew :app:testDebugUnitTest`.
4. Szybka walidacja samych paczek (bez Androida): `pip install jsonschema && python3 tools/validate_packs.py`.

## Plan MVP (etapy)
1. **Szkielet + format paczki** ✅ projekt, ciemny motyw, nawigacja między 4 ekranami-zaślepkami, model paczki, schemat JSON, 4 przykładowe karty, test parsowania.
2. **Treść** ✅ pełna paczka `csharp-basics.json`, ok. 20 kart „przewidź wynik” (typy wartościowe/referencyjne, stringi, LINQ, async) w 4 poziomach, skrypt walidujący paczki.
3. **Room + XP** ✅ zapis postępu kart i wyników rund, XP, poziom gracza, odblokowanie poziomów paczki.

## Zasady XP (etap 3)
- Dobra odpowiedź: 10 × `difficulty` karty. Karta zaliczona już wcześniej: połowa. Zła: 0.
- Bezbłędna runda: +20.
- Poziom gracza: progi 0, 100, 300, 600, 1000… XP (każdy kolejny skok o 100 większy).
- Poziom paczki odblokowuje się, gdy w poprzednim zaliczysz co najmniej 60% kart.
- Kod: `domain/Scoring.kt` (czyste funkcje z testami), zapis w `data/ProgressRepository.kt`.
4. **Ekrany** ✅ wybór języka → lista poziomów (z postępem) → runda z kodem i opcjami + wyjaśnienie → podsumowanie XP.
5. **Szlif:** kolorowanie składni, animacje odpowiedzi, ikona, build APK.

Później: „znajdź błąd”, T-SQL, serie, odznaki, bossowie, rundy na czas, synchronizacja ze StudyOnMe.
