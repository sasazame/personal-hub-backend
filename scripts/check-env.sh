#!/bin/bash

# Environment Configuration Checker for Personal Hub Backend
# This script validates your .env configuration

echo "========================================="
echo "Personal Hub Backend Configuration Check"
echo "========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}✗ .env file not found!${NC}"
    echo "  Please copy .env.example to .env and configure it:"
    echo "  cp .env.example .env"
    exit 1
fi

echo -e "${GREEN}✓ .env file found${NC}"
echo ""

# Source the .env file
export $(cat .env | grep -v '^#' | xargs)

# Function to check required variable
check_required() {
    local var_name=$1
    local var_value=${!var_name}
    
    if [ -z "$var_value" ] || [[ "$var_value" == *"your_"* ]] || [[ "$var_value" == *"_here"* ]]; then
        echo -e "${RED}✗ $var_name is not configured${NC}"
        return 1
    else
        echo -e "${GREEN}✓ $var_name is configured${NC}"
        return 0
    fi
}

# Function to check optional variable
check_optional() {
    local var_name=$1
    local var_value=${!var_name}
    
    if [ -z "$var_value" ] || [[ "$var_value" == *"your_"* ]] || [[ "$var_value" == *"_here"* ]]; then
        echo -e "${YELLOW}○ $var_name is not configured (optional)${NC}"
    else
        echo -e "${GREEN}✓ $var_name is configured${NC}"
    fi
}

echo "Checking required configuration..."
echo "---------------------------------"

# Check required variables
REQUIRED_VARS=(
    "DB_HOST"
    "DB_PORT"
    "DB_NAME"
    "DB_USERNAME"
    "DB_PASSWORD"
    "JWT_SECRET_KEY"
    "APP_BASE_URL"
    "APP_FRONTEND_URL"
)

errors=0
for var in "${REQUIRED_VARS[@]}"; do
    if ! check_required "$var"; then
        ((errors++))
    fi
done

# Special check for JWT_SECRET_KEY length
if [ ! -z "$JWT_SECRET_KEY" ] && [ ${#JWT_SECRET_KEY} -lt 32 ]; then
    echo -e "${RED}✗ JWT_SECRET_KEY must be at least 32 characters long${NC}"
    echo "  Current length: ${#JWT_SECRET_KEY}"
    echo "  Generate a secure key with: openssl rand -base64 32"
    ((errors++))
fi

echo ""
echo "Checking optional configuration..."
echo "---------------------------------"

# Check optional OAuth variables
echo "OAuth Providers:"
check_optional "GOOGLE_OIDC_CLIENT_ID"
check_optional "GOOGLE_OIDC_CLIENT_SECRET"
check_optional "GITHUB_CLIENT_ID"
check_optional "GITHUB_CLIENT_SECRET"

echo ""
echo "Email Configuration:"
if [ "$APP_EMAIL_PROVIDER" == "mock" ]; then
    echo -e "${GREEN}✓ Email provider set to 'mock' (development mode)${NC}"
elif [ "$APP_EMAIL_PROVIDER" == "brevo" ]; then
    check_required "BREVO_API_KEY"
elif [ "$APP_EMAIL_PROVIDER" == "sendgrid" ]; then
    check_required "SENDGRID_API_KEY"
elif [ "$APP_EMAIL_PROVIDER" == "smtp" ]; then
    check_required "MAIL_HOST"
    check_required "MAIL_PORT"
    check_required "MAIL_USERNAME"
    check_required "MAIL_PASSWORD"
else
    echo -e "${YELLOW}○ Email provider not configured (will use mock)${NC}"
fi

echo ""
echo "========================================="

if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✓ All required configurations are valid!${NC}"
    echo ""
    echo "You can start the application with:"
    echo "  mvn spring-boot:run"
    exit 0
else
    echo -e "${RED}✗ Found $errors configuration error(s)${NC}"
    echo ""
    echo "Please update your .env file with the missing values."
    echo "Refer to docs/ENVIRONMENT.md for detailed configuration guide."
    exit 1
fi