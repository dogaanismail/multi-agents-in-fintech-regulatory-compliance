# Bank Solution - Compliance Backoffice

A React TypeScript application for compliance officers to manage customers, accounts, and payment reviews.

## Features

- 💳 Payment Management with filters (status, fraud status, customer, account)
- ✅ Manual Review Actions (Approve/Reject payments)
- 👥 Customer Management and Details
- 🏦 Account Management with Balance Tracking
- 🎨 Modern UI with Tailwind CSS

## Tech Stack

- React 18
- TypeScript
- Vite
- React Router
- Axios
- Tailwind CSS
- Nginx (for production)

## Getting Started

### Development Mode

#### Install Dependencies

```bash
npm install
```

#### Run Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Production Build

```bash
npm run build
```

### Docker Deployment

#### Build and Run with Docker Compose

From the `bank-solution-backend` directory:

```bash
# Build the backoffice UI image
docker-compose build backoffice-ui

# Start the backoffice UI
docker-compose up -d backoffice-ui
```

The application will be available at `http://localhost:3000`

#### Or Build Standalone Docker Image

```bash
# Build the image
docker build -t backoffice-ui .

# Run the container
docker run -p 6060:80 backoffice-ui

# Or with custom backend URL
docker run -p 6060:80 -e BACKEND_URL=http://my-gateway:3030 backoffice-ui
```

## API Configuration

The app connects to the backoffice gateway at `http://localhost:3030/api/v1/`

In Docker, nginx proxies `/api/` requests to the `backoffice-gateway` service (configurable via `BACKEND_URL` environment variable).

## Project Structure

```
src/
├── api/           # API client and services
├── types/         # TypeScript interfaces
├── pages/         # Page components
├── components/    # Reusable UI components
├── hooks/         # Custom React hooks
└── utils/         # Utility functions
```

## Available Pages

- **Home** (`/`) - Dashboard with navigation
- **Payments** (`/payments`) - Payment list with filtering
- **Payment Detail** (`/payments/:id`) - Detailed payment view with approve/reject
- **Customers** (`/customers`) - Customer list
- **Customer Detail** (`/customers/:id`) - Customer profile with accounts and payments
- **Account Detail** (`/accounts/:id`) - Account details with multi-currency balances

## Environment Variables

No environment variables needed - the app uses relative API paths that are proxied by nginx in production.

