# Testing User.Read Request to Microsoft Graph API

This guide provides multiple methods to test the User.Read permission and fetch user profile data including `jobTitle` from Microsoft Graph API.

## Method 1: Microsoft Graph Explorer (Easiest - No Setup Required)

### Steps:
1. **Go to Microsoft Graph Explorer**: https://developer.microsoft.com/en-us/graph/graph-explorer
2. **Sign in** with your Microsoft account
3. **Select permissions**:
   - Click "Modify permissions" tab
   - Check `User.Read` permission
   - Click "Consent" if prompted
4. **Make the request**:
   - In the query box, enter: `https://graph.microsoft.com/v1.0/me?$select=id,displayName,mail,userPrincipalName,jobTitle`
   - Click "Run query"
5. **View response**: You'll see JSON with your user data including `jobTitle`

### Example Response:
```json
{
  "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#users(id,displayName,mail,userPrincipalName,jobTitle)",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "displayName": "Your Name",
  "mail": "your.email@domain.com",
  "userPrincipalName": "your.email@domain.com",
  "jobTitle": "22-1234-567"
}
```

---

## Method 2: Postman

### Prerequisites:
- Postman installed
- Azure App Registration with Client ID and Client Secret

### Steps:

#### Step 1: Get Access Token
1. Create a new **POST** request
2. URL: `https://login.microsoftonline.com/823cde44-4433-456d-b801-bdf0ab3d41fc/oauth2/v2.0/token`
**Note**: Using single-tenant endpoint (replace `/common` with your tenant ID)
3. Headers:
   ```
   Content-Type: application/x-www-form-urlencoded
   ```
4. Body (x-www-form-urlencoded):
   ```
   grant_type: client_credentials
   client_id: YOUR_CLIENT_ID
   client_secret: YOUR_CLIENT_SECRET
   scope: https://graph.microsoft.com/.default
   ```

#### Step 2: Use Access Token to Get User Info
1. Create a new **GET** request
2. URL: `https://graph.microsoft.com/v1.0/me?$select=id,displayName,mail,userPrincipalName,jobTitle`
3. Headers:
   ```
   Authorization: Bearer {access_token_from_step_1}
   ```

**Note**: For delegated permissions (User.Read), you need to use Authorization Code flow, not client credentials. See Method 3 for that.

---

## Method 3: Using curl (Command Line)

### Step 1: Get Authorization Code (Manual)
1. Open browser and navigate to:
   ```
   https://login.microsoftonline.com/823cde44-4433-456d-b801-bdf0ab3d41fc/oauth2/v2.0/authorize?
   client_id=f7de2082-19fd-4c38-b76a-07eefc673493
   **Note**: Using single-tenant endpoint with actual client ID
   &response_type=code
   &redirect_uri=http://localhost:8080/login/oauth2/code/microsoft
   &response_mode=query
   &scope=openid profile email User.Read
   &state=12345
   ```
2. Sign in and authorize
3. Copy the `code` parameter from the redirect URL

### Step 2: Exchange Code for Token
```bash
curl -X POST https://login.microsoftonline.com/823cde44-4433-456d-b801-bdf0ab3d41fc/oauth2/v2.0/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=YOUR_CLIENT_ID" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "code=AUTHORIZATION_CODE_FROM_STEP_1" \
  -d "redirect_uri=http://localhost:8080/login/oauth2/code/microsoft" \
  -d "grant_type=authorization_code"
```

### Step 3: Get User Info
```bash
curl -X GET "https://graph.microsoft.com/v1.0/me?\$select=id,displayName,mail,userPrincipalName,jobTitle" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Method 4: Test in Your Spring Boot Application

### Create a Test Controller

Create a test endpoint to verify the OAuth flow:

```java
package com.scholarsync.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final WebClient webClient;

    public TestController() {
        this.webClient = WebClient.builder()
            .baseUrl("https://graph.microsoft.com/v1.0")
            .build();
    }

    @GetMapping("/user-info")
    public Mono<Map<String, Object>> getUserInfo(@AuthenticationPrincipal OAuth2User oauth2User) {
        // Get access token from OAuth2User
        String accessToken = oauth2User.getAttribute("access_token");
        
        // If token is not directly available, you'll need to get it from OAuth2AuthorizedClient
        // For now, this is a simplified example
        
        return webClient.get()
            .uri("/me?$select=id,displayName,mail,userPrincipalName,jobTitle")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(Map.class);
    }

    @GetMapping("/oauth-user")
    public Map<String, Object> getOAuthUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        // This shows what's available from the OAuth2User directly
        return Map.of(
            "name", oauth2User.getName(),
            "attributes", oauth2User.getAttributes()
        );
    }
}
```

### Access the Endpoint:
1. Start your Spring Boot application
2. Navigate to: `http://localhost:8080/login/oauth2/authorization/microsoft`
3. After authentication, access: `http://localhost:8080/api/test/user-info`

