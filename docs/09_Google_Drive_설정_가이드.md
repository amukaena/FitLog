# Google Drive 백업 설정 가이드

FitLog 앱에서 Google Drive 백업 기능을 사용하려면 Google Cloud Console에서 OAuth 2.0 설정이 필요합니다.

## 1. Google Cloud Console 프로젝트 생성

1. [Google Cloud Console](https://console.cloud.google.com) 접속
2. 새 프로젝트 생성
   - 프로젝트 이름: `FitLog` (또는 원하는 이름)

## 2. Google Drive API 활성화

1. **APIs & Services > Library** 메뉴로 이동
2. "Google Drive API" 검색
3. **Enable** 버튼 클릭하여 활성화

## 3. OAuth 동의 화면 설정

1. **APIs & Services > OAuth consent screen** 메뉴로 이동
2. User Type: **External** 선택
3. 앱 정보 입력:
   - 앱 이름: `FitLog`
   - 사용자 지원 이메일: 본인 이메일
   - 개발자 연락처 이메일: 본인 이메일
4. 범위 추가:
   - `.../auth/drive.appdata` (앱 데이터 폴더 접근)
5. 테스트 사용자 추가 (개발 중에는 필수)

## 4. OAuth 2.0 클라이언트 ID 생성

1. **APIs & Services > Credentials** 메뉴로 이동
2. **Create Credentials > OAuth client ID** 클릭
3. 애플리케이션 유형: **Android** 선택
4. 다음 정보 입력:
   - 이름: `FitLog Android`
   - 패키지 이름: `com.fitlog`
   - SHA-1 인증서 지문: 아래 명령어로 확인

### SHA-1 인증서 지문 확인

**Debug 키스토어 (개발용):**
```bash
# Windows
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android

# Mac/Linux
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
```

**Release 키스토어 (배포용):**
```bash
keytool -list -v -keystore [키스토어_경로] -alias [별칭]
```

## 5. 앱 설정

### 5.1 필요한 의존성 (이미 추가됨)

```kotlin
// app/build.gradle.kts
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.api-client:google-api-client-android:2.2.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20231128-2.0.0")
```

### 5.2 인터넷 권한 (이미 추가됨)

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

## 6. 백업 데이터 구조

백업 파일은 `fitlog_backup.json` 이름으로 Google Drive의 앱 데이터 폴더(`appDataFolder`)에 저장됩니다.

이 폴더는:
- 사용자에게 보이지 않음
- 앱 삭제 시 자동 삭제됨
- 다른 앱에서 접근 불가

### JSON 구조

```json
{
  "version": 1,
  "exportedAt": "2024-01-30T15:30:45.123",
  "exercises": [
    {
      "id": 1,
      "name": "벤치프레스",
      "category": "가슴",
      "isCustom": false
    }
  ],
  "dailyWorkouts": [
    {
      "id": 1,
      "date": "2024-01-30",
      "title": "상체 운동",
      "memo": "좋은 운동",
      "records": [
        {
          "id": 1,
          "exerciseName": "벤치프레스",
          "order": 0,
          "sets": [
            { "setNumber": 1, "weight": 100.0, "reps": 8 }
          ]
        }
      ]
    }
  ]
}
```

## 7. 문제 해결

### "Sign in failed" 오류

1. SHA-1 지문이 올바르게 등록되었는지 확인
2. 패키지 이름이 `com.fitlog`인지 확인
3. Google Cloud Console에서 OAuth 동의 화면 설정 완료 여부 확인

### "Drive not initialized" 오류

1. 인터넷 연결 상태 확인
2. Google 계정 로그인 상태 확인
3. Drive API 스코프 권한 허용 여부 확인

### 테스트 중 "앱이 확인되지 않음" 경고

개발 중에는 정상입니다. **고급 > 안전하지 않음 - 계속**을 클릭하여 진행하세요.
배포 전에 OAuth 동의 화면 검증을 완료해야 합니다.

## 8. 배포 체크리스트

- [ ] Release 키스토어의 SHA-1 지문 등록
- [ ] OAuth 동의 화면 검증 요청 (프로덕션 환경)
- [ ] 개인정보처리방침 URL 등록
- [ ] 앱 로고 등록
