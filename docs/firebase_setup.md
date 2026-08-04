# Firebase Console setup (Auth + Firestore + App Check)

Project: [`f1-kotlin`](https://console.firebase.google.com/project/f1-kotlin/overview)  
Android package: `com.example.f1_kotlin`  
`app/google-services.json` is gitignored (CI uses `tool/ci/google-services.stub.json`).

Already used in the app: Core, Analytics, Crashlytics, Remote Config.

## Enable for Profile / Predictor

1. **Authentication → Sign-in method → Email/Password**
   - Enable Email/Password (not passwordless).
   - Do not enable Google / Apple / Anonymous for v1.
   - Templates → Email address verification — keep default or customize.

2. **Cloud Firestore**
   - Create database (production mode).
   - Prefer a region close to users (e.g. `eur3` / `europe-west`).
   - Rules (Console → Firestore → Rules → Publish; `firestore.rules` is local-only / gitignored):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() {
      return request.auth != null;
    }
    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }
    function isVerified() {
      return isSignedIn() && request.auth.token.email_verified == true;
    }

    match /users/{uid} {
      allow read, write: if isOwner(uid);
    }
    match /users/{uid}/seasons/{document=**} {
      allow read, write: if isOwner(uid) && isVerified();
    }

    match /nicknames/{normalized} {
      allow read: if isVerified();
      allow create: if isVerified()
        && request.resource.data.uid == request.auth.uid
        && request.resource.data.keys().hasOnly(['uid', 'nickname']);
      allow update, delete: if isVerified()
        && resource.data.uid == request.auth.uid;
    }

    match /leaderboards/{year}/entries/{uid} {
      allow read: if isVerified();
      allow create, update: if isVerified()
        && request.auth.uid == uid
        && request.resource.data.keys().hasAll(['nickname', 'totalPoints'])
        && request.resource.data.nickname is string
        && request.resource.data.totalPoints is int;
      allow delete: if isVerified() && request.auth.uid == uid;
    }
  }
}
```

3. **App Check** — optional / skipped for v1 (same as `f1_pet_project` practice).
   - If added later: register Android with Play Integrity; debug tokens before enforce on Firestore.

## Data shape

```
users/{uid}
  email, emailVerified, createdAt
  nickname?, nicknameNormalized?, leaderboardOptIn?, leaderboardOptInAt?

users/{uid}/seasons/{year}
  weekends: { "{round}": { … } }
  updatedAt

nicknames/{normalizedNickname}
  uid, nickname

leaderboards/{year}/entries/{uid}
  nickname, totalPoints, updatedAt
```

## Abuse hardening (in app, later stages)

- Registration sends email verification.
- Predictor cloud features gated until `emailVerified`.
- Leaderboard requires nickname + explicit opt-in; leave removes the public entry.
## Not needed yet

App Check, Storage, Cloud Messaging, Anonymous Auth, iOS app.
