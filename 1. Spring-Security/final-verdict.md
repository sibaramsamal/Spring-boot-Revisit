Spring Security has two distinct authentication flows:

Phase	Purpose	Who triggers it
Login (username/password)	Prove identity	AuthenticationManager
Request authorization (JWT)	Trust identity	Your JwtFilter

They do not overlap automatically.

### 1️⃣ During LOGIN (Username + Password)

This is the ONLY time Spring calls:
``` java
UserDetailsService.loadUserByUsername()
```

Flow:
```
login API
 → authenticationManager.authenticate(...)
   → ProviderManager
     → DaoAuthenticationProvider
       → loadUserByUsername()
       → PasswordEncoder.matches()

✔ Happens once
✔ DB is hit
✔ Password is verified
✔ JWT is generated
```
### After this, Spring Security is done with DB checks.

### 2️⃣ During JWT-Based Requests (Your Filter)
- When a request comes with JWT:
```
Request
 → FilterChainProxy
   → JwtFilter

Spring does NOT do this:

❌ Does NOT call AuthenticationManager
❌ Does NOT call DaoAuthenticationProvider
❌ Does NOT call UserDetailsService
❌ Does NOT check DB
❌ Does NOT verify password
```

### 3️⃣ So Who Checks Authentication During JWT?
We, Inside your `JwtFilter` - JwtFilter.java.
```java
    List<String> roles = jwtService.extractRoles(jwtToken);
    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        loginKey,
                        "",
                        roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList()
                );
```
This will just create the object with the data coming inside this jwt token. But if we need extra security, we can rely on another DB call. We have to call loadUserByUsername() of our custom implemetation.
```java
 // 1. Inject this
 @Autowired
 UserDetailsService userDetailsService;

 // 2. Call the DB hit logic
 UserDetails userDetails = userDetailsService.loadUserByUsername(loginKey);
```