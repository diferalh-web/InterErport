# Guarantees Module - Startup Guide

This guide ensures you can reliably start the application without the "Failed to create guarantee" error.

## 🚀 Quick Start (Recommended)

### Option 1: Start Everything at Once
```bash
./start-all.sh
```
This will start both backend and frontend in the correct order and keep them running.

### Option 2: Start Individually
```bash
# Start backend first
./start-backend.sh

# Then start frontend (in a new terminal)
./start-frontend.sh
```

### Option 3: Stop Everything
```bash
./stop-all.sh
```

## 📋 Application URLs

Once started, access the application at:

- **🖥️ Frontend (Main Application)**: http://localhost:3000
- **🔧 Backend API**: http://localhost:8080/api/v1
- **📚 API Documentation**: http://localhost:8080/api/v1/swagger-ui.html
- **🗄️ Database Console**: http://localhost:8080/api/v1/h2-console
  - Username: `sa`
  - Password: (empty)

## 🔑 Authentication

- **Username**: `admin`
- **Password**: `admin123`

## ❗ Troubleshooting Common Issues

### Problem: "Failed to create guarantee" popup
**Cause**: Backend is not running or frontend cannot connect to it.
**Solution**: 
1. Run `./stop-all.sh` to stop everything
2. Run `./start-all.sh` to start both applications

### Problem: "Port already in use" error
**Solution**:
```bash
# Check what's using the port
lsof -i :8080  # for backend
lsof -i :3000  # for frontend

# Kill processes on specific port
lsof -ti :8080 | xargs kill -9  # backend
lsof -ti :3000 | xargs kill -9  # frontend
```

### Problem: Backend fails to start
**Check**: 
1. Java 17+ is installed: `java -version`
2. Maven is installed: `mvn -version`
3. Port 8080 is free: `lsof -i :8080`

### Problem: Frontend fails to start
**Check**:
1. Node.js is installed: `node -version`
2. Dependencies are installed: `cd frontend && npm install`
3. Port 3000 is free: `lsof -i :3000`

### Problem: "Connection refused" errors in browser
**Cause**: Trying to access the application before both services are ready.
**Solution**: Wait for both startup scripts to show "✅ started successfully"

## 🔄 Development Workflow

### Daily Usage:
1. Start: `./start-all.sh`
2. Develop and test
3. Stop: `Ctrl+C` (or `./stop-all.sh`)

### Code Changes:
- **Frontend changes**: Auto-reload (hot-reload enabled)
- **Backend changes**: Restart backend: `./stop-all.sh` then `./start-all.sh`

## 📁 Project Structure

```
guarantees-module/
├── backend/          # Spring Boot API
├── frontend/         # React UI
├── start-all.sh      # Start everything
├── start-backend.sh  # Start backend only
├── start-frontend.sh # Start frontend only
├── stop-all.sh       # Stop everything
└── README-STARTUP.md # This guide
```

## 💡 Pro Tips

1. **Always start backend first** if starting manually
2. **Use `start-all.sh`** for the most reliable startup
3. **Check the terminal output** for success messages before testing
4. **Keep both terminals open** when running manually to see logs
5. **Use `stop-all.sh`** before shutting down your computer

## ✅ Success Indicators

You know everything is working when you see:
- ✅ Backend: "Backend started successfully!" message
- ✅ Frontend: "Frontend started successfully!" message
- ✅ Browser: Opens to http://localhost:3000 automatically
- ✅ UI: Can create guarantees without "Failed to create" errors




