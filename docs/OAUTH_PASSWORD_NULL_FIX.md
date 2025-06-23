# OAuth Password NULL Fix Documentation

## Problem
When users login through OAuth providers (Google/GitHub), the system was trying to create users with a NULL password, but the database had a NOT NULL constraint on the password column, causing a 500 error.

## Error Details
```
SQL Error: 0, SQLState: 23502
ERROR: null value in column "password" of relation "users" violates not-null constraint
```

## Solution Applied

### 1. Database Migration (V18)
Created migration file: `V18__make_password_nullable_for_oauth_users.sql`
```sql
-- Make password nullable for OAuth users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
COMMENT ON COLUMN users.password IS 'BCrypt hashed password for regular users. NULL for OAuth/social login users.';
```

### 2. Entity Updates
Updated `UserEntity.java` to reflect nullable password:
```java
@Column(nullable = true)  // Changed from nullable = false
private String password;
```

### 3. Security Enhancement
Updated `CustomUserDetailsService.java` to prevent OAuth users from using password-based login:
```java
// Prevent OAuth users (with null passwords) from using password-based authentication
if (user.getPassword() == null) {
    throw new UsernameNotFoundException("This account uses social login. Please use Google or GitHub to sign in.");
}
```

## Benefits
1. **OAuth Support**: Users can now successfully create accounts through Google/GitHub
2. **Security**: OAuth users cannot attempt password-based login
3. **Clear Separation**: Distinct authentication flows for regular vs. OAuth users

## Testing
After applying these changes:
1. OAuth login through Google/GitHub should work without database errors
2. OAuth users trying to login with email/password should receive an appropriate error message
3. Regular users with passwords can still login normally

## Future Considerations
1. Add UI indicators showing which login method an account uses
2. Consider allowing OAuth users to optionally set a password
3. Implement account linking (connecting multiple OAuth providers to one account)