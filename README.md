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
5. **Szlif** ✅ kolorowanie składni, animacje, ikona, podpisany APK.

## Podpisany APK
1. Utwórz klucz (raz): `keytool -genkeypair -v -keystore C:/Users/<ty>/keys/playonme.jks -alias playonme -keyalg RSA -keysize 2048 -validity 9125`. Trzymaj `.jks` i hasła poza repo, z kopią zapasową.
2. Skopiuj `keystore.properties.example` do `keystore.properties` i wpisz ścieżkę oraz hasła.
3. `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

Bez `keystore.properties` build release też przejdzie, ale APK będzie niepodpisany.

## Szybkie tryby (po MVP)
Na ekranie poziomów, w sekcji „Szybka gra”. Działają na istniejących kartach, bez zmian w paczkach.
- **Wyzwanie dnia**: 5 kart wylosowanych z dnia i przedmiotu (ze wszystkich poziomów, więc u każdego te same), jedna próba dziennie, licznik dni z rzędu. Bezbłędnie: +20 XP.
- **Blitz 60 s**: dobra odpowiedź +2 s, zła −5 s, gra sama przechodzi dalej. Rekord = najwięcej poprawnych.
- **Seria**: od najłatwiejszych do najtrudniejszych, pierwszy błąd kończy grę.
- Blitz i seria biorą karty tylko z odblokowanych poziomów. XP liczone jak w rundach (karta już zaliczona: połowa, także przy powtórce w tej samej grze).
- Po grze lista „Co przegapiłeś” z poprawną odpowiedzią i wyjaśnieniem.
- Kod: `domain/QuickModes.kt` (zasady, z testami), `ui/screens/QuickPlay*.kt`. Wyniki w tabeli `mode_result` (baza w wersji 2, migracja zachowuje dotychczasowy postęp).

Później: „znajdź błąd”, T-SQL, odznaki, bossowie, uzupełnianie kodu, łączenie w pary, spadający kod, synchronizacja ze StudyOnMe.