---

## Method 5: Using Azure CLI

### Prerequisites:
- Azure CLI installed
- Logged in: `az login`

### Get Access Token:
```bash
az account get-access-token --resource https://graph.microsoft.com
```

### Use Token in curl:
```bash
TOKEN=$(az account get-access-token --resource https://graph.microsoft.com --query accessToken -o tsv)

curl -X GET "https://graph.microsoft.com/v1.0/me?\$select=id,displayName,mail,userPrincipalName,jobTitle" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Method 6: PowerShell (Windows)

```powershell
# Get access token
$tokenResponse = Invoke-RestMethod -Method Post -Uri "https://login.microsoftonline.com/common/oauth2/v2.0/token" `
  -Body @{
    client_id = "YOUR_CLIENT_ID"
    client_secret = "YOUR_CLIENT_SECRET"
    grant_type = "client_credentials"
    scope = "https://graph.microsoft.com/.default"
  } -ContentType "application/x-www-form-urlencoded"

$accessToken = $tokenResponse.access_token

# Get user info
$headers = @{
    Authorization = "Bearer $accessToken"
}

Invoke-RestMethod -Method Get `
  -Uri "https://graph.microsoft.com/v1.0/me?`$select=id,displayName,mail,userPrincipalName,jobTitle" `
  -Headers $headers
```

---

## Troubleshooting

### Issue: "Insufficient privileges to complete the operation"
**Solution**: 
- Ensure `User.Read` permission is granted in Azure App Registration
- Admin consent may be required for your organization

### Issue: "Invalid client"
**Solution**: 
- Verify Client ID and Client Secret are correct
- Check redirect URI matches Azure App Registration

### Issue: "jobTitle is null"
**Solution**: 
- Verify the user's Microsoft account has jobTitle set
- Check if you're requesting the correct fields in the $select parameter
- Some accounts may not have jobTitle populated
- **Fallback**: System extracts institutional ID from `given_name` field if `jobTitle` is null
- Pattern: `(\\d{2}-\\d{4}-\\d{3}|\\d{4}-\\d{5}|1-\\d{4})` extracted from `given_name`

### Issue: Token expired
**Solution**: 
- Access tokens expire after 1 hour
- Use refresh token to get a new access token
- Or re-authenticate

---

## Quick Test Checklist

- [ ] Azure App Registration created
- [ ] `User.Read` permission added and consented
- [ ] Client ID and Client Secret obtained
- [ ] Tested with Graph Explorer (Method 1)
- [ ] Verified jobTitle is returned in response
- [ ] Tested role pattern matching with sample jobTitle values

---

## Recommended Testing Flow

1. **Start with Graph Explorer** (Method 1) - Quickest way to verify permissions work
2. **Test with Postman** (Method 2) - Simulates your application's API calls
3. **Integrate in Spring Boot** (Method 4) - Test the actual implementation
4. **Verify role determination** - Test with sample jobTitle values:
   - `22-1234-567` → Should map to STUDENT ✅
   - `2010-12345` → Should map to STUDENT ✅
   - `1-0001` → Should map to TEACHER ✅
   - `1-1234` → Should map to TEACHER ✅
   - `null` or unmatched → Should map to UNKNOWN (for testing)
   
5. **Test fallback extraction** - When `jobTitle` is null:
   - `given_name: "22-0369-330 Giles Anthony"` → Should extract `22-0369-330` → STUDENT ✅
   - `given_name: "1-1234 Teacher Name"` → Should extract `1-1234` → TEACHER ✅


