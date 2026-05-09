# Implementation Checklist - Symfony API Authentication for JavaFX

## Phase 1: Preparation (5 minutes)

- [ ] Read `AUTHENTICATION_FIX_SUMMARY.md` for overview
- [ ] Read `SYMFONY_API_INTEGRATION.md` for detailed guide
- [ ] Review `SYMFONY_API_EXAMPLE_CONTROLLER.php`
- [ ] Verify your Symfony project has JWT bundle installed
- [ ] Create backup of your Symfony `User` entity

## Phase 2: JavaFX Configuration (5 minutes)

- [ ] Update `src/main/java/tn/esprit/utils/ApiConfig.java`:
  ```java
  // Change this line to your actual Symfony API URL
  public static final String BASE_URL = "https://your-api.example.com";
  ```
- [ ] Verify Java 11+ is being used (for `java.net.http.HttpClient`)
- [ ] Build project to verify no compilation errors:
  ```bash
  mvn clean compile
  ```

## Phase 3: Implement Symfony Endpoints (30 minutes)

### Option A: Using Provided Example (Recommended)

1. [ ] Copy code from `SYMFONY_API_EXAMPLE_CONTROLLER.php`
2. [ ] Create `src/Controller/ApiAuthController.php` in your Symfony project
3. [ ] Paste the example code
4. [ ] Adjust namespace and imports for your project structure
5. [ ] Configure `config/routes.yaml` to include the new controller

### Option B: Manual Implementation

If you already have authentication endpoints, verify they:

**POST `/api/login` endpoint:**
- [ ] Accepts JSON: `{email, password}`
- [ ] Returns 200 with user object + token on success
- [ ] Returns 401 with error message on failure
- [ ] Does NOT throw exception (catches and returns proper error code)

**POST `/api/register` endpoint:**
- [ ] Accepts JSON: `{email, password, nom, prenom, telephone, type}`
- [ ] Returns 201 with user object + token on success
- [ ] Returns 409 if email already exists
- [ ] Returns 400 if validation fails
- [ ] Hashes password before saving (bcrypt/argon2)

## Phase 4: Symfony Setup (20 minutes)

- [ ] Install JWT Bundle:
  ```bash
  composer require lexik/jwt-authentication-bundle
  ```

- [ ] Generate JWT keys:
  ```bash
  php bin/console lexik:jwt:generate-keypair
  ```

- [ ] Configure `config/packages/lexik_jwt_authentication.yaml`:
  ```yaml
  lexik_jwt_authentication:
    secret_key: '%kernel.project_dir%/config/jwt/private.pem'
    public_key: '%kernel.project_dir%/config/jwt/public.pem'
    pass_phrase: 'your_pass_phrase_here'
  ```

- [ ] Configure CORS in `config/packages/nelmio_cors.yaml`:
  ```yaml
  nelmio_cors:
    defaults:
      allow_credentials: true
      allow_origin: ['https://your-javafx-domain', 'http://localhost:*']
      allow_methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS']
      allow_headers: ['*']
  ```

- [ ] Update `config/packages/security.yaml` for JWT authentication

## Phase 5: Test Symfony Endpoints with Postman (15 minutes)

### Test 1: Login with Valid User
- [ ] URL: `POST https://your-api.example.com/api/login`
- [ ] Headers: `Content-Type: application/json`
- [ ] Body:
  ```json
  {
    "email": "existing-user@example.com",
    "password": "their_password"
  }
  ```
- [ ] Expected: Status 200, response with `token` field
- [ ] **Result**: ✅ PASS / ❌ FAIL

### Test 2: Login with Wrong Password
- [ ] URL: `POST https://your-api.example.com/api/login`
- [ ] Body: same as above but wrong password
- [ ] Expected: Status 401, error message
- [ ] **Result**: ✅ PASS / ❌ FAIL

### Test 3: Register New User
- [ ] URL: `POST https://your-api.example.com/api/register`
- [ ] Body:
  ```json
  {
    "email": "newuser@example.com",
    "password": "password123",
    "nom": "Smith",
    "prenom": "John",
    "telephone": "+216 12 345 678",
    "type": "etudiant"
  }
  ```
- [ ] Expected: Status 201, response with `token` field
- [ ] **Result**: ✅ PASS / ❌ FAIL

### Test 4: Register with Existing Email
- [ ] URL: `POST https://your-api.example.com/api/register`
- [ ] Body: same as test 3 but use existing email
- [ ] Expected: Status 409, error about duplicate email
- [ ] **Result**: ✅ PASS / ❌ FAIL

## Phase 6: Test JavaFX App (15 minutes)

