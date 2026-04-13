# Frontend Flutter - Ticket Securite Mobile, Session et Routes Protegees

## Objectif
Mettre en place une authentification mobile solide avec persistance de session, envoi automatique du JWT sur les appels API, blocage des routes privees sans token, et deconnexion automatique si la session expire.

---

## 1. Prerequis backend a connaitre

## Endpoints publics auth
- POST /api/auth/signup
- POST /api/auth/login

## Regle de securite globale
- Tout endpoint non explicitement public est protege et requiert JWT.
- Sans token (ou token invalide), le backend renvoie 401 avec:
  - message: Non authentifie: token JWT manquant ou invalide

## Duree de vie du JWT
- expiration: 86400000 ms (24h)
- pas de refresh token implemente cote backend actuellement

Implication frontend:
- il faut gerer un logout automatique sur 401
- et eventuellement un re-login utilisateur (pas de refresh silencieux natif)

---

## 2. Stockage securise du token

## Recommande
- flutter_secure_storage (prioritaire pour mobile)

## Alternative
- SharedPreferences (moins securise, seulement si contrainte)

## pubspec.yaml
```yaml
dependencies:
  flutter_secure_storage: ^9.2.2
  dio: ^5.7.0
```

## Service TokenStorage
```dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  static const _tokenKey = 'auth_token';
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<void> saveToken(String token) async {
    await _storage.write(key: _tokenKey, value: token);
  }

  Future<String?> readToken() async {
    return _storage.read(key: _tokenKey);
  }

  Future<void> clearToken() async {
    await _storage.delete(key: _tokenKey);
  }
}
```

---

## 3. Login puis persistance session

## Login API
POST /api/auth/login
```json
{
  "email": "user@rezo.com",
  "password": "SecurePass123!"
}
```

## Reponse
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## Workflow frontend
1. appeler login
2. recuperer token
3. saveToken(token)
4. naviguer vers zone authentifiee

## Exemple service auth
```dart
class AuthService {
  final Dio dio;
  final TokenStorage tokenStorage;

  AuthService(this.dio, this.tokenStorage);

  Future<void> login(String email, String password) async {
    final response = await dio.post('/api/auth/login', data: {
      'email': email,
      'password': password,
    });

    final token = response.data['token'] as String;
    await tokenStorage.saveToken(token);
  }

  Future<void> logout() async {
    await tokenStorage.clearToken();
  }
}
```

---

## 4. Injection automatique du JWT dans chaque requete

## Option recommandee
- utiliser un interceptor Dio

```dart
class AuthInterceptor extends Interceptor {
  final TokenStorage tokenStorage;
  final VoidCallback onUnauthorized;

  AuthInterceptor({
    required this.tokenStorage,
    required this.onUnauthorized,
  });

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await tokenStorage.readToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    if (err.response?.statusCode == 401) {
      await tokenStorage.clearToken();
      onUnauthorized();
    }
    handler.next(err);
  }
}
```

## Initialisation Dio
```dart
final dio = Dio(BaseOptions(
  baseUrl: 'http://10.0.2.2:8080', // Android emulator
  connectTimeout: const Duration(seconds: 20),
  receiveTimeout: const Duration(seconds: 20),
));

dio.interceptors.add(
  AuthInterceptor(
    tokenStorage: tokenStorage,
    onUnauthorized: () {
      // redirection login globale
    },
  ),
);
```

---

## 5. Navigation protegee (routes privees)

## Besoin
Empencher l'acces a certaines pages sans token present.

## Strategie simple avec Navigator
1. au demarrage (Splash), lire token
2. si token absent -> Login/Welcome
3. si token present -> Home
4. sur 401 pendant la session -> clear token + retour Login

## Exemple garde au boot
```dart
Future<void> handleStartup(BuildContext context, TokenStorage storage) async {
  final token = await storage.readToken();
  if (token == null || token.isEmpty) {
    Navigator.pushReplacementNamed(context, '/login');
    return;
  }
  Navigator.pushReplacementNamed(context, '/home');
}
```

## Exemple protection manuelle d'un ecran
```dart
class ProtectedScreen extends StatelessWidget {
  final TokenStorage storage;
  const ProtectedScreen({super.key, required this.storage});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String?>(
      future: storage.readToken(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        final token = snapshot.data;
        if (token == null || token.isEmpty) {
          Future.microtask(() => Navigator.pushReplacementNamed(context, '/login'));
          return const SizedBox.shrink();
        }
        return const Scaffold(body: Center(child: Text('Zone privee')));
      },
    );
  }
}
```

---

## 6. Expiration/session et logout automatique

## Etat actuel backend
- Pas de refresh token expose.

## Comportement frontend recommande
- Toute reponse 401 =>
  1. clear token
  2. reset state utilisateur
  3. redirection login
  4. feedback: Session expiree, merci de vous reconnecter

## Option future (si refresh ajoute plus tard)
- intercepter 401
- appeler /api/auth/refresh
- rejouer la requete initiale
- sinon logout

---

## 7. Scenarios de test (validation ticket)

## A. Persistance session
- login succes
- fermer/reouvrir app
- verifier qu'on reste sur route authentifiee
- verifier qu'un endpoint prive passe sans ressaisir mot de passe

## B. Navigation securisee
- sans token, tenter acces direct route privee
- attendu: redirection login

## C. Header Authorization automatique
- apres login, appeler endpoint prive (ex: /api/users/me)
- attendu: 200
- verifier dans logs proxy/debug que header Authorization est envoye

## D. Token invalide/expire
- forcer token invalide en stockage
- appeler endpoint prive
- attendu: 401 puis logout auto + retour login

## E. Regression UX
- erreurs login 400/401 bien affichees
- loader visible pendant requetes
- bouton login desactive pendant submit

---

## 8. Checklist de fin de ticket

- [ ] token stocke en securise (flutter_secure_storage)
- [ ] token injecte automatiquement dans toutes les requetes API
- [ ] routes privees inaccessibles sans token
- [ ] 401 declenche logout automatique + redirection login
- [ ] session persiste apres redemarrage app
- [ ] tests manuels A/B/C/D/E valides

---

## 9. Erreurs backend utiles a mapper dans le front

## Login
- 400: Email et mot de passe obligatoires
- 401: Email ou mot de passe incorrect

## Auth globale
- 401: Non authentifie: token JWT manquant ou invalide
- 403: Acces refuse: role insuffisant

Suggestion UX:
- 401 login: Identifiants invalides
- 401 route privee: Session expiree, reconnectez-vous
- 403: Vous n'avez pas les droits necessaires

---

## 10. Implementation minimale recommandee (ordre)

1. TokenStorage
2. AuthService login/logout
3. Dio interceptor (Authorization + 401 auto-logout)
4. Splash route guard
5. Route guards pour pages privees
6. Tests manuels de validation

Avec ca, tu couvres exactement la validation demandee:
- persistance session
- navigation securisee
- zero acces aux routes privees sans token
