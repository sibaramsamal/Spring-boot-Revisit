## Global Exception Handling
### 1. Create an exception type class.
```java
public class ProductNotFoundException extends RuntimeException
```
### 2. Initialize the message and other required values.
```java
public class ProductNotFoundException extends RuntimeException {
    private HttpStatus errorCode;

    public ProductNotFoundException(String message, HttpStatus errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
```
### 3. Create a class extends `ResponseEntityExceptionHandler` (optional) and annotated with `@RestControllerAdvice`
### 4. Create a moethod inside this class and annotate with `@ExceptionHandler` by passing our exception class
### 5. Return the response

```java
@RestControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleProductNotFoundException(ProductNotFoundException exception) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .errorMessage(exception.getMessage())
                .errorCode(exception.getErrorCode())
                .build();
        return new ResponseEntity<>(errorResponse, exception.getErrorCode());
    }
}
```