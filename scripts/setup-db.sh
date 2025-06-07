#!/bin/bash
# Personal Hub Backend - Database Setup Script

set -e

echo "🚀 Setting up PostgreSQL database for Personal Hub Backend..."

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
    echo "❌ PostgreSQL is not running. Please start it first:"
    echo "   sudo systemctl start postgresql"
    exit 1
fi

echo "✅ PostgreSQL is running"

# Create user if not exists
echo "📝 Creating user..."
sudo -u postgres psql -t -c "SELECT 1 FROM pg_user WHERE usename = 'personalhub'" | grep -q 1 || sudo -u postgres psql -c "CREATE USER personalhub WITH PASSWORD 'personalhub';"

# Create database if not exists
echo "📝 Creating database..."
sudo -u postgres psql -t -c "SELECT 1 FROM pg_database WHERE datname = 'personalhub'" | grep -q 1 || sudo -u postgres psql -c "CREATE DATABASE personalhub OWNER personalhub;"

# Grant privileges
echo "📝 Granting privileges..."
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE personalhub TO personalhub;"
sudo -u postgres psql -c "ALTER USER personalhub CREATEDB;"

echo "✅ Database setup completed!"

# Test connection
echo "🔍 Testing database connection..."
if PGPASSWORD=personalhub psql -h localhost -p 5432 -U personalhub -d personalhub -c "SELECT version();" >/dev/null 2>&1; then
    echo "✅ Database connection successful!"
    echo ""
    echo "🎉 Setup complete! You can now run: mvn spring-boot:run"
else
    echo "❌ Database connection failed"
    exit 1
fi