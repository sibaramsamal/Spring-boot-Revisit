# Spring Security

By default, it we add the `spring-boot-starter-security` dependency, we will get the authentication feature. 

# Spring Security – API Authentication Flow

```
👤 User
    ↓
+-------------------------------+    +---------------------------+    +------------------------------------------+
| Spring Security Filter Chain  | →  | DispatcherServlet         | →  | Controllers                              |
|-------------------------------|    | (Front Controller)        |    |------------------------------------------|
| • SecurityContextPersistence  |    +---------------------------+    | WelcomeController   -> GET /             |
| • CsrfFilter                  |                                     | ProductController   -> GET /product      |
| • Authentication Filter (JWT) |                                     +------------------------------------------+
| • FilterSecurityInterceptor   |
| • Security Filter Chain       |
+-------------------------------+
```
In filter chain(Authentication Filter), there are multiple filters are there so it is called filter chain. Out of these filters, `security context` filter determines whether the user is `Authernticated` and `Authorized` to access the API or not. If not then from there itself they restrict that user with 401.

This filteration happend by `Authentication Filter`.
`Authentication Filter` has `Security Context`, which can understand what to do with the user, whether to authenticate or skip authentication etc.

Based on that context after decided, `Security Context` talks to `Authentication Manager`

Then `Authentication Manager` talks to `Authentication Provider`.

`Authentication Provider` the talks to something to get the actual data. These are `PasswordEncoder` and `UserDetailsService`.

`UserDetailsService` has somany inbuilt method like `loadUserByUsername()`

# Custom Configuration
1. Create a Configuration Class and anotate with `@EnableWebSecurity`
2. As all the requests are being filtered by the Filter chain's `SecurityFilerChain`, we have to modify that in our code, so that it wont execute that default authentication screen.
    For that: 
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // place for customizations
        return httpSecurity.build();
    }
}
```

By doing this only, we have successfully took control from the Spring security and get the ability to bypass or modify the filtering things. We can do what ever we want while filtering the APIs.
### Customizations:
To add condithion that any request to be authenticated, we have to add before returning.
```java
        httpSecurity.authorizeHttpRequests(
                request -> request.anyRequest().authenticated()
        );
```
By doing this we have restricted/secured all our endpoints. Now we can't access these endpoints directly. We will get `403` error. But for getting that default login screen, we have to use 
```java
         .httpBasic(Customizer.withDefaults()) // for basic form
         .formLogin(Customizer.withDefaults()) // for spring provided login 
```
To disable `csrf`:
```java
         .csrf(csrf -> csrf.disable())
```

After taking control of `Authentication Filter`, Now we will customize `UserDetailsService`
For that, we can see, it has one method which has only one method `loadUserByUsername()` whose return type is: `UserDetails`

As it is an interface, we have to make bean an interface, we have to make object of one of it's implemented classes(eg. `InMemoryUserDetailsManager` I have used here).

## Understanding `@Bean` with `UserDetailsService` and `PasswordEncoder`

```java
@Bean
UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {

    // Create first user (username, encoded password, roles)
    UserDetails userOne = User.withUsername("a@gmail.com")
            .password(passwordEncoder.encode("12345"))
            .roles("ADMIN", "USER")
            .build();

    // Create second user
    UserDetails userTwo = User.withUsername("b@gmail.com")
            .password(passwordEncoder.encode("12345"))
            .roles("ADMIN", "USER")
            .build();

    // Return an implementation of UserDetailsService
    // that knows how to find these users
    return new InMemoryUserDetailsManager(userOne, userTwo);
}

@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Clean & Confusion-Free Explanation

- `UserDetails` represents **WHO the user is**  
  (username, password, roles).

- `UserDetailsService` represents **HOW Spring Security finds a user**  
  (by username during login).

- Spring Security **does NOT work with `UserDetails` directly**.  
  It always asks for a **`UserDetailsService`**.

- `InMemoryUserDetailsManager` is a **ready-made implementation** of  
  `UserDetailsService` provided by Spring Security.

