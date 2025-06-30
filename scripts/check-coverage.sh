#!/bin/bash

# Test Coverage Checker for Personal Hub Backend

echo "========================================="
echo "Personal Hub Backend Test Coverage Check"
echo "========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if tests have been run
if [ ! -f "target/jacoco.exec" ]; then
    echo -e "${RED}✗ No coverage data found!${NC}"
    echo "  Please run: mvn clean test jacoco:report"
    exit 1
fi

echo "Analyzing test coverage..."
echo ""

# Run JaCoCo report if not exists
if [ ! -f "target/site/jacoco/jacoco.csv" ]; then
    mvn jacoco:report -q
fi

# Parse coverage from CSV
if [ -f "target/site/jacoco/jacoco.csv" ]; then
    echo "Coverage by Package:"
    echo "-------------------"
    
    # Skip header and aggregate by package
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        package = $2
        gsub(/\.[^.]*$/, "", package)  # Remove class name
        
        instructions[package] += $4 + $5
        covered[package] += $5
    }
    END {
        for (p in instructions) {
            if (instructions[p] > 0) {
                coverage = (covered[p] / instructions[p]) * 100
                printf "%-60s %6.2f%%\n", p, coverage
            }
        }
    }' | sort
    
    echo ""
    echo "Overall Coverage:"
    echo "----------------"
    
    # Calculate total coverage
    tail -n +2 target/site/jacoco/jacoco.csv | awk -F',' '
    {
        total_instructions += $4 + $5
        total_covered += $5
    }
    END {
        if (total_instructions > 0) {
            coverage = (total_covered / total_instructions) * 100
            printf "Total Instruction Coverage: %.2f%%\n", coverage
            
            if (coverage >= 80) {
                printf "\033[0;32m✓ Coverage meets 80%% target!\033[0m\n"
            } else {
                printf "\033[0;31m✗ Coverage below 80%% target\033[0m\n"
            }
        }
    }'
else
    echo -e "${RED}✗ Coverage report not found${NC}"
    exit 1
fi

echo ""
echo "========================================="
echo "For detailed report, open:"
echo "target/site/jacoco/index.html"