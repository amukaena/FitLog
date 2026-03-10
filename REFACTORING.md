# FitLog 리팩토링 문서

**날짜**: 2026-03-10
**범위**: 아키텍처 개선, 코드 중복 제거, 버그 수정

---

## 1. BackupService 추출 (Critical)

### 문제
`SettingsViewModel`과 `GoogleDriveViewModel`에 백업/복원 로직이 완전히 중복되어 있었음.
- 5개 데이터 클래스 (`BackupData`, `BackupExercise`, `BackupDailyWorkout`, `BackupWorkoutRecord`, `BackupWorkoutSet`) 중복
- 4개 매퍼 함수 (`toBackupExercise()`, `toDomain()`, `toBackupDailyWorkout()`, `toDomain()`) 중복
- 복원 로직 (clear → add exercises → map by name → restore records/sets) 중복

### 변경사항
- **신규**: `domain/service/BackupService.kt` - 백업/복원 로직과 데이터 클래스를 하나의 서비스로 통합
- **수정**: `SettingsViewModel.kt` - `BackupService` 주입으로 교체 (220줄 → 82줄)
- **수정**: `GoogleDriveViewModel.kt` - `BackupService` 주입으로 교체 (289줄 → 173줄)

### 효과
- 약 180줄의 중복 코드 제거
- 백업 로직 변경 시 한 곳만 수정하면 됨

---

## 2. Coroutine Flow Collection 누수 수정 (Critical)

### 문제
`CalendarViewModel`, `ExerciseViewModel`, `WorkoutViewModel`에서 `Flow.collect()`를 호출하는 코루틴이 이전 Job을 취소하지 않아, 함수 재호출 시 이전 collector가 계속 살아있는 메모리 누수 발생.

### 변경사항
- **CalendarViewModel**: `monthDataJob`, `selectedDayJob` 필드 추가 → 새 수집 전 이전 Job 취소
- **ExerciseViewModel**: `loadExercisesJob` 필드 추가 → 카테고리 변경 시 이전 Job 취소
- **WorkoutViewModel**: `exercisesJob`, `recentWorkoutsJob` 필드 추가 → `loadWorkout()` 재호출 시 이전 Job 취소. Flow를 수집하지 않는 일회성 로드 로직은 별도 코루틴으로 분리.

### 효과
- 캘린더에서 월 빠르게 전환 시 중복 collector 방지
- 운동 목록에서 카테고리 변경 시 중복 collector 방지

---

## 3. 데이터베이스 싱글톤 이중화 제거 (Critical)

### 문제
`FitLogDatabase`에 수동 싱글톤(`companion object`의 `INSTANCE`)과 Hilt `@Singleton @Provides`가 동시에 존재. 위젯이 수동 싱글톤을 사용하여 Hilt가 제공하는 인스턴스와 다른 DB 인스턴스를 생성할 수 있었음.

### 변경사항
- **수정**: `FitLogDatabase.kt` - 수동 싱글톤 패턴 제거 (`INSTANCE` 필드, `getDatabase()` 메서드 삭제)
- **수정**: `FitLogWidget.kt` - Hilt `@EntryPoint`를 통해 `DailyWorkoutDao`에 접근하도록 변경
  - `WidgetEntryPoint` 인터페이스 추가
  - `EntryPoints.get()`으로 Hilt 관리 DAO 인스턴스 사용

### 효과
- 앱 전체에서 단일 DB 인스턴스 보장
- Clean Architecture 원칙 준수 (위젯도 DI를 통해 데이터 접근)

---

## 4. ClipboardHelper 추출 (Major)

### 문제
`WorkoutViewModel`이 `Context.getSystemService()`로 `ClipboardManager`에 직접 접근. ViewModel에서 Android 프레임워크 서비스를 직접 사용하는 것은 테스트 어렵고 아키텍처 위반.

### 변경사항
- **신규**: `util/ClipboardHelper.kt` - 클립보드 접근을 캡슐화한 `@Singleton` 클래스
- **수정**: `WorkoutViewModel.kt` - `ClipboardHelper` 주입으로 교체

### 효과
- ViewModel의 Android 프레임워크 의존성 제거
- 테스트 시 ClipboardHelper를 mock 가능

---

## 5. deleteSet() 부작용 수정 (Major)

