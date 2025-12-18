# Cloudflare R2 Setup Instructions

## 1. Create Cloudflare R2 Bucket

1. Go to [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Navigate to **R2** → **Create bucket**
3. Choose a bucket name (e.g., `archdrive-files`)
4. Select a location for your bucket

## 2. Generate API Token

1. Go to **Manage R2 API Tokens**
2. Click **Create API token**
3. Set permissions:
   - **Object Read & Write** for your bucket
4. Copy the **Access Key ID** and **Secret Access Key**

## 3. Get Account ID and Endpoint

1. Your **Account ID** can be found in the Cloudflare dashboard URL or in the R2 overview page
2. The **Endpoint** format is: `https://<account-id>.r2.cloudflarestorage.com`

## 4. Configure application.yml

Update `src/main/resources/application.yml` with your credentials:

```yaml
cloudflare:
  r2:
    accountId: "your-account-id"
    accessKey: "your-access-key-id"
    secretKey: "your-secret-access-key"
    bucket: "your-bucket-name"
    endpoint: "https://your-account-id.r2.cloudflarestorage.com"
    publicUrl: "" # Optional: Custom domain URL (e.g., https://files.example.com)
```

## 5. Set Up Public Access (REQUIRED for downloads)

R2 buckets are private by default. To enable downloads, you MUST enable public access:

1. Go to your bucket settings
2. Enable **Public Access**
3. Optionally set up a **Custom Domain** for public URLs
4. If using custom domain, set `publicUrl` in `application.yml`

**Without public access, download links will return 403 Forbidden errors.**

## 6. CORS Configuration (if needed)

If accessing R2 directly from browser, configure CORS in bucket settings:
- Allowed origins: `http://localhost:3000`, `http://localhost:5173`
- Allowed methods: `GET`, `POST`, `DELETE`
- Allowed headers: `*`

**Note**: The backend handles file operations, so CORS on R2 bucket may not be necessary.