### Test 1: JavaFX Login - Valid Credentials
- [ ] Start JavaFX app
- [ ] Click Login screen
- [ ] Enter email and password of a Symfony user
- [ ] Click "Login"
- [ ] Expected: Success, navigate to dashboard
- [ ] **Result**: ✅ PASS / ❌ FAIL
- [ ] **Debug**: Check console for error messages

### Test 2: JavaFX Login - Wrong Password
- [ ] Enter valid email, wrong password
- [ ] Click "Login"
- [ ] Expected: Error message "❌ Email ou mot de passe incorrect. (Erreur 401)"
- [ ] **Result**: ✅ PASS / ❌ FAIL

### Test 3: JavaFX Register - New Account
- [ ] Click Register screen
- [ ] Fill form with new user data
- [ ] Submit
- [ ] Expected: Success message, redirect to login
- [ ] **Result**: ✅ PASS / ❌ FAIL

### Test 4: JavaFX Register - Existing Email
- [ ] Try to register with an email that already exists
- [ ] Expected: Error message about email already used
- [ ] **Result**: ✅ PASS / ❌ FAIL

## Phase 7: Debugging (if needed)

### If Postman Tests Fail:

1. [ ] Check Symfony logs:
   ```bash
   tail -f var/log/dev.log
   ```

2. [ ] Enable SQL logging to see database queries

3. [ ] Verify JWT bundle configuration is correct

4. [ ] Check that routes are properly configured

5. [ ] Verify CORS headers are being sent

### If JavaFX Tests Fail:

1. [ ] Check JavaFX console output for error messages

2. [ ] Add debug statements to `SymfonyAuthService.java`:
   ```java
   System.out.println("[API] Request: " + request);
   System.out.println("[API] Response Status: " + response.statusCode());
   System.out.println("[API] Response Body: " + response.body());
   ```

3. [ ] Enable network inspection in IDE debugger

4. [ ] Test with `curl`:
   ```bash
   curl -X POST https://your-api.example.com/api/login \
     -H "Content-Type: application/json" \
     -d '{"email":"user@test.com","password":"password"}'
   ```

## Phase 8: Production Deployment

- [ ] Update `ApiConfig.BASE_URL` to production URL
- [ ] Verify HTTPS certificate is valid
- [ ] Enforce HTTPS in Symfony security config
- [ ] Disable debug mode in JavaFX (remove verbose logging)
- [ ] Test again on production environment
- [ ] Monitor logs for any issues
- [ ] Update documentation for team

## Success Criteria ✅

You'll know everything is working when:

1. ✅ Postman tests all pass (login, register, both success and error cases)
2. ✅ JavaFX login works with users created in Symfony web
3. ✅ JavaFX register creates new accounts accessible from Symfony web
4. ✅ Wrong password shows 401 error properly
5. ✅ No errors in Symfony or JavaFX logs
6. ✅ JWT tokens are returned from API
7. ✅ User is redirected to dashboard after successful login

---

## Troubleshooting Reference

| Issue | Cause | Solution |
|-------|-------|----------|
| "401 Unauthorized" in Postman | Endpoint not implemented or password check failing | Verify endpoint code, check password hashing |
| "CORS error" in JavaFX | CORS not configured in Symfony | Add nelmio_cors config, set Allow-Origin header |
| "Connection refused" | API not running or wrong URL | Verify API is running, check BASE_URL in ApiConfig |
| "Certificate error" | Self-signed certificate | Use proper SSL cert, or disable for dev |
| "Timeout" | API too slow or not responding | Increase timeout in ApiConfig, check Symfony logs |
| "Email already exists" in register | Email already in database | Expected behavior, try different email |
| "Can't find endpoint" | Routes not configured | Check routes.yaml, clear cache: `php bin/console cache:clear` |

---

## Files Reference

| File | Purpose | Status |
|------|---------|--------|
| `ApiConfig.java` | API configuration | ✅ Created |
| `SymfonyAuthService.java` | Authentication service | ✅ Created |
| `ApiHttpClient.java` | HTTP helper | ✅ Created |
| `LoginController.java` | Updated for API | ✅ Modified |
| `RegisterController.java` | Updated for API | ✅ Modified |
| `SYMFONY_API_INTEGRATION.md` | Detailed guide | ✅ Created |
| `SYMFONY_API_EXAMPLE_CONTROLLER.php` | Example code | ✅ Created |
| `AUTHENTICATION_FIX_SUMMARY.md` | Quick summary | ✅ Created |

---

**Estimated Total Time: 90-120 minutes**

Start with Phase 1 and proceed sequentially. Stop and debug if any phase fails before continuing.

Good luck! 🚀