- By creating `UserDetails` objects and passing them to  
  `InMemoryUserDetailsManager`, we are:
  > Giving Spring Security **our own user-lookup logic**, backed by in-memory users.

- The return type is `UserDetailsService` (interface), but the actual object is  
  `InMemoryUserDetailsManager` (implementation).  
  This is **interface-based design**, which Spring follows everywhere.

### Why `@Bean` is Used Here (Important Concept)

- Interfaces like `UserDetailsService`, `PasswordEncoder` and `UserDetails` have **multiple implementations**. We have choosed `InMemoryUserDetailsManager` for `UserDetailsService`, `BCryptPasswordEncoder` for `PasswordEncoder` and `User` for `UserDetails` respectivelly in our program.

- later using these objects, we can easily use the methods of these interfaces, like: `loadUserByUsername()` of `UserDetailsService` interface, `encode()`, `matches()` and `upgradeEncoding()` of `PasswordEncoder` and so on.

- Because of this, Spring cannot guess which implementation you want.
- Using `@Bean` means:

  > “Spring, use THIS specific implementation whenever this interface is needed.”

### Default vs Custom Behavior (Easy to Remember)

- ❌ If you **do not define** these beans → Spring Security uses **default implementations**
- ✅ If you **define your own beans** → Spring Security uses **YOUR logic**

### One-Line Memory Rule

> `@Bean` lets you replace Spring Security’s default behavior with your own implementation.

As of now I have hard coded the user details. Now we have to get them from DB.
Above things which we have done by invoking the controller from spring by modifying UserDetailsService are good, but in real time, we have to fetch from DB.
- so in UserController, Service and Repository I have created and stored the records in DB.
while making the endpoints as unauthenticated.
```java
    requestMatchers("/web/api/v1/user/**").permitAll()
```

Now as we are dealing with DB now, we no need this piece of code anymore.
```java
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails userOne = User.withUsername("a@gmail.com")
                .password(passwordEncoder.encode("12345"))
                .roles("ADMIN", "USER")
                .build();
        UserDetails userTwo = User.withUsername("b@gmail.com")
                .password(passwordEncoder.encode("12345"))
                .roles("ADMIN", "USER")
                .build();
        return new InMemoryUserDetailsManager(userOne, userTwo);
    }
```

We have to create another class of type `UserDetailsService` and implement the methods which was done by spring automatically through the above code.

New approach will be:
- new class implementing `UserDetailsService`
- As we removed this above code, and implemented another class, the implementation method 
```java
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // We need another class implementing `UserDetails` interface as well
    }
```
- While we were suing default ones, they were being handled by Spring, as now we have our own implementation, time to take control over `AuthenticationProvider` section which will use these newly created classes. Earlier it was `InMemoryUserDetailsManager`, now we have `DaoAuthenticationProvider`

for that, inside `WebSecurityConfiguration` we have to add that configurations.
```java
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
        return authenticationProvider;
    }
```

By doing this it will work like previously how it was working with `InMemoryUserDetailsManager`

### 🧠 One Sentence to Remember Forever

permitAll() does not bypass authentication — it only removes the requirement for it. So keep in mind never pass authentication details while calling these APIs.

### JWT Implementation
Now time to take control over `AuthenticationManager`
For that, we have to modify our code in to `AuthenticationManager` in `WebSecurityConfiguration`
We need it's `authenticate()` while logging in inside our login API. 
```java
    @Override
    public boolean login(LoginRequestModel loginRequestModel) {
        log.info("Logging in User..");
        authenticationManager.authenticate()
        userInfoRepository.findByEmail(loginRequestModel.getEmail()).orElseThrow(
                () -> new RuntimeException("User not found"));
        return true;
    }
```
So we have to create a bean for it,
```java
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
```
`AuthenticationConfiguration` is a class, so no need to create bean for it manually. Spring will provide it.

Now, as `authenticate()` take input of type `Authentication`, we have to pass one of it's implementation class object.

So the updated login method will be:
```java
    @Override
    public boolean login(LoginRequestModel loginRequestModel) {
        log.info("Logging in User..");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestModel.getEmail(), loginRequestModel.getPassword())
        );
        userInfoRepository.findByEmail(loginRequestModel.getEmail()).orElseThrow(
                () -> new RuntimeException("User not found"));
        return true;
    }
```

