# FavPeople — ДЗ №6: Flow поверх проекта из ДЗ 4/5

**ФИО:** Алалин Алексей Юрьевич  
**Группа:** ПИКД Б9123-09.03.03, 5 подгруппа  
**VariantCode:** TVM-PEOPLE-MOD_E25_FAV_SORT · API: TVmaze (`https://api.tvmaze.com`)

## Что добавлено
Главный экран стал **реактивным**: его состояние не присваивается вручную, а собирается из
**4 независимых источников** с помощью операторов Flow.

| № | Источник | Тип | Откуда |
|---|---|---|---|
| 1 | строка поиска `query` | `MutableStateFlow<String>` | UI (ввод пользователя) |
| 2 | фильтр по полу `filter` | `MutableStateFlow<GenderFilter>` | UI (чипы «Все / Мужчины / Женщины») |
| 3 | избранное `observeFavorites()` | `Flow<List<FavoriteEntry>>` | **data layer — Room** |
| 4 | действия «Обновить / Retry» `refreshActions` | `MutableSharedFlow<Unit>` | поток событий пользователя |

### Цепочка
```
query ─ debounce(400 мс; для пустой строки 0) ─ map(trim) ─ distinctUntilChanged ─┐
                                                                                  ├─ combine ─ flatMapLatest(загрузка) ─┐
refreshActions ─ onStart{ emit(Unit) } ───────────────────────────────────────────┘                                     │
                                                                                                                        ├─ combine ─ stateIn ─ uiState
query, filter, favorites (Room) ────────────────────────────────────────────────────────────────────────────────────────┘
```
- **debounce** — запрос уходит, только когда пользователь перестал печатать на 400 мс.
- **distinctUntilChanged** — «bob» и «bob  » не дают повторного запроса.
- **flatMapLatest** — новый запрос отменяет старый: устаревший ответ не попадёт в UI.
- **combine** (2 раза) — объединение источников. Смена фильтра и изменения избранного пересчитывают
  экран **без запроса в сеть**.
- Пустая строка поиска → `GET /people?page=0`, непустая → `GET /search/people?q=...`.

### Почему StateFlow и SharedFlow именно здесь
- `StateFlow` — у строки поиска, фильтра и итогового `uiState` всегда есть текущее значение.
- `SharedFlow` — нажатие «Обновить» это **событие**, а не состояние: два одинаковых нажатия подряд —
  два события, и каждое перезапускает загрузку (StateFlow склеил бы одинаковые значения).
  Это не one-shot для snackbar, а входной поток действий, встроенный в цепочку через `combine`.

### Проект остался рабочим
Экраны List/Search, Detail/{id}, History · Navigation Compose · Hilt · Room · Retrofit + coroutines +
`viewModelScope` · состояния Loading / Error + Retry / Empty / Content.

## Тесты
`./gradlew testDebugUnitTest` (Windows: `gradlew.bat testDebugUnitTest`).

Новые тесты реактивной логики — `ReactiveSearchTest`:
- debounce: быстрый ввод «b → bo → bob» даёт один запрос «bob»;
- distinctUntilChanged: тот же запрос с пробелами не уходит в сеть повторно;
- flatMapLatest: ответ устаревшего поиска не попадает на экран;
- смена фильтра пересчитывает список без нового запроса;
- изменение избранного в data-слое само обновляет звёздочки без перезагрузки;
- пустой результат поиска → `Empty`, очистка строки возвращает обычный список.

Тесты из ДЗ 5 обновлены под новую ViewModel (Retry теперь — `refresh()`), включая полную
последовательность эмиссий `Loading → Error → Loading → Content` и отмену устаревшего запроса.
