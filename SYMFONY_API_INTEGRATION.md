# JavaFX Symfony API Authentication - Implementation Guide

## Overview
Your JavaFX app now uses a proper API-based authentication system with Symfony backend. The key principle: **JavaFX sends plain passwords over HTTPS, Symfony handles all password hashing and verification.**

---

## 🔧 Configuration

### Step 1: Update API URL
Edit `src/main/java/tn/esprit/utils/ApiConfig.java`:

```java
public static final String BASE_URL = "https://your-symfony-api.com";  // ✅ Change this
```

Example:
- Development: `http://localhost:8000`
- Production: `https://api.smartpath.tn`

---

## 📡 API Endpoints Required

Your Symfony backend **must** implement these endpoints:

### 1. Login Endpoint
**POST** `/api/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "plaintext_password"
}
```

**Success Response (200/201):**
```json
{
  "id": 1,
  "email": "user@example.com",
  "nom": "Dupont",
  "prenom": "Jean",
  "type": "etudiant",
  "roles": "[\"ROLE_ETUDIANT\"]",
  "status": "actif",
  "photo": "https://cloudinary.url/photo.jpg",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Failure Response (401):**
```json
{
  "error": "Invalid credentials"
}
```

### 2. Register Endpoint
**POST** `/api/register`

**Request:**
```json
{
  "email": "newuser@example.com",
  "password": "plaintext_password",
  "nom": "Smith",
  "prenom": "Alice",
  "telephone": "+216 12 345 678",
  "type": "etudiant"
}
```

**Success Response (200/201):**
```json
{
  "id": 2,
  "email": "newuser@example.com",
  "nom": "Smith",
  "prenom": "Alice",
  "type": "etudiant",
  "roles": "[\"ROLE_ETUDIANT\"]",
  "status": "actif",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Failure Response (400/409):**
```json
{
  "error": "Email already exists"
}
```

---

## 🔐 Password Handling

### JavaFX Side (✅ Already Implemented)
1. **Never hash passwords** in JavaFX
2. Send plain passwords over **HTTPS only**
3. Clear password from memory after sending
4. Use `java.net.http.HttpClient` with TLS

### Symfony Side (⚠️ You Must Implement)
1. **Hash passwords** with bcrypt/argon2:
   ```php
   // Symfony
   $hashedPassword = $passwordHasher->hashPassword($user, $plainPassword);
   $user->setPassword($hashedPassword);
   ```
2. **Verify passwords** during login:
   ```php
   $passwordHasher->isPasswordValid($user, $plainPassword);
   ```
3. **Use HTTPS** in production (enforce in security headers)
4. **Store only hashed passwords** in database

---

## 📝 Code Usage

### In LoginController
```java
// Old (❌ Database)
User user = userService.login(email, password);

// New (✅ API)
SymfonyAuthService authService = new SymfonyAuthService();
User user = authService.login(email, password);
```

### In RegisterController
```java
// Old (❌ Database)
boolean success = userService.registerEtudiant(etudiant);

// New (✅ API)
SymfonyAuthService authService = new SymfonyAuthService();
User user = authService.register(email, password, nom, prenom, telephone, "etudiant");
```

### Store JWT Token for Future Requests
```java
// After successful login/register
String token = response.get("token"); // from API response
SymfonyAuthService.storeAuthToken(token);

// Use token in future API calls
HttpRequest.Builder builder = HttpRequest.newBuilder()
    .uri(URI.create(ApiConfig.USER_PROFILE_ENDPOINT));
SymfonyAuthService.addAuthorizationHeader(builder);
HttpRequest request = builder.build();
```

---

## ⚠️ Common Issues & Solutions

### Issue: "Wrong credentials" when user exists in Symfony
**Cause:** Password format mismatch between Symfony hashing and Java verification

**Solution:**
1. Ensure Symfony uses bcrypt: `$2a$`, `$2b$`, `$2y$` prefix
2. Ensure Java `SymfonyAuthService.login()` sends **plain password**
3. Check Symfony endpoint returns 200/201 on success

**Verification:**
```php
// In Symfony controller
$isValid = $passwordHasher->isPasswordValid($user, $plainPassword);
if (!$isValid) {
    return $this->json(['error' => 'Invalid credentials'], Response::HTTP_UNAUTHORIZED);
}
```

### Issue: HTTPS Certificate Error
**Cause:** Self-signed certificate or untrusted CA

**Solution (Development Only):**
```java
// Disable SSL verification (NEVER in production!)
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, new TrustManager[]{
    new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return null; }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }
}, new java.security.SecureRandom());

HttpClient client = HttpClient.newBuilder()
    .sslContext(sslContext)
    .build();
```

### Issue: 401 Response for All Logins
**Cause:** 
- API endpoint not implemented correctly
- Password hashing mismatch
- JSON format incorrect

**Debug Steps:**
1. Test API with Postman:
   ```
   POST http://localhost:8000/api/login
   Content-Type: application/json
   
   {
     "email": "test@example.com",
     "password": "password123"
   }
   ```
2. Check Symfony logs: `tail -f var/log/dev.log`
3. Verify password hash in database matches Symfony's hasher

---

## 🧪 Testing

### Test 1: Local Testing (Development)
```java
// In main()
SymfonyAuthService authService = new SymfonyAuthService();
User user = authService.login("test@example.com", "password123");
if (user != null) {
    System.out.println("✅ Login successful: " + user.getEmail());
} else {
    System.out.println("❌ Login failed");
}
```

### Test 2: Using Postman
1. **Login Test:**
   - URL: `https://your-api.com/api/login`
   - Method: POST
   - Body (JSON):
     ```json
     {
       "email": "student@example.com",
       "password": "plaintext_password"
     }
     ```
   - Expected: 200 + user object + token

2. **Register Test:**
   - URL: `https://your-api.com/api/register`
   - Method: POST
   - Body (JSON):
     ```json
     {
       "email": "newstudent@example.com",
       "password": "newpassword123",
       "nom": "Student",
       "prenom": "New",
       "type": "etudiant"
     }
     ```
   - Expected: 201 + user object + token

### Test 3: Error Cases
1. **Wrong password:**
   - Request with wrong password
   - Expected: 401 with "Invalid credentials"

2. **Email not found:**
   - Request with non-existent email
   - Expected: 401 with "User not found"

3. **Email already exists (register):**
   - Register with existing email
   - Expected: 409 with "Email already exists"

---

## 🛡️ Security Best Practices

### ✅ DO
- Use HTTPS in production (enforce via headers)
- Hash passwords in Symfony (bcrypt/argon2)
- Store JWT tokens securely in JavaFX (avoid plain text files)
- Use timeout on HTTP requests (30 seconds default)
- Validate email format before sending
- Return generic "Invalid credentials" messages (no user enumeration)

### ❌ DON'T
- Hash passwords in JavaFX
- Use HTTP in production
- Send passwords in query parameters
- Store passwords in plain text
- Use weak hashing algorithms
- Expose specific error messages ("user not found" vs "wrong password")

---

## 📦 Dependencies Used

- `java.net.http.HttpClient` (Java 11+)
- `org.json` (already in pom.xml)
- HTTPS support (built-in Java SSL/TLS)

---

## 🔄 Integration Checklist

- [ ] Update `ApiConfig.BASE_URL` with your Symfony API URL
- [ ] Implement `/api/login` endpoint in Symfony
- [ ] Implement `/api/register` endpoint in Symfony
- [ ] Ensure Symfony hashes passwords with bcrypt/argon2
- [ ] Test with Postman first
- [ ] Test JavaFX login with valid credentials
- [ ] Test JavaFX login with invalid credentials (should show 401 error)
- [ ] Test JavaFX register with new email
- [ ] Test JavaFX register with existing email (should show 400 error)
- [ ] Ensure HTTPS certificate is valid in production
- [ ] Enable CORS headers in Symfony if needed
- [ ] Store JWT token for future authenticated requests
- [ ] Monitor Symfony logs for errors

---

## 📞 Troubleshooting

### Enable Debug Logging
In JavaFX:
```java
// Add to SymfonyAuthService for detailed logging
System.out.println("[API] Request: " + request);
System.out.println("[API] Response Status: " + response.statusCode());
System.out.println("[API] Response Body: " + response.body());
```

In Symfony:
```php
// In your controller
$logger->info('Login attempt', [
    'email' => $email,
    'timestamp' => time()
]);
```

---

## ✨ Next Steps

1. **Implement Symfony API endpoints** (see above)
2. **Test with Postman** before testing with JavaFX
3. **Update `ApiConfig.BASE_URL`** with your actual API URL
4. **Run JavaFX app** and try login/register
5. **Monitor logs** on both sides
6. **Fix any HTTPS/certificate issues**
7. **Deploy to production** with HTTPS enforced