If we further improvise, we can use that object and check whether the credentials are correct, based on that it will return the response. Now the updated code will be:
```java
    @Override
    public boolean login(LoginRequestModel loginRequestModel) {
        log.info("Logging in User..");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestModel.getEmail(), loginRequestModel.getPassword())
        );
        return authentication.isAuthenticated();
    }
```

Now to pass actual JWT token if authenticated, so that we can pass some extra data in it. For that we need these dependencies.
- jjwt-api 
- jjwt-impl
- jjwt-jackson

After that create a service for generate token

```java
@Service
public class JwtTokenGeneratorService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationInMs;
    
    public String generateToken(String email, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationInMs))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
```

If Login Success, we will return the generated token.

### For enable JWT based authentication, we have to use filter
Here we have to create a class extending from `OncePerRequestFilter` and override required methods.

Here, I have override the `doFilterInternal()` and add that in configuration.
```java
    .authenticationProvider(authenticationProvider()) // REGISTER CUSTOM AUTH PROVIDER
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

THe final block of code will be:
```java
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers("web/api/v1/login").permitAll()
                                .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(
                                (req, res, ex) ->
                                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .authenticationProvider(authenticationProvider()) // CUSTOM AUTH PROVIDER
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
```
## Who Does What

### 1️⃣ Absolute Entry Point of Every Request
- Servlet Container (Tomcat / Jetty) ➡ calls `org.springframework.web.filter.DelegatingFilterProxy` which is `registered automatically` by Spring Boot.
### 2️⃣ DelegatingFilterProxy → Security Filter Chain
- `DelegatingFilterProxy` delegates to: `springSecurityFilterChain` which is created by: `SecurityFilterChain securityFilterChain(HttpSecurity http)` which we kept in our `WebSecurityConfiguration.java` file of our project.
- 📌 **Breakpoint location** `org.springframework.security.web.FilterChainProxy#doFilter` which is the real Gatekeeper.
### 3️⃣ FilterChainProxy – The Real Gatekeeper
- doFilter(ServletRequest, ServletResponse, FilterChain)
- Responsibilities:
-- Finds matching `SecurityFilterChain`
-- Executes all filters in order
### 4️⃣ Order of Important Filters (JWT Perspective)
```
SecurityContextPersistenceFilter
↓
CorsFilter
↓
CsrfFilter
↓
LogoutFilter
↓
UsernamePasswordAuthenticationFilter (FORM / BASIC)
↓
⬅ My "JwtFilter.java" (extending `OncePerRequestFilter`)
↓
FilterSecurityInterceptor
```
```java
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // We have added this in securityFilterChain() of WebSecurityConfiguration class.

    📌 Why addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)?
    Because:
        - JWT should authenticate BEFORE Spring tries form/basic auth. This filter is skipped for JWT requests because authentication is already set by JwtFilter.
```
### 5️⃣ My JWT Filter (exteding from OncePerRequestFilter)
- org.springframework.web.filter.OncePerRequestFilter
- Spring calls it's `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)`
So my code changes responsible for
Your responsibilities here:
```
1. Read Authorization header
2. Extract token
3. Validate token
4. Build Authentication object
5. Put it into SecurityContext
```
### 6️⃣ SecurityContextHolder – Where Authentication Lives
- org.springframework.security.core.context.SecurityContextHolder
- Important call: SecurityContextHolder.getContext().setAuthentication(authentication)
- Once this is set:
-- Spring treats user as authenticated
-- Authorization checks can proceed
### 7️⃣ Authorization Phase (AFTER Authentication)
- org.springframework.security.web.access.intercept.FilterSecurityInterceptor
- Method: doFilter()
- It internally calls: `AccessDecisionManager.decide()`
- This is where:
-- Roles are checked
-- @PreAuthorize works
-- requestMatchers().hasRole() works

📌 **If user is authenticated but gets 403 → problem is here**

### 8️⃣ Login API – Where Authentication STARTS Manually
- Inside our /login endpoint, we are validating users by calling this:
```java
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(inputKey, password));
```
From this point we are **manually entering Spring Security’s authentication pipeline**.

### 9️⃣ AuthenticationManager Interface
- org.springframework.security.authentication.`AuthenticationManager`
- Implementation used by Spring: `ProviderManager`
- Method: `authenticate(Authentication authentication)`
- Responsibilities:
-- Iterates through AuthenticationProviders
-- Finds one that supports the Authentication type
### 🔟 AuthenticationProvider Interface (Actual Credential Check)
- org.springframework.security.authentication.`AuthenticationProvider`
- My implementation: `DaoAuthenticationProvider.java`, which I configured using `@Bean`.
The class itself is provided by Spring Security.
- Important methods: 
```java 
authenticate(Authentication authentication)
supports(Class<?> authentication)
```
- Internally calls: `UserDetailsService.loadUserByUsername()`and `PasswordEncoder.matches()`
- 📌 **Breakpoint** DaoAuthenticationProvider#authenticate
### 1️⃣1️⃣ UserDetailsService Interface – User Loader
- org.springframework.security.core.userdetails.`UserDetailsService`
- Methods: `loadUserByUsername(String username)`
- Spring calls this:
-- During login
-- NEVER during JWT validation
- My implementation: `CustomUserDetailsService.java`
- 📌 **Breakpoint** CustomUserDetailsService#loadUserByUsername
### 1️⃣2️⃣ PasswordEncoder Interface – Password Validation
- org.springframework.security.crypto.password.`PasswordEncoder`
- My implementation: We have created it's object by one of it's implemeting class `BCryptPasswordEncoder` manually using `@Bean`
- commonly used methods:
```java
matches(raw, encoded)
encode(raw)
```
- 📌 **Breakpoint** BCryptPasswordEncoder#matches
### 1️⃣3️⃣ JWT Generation (Post Authentication)
- we are generating this only after `authentication.isAuthenticated() == true`
- used class: io.jsonwebtoken.`Jwts`
Object creation will be something like this:
``` 
Jwts.builder()
→ setClaims()
→ setSubject()
→ setIssuedAt()
→ setExpiration()
→ signWith()
→ compact()
```
📌 **JWT generation is NOT Spring Security logic**, We fully control this.

### 1️⃣4️⃣ JWT Validation Flow (Every Request)
- Spring does NOT know JWT. So in your filter:
```
1. Extract token
2. Parse claims
3. Validate signature + expiry
4. Create: UsernamePasswordAuthenticationToken (used only as a carrier, not for re-authentication)
5. Store in SecurityContext
```

### 1️⃣5️⃣ Stateless Nature (Why Session Is Disabled)
- In the security filter chain method of our security configuration, we have disabled it using `SessionCreationPolicy.STATELESS`
- Effect:
```
- No HttpSession
- No server-side login state
- Every request must carry token
```
### 1️⃣6️⃣ Exception Handling (401 vs 403)

| Situation | Where | Result |
|--------|------|--------|
| Invalid token | JWT Filter | 401 |
| No token but required | FilterSecurityInterceptor | 401 |
| Authenticated but no role | AccessDecisionManager | 403 |

## 1️⃣7️⃣ What You Should Modify vs Never Touch

### Safe to Modify
- JwtFilter
- UserDetailsService
- AuthenticationProvider
- SecurityFilterChain rules
- Token claims

### Never Touch
- FilterChainProxy
- ProviderManager
- SecurityContextHolder internals

---

### Final Summary (Debug Mental Map)

Request
↓
DelegatingFilterProxy
↓
FilterChainProxy
↓
JwtFilter
↓
SecurityContextHolder
↓
FilterSecurityInterceptor
↓
DispatcherServlet
↓
Controller

---

## Final Truth (Remember This)

> I am not calling Spring Security.  
> Spring Security is calling Me.

We only decide:
- Where to plug in
- What logic to run
- What data to trust

Everything else is orchestration.

**Codebase:** [Link](https://github.com/sibaramsamal/Media-Gallery)