### 문제
`WorkoutSetViewModel.deleteSet()`에서 `_uiState.update {}` 람다 내부에서 DB 쓰기(`workoutRepository.updateWorkoutSet()`)를 수행. `StateFlow.update`의 람다는 순수 변환이어야 하며, CAS 실패 시 재실행될 수 있어 DB 쓰기가 중복 실행될 위험.

### 변경사항
- **수정**: `WorkoutSetViewModel.kt` - DB 쓰기를 `_uiState.update {}` 블록 외부로 이동

### 효과
- StateFlow 업데이트의 원자성 보장
- 중복 DB 쓰기 방지

---

## 6. FitLogCard 일관성 개선 (Minor)

### 문제
`DailyWorkoutScreen`의 `VolumeSummarySection`과 `GoogleDriveBackupScreen`에서 `FitLogCard` 대신 raw `Card`를 직접 사용하여 `RoundedCornerShape(12.dp)`과 `surfaceVariant` 색상을 수동으로 지정.

### 변경사항
- **수정**: `DailyWorkoutScreen.kt` - `VolumeSummarySection`에서 `FitLogCard` 사용
- **수정**: `GoogleDriveBackupScreen.kt` - 계정 연결 카드에서 `FitLogCard` 사용
- 미사용 import 정리 (`Card`, `CardDefaults`, `RoundedCornerShape`)

### 효과
- UI 컴포넌트 일관성 확보
- 카드 스타일 변경 시 `FitLogCard` 한 곳만 수정하면 됨

---

## 7. 기본 운동 초기화 위치 이동 (Minor)

### 문제
`CalendarViewModel.init`에서 `exerciseRepository.initializeDefaultExercises()`를 호출. 데이터 시딩은 ViewModel의 책임이 아니며, `CalendarViewModel`이 `ExerciseRepository`에 불필요하게 의존.

### 변경사항
- **수정**: `FitLogApplication.kt` - `onCreate()`에서 `exerciseRepository.initializeDefaultExercises()` 호출
- **수정**: `CalendarViewModel.kt` - `ExerciseRepository` 의존성 제거, `initializeDefaultExercises()` 호출 제거

### 효과
- 앱 시작 시 한 번만 초기화 (여러 ViewModel에서 중복 호출 방지)
- CalendarViewModel의 책임 범위 축소
- `updateRecordOrder()`를 `private`으로 변경하여 내부 구현 은닉

---

## 변경 파일 요약

| 파일 | 변경 유형 |
|------|----------|
| `domain/service/BackupService.kt` | 신규 생성 |
| `util/ClipboardHelper.kt` | 신규 생성 |
| `presentation/settings/SettingsViewModel.kt` | 대폭 간소화 |
| `presentation/settings/GoogleDriveViewModel.kt` | 대폭 간소화 |
| `presentation/calendar/CalendarViewModel.kt` | 의존성 제거 + 누수 수정 |
| `presentation/exercise/ExerciseViewModel.kt` | 누수 수정 |
| `presentation/workout/WorkoutViewModel.kt` | 누수 수정 + ClipboardHelper |
| `presentation/workout/WorkoutSetViewModel.kt` | 부작용 수정 |
| `data/local/FitLogDatabase.kt` | 수동 싱글톤 제거 |
| `widget/FitLogWidget.kt` | Hilt EntryPoint 적용 |
| `FitLogApplication.kt` | 기본 운동 초기화 추가 |
| `presentation/workout/DailyWorkoutScreen.kt` | FitLogCard 적용 |
| `presentation/settings/GoogleDriveBackupScreen.kt` | FitLogCard 적용 |

---

## 향후 개선 권장사항

아래 항목들은 이번 리팩토링 범위에서 제외되었으나, 추후 개선을 권장합니다:

1. **N+1 쿼리 최적화**: `WorkoutRepositoryImpl.loadFullDailyWorkout()`에서 Room `@Relation` 사용
2. **일관된 에러 핸들링**: 모든 ViewModel에 try/catch 또는 sealed Result 패턴 적용
3. **운동 이름 중복 검증**: `ExerciseEntity`에 unique index 추가 및 ViewModel 검증
4. **하드코딩된 Dimension 값**: `Dimens` 객체에 추가 상수 정의
5. **BottomSheet 고정 높이**: `400.dp` 고정 대신 화면 비율 기반으로 변경
