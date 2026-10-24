# Format paczki zadań (wersja 1)

Wspólny dla **PlayOnMe** (gra) i **StudyOnMe** (tryb nauki). Treść piszesz raz.
Schemat do walidacji: [`schema/pack.schema.json`](schema/pack.schema.json).

## Plik
- Jeden plik = jedna paczka, nazwa pliku = `id` paczki, np. `csharp-basics.json`, `tsql-null.json`.
- Kodowanie UTF-8, polskie znaki wprost (bez `\u…`).

## Paczka
| Pole | Wymagane | Opis |
|---|---|---|
| `schemaVersion` | tak | Wersja formatu, teraz `1`. Aplikacja odrzuca nowszą niż obsługuje. |
| `id` | tak | kebab-case, równe nazwie pliku. |
| `title` | tak | Nazwa widoczna dla gracza. |
| `subject` | tak | Przedmiot: `csharp`, `tsql`, później `networking`, `ladder`. Ekran wyboru języka grupuje po nim. |
| `locale` | nie | Język treści, domyślnie `pl`. |
| `version` | nie | Wersja treści paczki (podbijana przy poprawkach). |
| `description` | nie | Krótki opis. |
| `levels` | nie | Nazwy poziomów: `{ "number": 1, "title": "…" }`. |
| `cards` | tak | Lista kart. |

## Karta
| Pole | Wymagane | Opis |
|---|---|---|
| `id` | tak | Stabilne, unikalne w paczce (np. `cs-linq-001`). Po nim zapisywany jest postęp, więc **nie zmieniaj go** po publikacji. |
| `type` | tak | `predict_output`, `find_bug`, `quiz`, `flashcard`. |
| `level` | nie | Numer poziomu (domyślnie 1). |
| `difficulty` | nie | 1–5, wpływa na XP. |
| `tags` | nie | Np. `linq`, `deferred-execution`. Do filtrowania i „bossów”. |
| `prompt` | tak | Polecenie, np. „Co wypisze ten kod?”. Dla fiszki: przód. |
| `code` | nie | Fragment kodu (zwykły tekst z `\n`). |
| `codeLanguage` | nie | Do kolorowania składni: `csharp`, `tsql`… |
| `answer` | tak | Jedna z trzech postaci niżej. |
| `explanation` | tak | Dlaczego tak. Pokazywane po odpowiedzi; dla fiszki to tył. |

## Odpowiedź (`answer.kind`)
- `choice`: `{ "kind": "choice", "options": ["…", "…"], "correct": 1 }` (indeks od 0). MVP używa tylko tego.
- `text`: `{ "kind": "text", "accepted": ["3", "trzy"] }`. Odpowiedź wpisywana.
- `line`: `{ "kind": "line", "line": 4, "fix": "…" }`. Na później, dla „znajdź błąd” (linia od 1).

Nieznane pola są ignorowane, więc StudyOnMe może dodać własne (np. `source`) bez psucia gry.
