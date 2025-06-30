# Release Management Guide

## Branch Strategy

### Branch Structure
- `main`: Development branch (latest development version)
- `staging`: Staging environment (pre-production validation)
- `production`: Production environment (stable version)
- `feature/*`: Feature development
- `hotfix/*`: Emergency fixes

### Workflows

#### 1. Regular Feature Development
```bash
# Create feature branch from main
git checkout main
git pull origin main
git checkout -b feature/new-feature

# After development, create PR to main
git push origin feature/new-feature
# Create and merge PR on GitHub

# Deploy to staging
git checkout staging
git merge main
git push origin staging
```

#### 2. Production Release
```bash
# After staging tests pass
git checkout production
git merge staging
git push origin production

# Create release tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

#### 3. Hotfix
```bash
# Fix directly from production
git checkout production
git checkout -b hotfix/critical-fix

# After fix
git push origin hotfix/critical-fix
# Create PR and merge to production

# Sync with main and staging
git checkout main
git merge hotfix/critical-fix
git push origin main

git checkout staging
git merge hotfix/critical-fix
git push origin staging
```

## Tag Strategy

### Semantic Versioning
- Use `vMAJOR.MINOR.PATCH` format
- MAJOR: Breaking changes
- MINOR: Backward compatible features
- PATCH: Bug fixes

### Creating Tags
```bash
# Regular release
git tag -a v1.2.0 -m "Feature: Add user authentication"

# Pre-release
git tag -a v1.2.0-beta.1 -m "Beta release for testing"

# Push tag
git push origin v1.2.0
```

## render.com Configuration

### Environment-specific Settings
1. **Staging Service**
   - Branch: `staging`
   - Auto-Deploy: Enabled
   - Environment: `staging`

2. **Production Service**
   - Branch: `production`
   - Auto-Deploy: Enabled (or Manual)
   - Environment: `production`

### Environment Variables Management
Set different values per environment:
- `DATABASE_URL`
- `API_KEY`
- `ENVIRONMENT`
- `LOG_LEVEL`

## GitHub Actions Integration Example

`.github/workflows/release.yml`:
```yaml
name: Release Process

on:
  push:
    branches:
      - production
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Create Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          draft: false
          prerelease: false
```

## Checklists

### Pre-release Checklist
- [ ] All tests passing
- [ ] Staging validation complete
- [ ] Database migrations verified
- [ ] Environment variables configured
- [ ] Documentation updated
- [ ] CHANGELOG updated

### Post-release Checklist
- [ ] Production health check
- [ ] Log monitoring
- [ ] Performance metrics check
- [ ] Rollback procedure verified